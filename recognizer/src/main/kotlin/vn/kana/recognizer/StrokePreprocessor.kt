package vn.kana.recognizer

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Dữ liệu của một nét sau khi đã resample, làm mượt và chuẩn hoá theo bounding box toàn chữ.
 */
data class NormalizedStroke(
    /** 32 điểm trong không gian chuẩn hoá toàn chữ (tâm chữ tại 0.5, 0.5, giữ nguyên tỉ lệ) */
    val points: List<Point2D>,
    /** 32 điểm dịch chuyển về tâm của riêng nét đó (0, 0) để đánh giá hình dáng độc lập với vị trí */
    val centeredPoints: List<Point2D>,
    /** Trọng tâm của nét trong không gian chuẩn hoá toàn chữ */
    val centroid: Point2D,
    /** Độ dài cung của nét trong không gian chuẩn hoá toàn chữ */
    val arcLength: Float,
    /** Tỉ trọng độ dài của nét này so với tổng độ dài mọi nét trong chữ (0..1) */
    val lengthFraction: Float,
    /** Vector từ điểm đầu tới điểm cuối của nét */
    val startToEndDx: Float,
    val startToEndDy: Float,
    val startToEndLen: Float,
    /** Danh sách N-1 vector tiếp tuyến đơn vị dọc theo nét */
    val tangents: List<Point2D>
)

/**
 * Dữ liệu toàn chữ đã chuẩn hoá theo bounding box toàn chữ, giữ nguyên tỉ lệ khung (aspect ratio).
 */
data class NormalizedCharacter(
    val strokes: List<NormalizedStroke>,
    val originalSpan: Float,
    val widthToSpanRatio: Float,
    val heightToSpanRatio: Float
)

object StrokePreprocessor {

    /**
     * Tính độ dài đường đi của một chuỗi điểm.
     */
    fun polylineLength(points: List<Point2D>): Float {
        if (points.size < 2) return 0f
        var sum = 0f
        for (i in 1 until points.size) {
            sum += hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
        }
        return sum
    }

    /**
     * Bước 1a: Lọc bỏ các nét chấm vô tình quá ngắn.
     */
    fun filterShortStrokes(strokes: List<Stroke>, minLength: Float): List<Stroke> {
        return strokes.filter { stroke ->
            stroke.points.size >= 2 && polylineLength(stroke.points) >= minLength
        }
    }

    /**
     * Bước 1b: Resample một nét thành đúng [numPoints] điểm cách đều nhau theo độ dài cung.
     */
    fun resampleStroke(stroke: Stroke, numPoints: Int = 32): Stroke {
        val pts = stroke.points
        if (pts.isEmpty()) {
            return Stroke(List(numPoints) { Point2D(0.5f, 0.5f) })
        }
        if (pts.size == 1) {
            return Stroke(List(numPoints) { pts[0] })
        }

        val cumDist = FloatArray(pts.size)
        var totalLen = 0f
        for (i in 1 until pts.size) {
            totalLen += hypot(pts[i].x - pts[i - 1].x, pts[i].y - pts[i - 1].y)
            cumDist[i] = totalLen
        }

        if (totalLen < 1e-6f) {
            return Stroke(List(numPoints) { pts[0] })
        }

        val result = ArrayList<Point2D>(numPoints)
        result.add(pts.first())
        val step = totalLen / (numPoints - 1)
        var segIdx = 0

        for (k in 1 until numPoints - 1) {
            val target = k * step
            while (segIdx < pts.size - 2 && cumDist[segIdx + 1] < target) {
                segIdx++
            }
            val segStart = cumDist[segIdx]
            val segEnd = cumDist[segIdx + 1]
            val segLen = segEnd - segStart
            if (segLen < 1e-7f) {
                result.add(pts[segIdx])
            } else {
                val ratio = (target - segStart) / segLen
                val p0 = pts[segIdx]
                val p1 = pts[segIdx + 1]
                val x = p0.x + ratio * (p1.x - p0.x)
                val y = p0.y + ratio * (p1.y - p0.y)
                val t = p0.timestampMs + ((p1.timestampMs - p0.timestampMs) * ratio).toLong()
                result.add(Point2D(x, y, t))
            }
        }
        result.add(pts.last())
        return Stroke(result)
    }

