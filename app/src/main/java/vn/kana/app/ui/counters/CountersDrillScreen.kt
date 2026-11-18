package vn.kana.app.ui.counters

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import vn.kana.app.domain.CounterCategory
import vn.kana.app.domain.CounterEntry
import vn.kana.app.domain.N5CountersCatalog
import vn.kana.app.ui.components.AnimatedMemeFeedbackBanner

private data class CounterQuizItem(
    val target: CounterEntry,
    val choices: List<CounterEntry>
)

@Composable
fun CountersDrillScreen(
    autoAdvanceDelayMs: Long,
    onSpeak: (String, String?) -> Unit,
    onRecordStudyActivity: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(CounterCategory.HOURS) }
    var inQuizMode by remember { mutableStateOf(false) }
    var quizItems by remember { mutableStateOf<List<CounterQuizItem>>(emptyList()) }
    var quizIndex by remember { mutableIntStateOf(0) }
    var chosenKana by remember { mutableStateOf<String?>(null) }
    var correctCount by remember { mutableIntStateOf(0) }
    var quizFinished by remember { mutableStateOf(false) }

    val entries = N5CountersCatalog.entriesByCategory[selectedCategory].orEmpty()

    fun startQuiz(allCategories: Boolean) {
        val pool = if (allCategories) {
            N5CountersCatalog.entriesByCategory.values.flatten()
        } else {
            entries
        }
        val irregularFirst = pool.filter { it.isIrregular } + pool.shuffled()
        val picked = irregularFirst.distinctBy { it.kanji }.shuffled().take(10)
        quizItems = picked.map { target ->
            val sameCategoryPool = N5CountersCatalog.entriesByCategory.values
                .firstOrNull { it.contains(target) }
                ?: pool
            val distractors = sameCategoryPool
                .filter { it.kana != target.kana }
                .shuffled()
                .take(3)
            CounterQuizItem(
                target = target,
                choices = (distractors + target).shuffled()
            )
        }
        quizIndex = 0
        chosenKana = null
        correctCount = 0
        quizFinished = false
        inQuizMode = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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
                    .clickable {
                        if (inQuizMode) inQuizMode = false else onBack()
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
            Text(
                text = "SỐ ĐẾM, GIỜ & TRỢ SỐ TỪ",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (inQuizMode && quizItems.isNotEmpty()) {
            if (quizFinished) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.dp, MaterialTheme.colorScheme.primary)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "HOÀN THÀNH LUYỆN TRỢ SỐ TỪ",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "$correctCount / ${quizItems.size} câu đúng",
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
                            Text("Về bảng tra cứu")
                        }
                        Button(
                            onClick = { startQuiz(allCategories = true) },
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Luyện lại")
                        }
                    }
                }
                return
            }

            val current = quizItems[quizIndex]
            LaunchedEffect(chosenKana) {
                if (chosenKana == current.target.kana) {
                    delay(autoAdvanceDelayMs.coerceAtLeast(1100L))
                    if (quizIndex < quizItems.lastIndex) {
                        quizIndex++
                        chosenKana = null
                    } else {
                        quizFinished = true
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Câu ${quizIndex + 1} / ${quizItems.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Đúng: $correctCount",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = current.target.numberLabel.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = current.target.kanji,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (current.target.isIrregular) {
                    Text(
                        text = "★ Biến âm cần nhớ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            current.choices.forEach { choice ->
                val isChosen = chosenKana == choice.kana
                val isCorrect = choice.kana == current.target.kana
                val borderColor = when {
                    chosenKana == null -> MaterialTheme.colorScheme.outline
                    isCorrect -> MaterialTheme.colorScheme.primary
                    isChosen -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.outline
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(if (isChosen || (chosenKana != null && isCorrect)) 2.dp else 1.dp, borderColor)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(enabled = chosenKana == null) {
                            chosenKana = choice.kana
                            onSpeak(current.target.kana, current.target.romaji)
                            if (isCorrect) correctCount++
                            onRecordStudyActivity()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${choice.kana}  (${choice.romaji})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (chosenKana != null && isCorrect) {
                        Text("✓", color = MaterialTheme.colorScheme.primary)
                    } else if (chosenKana != null && isChosen) {
                        Text("✗", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (chosenKana != null) {
                val isAnsCorrect = chosenKana == current.target.kana
                AnimatedMemeFeedbackBanner(
                    message = "Đáp án: ${current.target.kanji} = ${current.target.kana} (${current.target.romaji})",
                    isError = !isAnsCorrect,
                    seed = quizIndex * 19 + (chosenKana?.hashCode() ?: 0),
                    onContinueAction = if (!isAnsCorrect) {
                        {
                            if (quizIndex < quizItems.lastIndex) {
                                quizIndex++
                                chosenKana = null
                            } else {
                                quizFinished = true
                            }
                        }
                    } else null
                )
            }

            if (chosenKana != null && chosenKana != current.target.kana) {
                Button(
                    onClick = {
                        if (quizIndex < quizItems.lastIndex) {
                            quizIndex++
                            chosenKana = null
                        } else {
                            quizFinished = true
                        }
                    },
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tiếp theo →")
                }
            }
            return
        }

        // Category selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CounterCategory.entries.forEach { cat ->
                val selected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat.labelVi,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { startQuiz(allCategories = false) },
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Luyện mục này")
            }
            Button(
                onClick = { startQuiz(allCategories = true) },
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Luyện tổng hợp")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(entries, key = { it.kanji + it.kana }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (item.isIrregular) 2.dp else 1.dp,
                            color = if (item.isIrregular) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onSpeak(item.kana, item.romaji) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.numberLabel} (${item.kanji})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (item.isIrregular) {
                                Text(
                                    text = "Biến âm",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.error)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "${item.kana} • ${item.romaji}" +
                                if (item.noteVi.isNotBlank()) " — ${item.noteVi}" else "",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Nghe",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}
