package vn.kana.app.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke as ComposeStroke
import androidx.compose.ui.unit.dp
import vn.kana.recognizer.RecognitionResult
import vn.kana.recognizer.Stroke

private val SMALL_KANA_SET = setOf(
    "ゃ", "ゅ", "ょ", "っ", "ぁ", "ぃ", "ぅ", "ぇ", "ぉ",
    "ャ", "ュ", "ョ", "ッ", "ァ", "ィ", "ゥ", "ェ", "ォ"
)

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
) {
    val isSmallKana: Boolean
        get() = targetChar in SMALL_KANA_SET
}

/**
 * Dãy ô vuông liền kề kiểu giấy luyện viết Nhật Bản (原稿用紙 - Genkō yōshi):
 * - Chữ chưa học: vẽ sẵn nét chữ màu xám trong ô, không bắt viết.
 * - Chữ nhỏ (拗音 ゃゅょ / 促音 っ): hiển thị khung nét đứt nhỏ bên trong và nhãn "(nhỏ)" để người viết nhận biết.
 * - Chữ đã học: hiển thị nét người dùng đã vẽ trong ô, viền đậm cho ô đang active,
 *   viền màu nhấn đỏ nếu ô đó bị chấm sai.
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
            modifier = Modifier.horizontalScroll(rememberScrollState()),
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
                            .size(56.dp)
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

                            // Nếu là chữ nhỏ (ゃ, ゅ, ょ, っ...), vẽ khung gợi ý chữ nhỏ bên trong ô 原稿用紙
                            if (slot.isSmallKana) {
                                val inset = size.width * 0.16f
                                drawRect(
                                    color = outlineColor,
                                    topLeft = Offset(inset, inset),
                                    size = Size(size.width - inset * 2f, size.height - inset * 2f),
                                    style = ComposeStroke(
                                        width = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                                    )
                                )
                            }

                            val strokeW = size.minDimension * 0.065f
                            if (slot.isPreFilledUnlearned) {
                                for (tStroke in slot.templateStrokes) {
                                    drawSmoothStrokePath(
                                        points = tStroke.points,
                                        progress = 1f,
                                        color = grayStrokeColor,
                                        strokeWidth = strokeW
                                    )
                                }
                            } else {
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

                    val smallSuffix = if (slot.isSmallKana) " (nhỏ)" else ""
                    val caption = when {
                        slot.isPreFilledUnlearned -> "Hiện sẵn"
                        isSelected -> "${slot.userStrokes.size}/${slot.expectedStrokeCount} nét$smallSuffix"
                        slot.userStrokes.isNotEmpty() -> "Đã viết$smallSuffix"
                        else -> "Ô ${slot.index + 1}$smallSuffix"
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
