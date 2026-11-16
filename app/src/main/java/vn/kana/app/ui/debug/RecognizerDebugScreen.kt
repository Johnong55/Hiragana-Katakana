package vn.kana.app.ui.debug

import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import vn.kana.app.ui.components.KanaDrawingCanvas
import vn.kana.recognizer.StrictnessLevel
import java.util.Locale

/**
 * Màn hình Debug bộ chấm điểm (`KanaRecognizer`) — Đã tách cố định vùng Canvas 1:1
 * để không bị kẹt cử chỉ cuộn màn hình vào vùng vẽ.
 */
@Composable
fun RecognizerDebugScreen(
    viewModel: RecognizerDebugViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val localView = LocalView.current
    val target = uiState.targetTemplate

    val errorStrokeSet = uiState.lastResult?.perStroke
        ?.filter { it.errors.isNotEmpty() }
        ?.map { it.strokeIndex }
        ?.toSet()
        .orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Thanh tiêu đề cố định
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
            Text(
                text = "DEBUG BỘ CHẤM ĐIỂM",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (uiState.showTemplateComparison) "So chuẩn: BẬT" else "So chuẩn: TẮT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .clickable(onClick = viewModel::toggleTemplateComparison)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        // Chọn mức nghiêm + Lọc Hiragana/Katakana + Thanh cuộn ngang chọn chữ
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (level in StrictnessLevel.entries) {
                    val selected = uiState.strictnessLevel == level
                    Text(
                        text = level.displayNameVi,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(1.dp, MaterialTheme.colorScheme.onBackground)
                            .clickable { viewModel.setStrictness(level) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("hiragana" to "あ", "katakana" to "ア").forEach { (scriptKey, label) ->
                    val active = uiState.selectedScriptFilter == scriptKey
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(1.dp, MaterialTheme.colorScheme.onBackground)
                            .clickable { viewModel.setScriptFilter(scriptKey) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = if (uiState.showGhostOverlay) "Mờ: BẬT" else "Mờ: TẮT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .clickable(onClick = viewModel::toggleGhostOverlay)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Thanh cuộn ngang chọn chữ (ưu tiên hiển thị các cặp dễ nhầm ở đầu danh sách)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val filtered = uiState.characters.filter { it.script == uiState.selectedScriptFilter }
            for (item in filtered) {
                val selected = item.char == target?.char
                Text(
                    text = item.char,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .clickable { viewModel.selectTargetCharacter(item) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        if (target != null) {
            // Thông tin chữ đang test + nút Lưu mẫu JSON
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = target.char,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.clickable(onClick = viewModel::speakTarget)
                    )
                    Column {
                        Text(
                            text = "${target.romaji} • ${target.row} (${uiState.userStrokes.size}/${target.strokeCount} nét)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        uiState.statusBannerMessage?.let { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = "Lưu mẫu (${uiState.savedSampleCount})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.onBackground)
                        .clickable(onClick = viewModel::saveCurrentSample)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            // Ô vuông Canvas 1:1 nằm trong vùng cố định (weight = 1f), KHÔNG BAO GIỜ cuộn dọc!
            KanaDrawingCanvas(
                userStrokes = uiState.userStrokes,
                onStrokeCompleted = viewModel::addStroke,
                templateStrokes = uiState.templateStrokes,
                showGhostTemplate = uiState.showGhostOverlay,
                showTemplateComparison = uiState.showTemplateComparison,
                errorStrokeIndices = errorStrokeSet,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            // 3 nút cố định: Hoàn tác nét cuối, Xoá hết, Kiểm tra
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CanvasActionButton(
                    text = "Hoàn tác nét cuối",
                    filled = false,
                    onClick = viewModel::undoLastStroke,
                    modifier = Modifier.weight(1f)
                )
                CanvasActionButton(
                    text = "Xoá hết",
                    filled = false,
                    onClick = viewModel::clearCanvas,
                    modifier = Modifier.weight(0.75f)
                )
                CanvasActionButton(
                    text = "Kiểm tra",
                    filled = true,
                    onClick = {
                        viewModel.evaluateCurrentDrawing()
                        val res = viewModel.uiState.value.lastResult
                        if (res != null && !res.passed) {
                            localView.performHapticFeedback(HapticFeedbackConstants.REJECT)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Bảng kết quả chấm chi tiết (nếu có) — chỉ cuộn bên trong hộp nhỏ này (cao tối đa 150dp)
            val result = uiState.lastResult
            if (result != null) {
                val statusColor = if (result.passed) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.error
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 145.dp)
                        .border(1.dp, statusColor)
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (result.passed) "ĐẠT (PASSED)" else "CHƯA ĐẠT (REJECTED)",
                            style = MaterialTheme.typography.labelLarge,
                            color = statusColor
                        )
                        Text(
                            text = "Điểm: ${result.score}/100 • Best: ${result.bestMatchChar ?: "—"} (${result.bestMatchScore}) • ${result.elapsedTimeMs}ms",
                            style = MaterialTheme.typography.labelLarge,
                            color = statusColor
                        )
                    }

                    if (result.errors.isNotEmpty()) {
                        Text(
                            text = "Lỗi: " + result.errors.joinToString(", ") { "${it.name} (${it.messageVi})" },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    for (strokeEval in result.perStroke) {
                        val hasErr = strokeEval.errors.isNotEmpty()
                        Text(
                            text = String.format(
                                Locale.US,
                                "Nét %d (#%d): %dđ | dAvg=%.2f Fréchet=%.2f Góc=%.0f° Cos=%.2f %s",
                                strokeEval.strokeIndex + 1,
                                strokeEval.matchedTemplateStrokeIndex + 1,
                                strokeEval.score,
                                strokeEval.avgPointDistance,
                                strokeEval.frechetDistance,
                                strokeEval.directionAngleDiffDeg,
                                strokeEval.tangentSimilarity,
                                if (hasErr) strokeEval.errors.joinToString(",") { it.name } else ""
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (hasErr) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CanvasActionButton(
    text: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                if (filled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .border(1.dp, MaterialTheme.colorScheme.onBackground)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (filled) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onBackground
        )
    }
}