    /**
     * Bước 1c: Làm mượt nhẹ bằng trung bình trượt có trọng số [0.2, 0.6, 0.2], giữ nguyên điểm đầu và cuối.
     */
    fun smoothStroke(stroke: Stroke): Stroke {
        val pts = stroke.points
        if (pts.size < 3) return stroke
        val smoothed = ArrayList<Point2D>(pts.size)
        smoothed.add(pts.first())
        for (i in 1 until pts.size - 1) {
            val prev = pts[i - 1]
            val curr = pts[i]
            val next = pts[i + 1]
            smoothed.add(
                Point2D(
                    x = 0.20f * prev.x + 0.60f * curr.x + 0.20f * next.x,
                    y = 0.20f * prev.y + 0.60f * curr.y + 0.20f * next.y,
                    timestampMs = curr.timestampMs
                )
            )
        }
        smoothed.add(pts.last())
        return Stroke(smoothed)
    }

    /**
     * Bước 2: Chuẩn hoá TOÀN CHỮ theo bounding box của tất cả các nét, giữ nguyên tỉ lệ (aspect ratio).
     * Chữ mẫu và chữ người dùng đều đi qua đúng hàm chuẩn hoá này.
     */
    fun normalizeCharacter(
        rawStrokes: List<Stroke>,
        resamplePoints: Int = 32,
        applySmoothing: Boolean = true
    ): NormalizedCharacter {
        if (rawStrokes.isEmpty()) {
            return NormalizedCharacter(
                strokes = emptyList(),
                originalSpan = 0f,
                widthToSpanRatio = 1f,
                heightToSpanRatio = 1f
            )
        }

        // Tiền xử lý từng nét: làm mượt nhẹ + resample thành 32 điểm cách đều
        val processedStrokes = rawStrokes.map { s ->
            val resampled = resampleStroke(s, resamplePoints)
            if (applySmoothing) {
                resampleStroke(smoothStroke(resampled), resamplePoints)
            } else {
                resampled
            }
        }

        // Tính bounding box toàn chữ
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY

        for (stroke in processedStrokes) {
            for (pt in stroke.points) {
                minX = min(minX, pt.x)
                maxX = max(maxX, pt.x)
                minY = min(minY, pt.y)
                maxY = max(maxY, pt.y)
            }
        }

        val width = (maxX - minX).coerceAtLeast(1e-4f)
        val height = (maxY - minY).coerceAtLeast(1e-4f)
        val rawSpan = max(width, height)
        val effectiveSpan = rawSpan.coerceAtLeast(0.08f)
        val centerX = (minX + maxX) * 0.5f
        val centerY = (minY + maxY) * 0.5f

        // Chuẩn hoá toàn bộ điểm về hộp tâm (0.5, 0.5), kích thước chuẩn hoá 1.0 giữ nguyên tỉ lệ
        val scaledPointsPerStroke = processedStrokes.map { stroke ->
            stroke.points.map { pt ->
                Point2D(
                    x = 0.5f + (pt.x - centerX) / effectiveSpan,
                    y = 0.5f + (pt.y - centerY) / effectiveSpan,
                    timestampMs = pt.timestampMs
                )
            }
        }

        val strokeLengths = scaledPointsPerStroke.map { polylineLength(it).coerceAtLeast(1e-4f) }
        val totalCharLength = strokeLengths.sum().coerceAtLeast(1e-4f)

        val normalizedStrokes = scaledPointsPerStroke.mapIndexed { idx, pts ->
            val cx = pts.map { it.x }.average().toFloat()
            val cy = pts.map { it.y }.average().toFloat()
            val centroid = Point2D(cx, cy)

            val centered = pts.map { pt ->
                Point2D(pt.x - cx, pt.y - cy, pt.timestampMs)
            }

            val first = pts.first()
            val last = pts.last()
            val dx = last.x - first.x
            val dy = last.y - first.y
            val startEndLen = hypot(dx, dy)

            val tangents = ArrayList<Point2D>((pts.size - 1).coerceAtLeast(0))
            for (i in 0 until pts.size - 1) {
                val segDx = pts[i + 1].x - pts[i].x
                val segDy = pts[i + 1].y - pts[i].y
                val segLen = hypot(segDx, segDy)
                if (segLen > 1e-6f) {
                    tangents.add(Point2D(segDx / segLen, segDy / segLen))
                } else {
                    tangents.add(Point2D(0f, 0f))
                }
            }

            NormalizedStroke(
                points = pts,
                centeredPoints = centered,
                centroid = centroid,
                arcLength = strokeLengths[idx],
                lengthFraction = strokeLengths[idx] / totalCharLength,
                startToEndDx = dx,
                startToEndDy = dy,
                startToEndLen = startEndLen,
                tangents = tangents
            )
        }

        return NormalizedCharacter(
            strokes = normalizedStrokes,
            originalSpan = rawSpan,
            widthToSpanRatio = width / effectiveSpan,
            heightToSpanRatio = height / effectiveSpan
        )
    }
}
