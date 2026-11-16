package vn.kana.recognizer

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Tính khoảng cách Discrete Fréchet giữa hai polyline P và Q.
 * Sử dụng mảng 1 chiều phẳng để tối ưu tốc độ (< 0.01ms cho 32x32 điểm trên JVM/Android).
 */
object DiscreteFrechet {

    fun distance(p: List<Point2D>, q: List<Point2D>): Float {
        val n = p.size
        val m = q.size
        if (n == 0 || m == 0) return Float.MAX_VALUE

        val dp = FloatArray(n * m)
        for (i in 0 until n) {
            val pi = p[i]
            val rowOffset = i * m
            for (j in 0 until m) {
                val qj = q[j]
                val d = hypot(pi.x - qj.x, pi.y - qj.y)
                dp[rowOffset + j] = when {
                    i == 0 && j == 0 -> d
                    i == 0 -> max(dp[j - 1], d)
                    j == 0 -> max(dp[(i - 1) * m], d)
                    else -> {
                        val prevMin = min(
                            min(dp[(i - 1) * m + j], dp[rowOffset + j - 1]),
                            dp[(i - 1) * m + j - 1]
                        )
                        max(prevMin, d)
                    }
                }
            }
        }
        return dp[n * m - 1]
    }
}
