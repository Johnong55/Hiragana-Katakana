package vn.kana.app.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import vn.kana.recognizer.Point2D
import vn.kana.recognizer.Stroke
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Ô vuông vẽ chữ Kana tỉ lệ 1:1:
 * - Tự động lấy kích thước vuông 1:1 lớn nhất vừa khít chiều rộng (và chiều cao khả dụng),
 *   giúp màn hình luyện viết không bao giờ phải cuộn dọc gây kẹt cảm ứng.
 * - Đường chữ thập mờ và viền mảnh 1dp.
 * - Compose Canvas + pointerInput thu thập (x, y, timestampMs) chuẩn hoá về [0.0..1.0].
 * - Thu thập đầy đủ historical points (`change.historical`) và làm mượt bằng Quadratic Bezier.
 * - Hỗ trợ ngón tay (Touch) và bút cảm ứng (Stylus), chặn palm (palm rejection).
 * - Hiệu ứng rung lắc ngang nhẹ khi `shakeTrigger` tăng (trạng thái trả lời sai).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun KanaDrawingCanvas(
    userStrokes: List<Stroke>,
    onStrokeCompleted: (Stroke) -> Unit,
    onStrokeActivity: () -> Unit = {},
    templateStrokes: List<Stroke> = emptyList(),
    showGhostTemplate: Boolean = false,
    showTemplateComparison: Boolean = false,
    nextHintStrokeIndex: Int? = null,
    errorStrokeIndices: Set<Int> = emptySet(),
    shakeTrigger: Int = 0,
    successOverlayText: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    val inkColor = MaterialTheme.colorScheme.onSurface
    val borderColor = if (errorStrokeIndices.isNotEmpty()) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val crosshairColor = MaterialTheme.colorScheme.outline
    val ghostColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.42f)
    val comparisonTemplateColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f)
    val errorColor = MaterialTheme.colorScheme.error
    val badgeBgColor = MaterialTheme.colorScheme.surface

    var currentStrokePoints by remember { mutableStateOf<List<Point2D>>(emptyList()) }

    val latestOnStrokeCompleted by rememberUpdatedState(onStrokeCompleted)
    val latestOnStrokeActivity by rememberUpdatedState(onStrokeActivity)
    val latestEnabled by rememberUpdatedState(enabled)

    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger > 0) {
            val offsets = listOf(-18f, 18f, -12f, 12f, -6f, 6f, 0f)
            for (target in offsets) {
                shakeOffset.animateTo(target, animationSpec = tween(durationMillis = 42))
            }
        }
    }

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
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                .background(backgroundColor)
                .border(1.dp, borderColor)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        if (!latestEnabled) return@awaitEachGesture

                        val widthPx = size.width.toFloat().coerceAtLeast(1f)
                        val heightPx = size.height.toFloat().coerceAtLeast(1f)

                        var activePointerId: PointerId? = null
                        var activePointerType: PointerType = PointerType.Touch
                        val pointsBuffer = ArrayList<Point2D>(64)

                        fun addNormalizedPoint(pos: Offset, uptimeMillis: Long) {
                            val nx = (pos.x / widthPx).coerceIn(0f, 1f)
                            val ny = (pos.y / heightPx).coerceIn(0f, 1f)
                            val prev = pointsBuffer.lastOrNull()
                            if (prev == null || hypot(nx - prev.x, ny - prev.y) >= 0.0015f) {
                                pointsBuffer.add(Point2D(nx, ny, uptimeMillis))
                            }
                        }

                        while (true) {
                            // Bắt sự kiện ở Initial pass để không bao giờ bị thành phần cha (nếu có) cướp cử chỉ kéo
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (!latestEnabled) {
                                currentStrokePoints = emptyList()
                                break
                            }

                            val stylusChange = event.changes.firstOrNull {
                                it.type == PointerType.Stylus && it.pressed
                            }
                            if (stylusChange != null && activePointerType != PointerType.Stylus) {
                                activePointerId = stylusChange.id
                                activePointerType = PointerType.Stylus
                                pointsBuffer.clear()
                            }

                            if (activePointerId == null) {
                                val pressedChanges = event.changes.filter { it.pressed }
                                if (pressedChanges.size >= 3 && stylusChange == null) {
                                    pressedChanges.forEach { it.consume() }
                                    continue
                                }
                                val firstChange = stylusChange ?: pressedChanges.firstOrNull()
                                if (firstChange != null) {
                                    activePointerId = firstChange.id
                                    activePointerType = firstChange.type
                                    addNormalizedPoint(firstChange.position, firstChange.uptimeMillis)
                                    firstChange.consume()
                                    currentStrokePoints = pointsBuffer.toList()
                                    latestOnStrokeActivity()
                                }
                                if (pressedChanges.isEmpty()) break
                                continue
                            }

                            val trackedChange = event.changes.firstOrNull { it.id == activePointerId }
                            event.changes.forEach { change ->
                                if (change.id != activePointerId) {
                                    change.consume()
                                }
                            }

                            if (trackedChange == null) {
                                break
                            }

                            if (trackedChange.pressed) {
                                for (hist in trackedChange.historical) {
                                    addNormalizedPoint(hist.position, hist.uptimeMillis)
                                }
                                addNormalizedPoint(trackedChange.position, trackedChange.uptimeMillis)
                                trackedChange.consume()
                                currentStrokePoints = pointsBuffer.toList()
                                latestOnStrokeActivity()
                            } else {
                                for (hist in trackedChange.historical) {
                                    addNormalizedPoint(hist.position, hist.uptimeMillis)
                                }
                                addNormalizedPoint(trackedChange.position, trackedChange.uptimeMillis)
                                trackedChange.consume()
                                val finishedPoints = pointsBuffer.toList()
                                currentStrokePoints = emptyList()
                                latestOnStrokeActivity()
                                if (finishedPoints.size >= 2 &&
                                    computeNormalizedPathLength(finishedPoints) >= MIN_TAP_FILTER_LENGTH
                                ) {
                                    latestOnStrokeCompleted(Stroke(finishedPoints))
                                }
                                break
                            }
                        }
                    }
                }
        ) {
            val canvasSize = size.minDimension
            val strokeWidthPx = canvasSize * 0.055f

            // 1. Đường chữ thập (+) mờ
            drawCrosshair(crosshairColor)

            // 2. Nét mẫu mờ (Bước 2: Đồ theo)
            if (showGhostTemplate && templateStrokes.isNotEmpty()) {
                for (tStroke in templateStrokes) {
                    drawSmoothStrokePath(
                        points = tStroke.points,
                        progress = 1f,
                        color = ghostColor,
                        strokeWidth = strokeWidthPx
                    )
                }
            }

            // 3. Chế độ so sánh nét chuẩn trong màn Debug (vẽ nét chuẩn bằng nét đứt + số thứ tự nét)
            if (showTemplateComparison && templateStrokes.isNotEmpty()) {
                textPaint.textSize = canvasSize * 0.036f
                for ((idx, tStroke) in templateStrokes.withIndex()) {
                    drawDashedTemplateStroke(
                        points = tStroke.points,
                        color = comparisonTemplateColor,
                        strokeWidth = strokeWidthPx * 0.65f
                    )
                    if (tStroke.points.isNotEmpty()) {
                        drawStrokeNumberBadge(
                            strokeNumber = idx + 1,
                            points = tStroke.points,
                            isCurrent = false,
                            badgeBgColor = badgeBgColor,
                            badgeBorderColor = comparisonTemplateColor,
                            inkColor = comparisonTemplateColor,
                            textPaint = textPaint
                        )
                    }
                }
            }

            // 4. Bước 3 (Viết có gợi ý): chỉ hiện số thứ tự và điểm bắt đầu + mũi tên ngắn của nét kế tiếp
            if (nextHintStrokeIndex != null && nextHintStrokeIndex in templateStrokes.indices) {
                val hintStroke = templateStrokes[nextHintStrokeIndex]
                if (hintStroke.points.isNotEmpty()) {
                    textPaint.textSize = canvasSize * 0.038f
                    val startPt = Offset(
                        hintStroke.points[0].x * size.width,
                        hintStroke.points[0].y * size.height
                    )
                    drawCircle(
                        color = inkColor.copy(alpha = 0.25f),
                        radius = strokeWidthPx * 0.65f,
                        center = startPt
                    )
                    drawStartDirectionHint(
                        points = hintStroke.points,
                        color = inkColor
                    )
                    drawStrokeNumberBadge(
                        strokeNumber = nextHintStrokeIndex + 1,
                        points = hintStroke.points,
                        isCurrent = true,
                        badgeBgColor = badgeBgColor,
                        badgeBorderColor = inkColor,
                        inkColor = inkColor,
                        textPaint = textPaint
                    )
                }
            }

            // 5. Các nét người dùng đã vẽ
            for ((index, uStroke) in userStrokes.withIndex()) {
                val isErrorStroke = errorStrokeIndices.contains(index)
                val strokeColor = if (isErrorStroke) errorColor else inkColor
                drawSmoothStrokePath(
                    points = uStroke.points,
                    progress = 1f,
                    color = strokeColor,
                    strokeWidth = strokeWidthPx
                )
            }

            // 6. Nét đang vẽ dưới ngón tay / bút
            if (currentStrokePoints.isNotEmpty()) {
                drawSmoothStrokePath(
                    points = currentStrokePoints,
                    progress = 1f,
                    color = inkColor,
                    strokeWidth = strokeWidthPx
                )
            }
        }

        // 7. Hiệu ứng thông báo ĐÚNG (✓ CHÍNH XÁC!) nổi bật giữa ô vẽ khi người dùng vượt qua
        if (successOverlayText != null) {
            val scaleAnim = remember(successOverlayText) { Animatable(0.72f) }
            LaunchedEffect(successOverlayText) {
                scaleAnim.animateTo(1.0f, animationSpec = tween(durationMillis = 160))
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(squareSide)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .scale(scaleAnim.value)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(2.dp, MaterialTheme.colorScheme.onBackground)
                        .padding(horizontal = 28.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = "✓ HAY BẠN!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = successOverlayText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

private const val MIN_TAP_FILTER_LENGTH = 0.015f

private fun computeNormalizedPathLength(points: List<Point2D>): Float {
    var sum = 0f
    for (i in 1 until points.size) {
        sum += hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
    }
    return sum
}

private fun DrawScope.drawCrosshair(color: Color) {
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

private fun DrawScope.drawDashedTemplateStroke(
    points: List<Point2D>,
    color: Color,
    strokeWidth: Float
) {
    if (points.size < 2) return
    val w = size.width
    val h = size.height
    val scaled = points.map { Offset(it.x * w, it.y * h) }
    val path = Path().apply {
        moveTo(scaled[0].x, scaled[0].y)
        for (i in 1 until scaled.size - 1) {
            val ctrl = scaled[i]
            val next = scaled[i + 1]
            quadraticTo(ctrl.x, ctrl.y, (ctrl.x + next.x) / 2f, (ctrl.y + next.y) / 2f)
        }
        lineTo(scaled.last().x, scaled.last().y)
    }
    drawPath(
        path = path,
        color = color,
        style = ComposeStroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
        )
    )
}
