package vn.kana.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.kana.app.data.local.CharacterProgressEntity
import vn.kana.app.ui.map.LessonMapUiState
import vn.kana.recognizer.KanaStrokeTemplate

private data class CategoryProgressSummary(
    val titleVi: String,
    val learned: Int,
    val total: Int
)

private data class WeakCharacterItem(
    val progress: CharacterProgressEntity,
    val template: KanaStrokeTemplate?
)

@Composable
fun ProgressStatsScreen(
    uiState: LessonMapUiState,
    onPracticeCharacter: (rowName: String, script: String, targetChar: String) -> Unit,
    onStartReview: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val templateByChar = remember(uiState.allCurriculumCharacters) {
        uiState.allCurriculumCharacters.associateBy { it.char }
    }

    val categorySummaries = remember(
        uiState.basicHiragana,
        uiState.basicKatakana,
        uiState.extendedCharacters,
        uiState.n5KanjiCharacters,
        uiState.characterProgressByChar
    ) {
        fun countLearned(list: List<KanaStrokeTemplate>): Int =
            list.count { uiState.characterProgressByChar[it.char]?.learned == true }

        listOf(
            CategoryProgressSummary(
                titleVi = "Hiragana cơ bản",
                learned = countLearned(uiState.basicHiragana),
                total = uiState.basicHiragana.size
            ),
            CategoryProgressSummary(
                titleVi = "Katakana cơ bản",
                learned = countLearned(uiState.basicKatakana),
                total = uiState.basicKatakana.size
            ),
            CategoryProgressSummary(
                titleVi = "Âm đục & Âm ghép",
                learned = countLearned(uiState.extendedCharacters),
                total = uiState.extendedCharacters.size
            ),
            CategoryProgressSummary(
                titleVi = "Kanji JLPT N5",
                learned = countLearned(uiState.n5KanjiCharacters),
                total = uiState.n5KanjiCharacters.size
            )
        )
    }

    val weakCharacters = remember(uiState.characterProgressByChar, templateByChar) {
        uiState.characterProgressByChar.values
            .filter { it.errorCount > 0 || (!it.learned && it.practiceCount > 0) }
            .sortedByDescending { it.errorCount * 20 + (100 - it.lastScore) }
            .take(20)
            .map { WeakCharacterItem(progress = it, template = templateByChar[it.char]) }
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
                text = "TIẾN ĐỘ & CHỮ HAY SAI",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.padding(horizontal = 24.dp))
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Thẻ tổng quan Streak + Tổng chữ + SRS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatTile(
                        label = "Chuỗi ngày",
                        value = "🔥 ${uiState.settings.streakDays}",
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Đã thuộc",
                        value = "${uiState.totalLearnedCount}/${uiState.allCurriculumCharacters.size}",
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Cần ôn SRS",
                        value = "${uiState.dueSrsCardsCount}",
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = uiState.dueSrsCardsCount > 0) { onStartReview() }
                    )
                }
            }

            if (uiState.dueSrsCardsCount > 0) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onStartReview() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ôn tập ${uiState.dueSrsCardsCount} thẻ SRS đến hạn →",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // 2. Tiến độ từng nhóm chữ
            item {
                Text(
                    text = "TIẾN ĐỘ THEO NHÓM",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(categorySummaries) { summary ->
                val ratio = if (summary.total > 0) {
                    (summary.learned.toFloat() / summary.total.toFloat()).coerceIn(0f, 1f)
                } else 0f
                val percent = (ratio * 100).toInt()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = summary.titleVi,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${summary.learned}/${summary.total} ($percent%)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        if (ratio > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }

            // 3. Danh sách chữ hay viết sai
            item {
                Text(
                    text = "CHỮ HAY VIẾT SAI / CẦN LUYỆN THÊM",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (weakCharacters.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Chưa có chữ nào bị sai nhiều. Cứ giữ vững phong độ nhé!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(weakCharacters, key = { it.progress.char }) { item ->
                    val prog = item.progress
                    val romaji = item.template?.romaji.orEmpty()
                    val scriptLabel = when (prog.script) {
                        "hiragana" -> "Hiragana"
                        "katakana" -> "Katakana"
                        "kanji" -> "Kanji N5"
                        else -> prog.script
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = prog.char,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (romaji.isNotBlank()) "$romaji • $scriptLabel" else scriptLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Sai ${prog.errorCount} lần • Điểm gần nhất: ${prog.lastScore}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (prog.errorCount > 0) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    onPracticeCharacter(prog.rowName, prog.script, prog.char)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Luyện",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
