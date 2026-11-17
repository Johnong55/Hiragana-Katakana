package vn.kana.app.ui.listening

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import vn.kana.recognizer.KanaStrokeTemplate

enum class ListeningScope(val labelVi: String) {
    HIRAGANA("Hiragana"),
    KATAKANA("Katakana"),
    EXTENDED("Âm đục/ghép"),
    KANJI_N5("Kanji N5")
}

private data class ListeningQuestion(
    val target: KanaStrokeTemplate,
    val options: List<KanaStrokeTemplate>
)

private const val QUESTIONS_PER_ROUND = 10

@Composable
fun ListeningQuizScreen(
    basicHiragana: List<KanaStrokeTemplate>,
    basicKatakana: List<KanaStrokeTemplate>,
    extendedCharacters: List<KanaStrokeTemplate>,
    n5KanjiCharacters: List<KanaStrokeTemplate>,
    autoAdvanceDelayMs: Long,
    onSpeak: (text: String, romaji: String?) -> Unit,
    onRecordStudyActivity: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedScope by remember { mutableStateOf(ListeningScope.HIRAGANA) }
    var roundSeed by remember { mutableIntStateOf(0) }

    val pool = remember(
        selectedScope,
        basicHiragana,
        basicKatakana,
        extendedCharacters,
        n5KanjiCharacters
    ) {
        when (selectedScope) {
            ListeningScope.HIRAGANA -> basicHiragana
            ListeningScope.KATAKANA -> basicKatakana
            ListeningScope.EXTENDED -> extendedCharacters
            ListeningScope.KANJI_N5 -> n5KanjiCharacters
        }
    }

    val questions = remember(pool, roundSeed) {
        buildListeningQuestions(pool, QUESTIONS_PER_ROUND)
    }

    var currentIndex by remember(questions) { mutableIntStateOf(0) }
    var selectedChar by remember(questions) { mutableStateOf<String?>(null) }
    var correctCount by remember(questions) { mutableIntStateOf(0) }
    var currentCombo by remember(questions) { mutableIntStateOf(0) }
    var maxCombo by remember(questions) { mutableIntStateOf(0) }
    var isCompleted by remember(questions) { mutableStateOf(false) }

    val currentQuestion = questions.getOrNull(currentIndex)

    // Tự động phát âm khi sang câu mới
    LaunchedEffect(roundSeed, selectedScope, currentIndex, isCompleted) {
        if (!isCompleted && currentQuestion != null) {
            onSpeak(currentQuestion.target.char, currentQuestion.target.romaji)
        }
    }

    // Tự động chuyển câu nếu chọn đúng
    LaunchedEffect(currentIndex, selectedChar, isCompleted) {
        if (!isCompleted && currentQuestion != null && selectedChar == currentQuestion.target.char) {
            delay(autoAdvanceDelayMs.coerceIn(500L, 1400L))
            if (currentIndex + 1 < questions.size) {
                currentIndex += 1
                selectedChar = null
            } else {
                isCompleted = true
            }
        }
    }

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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "← Quay lại",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            )
            Text(
                text = "LUYỆN NGHE & PHẢN XẠ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (questions.isNotEmpty() && !isCompleted) {
                    "${currentIndex + 1}/${questions.size}"
                } else {
                    "Xong"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        // Bộ lọc nhóm chữ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ListeningScope.entries.forEach { scope ->
                val isSelected = scope == selectedScope
                Box(
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background
                        )
                        .clickable {
                            if (selectedScope != scope) {
                                selectedScope = scope
                                roundSeed++
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = scope.labelVi,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        if (questions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa đủ dữ liệu cho nhóm này.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        if (isCompleted) {
            ListeningResultSummary(
                total = questions.size,
                correct = correctCount,
                maxCombo = maxCombo,
                onReplayRound = { roundSeed++ },
                onBack = onBack,
                modifier = Modifier.weight(1f)
            )
            return@Column
        }

        val q = currentQuestion!!
        val hasAnswered = selectedChar != null
        val isCorrectAnswer = selectedChar == q.target.char

        // Thanh chỉ số nhanh
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Đúng: $correctCount/${questions.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (currentCombo >= 2) "🔥 Combo x$currentCombo" else "Combo: $currentCombo",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Khu vực phát âm thanh
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (selectedScope == ListeningScope.KANJI_N5) {
                    "Nghe âm đọc & chọn chữ Kanji đúng"
                } else {
                    "Nghe phát âm & chọn chữ đúng"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.primary)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onSpeak(q.target.char, q.target.romaji) }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "🔊  Nghe lại phát âm",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            if (hasAnswered) {
                Text(
                    text = if (isCorrectAnswer) {
                        "✓ Chính xác!  ${q.target.char} (${q.target.romaji})"
                    } else {
                        "✗ Chưa đúng! Đáp án: ${q.target.char} (${q.target.romaji})"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCorrectAnswer) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Chạm vào 1 trong 4 ô bên dưới",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Lưới 2x2 đáp án
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val rows = q.options.chunked(2)
            rows.forEach { pair ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { option ->
                        val isTarget = option.char == q.target.char
                        val isUserChoice = option.char == selectedChar
                        val borderColor = when {
                            !hasAnswered -> MaterialTheme.colorScheme.outline
                            isTarget -> MaterialTheme.colorScheme.primary
                            isUserChoice -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.outline
                        }
                        val bgColor = when {
                            !hasAnswered -> MaterialTheme.colorScheme.background
                            isTarget -> MaterialTheme.colorScheme.primary
                            isUserChoice -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.background
                        }
                        val textColor = when {
                            hasAnswered && isTarget -> MaterialTheme.colorScheme.onPrimary
                            hasAnswered && isUserChoice -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onBackground
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .border(if (hasAnswered && (isTarget || isUserChoice)) 2.dp else 1.dp, borderColor)
                                .background(bgColor)
                                .clickable(enabled = !hasAnswered) {
                                    selectedChar = option.char
                                    if (option.char == q.target.char) {
                                        correctCount++
                                        currentCombo++
                                        if (currentCombo > maxCombo) maxCombo = currentCombo
                                        onRecordStudyActivity()
                                    } else {
                                        currentCombo = 0
                                        onSpeak(q.target.char, q.target.romaji)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = option.char,
                                    style = MaterialTheme.typography.displayMedium,
                                    color = textColor
                                )
                                if (hasAnswered) {
                                    Text(
                                        text = option.romaji,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Nút Tiếp tục khi trả lời sai (khi đúng đã tự chuyển câu)
        if (hasAnswered && !isCorrectAnswer) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        if (currentIndex + 1 < questions.size) {
                            currentIndex += 1
                            selectedChar = null
                        } else {
                            isCompleted = true
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (currentIndex + 1 < questions.size) "Tiếp tục →" else "Xem kết quả",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ListeningResultSummary(
    total: Int,
    correct: Int,
    maxCombo: Int,
    onReplayRound: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accuracy = if (total > 0) (correct * 100) / total else 0
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (accuracy >= 80) "PHẢN XẠ TUYỆT VỜI!" else "HOÀN THÀNH LƯỢT NGHE",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "$correct / $total câu đúng ($accuracy%)",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Chuỗi đúng liên tiếp cao nhất: $maxCombo",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onReplayRound() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Luyện lượt mới (10 câu)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .clickable { onBack() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Về Lộ trình",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

private fun buildListeningQuestions(
    pool: List<KanaStrokeTemplate>,
    count: Int
): List<ListeningQuestion> {
    if (pool.size < 4) return emptyList()
    val targets = pool.shuffled().take(count.coerceAtMost(pool.size))
    return targets.map { target ->
        val distractors = pool
            .filter { it.char != target.char && !it.romaji.equals(target.romaji, ignoreCase = true) }
            .distinctBy { it.romaji.lowercase() }
            .shuffled()
            .take(3)
            .let { list ->
                if (list.size == 3) list
                else pool.filter { it.char != target.char }.shuffled().take(3)
            }
        ListeningQuestion(
            target = target,
            options = (distractors + target).shuffled()
        )
    }
}
