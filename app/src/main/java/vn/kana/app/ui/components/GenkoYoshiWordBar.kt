package vn.kana.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import vn.kana.recognizer.RecognitionResult
import vn.kana.recognizer.Stroke

/**
 * Trạng thái của một ô chữ trong từ nhiều chữ (kiểu giấy 原稿用紙).
 */
data class WordCharSlot(
    val index: Int,
    val targetChar: String,
    val expectedStrokeCount: Int,
    val templateStrokes: List<Stroke>,
    /** true nếu chữ này chưa được học -> hiển thị sẵn màu xám, không bắt người dùng viết */
    val isPreFilledUnlearned: Boolean,
    val userStrokes: List<Stroke> = emptyList(),
    val evaluation: RecognitionResult? = null
)

/**
 * Dãy ô vuông liền kề kiểu giấy luyện viết Nhật Bản (原稿用紙 - Genkō yōshi):
 * - Chữ chưa học: vẽ sẵn nét chữ màu xám trong ô, không bắt viết.
 * - Chữ đã học: hiển thị nét người dùng đã vẽ trong ô, viền đậm cho ô đang active,
 *   viền màu nhấn đỏ nếu ô đó bị chấm sai.
 * - Cho phép bấm vào bất kỳ ô cần viết nào để chọn viết/sửa lại ô đó.
 */
@Composable
fun GenkoYoshiWordBar(
    slots: List<WordCharSlot>,
    activeSlotIndex: Int,
    onSelectSlot: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val preFilledBgColor = MaterialTheme.colorScheme.surfaceVariant
    val inkColor = MaterialTheme.colorScheme.onSurface
    val grayStrokeColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val outlineColor = MaterialTheme.colorScheme.outline
    val errorColor = MaterialTheme.colorScheme.error

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (slot in slots) {
                val isSelected = !slot.isPreFilledUnlearned && slot.index == activeSlotIndex
                val isFailed = slot.evaluation != null && !slot.evaluation.passed

                val borderColor = when {
                    isFailed -> errorColor
                    isSelected -> inkColor
                    else -> outlineColor
                }
                val borderWidth = if (isSelected || isFailed) 2.dp else 1.dp
                val boxBg = if (slot.isPreFilledUnlearned) preFilledBgColor else surfaceColor

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .background(boxBg)
                            .border(borderWidth, borderColor)
                            .clickable(enabled = !slot.isPreFilledUnlearned) {
                                onSelectSlot(slot.index)
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            drawLine(
                                color = outlineColor,
                                start = Offset(size.width / 2f, 0f),
                                end = Offset(size.width / 2f, size.height),
                                strokeWidth = 0.8.dp.toPx(),
                                pathEffect = dashEffect
                            )
                            drawLine(
                                color = outlineColor,
                                start = Offset(0f, size.height / 2f),
                                end = Offset(size.width, size.height / 2f),
                                strokeWidth = 0.8.dp.toPx(),
                                pathEffect = dashEffect
                            )

                            val strokeW = size.minDimension * 0.065f
                            if (slot.isPreFilledUnlearned) {
                                // Chữ chưa học: hiển thị sẵn màu xám từ dữ liệu nét KanjiVG
                                for (tStroke in slot.templateStrokes) {
                                    drawSmoothStrokePath(
                                        points = tStroke.points,
                                        progress = 1f,
                                        color = grayStrokeColor,
                                        strokeWidth = strokeW
                                    )
                                }
                            } else {
                                // Chữ cần viết: hiển thị các nét người dùng đã vẽ trong ô này
                                val drawColor = if (isFailed) errorColor else inkColor
                                for (uStroke in slot.userStrokes) {
                                    drawSmoothStrokePath(
                                        points = uStroke.points,
                                        progress = 1f,
                                        color = drawColor,
                                        strokeWidth = strokeW
                                    )
                                }
                            }
                        }
                    }

                    val caption = when {
                        slot.isPreFilledUnlearned -> "Hiện sẵn"
                        isSelected -> "Đang viết (${slot.userStrokes.size}/${slot.expectedStrokeCount})"
                        slot.userStrokes.isNotEmpty() -> "Đã viết"
                        else -> "Ô ${slot.index + 1}"
                    }
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            isFailed -> errorColor
                            isSelected -> inkColor
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}
