package vn.kana.recognizer

/**
 * Ba mức độ nghiêm của bộ chấm điểm chữ viết tay.
 * Mặc định ở mức EASY / STANDARD đã được nới lỏng để viết bằng ngón tay trên điện thoại thoải mái, tự nhiên.
 */
enum class StrictnessLevel(val displayNameVi: String) {
    EASY("Dễ (Khuyên dùng)"),
    STANDARD("Chuẩn"),
    STRICT("Nghiêm")
}

/**
 * Toàn bộ ngưỡng cấu hình của bộ chấm điểm đặt tập trung tại một nơi duy nhất.
 */
data class RecognizerConfig(
    val strictness: StrictnessLevel = StrictnessLevel.STANDARD,
    /** Số điểm chuẩn hoá trên mỗi nét */
    val resamplePointsCount: Int = 32,
    /** Ngưỡng lọc bỏ nét chấm vô tình (tính theo tỉ lệ đường đi trên khung canvas 0..1) */
    val minStrokeLengthCanvasRatio: Float = 0.030f,
    /** Kích thước bounding box tối thiểu của toàn chữ trên canvas (0..1) */
    val minCharacterBoundingBoxSpan: Float = 0.08f,
    /** Tỉ lệ chiều rộng/chiều cao lệch tối đa so với chữ mẫu */
    val maxAspectRatioDeviation: Float = 0.75f,
    /** Ngưỡng điểm tổng tối thiểu (0..100) để được tính là Đạt (passed) */
    val passScoreThreshold: Int = 56,
    /** Ngưỡng điểm tối thiểu cho từng nét đơn lẻ */
    val minPerStrokeScore: Int = 42,
    /** Khoảng cách điểm-điểm trung bình tối đa (trong không gian chuẩn hoá toàn chữ) */
    val maxPointToPointDistance: Float = 0.34f,
    /** Khoảng cách Discrete Fréchet tối đa (trong không gian chuẩn hoá toàn chữ) */
    val maxFrechetDistance: Float = 0.54f,
    /** Độ lệch góc vector hướng tối đa (độ) trước khi báo WRONG_DIRECTION */
    val maxDirectionAngleDeg: Float = 75.0f,
    /** Mức cosine similarity tối thiểu của vector tiếp tuyến dọc theo nét */
    val minTangentCosineSimilarity: Float = 0.18f,
    /** Độ lệch tâm nét tối đa (trong không gian chuẩn hoá toàn chữ) trước khi báo WRONG_POSITION */
    val maxStrokeCentroidOffset: Float = 0.36f,
    /** Biên độ cải thiện khoảng cách khi đối chiếu chéo để phát hiện viết sai thứ tự nét (WRONG_ORDER) */
    val orderSwapDistanceRatio: Float = 0.62f,
    /** Hệ số phạt quy đổi tổng khoảng cách sai lệch sang thang điểm 0..100 */
    val scorePenaltyMultiplier: Float = 95.0f
) {
    companion object {
        fun forLevel(level: StrictnessLevel): RecognizerConfig = when (level) {
            StrictnessLevel.EASY -> RecognizerConfig(
                strictness = StrictnessLevel.EASY,
                passScoreThreshold = 48,
                minPerStrokeScore = 35,
                maxPointToPointDistance = 0.40f,
                maxFrechetDistance = 0.62f,
                maxDirectionAngleDeg = 85.0f,
                minTangentCosineSimilarity = 0.08f,
                maxStrokeCentroidOffset = 0.42f,
                scorePenaltyMultiplier = 80.0f
            )

            StrictnessLevel.STANDARD -> RecognizerConfig(
                strictness = StrictnessLevel.STANDARD,
                passScoreThreshold = 56,
                minPerStrokeScore = 42,
                maxPointToPointDistance = 0.34f,
                maxFrechetDistance = 0.54f,
                maxDirectionAngleDeg = 75.0f,
                minTangentCosineSimilarity = 0.18f,
                maxStrokeCentroidOffset = 0.36f,
                scorePenaltyMultiplier = 95.0f
            )

            StrictnessLevel.STRICT -> RecognizerConfig(
                strictness = StrictnessLevel.STRICT,
                passScoreThreshold = 70,
                minPerStrokeScore = 58,
                maxPointToPointDistance = 0.24f,
                maxFrechetDistance = 0.38f,
                maxDirectionAngleDeg = 55.0f,
                minTangentCosineSimilarity = 0.45f,
                maxStrokeCentroidOffset = 0.26f,
                scorePenaltyMultiplier = 125.0f
            )
        }
    }
}
