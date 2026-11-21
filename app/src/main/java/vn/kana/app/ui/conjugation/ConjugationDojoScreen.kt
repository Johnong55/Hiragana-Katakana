package vn.kana.app.ui.conjugation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.kana.app.R
import vn.kana.app.domain.ConjugationDrillItem
import vn.kana.app.domain.ConjugationFilter
import vn.kana.app.domain.N5ConjugationCatalog
import vn.kana.app.ui.components.AnimatedMemeFeedbackBanner
import vn.kana.app.ui.components.MemeBadge

private enum class DojoTab(val labelVi: String) {
    DRILL("Luyện Chia Thể"),
    CHEAT_SHEET("Bảng Quy Tắc")
}

@Composable
fun ConjugationDojoScreen(
    onSpeak: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(DojoTab.DRILL) }

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
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MemeBadge(resId = R.drawable.meme_cat_reading, size = 24.dp)
                Text(
                    text = "VÕ ĐƯỜNG CHIA THỂ N5",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Chuyển tab Luyện tập / Bảng quy tắc
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DojoTab.entries.forEach { tab ->
                val selected = tab == activeTab
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.onBackground)
                        .clickable { activeTab = tab }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = tab.labelVi,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        when (activeTab) {
            DojoTab.DRILL -> ConjugationDrillTab(
                onSpeak = onSpeak,
                modifier = Modifier.weight(1f)
            )
            DojoTab.CHEAT_SHEET -> ConjugationCheatSheetTab(
                onSpeak = onSpeak,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ConjugationDrillTab(
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeFilter by remember { mutableStateOf(ConjugationFilter.ALL) }
    var sessionSeed by remember { mutableIntStateOf(0) }

    val deck = remember(activeFilter, sessionSeed) {
        val base = if (activeFilter == ConjugationFilter.ALL) {
            N5ConjugationCatalog.drillItems
        } else {
            N5ConjugationCatalog.drillItems.filter { it.filter == activeFilter }
        }
        base.shuffled()
    }

    var currentIndex by remember(activeFilter, sessionSeed) { mutableIntStateOf(0) }
    var selectedAnswer by remember(activeFilter, sessionSeed, currentIndex) { mutableStateOf<String?>(null) }
    var correctCount by remember(activeFilter, sessionSeed) { mutableIntStateOf(0) }
    var streakCount by remember(activeFilter, sessionSeed) { mutableIntStateOf(0) }
    var isCompleted by remember(activeFilter, sessionSeed) { mutableStateOf(false) }

    val currentItem: ConjugationDrillItem? = deck.getOrNull(currentIndex)
    val options = remember(currentItem?.id, sessionSeed) {
        if (currentItem == null) emptyList()
        else (currentItem.distractors + currentItem.correctAnswer).distinct().shuffled()
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Bộ lọc thể
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ConjugationFilter.entries.forEach { f ->
                    val selected = f == activeFilter
                    Text(
                        text = f.labelVi,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .clickable { activeFilter = f }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            if (isCompleted || currentItem == null) {
                // Màn hình hoàn thành bộ chia thể
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onBackground)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MemeBadge(resId = R.drawable.meme_shiba, size = 72.dp)
                    Text(
                        text = "HOÀN THÀNH LUYỆN CHIA THỂ",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Đúng $correctCount / ${deck.size} câu (${if (deck.isNotEmpty()) (correctCount * 100 / deck.size) else 0}%)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .border(1.dp, MaterialTheme.colorScheme.onBackground)
                            .clickable { sessionSeed++ }
                            .padding(vertical = 11.dp)
                    ) {
                        Text(
                            text = "Luyện vòng mới (trộn đề) →",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            } else {
                // Thanh tiến độ & Combo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Câu ${currentIndex + 1} / ${deck.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Đúng: $correctCount  •  Combo: x$streakCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Thẻ đề bài
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.onBackground)
                        .clickable { onSpeak(currentItem.baseKana) }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentItem.groupBadgeVi,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                        Text(
                            text = "Chạm để nghe",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = currentItem.baseJp,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "(${currentItem.baseKana}) — ${currentItem.meaningVi}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

                    Text(
                        text = "Yêu cầu: ${currentItem.targetFormVi}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // 4 lựa chọn đáp án
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { opt ->
                        val isChosen = selectedAnswer == opt
                        val isCorrectOpt = opt == currentItem.correctAnswer
                        val answered = selectedAnswer != null

                        val bg = when {
                            answered && isCorrectOpt -> MaterialTheme.colorScheme.primary
                            answered && isChosen && !isCorrectOpt -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val fg = when {
                            answered && (isCorrectOpt || isChosen) -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onBackground
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bg)
                                .border(1.dp, MaterialTheme.colorScheme.onBackground)
                                .clickable(enabled = !answered) {
                                    selectedAnswer = opt
                                    if (opt == currentItem.correctAnswer) {
                                        correctCount++
                                        streakCount++
                                    } else {
                                        streakCount = 0
                                    }
                                    onSpeak(currentItem.correctAnswer)
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = opt,
                                style = MaterialTheme.typography.titleMedium,
                                color = fg
                            )
                            if (answered && isCorrectOpt) {
                                Text(
                                    text = "✓ Đúng",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = fg
                                )
                            } else if (answered && isChosen) {
                                Text(
                                    text = "✕ Sai",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = fg
                                )
                            }
                        }
                    }
                }

                // Giải thích quy tắc sau khi chọn
                if (selectedAnswer != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Giải thích quy tắc:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = currentItem.explanationVi,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .border(1.dp, MaterialTheme.colorScheme.onBackground)
                            .clickable {
                                if (currentIndex + 1 >= deck.size) {
                                    isCompleted = true
                                } else {
                                    currentIndex++
                                }
                            }
                            .padding(vertical = 11.dp)
                    ) {
                        Text(
                            text = if (currentIndex + 1 >= deck.size) "Xem kết quả →" else "Câu tiếp theo →",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // Pop-up meme đúng / sai đồng bộ toàn app
        if (currentItem != null && selectedAnswer != null) {
            val isCorrect = selectedAnswer == currentItem.correctAnswer
            AnimatedMemeFeedbackBanner(
                message = if (isCorrect) "Chuẩn xác: ${currentItem.correctAnswer}" else "Đáp án đúng: ${currentItem.correctAnswer}",
                isError = !isCorrect,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
private fun ConjugationCheatSheetTab(
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Sổ tay tóm tắt toàn bộ quy tắc chia Động từ & Tính từ JLPT N5 — chạm vào dòng ví dụ để nghe phát âm:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        N5ConjugationCatalog.ruleCards.forEach { card ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = card.titleVi,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = card.subtitleVi,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
                card.rules.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onSpeak(card.exampleJp) }
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = card.exampleJp,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = card.exampleVi,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
