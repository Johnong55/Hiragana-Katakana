package vn.kana.app.ui.mocktest

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
import vn.kana.app.domain.MockTestSection
import vn.kana.app.domain.N5MockTestCatalog

private enum class MockTestPhase {
    INTRO,
    IN_PROGRESS,
    RESULT
}

private const val EXAM_DURATION_SECONDS = 15 * 60

@Composable
fun N5MockTestScreen(
    bestScorePercent: Int,
    attemptCount: Int,
    onSpeak: (String) -> Unit,
    onSaveMockResult: (scorePercent: Int) -> Unit,
    onRecordStudyActivity: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val questions = N5MockTestCatalog.questions
    var phase by remember { mutableStateOf(MockTestPhase.INTRO) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableIntStateOf(EXAM_DURATION_SECONDS) }
    val selectedAnswers = remember { mutableStateMapOf<Int, Int>() }
    var resultSaved by remember { mutableStateOf(false) }

    fun finishExam() {
        phase = MockTestPhase.RESULT
        if (!resultSaved) {
            resultSaved = true
            val correct = questions.indices.count { selectedAnswers[it] == questions[it].correctIndex }
            val percent = (correct * 100) / questions.size
            onSaveMockResult(percent)
            onRecordStudyActivity()
        }
    }

    LaunchedEffect(phase) {
        if (phase == MockTestPhase.IN_PROGRESS) {
            while (remainingSeconds > 0 && phase == MockTestPhase.IN_PROGRESS) {
                delay(1000L)
                remainingSeconds--
            }
            if (remainingSeconds <= 0 && phase == MockTestPhase.IN_PROGRESS) {
                finishExam()
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
        when (phase) {
            MockTestPhase.INTRO -> {
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
                        text = "THI THỬ JLPT N5",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ĐỀ THI MÔ PHỎNG JLPT N5 (OFFLINE)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "• Tổng cộng 24 câu hỏi (15 phút làm bài)\n• Phần 1: Từ vựng & Kanji (8 câu)\n• Phần 2: Ngữ pháp & Đọc hiểu (8 câu)\n• Phần 3: Nghe hiểu TTS offline (8 câu)\n• Mốc đạt JLPT N5 ước tính: từ 60% trở lên.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "ĐIỂM CAO NHẤT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (attemptCount > 0) "$bestScorePercent%" else "--",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "SỐ LẦN THI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$attemptCount lần",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        selectedAnswers.clear()
                        currentIndex = 0
                        remainingSeconds = EXAM_DURATION_SECONDS
                        resultSaved = false
                        phase = MockTestPhase.IN_PROGRESS
                    },
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Bắt đầu thi thử (15:00) →")
                }
            }

            MockTestPhase.IN_PROGRESS -> {
                val mins = remainingSeconds / 60
                val secs = remainingSeconds % 60
                val timeLabel = "%02d:%02d".format(mins, secs)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "← Thoát",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .clickable { onBack() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                    Text(
                        text = "⏱ $timeLabel  •  ${selectedAnswers.size}/${questions.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (remainingSeconds < 120) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Nộp bài",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { finishExam() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                // Quick question navigator strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    questions.indices.forEach { idx ->
                        val isCurrent = idx == currentIndex
                        val isAnswered = selectedAnswers.containsKey(idx)
                        Box(
                            modifier = Modifier
                                .border(
                                    width = if (isCurrent) 2.dp else 1.dp,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                                .background(
                                    when {
                                        isCurrent -> MaterialTheme.colorScheme.primary
                                        isAnswered -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                )
                                .clickable { currentIndex = idx }
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                val q = questions[currentIndex]
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${q.section.titleVi} (${q.section.badge})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Câu ${currentIndex + 1}: ${q.promptVi}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (q.audioJa != null) {
                            Button(
                                onClick = { onSpeak(q.audioJa) },
                                shape = RoundedCornerShape(0.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🔊 Nghe câu hỏi tiếng Nhật")
                            }
                        } else {
                            Text(
                                text = q.promptJa,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    q.options.forEachIndexed { oIdx, optText ->
                        val isChosen = selectedAnswers[currentIndex] == oIdx
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isChosen) 2.dp else 1.dp,
                                    color = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                                .background(
                                    if (isChosen) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable { selectedAnswers[currentIndex] = oIdx }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${('A' + oIdx)}. $optText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0,
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("← Câu trước")
                    }
                    if (currentIndex < questions.lastIndex) {
                        Button(
                            onClick = { currentIndex++ },
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Câu sau →")
                        }
                    } else {
                        Button(
                            onClick = { finishExam() },
                            shape = RoundedCornerShape(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Nộp bài ngay")
                        }
                    }
                }
            }

            MockTestPhase.RESULT -> {
                val totalCorrect = questions.indices.count { selectedAnswers[it] == questions[it].correctIndex }
                val percent = (totalCorrect * 100) / questions.size
                val passed = percent >= 60

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
                        text = "KẾT QUẢ THI THỬ N5",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, if (passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (passed) "✓ ĐẠT CHUẨN N5 ($percent%)" else "CẦN ÔN THÊM ($percent%)",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Tổng điểm: $totalCorrect / ${questions.size} câu đúng",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        MockTestSection.entries.forEach { sec ->
                            val secIndices = questions.indices.filter { questions[it].section == sec }
                            val secCorrect = secIndices.count { selectedAnswers[it] == questions[it].correctIndex }
                            Text(
                                text = "• ${sec.titleVi}: $secCorrect / ${secIndices.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "CHI TIẾT ĐÁP ÁN",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    questions.forEachIndexed { idx, q ->
                        val chosen = selectedAnswers[idx]
                        val isCorrect = chosen == q.correctIndex
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (isCorrect) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error
                                )
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Câu ${idx + 1} (${q.section.badge})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isCorrect) "✓ Đúng" else "✗ Sai",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                            if (q.audioJa != null) {
                                Text(
                                    text = "🔊 ${q.audioJa}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.clickable { onSpeak(q.audioJa) }
                                )
                            } else {
                                Text(
                                    text = q.promptJa,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Text(
                                text = "Đáp án đúng: ${('A' + q.correctIndex)}. ${q.options[q.correctIndex]}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = q.explanationVi,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Button(
                    onClick = { phase = MockTestPhase.INTRO },
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Thi lại đề này")
                }
            }
        }
    }
}
