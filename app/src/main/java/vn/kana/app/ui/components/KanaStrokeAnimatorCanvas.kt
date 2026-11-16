package vn.kana.app.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as ComposeStroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import vn.kana.recognizer.Point2D
import vn.kana.recognizer.Stroke
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Ô vuông 1:1 hiển thị và animate từng nét chữ Kana từ dữ liệu KanjiVG.
 * - Chiếm tối đa chiều rộng, tỉ lệ 1:1.
 * - Viền mảnh 1dp, đường chữ thập (+) mờ chia 4 góc phần tư.
 * - Vẽ dần từng nét bằng đường cong mượt Quadratic Bezier, hiển thị số thứ tự nét và mũi tên hướng viết.
 */
@Composable
fun KanaStrokeAnimatorCanvas(
    strokes: List<Stroke>,
    activeStrokeIndex: Int,
    activeStrokeProgress: Float,
    showGhostStrokes: Boolean = true,
    showStrokeNumbers: Boolean = true,
    showDirectionArrows: Boolean = true,
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    val inkColor = MaterialTheme.colorScheme.onSurface
    val borderColor = MaterialTheme.colorScheme.onSurface
    val crosshairColor = MaterialTheme.colorScheme.outline
    val ghostStrokeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val badgeBgColor = MaterialTheme.colorScheme.surface
    val badgeBorderColor = MaterialTheme.colorScheme.onSurface

    val textPaint = remember(inkColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor.toArgb()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        val squareSide = if (maxHeight.value.isInfinite()) {
            maxWidth
        } else {
            min(maxWidth, maxHeight)
        }
        Canvas(
            modifier = Modifier
                .size(squareSide)
                .background(backgroundColor)
                .border(1.dp, borderColor)
        ) {
            val canvasSize = size.minDimension
            val strokeWidthPx = canvasSize * 0.055f

            // 1. Đường chữ thập mờ ở giữa ô
            drawCrosshairGrid(crosshairColor)

            // 2. Nét mờ (ghost strokes) làm khung tham chiếu
            if (showGhostStrokes) {
                for (stroke in strokes) {
                    drawSmoothStrokePath(
                        points = stroke.points,
                        progress = 1f,
                        color = ghostStrokeColor,
                        strokeWidth = strokeWidthPx
                    )
                }
            }

            // 3. Các nét đã vẽ hoàn tất (trước activeStrokeIndex)
            val completedCount = activeStrokeIndex.coerceAtMost(strokes.size)
            for (i in 0 until completedCount) {
                drawSmoothStrokePath(
                    points = strokes[i].points,
                    progress = 1f,
                    color = inkColor,
                    strokeWidth = strokeWidthPx
                )
            }

            // 4. Nét đang được animate (activeStrokeIndex)
            if (activeStrokeIndex in strokes.indices) {
                val activeStroke = strokes[activeStrokeIndex]
                val clampedProgress = activeStrokeProgress.coerceIn(0f, 1f)
                val tipAndAngle = drawSmoothStrokePath(
                    points = activeStroke.points,
                    progress = clampedProgress,
                    color = inkColor,
                    strokeWidth = strokeWidthPx
                )
                if (showDirectionArrows && tipAndAngle != null && clampedProgress > 0.03f) {
                    drawDirectionArrowhead(
                        tip = tipAndAngle.first,
                        angleRad = tipAndAngle.second,
                        arrowSize = strokeWidthPx * 1.15f,
                        color = inkColor
                    )
                }
            }

            // 5. Mũi tên hướng khởi đầu + Số thứ tự nét (1, 2, 3...)
            if (showStrokeNumbers) {
                textPaint.textSize = canvasSize * 0.038f
                for ((index, stroke) in strokes.withIndex()) {
                    if (stroke.points.isEmpty()) continue
                    if (showDirectionArrows && index >= activeStrokeIndex) {
                        drawStartDirectionHint(
                            points = stroke.points,
                            color = inkColor.copy(alpha = if (index == activeStrokeIndex) 0.9f else 0.45f)
                        )
                    }
                    drawStrokeNumberBadge(
                        strokeNumber = index + 1,
                        points = stroke.points,
                        isCurrent = index == activeStrokeIndex,
                        badgeBgColor = badgeBgColor,
                        badgeBorderColor = badgeBorderColor,
                        inkColor = inkColor,
                        textPaint = textPaint
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawCrosshairGrid(color: Color) {
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    val midX = size.width / 2f
    val midY = size.height / 2f

    drawLine(
        color = color,
        start = Offset(midX, 0f),
        end = Offset(midX, size.height),
        strokeWidth = 1.dp.toPx(),
        pathEffect = dashEffect
    )
    drawLine(
        color = color,
        start = Offset(0f, midY),
        end = Offset(size.width, midY),
        strokeWidth = 1.dp.toPx(),
        pathEffect = dashEffect
    )
}

/**
 * Vẽ một nét từ danh sách điểm chuẩn hoá [0..1] theo phần trăm [progress] (0..1)
 * sử dụng nội suy mượt Quadratic Bezier.
 * @return Cặp (Toạ độ đầu mút hiện tại, Góc hướng tiếp tuyến radian) để vẽ mũi tên.
 */
internal fun DrawScope.drawSmoothStrokePath(
    points: List<Point2D>,
    progress: Float,
    color: Color,
    strokeWidth: Float
): Pair<Offset, Float>? {
    if (points.isEmpty() || progress <= 0f) return null
    val w = size.width
    val h = size.height

    val scaled = points.map { Offset(it.x * w, it.y * h) }
    if (scaled.size == 1) {
        drawCircle(color = color, radius = strokeWidth / 2f, center = scaled[0])
        return scaled[0] to 0f
    }

    val totalSegments = scaled.size - 1
    val exactPos = (progress.coerceIn(0f, 1f)) * totalSegments
    val fullSegments = exactPos.toInt().coerceAtMost(totalSegments)
    val remainder = exactPos - fullSegments

    val subPoints = ArrayList<Offset>(fullSegments + 2)
    for (i in 0..fullSegments) {
        subPoints.add(scaled[i])
    }
    if (fullSegments < totalSegments && remainder > 0.001f) {
        val a = scaled[fullSegments]
        val b = scaled[fullSegments + 1]
        subPoints.add(
            Offset(
                x = a.x + (b.x - a.x) * remainder,
                y = a.y + (b.y - a.y) * remainder
            )
        )
    }

    if (subPoints.size < 2) {
        drawCircle(color = color, radius = strokeWidth / 2f, center = subPoints[0])
        val nextPt = scaled.getOrElse(1) { subPoints[0] }
        val angle = atan2(nextPt.y - subPoints[0].y, nextPt.x - subPoints[0].x)
        return subPoints[0] to angle
    }

    val path = Path().apply {
        moveTo(subPoints[0].x, subPoints[0].y)
        if (subPoints.size == 2) {
            lineTo(subPoints[1].x, subPoints[1].y)
        } else {
            for (i in 1 until subPoints.size - 1) {
                val ctrl = subPoints[i]
                val next = subPoints[i + 1]
                val midX = (ctrl.x + next.x) / 2f
                val midY = (ctrl.y + next.y) / 2f
                quadraticTo(ctrl.x, ctrl.y, midX, midY)
            }
            val last = subPoints.last()
            lineTo(last.x, last.y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = ComposeStroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    val tip = subPoints.last()
    val prevIdx = (subPoints.size - 3).coerceAtLeast(0)
    val prev = subPoints[prevIdx]
    val angle = atan2(tip.y - prev.y, tip.x - prev.x)
    return tip to angle
}

internal fun DrawScope.drawDirectionArrowhead(
    tip: Offset,
    angleRad: Float,
    arrowSize: Float,
    color: Color
) {
    val wingAngle = 0.55f // ~31.5 degrees
    val forwardTip = Offset(
        x = tip.x + cos(angleRad) * (arrowSize * 0.25f),
        y = tip.y + sin(angleRad) * (arrowSize * 0.25f)
    )
    val leftWing = Offset(
        x = forwardTip.x - cos(angleRad - wingAngle) * arrowSize,
        y = forwardTip.y - sin(angleRad - wingAngle) * arrowSize
    )
    val rightWing = Offset(
        x = forwardTip.x - cos(angleRad + wingAngle) * arrowSize,
        y = forwardTip.y - sin(angleRad + wingAngle) * arrowSize
    )

    val arrowPath = Path().apply {
        moveTo(forwardTip.x, forwardTip.y)
        lineTo(leftWing.x, leftWing.y)
        lineTo(rightWing.x, rightWing.y)
        close()
    }
    drawPath(path = arrowPath, color = color)
}

internal fun DrawScope.drawStartDirectionHint(
    points: List<Point2D>,
    color: Color
) {
    if (points.size < 4) return
    val w = size.width
    val h = size.height
    val p0 = Offset(points[0].x * w, points[0].y * h)
    val p3 = Offset(points[3].x * w, points[3].y * h)
    val angle = atan2(p3.y - p0.y, p3.x - p0.x)
    val length = size.minDimension * 0.055f
    val end = Offset(
        x = p0.x + cos(angle) * length,
        y = p0.y + sin(angle) * length
    )
    drawLine(
        color = color,
        start = p0,
        end = end,
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawDirectionArrowhead(
        tip = end,
        angleRad = angle,
        arrowSize = size.minDimension * 0.024f,
        color = color
    )
}

internal fun DrawScope.drawStrokeNumberBadge(
    strokeNumber: Int,
    points: List<Point2D>,
    isCurrent: Boolean,
    badgeBgColor: Color,
    badgeBorderColor: Color,
    inkColor: Color,
    textPaint: Paint
) {
    val w = size.width
    val h = size.height
    val start = Offset(points[0].x * w, points[0].y * h)
    val lookAhead = points.getOrElse(3) { points.last() }
    val dirX = lookAhead.x * w - start.x
    val dirY = lookAhead.y * h - start.y
    val len = hypot(dirX, dirY)

    val offsetDist = size.minDimension * 0.052f
    // Đặt số thứ tự nét lệch nhẹ về phía ngược hướng xuất phát để không đè lên điểm đặt bút
    val badgeCenter = if (len > 1f) {
        Offset(
            x = (start.x - (dirX / len) * offsetDist).coerceIn(offsetDist, w - offsetDist),
            y = (start.y - (dirY / len) * offsetDist).coerceIn(offsetDist, h - offsetDist)
        )
    } else {
        Offset(
            x = (start.x - offsetDist).coerceIn(offsetDist, w - offsetDist),
            y = (start.y - offsetDist).coerceIn(offsetDist, h - offsetDist)
        )
    }

    val radius = size.minDimension * 0.030f
    drawCircle(
        color = if (isCurrent) inkColor else badgeBgColor,
        radius = radius,
        center = badgeCenter
    )
    drawCircle(
        color = badgeBorderColor,
        radius = radius,
        center = badgeCenter,
        style = ComposeStroke(width = 1.2.dp.toPx())
    )

    val prevColor = textPaint.color
    textPaint.color = if (isCurrent) badgeBgColor.toArgb() else inkColor.toArgb()
    val fontMetrics = textPaint.fontMetrics
    val textBaseline = badgeCenter.y - (fontMetrics.ascent + fontMetrics.descent) / 2f
    drawContext.canvas.nativeCanvas.drawText(
        strokeNumber.toString(),
        badgeCenter.x,
        textBaseline,
        textPaint
    )
    textPaint.color = prevColor
}
