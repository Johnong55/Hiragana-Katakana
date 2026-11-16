package vn.kana.recognizer

import kotlinx.serialization.Serializable

/**
 * Các loại lỗi nhận dạng nét chữ theo đặc tả.
 */
@Serializable
enum class RecognitionError(val messageVi: String) {
    WRONG_STROKE_COUNT("Sai số lượng nét"),
    WRONG_ORDER("Sai thứ tự nét"),
    WRONG_DIRECTION("Sai hướng viết nét"),
    WRONG_SHAPE("Hình dáng nét chưa đúng"),
    WRONG_POSITION("Vị trí nét bị lệch")
}

/**
 * Kết quả chấm chi tiết cho từng nét thứ [strokeIndex] (0-based).
 */
@Serializable
data class StrokeEvaluation(
    val strokeIndex: Int,
    val score: Int,
    val avgPointDistance: Float,
    val frechetDistance: Float,
    val directionAngleDiffDeg: Float,
    val tangentSimilarity: Float,
    val centroidOffset: Float,
    val matchedTemplateStrokeIndex: Int,
    val errors: List<RecognitionError>
)

/**
 * Kết quả chấm điểm tổng thể cho một chữ Kana.
 */
@Serializable
data class RecognitionResult(
    val targetChar: String,
    val passed: Boolean,
    val score: Int,
    val perStroke: List<StrokeEvaluation>,
    val errors: List<RecognitionError>,
    val bestMatchChar: String?,
    val bestMatchScore: Int = 0,
    val secondBestChar: String? = null,
    val secondBestScore: Int = 0,
    val elapsedTimeMs: Long = 0L
)
