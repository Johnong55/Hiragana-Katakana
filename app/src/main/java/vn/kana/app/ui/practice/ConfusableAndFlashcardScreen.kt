package vn.kana.app.ui.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import vn.kana.app.domain.ConfusableCharItem
import vn.kana.app.domain.ConfusableGroup
import vn.kana.app.domain.ConfusablePairsCatalog
import vn.kana.app.domain.KanaVocabWord
import vn.kana.recognizer.KanaStrokeTemplate
import kotlin.math.roundToInt

private enum class PracticeModeTab(val label: String) {
    CONFUSABLE("👀 Chữ dễ nhầm"),
    FLASHCARD("🃏 Flashcard")
}

private enum class FlashcardDeck(val label: String) {
    KANJI("80 Kanji N5"),
    VOCAB("Từ vựng N5")
}

private data class SimpleFlashcard(
    val id: String,
    val frontText: String,
    val subFrontText: String,
    val readingText: String,
    val meaningVi: String,
    val speakText: String
)

private data class ConfusableQuizQuestion(
    val group: ConfusableGroup,
    val target: ConfusableCharItem,
    val choices: List<ConfusableCharItem>
)

@Composable
fun ConfusableAndFlashcardScreen(
    n5KanjiCharacters: List<KanaStrokeTemplate>,
    wordsByChar: Map<String, List<KanaVocabWord>>,
    autoAdvanceDelayMs: Long,
    onSpeak: (String, String?) -> Unit,
    onRecordStudyActivity: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(PracticeModeTab.CONFUSABLE) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "← Quay lại",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .clickable { onBack() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
            Text(
                text = "THẺ NHANH & CHỮ DỄ NHẦM",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PracticeModeTab.entries.forEach { tab ->
                val selected = activeTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable { activeTab = tab }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        when (activeTab) {
            PracticeModeTab.CONFUSABLE -> ConfusablePairsView(
                autoAdvanceDelayMs = autoAdvanceDelayMs,
                onSpeak = onSpeak,
                onRecordStudyActivity = onRecordStudyActivity
            )

            PracticeModeTab.FLASHCARD -> QuickFlashcardView(
                n5KanjiCharacters = n5KanjiCharacters,
                wordsByChar = wordsByChar,
                onSpeak = onSpeak,
                onRecordStudyActivity = onRecordStudyActivity
            )
        }
    }
}

@Composable
private fun ConfusablePairsView(
    autoAdvanceDelayMs: Long,
    onSpeak: (String, String?) -> Unit,
    onRecordStudyActivity: () -> Unit
) {
    var inQuizMode by remember { mutableStateOf(false) }
    var quizQuestions by remember { mutableStateOf<List<ConfusableQuizQuestion>>(emptyList()) }
    var quizIndex by remember { mutableIntStateOf(0) }
    var selectedChar by remember { mutableStateOf<String?>(null) }
    var correctCount by remember { mutableIntStateOf(0) }
    var quizDone by remember { mutableStateOf(false) }

    fun startQuiz(filterGroup: ConfusableGroup? = null) {
        val pool = filterGroup?.let { listOf(it) } ?: ConfusablePairsCatalog.groups
        val generated = mutableListOf<ConfusableQuizQuestion>()
        repeat(10) {
            val g = pool.random()
            val t = g.items.random()
            generated += ConfusableQuizQuestion(
                group = g,
                target = t,
                choices = g.items.shuffled()
            )
        }
        quizQuestions = generated
        quizIndex = 0
        selectedChar = null
        correctCount = 0
        quizDone = false
        inQuizMode = true
    }

    if (inQuizMode && quizQuestions.isNotEmpty()) {
        if (quizDone) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, MaterialTheme.colorScheme.primary)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "HOÀN THÀNH LUYỆN CHỮ DỄ NHẦM",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$correctCount / ${quizQuestions.size} câu đúng",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { inQuizMode = false },
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Về bảng so sánh")
                    }
                    Button(
                        onClick = { startQuiz(null) },
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Luyện lại")
                    }
                }
            }
            return
        }

        val current = quizQuestions[quizIndex]
        LaunchedEffect(quizIndex) {
            onSpeak(current.target.char, current.target.romaji)
        }

        LaunchedEffect(selectedChar) {
            if (selectedChar == current.target.char) {
                delay(autoAdvanceDelayMs.coerceAtLeast(1100L))
                if (quizIndex < quizQuestions.lastIndex) {
                    quizIndex++
                    selectedChar = null
                } else {
                    quizDone = true
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← Bảng so sánh",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .clickable { inQuizMode = false }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Text(
                    text = "Câu ${quizIndex + 1}/${quizQuestions.size} • Đúng: $correctCount",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "CHỌN ĐÚNG CHỮ ${current.group.scriptLabel.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "「${current.target.romaji}」",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                OutlinedButton(
                    onClick = { onSpeak(current.target.char, current.target.romaji) },
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text("🔊 Nghe lại")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                current.choices.forEach { choice ->
                    val isSelected = selectedChar == choice.char
                    val isCorrect = choice.char == current.target.char
                    val borderColor = when {
                        selectedChar == null -> MaterialTheme.colorScheme.outline
                        isCorrect -> MaterialTheme.colorScheme.primary
                        isSelected -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outline
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .border(if (isSelected || (selectedChar != null && isCorrect)) 2.dp else 1.dp, borderColor)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(enabled = selectedChar == null) {
                                selectedChar = choice.char
                                if (isCorrect) correctCount++
                                onRecordStudyActivity()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = choice.char,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            if (selectedChar != null && selectedChar != current.target.char) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.error)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "✗ Chưa đúng — Đáp án là ${current.target.char} (${current.target.romaji})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = current.group.tipVi,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            if (quizIndex < quizQuestions.lastIndex) {
                                quizIndex++
                                selectedChar = null
                            } else {
                                quizDone = true
                            }
                        },
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tiếp theo →")
                    }
                }
            }
        }
        return
    }

    // Comparison browser
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { startQuiz(null) },
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("⚡ Luyện phản xạ tất cả cặp dễ nhầm (10 câu)")
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ConfusablePairsCatalog.groups, key = { it.id }) { group ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${group.scriptLabel} • ${group.titleVi}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Luyện →",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { startQuiz(group) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        group.items.forEach { item ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, MaterialTheme.colorScheme.outline)
                                    .background(MaterialTheme.colorScheme.background)
                                    .clickable { onSpeak(item.char, item.romaji) }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = item.char,
                                    fontSize = 32.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "🔊 ${item.romaji}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = item.cueVi,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Text(
                        text = group.tipVi,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun QuickFlashcardView(
    n5KanjiCharacters: List<KanaStrokeTemplate>,
    wordsByChar: Map<String, List<KanaVocabWord>>,
    onSpeak: (String, String?) -> Unit,
    onRecordStudyActivity: () -> Unit
) {
    var selectedDeck by remember { mutableStateOf(FlashcardDeck.KANJI) }
    val deckQueue = remember { mutableStateListOf<SimpleFlashcard>() }
    var masteredCount by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    fun rebuildDeck(deck: FlashcardDeck) {
        deckQueue.clear()
        val cards = when (deck) {
            FlashcardDeck.KANJI -> n5KanjiCharacters.shuffled().take(20).map { tmpl ->
                val sampleWord = wordsByChar[tmpl.char]?.firstOrNull()
                SimpleFlashcard(
                    id = "k_${tmpl.char}",
                    frontText = tmpl.char,
                    subFrontText = "Kanji N5 • ${tmpl.strokeCount} nét",
                    readingText = tmpl.romaji,
                    meaningVi = sampleWord?.let { "${it.word} (${it.romaji}): ${it.meaningVi}" }
                        ?: "Âm đọc / nghĩa: ${tmpl.romaji}",
                    speakText = tmpl.char
                )
            }

            FlashcardDeck.VOCAB -> wordsByChar.values.flatten()
                .distinctBy { it.word }
                .shuffled()
                .take(25)
                .map { v ->
                    SimpleFlashcard(
                        id = "v_${v.word}",
                        frontText = v.word,
                        subFrontText = "Từ vựng N5",
                        readingText = v.romaji,
                        meaningVi = v.meaningVi,
                        speakText = v.word
                    )
                }
        }
        deckQueue.addAll(cards)
        masteredCount = 0
        isFlipped = false
        dragOffsetX = 0f
    }

    LaunchedEffect(selectedDeck) {
        rebuildDeck(selectedDeck)
    }

    fun handleCardDecision(remembered: Boolean) {
        if (deckQueue.isEmpty()) return
        val top = deckQueue.removeAt(0)
        if (remembered) {
            masteredCount++
        } else {
            deckQueue.add(top)
        }
        isFlipped = false
        dragOffsetX = 0f
        onRecordStudyActivity()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlashcardDeck.entries.forEach { deck ->
                val active = selectedDeck == deck
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = if (active) 2.dp else 1.dp,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        .background(
                            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable { selectedDeck = deck }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = deck.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        if (deckQueue.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.primary)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "✓ ĐÃ THUỘC HẾT $masteredCount THẺ!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = { rebuildDeck(selectedDeck) },
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Trộn bộ thẻ mới")
                }
            }
            return
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Còn lại: ${deckQueue.size} thẻ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Đã thuộc: $masteredCount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        val card = deckQueue.first()
        val animatedOffsetX by animateFloatAsState(
            targetValue = dragOffsetX,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "card_drag"
        )
        val cardScale by animateFloatAsState(
            targetValue = if (isFlipped) 1.02f else 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "card_flip_scale"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .graphicsLayer {
                    rotationZ = (animatedOffsetX / 35f).coerceIn(-10f, 10f)
                    scaleX = cardScale
                    scaleY = cardScale
                }
                .border(2.dp, MaterialTheme.colorScheme.outline)
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(card.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                dragOffsetX > 140f -> handleCardDecision(remembered = true)
                                dragOffsetX < -140f -> handleCardDecision(remembered = false)
                                else -> dragOffsetX = 0f
                            }
                        },
                        onDragCancel = { dragOffsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffsetX += dragAmount
                        }
                    )
                }
                .clickable { isFlipped = !isFlipped }
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = card.subFrontText.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = card.frontText,
                    fontSize = if (card.frontText.length <= 2) 64.sp else 40.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                OutlinedButton(
                    onClick = { onSpeak(card.speakText, card.readingText) },
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text("🔊 Nghe phát âm")
                }

                if (isFlipped) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = card.readingText,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = card.meaningVi,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = "Chạm thẻ để lật xem nghĩa • Vuốt phải: Đã thuộc / Vuốt trái: Ôn lại",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { handleCardDecision(remembered = false) },
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("← Chưa nhớ")
            }
            Button(
                onClick = { handleCardDecision(remembered = true) },
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Đã thuộc →")
            }
        }
    }
}
