package vn.kana.recognizer

/**
 * Ba mức độ nghiêm của bộ chấm điểm chữ viết tay.
 */
enum class StrictnessLevel(val displayNameVi: String) {
    EASY("Dễ"),
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
    val minStrokeLengthCanvasRatio: Float = 0.035f,
    /** Kích thước bounding box tối thiểu của toàn chữ trên canvas (0..1) */
    val minCharacterBoundingBoxSpan: Float = 0.10f,
    /** Tỉ lệ chiều rộng/chiều cao lệch tối đa so với chữ mẫu (cho các chữ không suy biến) */
    val maxAspectRatioDeviation: Float = 0.55f,
    /** Ngưỡng điểm tổng tối thiểu (0..100) để được tính là Đạt (passed) */
    val passScoreThreshold: Int = 70,
    /** Ngưỡng điểm tối thiểu cho từng nét đơn lẻ */
    val minPerStrokeScore: Int = 58,
    /** Khoảng cách điểm-điểm trung bình tối đa (trong không gian chuẩn hoá toàn chữ) */
    val maxPointToPointDistance: Float = 0.22f,
    /** Khoảng cách Discrete Fréchet tối đa (trong không gian chuẩn hoá toàn chữ) */
    val maxFrechetDistance: Float = 0.36f,
    /** Độ lệch góc vector hướng tối đa (độ) trước khi báo WRONG_DIRECTION */
    val maxDirectionAngleDeg: Float = 52.0f,
    /** Mức cosine similarity tối thiểu của vector tiếp tuyến dọc theo nét */
    val minTangentCosineSimilarity: Float = 0.52f,
    /** Độ lệch tâm nét tối đa (trong không gian chuẩn hoá toàn chữ) trước khi báo WRONG_POSITION */
    val maxStrokeCentroidOffset: Float = 0.24f,
    /** Biên độ cải thiện khoảng cách khi đối chiếu chéo để phát hiện viết sai thứ tự nét (WRONG_ORDER) */
    val orderSwapDistanceRatio: Float = 0.68f,
    /** Hệ số phạt quy đổi tổng khoảng cách sai lệch sang thang điểm 0..100 */
    val scorePenaltyMultiplier: Float = 135.0f
) {
    companion object {
        fun forLevel(level: StrictnessLevel): RecognizerConfig = when (level) {
            StrictnessLevel.EASY -> RecognizerConfig(
                strictness = StrictnessLevel.EASY,
                passScoreThreshold = 62,
                minPerStrokeScore = 50,
                maxPointToPointDistance = 0.27f,
                maxFrechetDistance = 0.43f,
                maxDirectionAngleDeg = 62.0f,
                minTangentCosineSimilarity = 0.42f,
                maxStrokeCentroidOffset = 0.29f,
                scorePenaltyMultiplier = 115.0f
            )

            StrictnessLevel.STANDARD -> RecognizerConfig(
                strictness = StrictnessLevel.STANDARD,
                passScoreThreshold = 70,
                minPerStrokeScore = 58,
                maxPointToPointDistance = 0.22f,
                maxFrechetDistance = 0.36f,
                maxDirectionAngleDeg = 52.0f,
                minTangentCosineSimilarity = 0.52f,
                maxStrokeCentroidOffset = 0.24f,
                scorePenaltyMultiplier = 135.0f
            )

            StrictnessLevel.STRICT -> RecognizerConfig(
                strictness = StrictnessLevel.STRICT,
                passScoreThreshold = 80,
                minPerStrokeScore = 70,
                maxPointToPointDistance = 0.16f,
                maxFrechetDistance = 0.28f,
                maxDirectionAngleDeg = 38.0f,
                minTangentCosineSimilarity = 0.66f,
                maxStrokeCentroidOffset = 0.18f,
                scorePenaltyMultiplier = 155.0f
            )
        }
    }
}
