package vn.kana.recognizer

import kotlinx.serialization.Serializable

/**
 * Điểm 2D chuẩn hoá hoặc trên canvas kèm timestamp (ms).
 * Thuần Kotlin/JVM, không phụ thuộc Android SDK.
 */
@Serializable
data class Point2D(
    val x: Float,
    val y: Float,
    val timestampMs: Long = 0L
)

/**
 * Một nét vẽ gồm danh sách các điểm theo thứ tự thời gian.
 */
@Serializable
data class Stroke(
    val points: List<Point2D>
)

/**
 * Dữ liệu mẫu của một chữ Kana được parse từ KanjiVG.
 * @property char Ký tự Kana (ví dụ: "あ", "ア")
 * @property unicode Mã Unicode (ví dụ: "U+3042")
 * @property romaji Phiên âm Hepburn (ví dụ: "a", "ka", "shi")
 * @property row Hàng gojūon (ví dụ: "あ行", "か行")
 * @property script Hệ chữ: "hiragana" hoặc "katakana"
 * @property category Phân loại: "basic", "dakuten", "small"
 * @property strokeCount Tổng số nét chuẩn
 * @property strokes Danh sách các nét, mỗi nét là danh sách toạ độ [x, y] trong miền [0.0, 1.0]
 */
@Serializable
data class KanaStrokeTemplate(
    val char: String,
    val unicode: String,
    val romaji: String,
    val row: String,
    val script: String,
    val category: String,
    val strokeCount: Int,
    val strokes: List<List<List<Float>>>
) {
    /**
     * Chuyển đổi danh sách toạ độ thô [x, y] sang danh sách [Stroke] kiểu mạnh.
     */
    fun toTypedStrokes(): List<Stroke> = strokes.map { rawStroke ->
        Stroke(
            points = rawStroke.map { coords ->
                Point2D(
                    x = coords.getOrElse(0) { 0f },
                    y = coords.getOrElse(1) { 0f }
                )
            }
        )
    }
}

/**
 * Cấu trúc file JSON gốc `assets/strokes/kana_strokes.json`.
 */
@Serializable
data class KanaStrokeDatabase(
    val source: String,
    val license: String,
    val copyright: String,
    val pointsPerStroke: Int,
    val coordinateRange: List<Float>,
    val totalCharacters: Int,
    val characters: List<KanaStrokeTemplate>
)
