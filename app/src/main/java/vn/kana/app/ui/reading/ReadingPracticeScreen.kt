package vn.kana.app.ui.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.kana.app.domain.N5ReadingCatalog
import vn.kana.app.domain.N5ReadingPassage
import vn.kana.app.domain.N5SentenceBuilderItem
import vn.kana.app.ui.components.AnimatedMemeFeedbackBanner

private enum class ReadingTab(val label: String) {
    PASSAGES("Đọc hiểu"),
    SENTENCE_BUILDER("Ghép câu ★")
}

@Composable
fun ReadingPracticeScreen(
    onSpeak: (String) -> Unit,
    onRecordStudyActivity: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(ReadingTab.PASSAGES) }
    var selectedPassage by remember { mutableStateOf<N5ReadingPassage?>(null) }
    val completedPassages = remember { mutableStateMapOf<String, Boolean>() }

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
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .clickable {
                        if (activeTab == ReadingTab.PASSAGES && selectedPassage != null) {
                            selectedPassage = null
                        } else {
                            onBack()
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
            Text(
                text = "ĐỌC HIỂU & GHÉP CÂU N5",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 2-Tab switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReadingTab.entries.forEach { tab ->
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
            ReadingTab.PASSAGES -> {
                val passage = selectedPassage
                if (passage == null) {
                    PassageListView(
                        passages = N5ReadingCatalog.passages,
                        completedIds = completedPassages.keys,
                        onSelectPassage = { selectedPassage = it }
                    )
                } else {
                    PassageDetailView(
                        passage = passage,
                        onSpeak = onSpeak,
                        onCompleted = {
                            completedPassages[passage.id] = true
                            onRecordStudyActivity()
                        },
                        onNextPassage = {
                            val idx = N5ReadingCatalog.passages.indexOfFirst { it.id == passage.id }
                            selectedPassage = N5ReadingCatalog.passages.getOrNull(idx + 1)
                        },
                        onBackToList = { selectedPassage = null }
                    )
                }
            }

            ReadingTab.SENTENCE_BUILDER -> {
                SentenceBuilderView(
                    items = N5ReadingCatalog.sentenceBuilders,
                    onSpeak = onSpeak,
                    onRecordStudyActivity = onRecordStudyActivity
                )
            }
        }
    }
}

@Composable
private fun PassageListView(
    passages: List<N5ReadingPassage>,
    completedIds: Set<String>,
    onSelectPassage: (N5ReadingPassage) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(passages, key = { it.id }) { passage ->
            val isDone = completedIds.contains(passage.id)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isDone) 2.dp else 1.dp,
                        color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onSelectPassage(passage) }
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BÀI ${passage.number.toString().padStart(2, '0')} • ${passage.titleVi}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (isDone) {
                        Text(
                            text = "✓ Đã xong",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = passage.titleJa,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ngữ pháp: ${passage.grammarFocus} • ${passage.questions.size} câu hỏi",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun PassageDetailView(
    passage: N5ReadingPassage,
    onSpeak: (String) -> Unit,
    onCompleted: () -> Unit,
    onNextPassage: () -> Unit,
    onBackToList: () -> Unit
) {
    var showKana by remember(passage.id) { mutableStateOf(true) }
    var showVietnamese by remember(passage.id) { mutableStateOf(false) }
    val selectedAnswers = remember(passage.id) { mutableStateMapOf<Int, Int>() }
    var recordedCompletion by remember(passage.id) { mutableStateOf(false) }

    val allAnswered = selectedAnswers.size == passage.questions.size
    if (allAnswered && !recordedCompletion) {
        recordedCompletion = true
        onCompleted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Title & controls
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "BÀI ${passage.number.toString().padStart(2, '0')} • ${passage.titleVi}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = passage.titleJa,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Danh sách",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .clickable { onBackToList() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ToggleChip(
                    label = if (showKana) "あ Kana: Bật" else "あ Kana: Tắt",
                    active = showKana,
                    onClick = { showKana = !showKana },
                    modifier = Modifier.weight(1f)
                )
                ToggleChip(
                    label = if (showVietnamese) "VN Dịch: Bật" else "VN Dịch: Tắt",
                    active = showVietnamese,
                    onClick = { showVietnamese = !showVietnamese },
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.primary)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            onSpeak(passage.sentences.joinToString(" ") { it.japanese })
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nghe",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // Sentences
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "ĐOẠN VĂN (chạm từng câu để nghe)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            passage.sentences.forEachIndexed { idx, sentence ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSpeak(sentence.japanese) }
                        .padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = sentence.japanese,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (showKana && sentence.kanaOnly != sentence.japanese) {
                        Text(
                            text = sentence.kanaOnly,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (showVietnamese) {
                        Text(
                            text = "→ ${sentence.vietnamese}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (idx < passage.sentences.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                }
            }
        }

        // Grammar focus
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "NGỮ PHÁP TRỌNG TÂM: ${passage.grammarFocus}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = passage.grammarNoteVi,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Comprehension questions
        Text(
            text = "CÂU HỎI ĐỌC HIỂU",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        passage.questions.forEachIndexed { qIdx, question ->
            val chosen = selectedAnswers[qIdx]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Câu ${qIdx + 1}: ${question.questionVi}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                question.optionsVi.forEachIndexed { oIdx, optionText ->
                    val isChosen = chosen == oIdx
                    val isCorrect = oIdx == question.correctIndex
                    val borderColor = when {
                        chosen == null -> MaterialTheme.colorScheme.outline
                        isCorrect -> MaterialTheme.colorScheme.primary
                        isChosen -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outline
                    }
                    val bgColor = when {
                        chosen != null && isCorrect -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        chosen != null && isChosen && !isCorrect -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.background
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (chosen != null && (isCorrect || isChosen)) 2.dp else 1.dp,
                                color = borderColor
                            )
                            .background(bgColor)
                            .clickable(enabled = chosen == null) {
                                selectedAnswers[qIdx] = oIdx
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${('A' + oIdx)}. $optionText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        if (chosen != null && isCorrect) {
                            Text(
                                text = "✓",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (chosen != null && isChosen) {
                            Text(
                                text = "✗",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                if (chosen != null) {
                    val isQErr = chosen != question.correctIndex
                    AnimatedMemeFeedbackBanner(
                        message = question.explanationVi,
                        isError = isQErr,
                        seed = passage.number * 31 + qIdx * 7 + chosen
                    )
                    Text(
                        text = question.explanationVi,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (chosen == question.correctIndex) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }

        if (allAnswered) {
            val correctCount = passage.questions.indices.count { selectedAnswers[it] == passage.questions[it].correctIndex }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.primary)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Hoàn thành bài đọc: $correctCount/${passage.questions.size} câu đúng",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackToList,
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Về danh sách")
                    }
                    if (passage.number < N5ReadingCatalog.passages.size) {
                        Button(
                            onClick = onNextPassage,
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Bài tiếp theo →")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SentenceBuilderView(
    items: List<N5SentenceBuilderItem>,
    onSpeak: (String) -> Unit,
    onRecordStudyActivity: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }

    val item = items[currentIndex]
    // Stores fragment indices chosen for slots 0..3; null if empty
    var slotAssignments by remember(item.id) { mutableStateOf<List<Int?>>(listOf(null, null, null, null)) }
    var checkedResult by remember(item.id) { mutableStateOf<Boolean?>(null) }

    if (isFinished) {
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
                text = "HOÀN THÀNH GHÉP CÂU ★",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Kết quả: $correctCount / ${items.size} câu đúng",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = {
                    currentIndex = 0
                    correctCount = 0
                    isFinished = false
                },
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Luyện lại từ đầu")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Câu ${currentIndex + 1} / ${items.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Đúng: $correctCount",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Vietnamese meaning prompt
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "NGHĨA TIẾNG VIỆT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = item.promptVi,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Sentence frame with 4 slots (including ★)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "SẮP XẾP CÁC MẢNH VÀO ĐÚNG VỊ TRÍ (chạm ô đã điền để bỏ chọn)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (item.prefixJa.isNotBlank()) {
                    Text(
                        text = item.prefixJa,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                for (slotIdx in 0..3) {
                    val fragIdx = slotAssignments[slotIdx]
                    val isStar = slotIdx == item.starSlotIndex
                    val borderColor = when {
                        checkedResult == true -> MaterialTheme.colorScheme.primary
                        checkedResult == false && fragIdx != item.correctOrder[slotIdx] -> MaterialTheme.colorScheme.error
                        fragIdx != null -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }
                    Box(
                        modifier = Modifier
                            .border(if (isStar || fragIdx != null) 2.dp else 1.dp, borderColor)
                            .background(
                                if (fragIdx != null) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.background
                            )
                            .clickable(enabled = checkedResult == null && fragIdx != null) {
                                val updated = slotAssignments.toMutableList()
                                updated[slotIdx] = null
                                slotAssignments = updated
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (fragIdx != null) {
                                item.fragments[fragIdx]
                            } else if (isStar) {
                                "  ★  "
                            } else {
                                "  ${slotIdx + 1}  "
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (fragIdx != null) {
                                MaterialTheme.colorScheme.onBackground
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                if (item.suffixJa.isNotBlank()) {
                    Text(
                        text = item.suffixJa,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }

        // Available fragment chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item.fragments.forEachIndexed { fragIdx, text ->
                val alreadyUsed = slotAssignments.contains(fragIdx)
                Box(
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (alreadyUsed) MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.outline
                        )
                        .background(
                            if (alreadyUsed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable(enabled = checkedResult == null && !alreadyUsed) {
                            val firstEmpty = slotAssignments.indexOfFirst { it == null }
                            if (firstEmpty != -1) {
                                val updated = slotAssignments.toMutableList()
                                updated[firstEmpty] = fragIdx
                                slotAssignments = updated
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (alreadyUsed) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        }
                    )
                }
            }
        }

        val allFilled = slotAssignments.none { it == null }

        if (checkedResult == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { slotAssignments = listOf(null, null, null, null) },
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Đặt lại")
                }
                Button(
                    onClick = {
                        val isCorrect = slotAssignments == item.correctOrder
                        checkedResult = isCorrect
                        if (isCorrect) correctCount++
                        onRecordStudyActivity()
                    },
                    enabled = allFilled,
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(2f)
                ) {
                    Text("Kiểm tra")
                }
            }
        } else {
            val isCorrect = checkedResult == true
            AnimatedMemeFeedbackBanner(
                message = "${item.fullSentenceJa}\n${item.explanationVi}",
                isError = !isCorrect,
                seed = currentIndex * 29 + (if (isCorrect) 1 else 2)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        2.dp,
                        if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isCorrect) "✓ Chính xác!" else "✗ Chưa đúng",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.fullSentenceJa,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Nghe",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onSpeak(item.fullSentenceJa) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = item.explanationVi,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        if (currentIndex < items.lastIndex) {
                            currentIndex++
                        } else {
                            isFinished = true
                        }
                    },
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (currentIndex < items.lastIndex) "Câu tiếp theo →" else "Xem kết quả")
                }
            }
        }
    }
}

@Composable
private fun ToggleChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.background
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )
    }
}
