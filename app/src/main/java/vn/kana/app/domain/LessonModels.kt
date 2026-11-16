package vn.kana.app.domain

import vn.kana.recognizer.KanaStrokeTemplate

/**
 * 5 bước học một chữ Kana + bước kiểm tra tổng hợp cuối bài.
 */
enum class LessonStepType(val stepNumber: Int, val titleVi: String, val instructionVi: String) {
    STEP_1_OBSERVE(
        stepNumber = 1,
        titleVi = "Bước 1/5: XEM",
        instructionVi = "Quan sát thứ tự nét, hướng viết và nghe phát âm"
    ),
    STEP_2_TRACE(
        stepNumber = 2,
        titleVi = "Bước 2/5: ĐỒ THEO",
        instructionVi = "Vẽ chồng lên nét mờ theo đúng thứ tự rồi bấm Kiểm tra"
    ),
    STEP_3_GUIDED(
        stepNumber = 3,
        titleVi = "Bước 3/5: VIẾT CÓ GỢI Ý",
        instructionVi = "Nhìn điểm bắt đầu của nét kế tiếp, vẽ hết chữ rồi bấm Kiểm tra"
    ),
    STEP_4_RECALL(
        stepNumber = 4,
        titleVi = "Bước 4/5: VIẾT TỪ TRÍ NHỚ",
        instructionVi = "Nhìn Romaji & nghe âm thanh, tự viết lại chữ không cần gợi ý"
    ),
    STEP_5_VOCAB(
        stepNumber = 5,
        titleVi = "Bước 5/5: TỪ VỰNG",
        instructionVi = "Viết từ vựng vào dãy ô vuông (chữ chưa học được in xám sẵn)"
    ),
    END_OF_ROW_QUIZ(
        stepNumber = 6,
        titleVi = "KIỂM TRA TỔNG HỢP CUỐI BÀI",
        instructionVi = "Tổng hợp chữ và từ vựng của hàng này cùng các hàng trước"
    )
}

/**
 * 4 dạng câu hỏi bắt buộc trong bài Kiểm tra tổng hợp / Ôn tập (100% viết trên canvas).
 */
enum class QuizQuestionType(val labelVi: String) {
    /** (a) romaji → viết kana */
    ROMAJI_TO_KANA("Dạng (a): Nhìn Romaji → Viết chữ Kana"),
    /** (b) nghe → viết kana */
    LISTEN_TO_KANA("Dạng (b): Nghe phát âm → Viết chữ Kana"),
    /** (c) nghĩa Việt + romaji → viết cả từ */
    MEANING_ROMAJI_TO_WORD("Dạng (c): Nghĩa tiếng Việt + Romaji → Viết từ"),
    /** (d) nghe → viết cả từ */
    LISTEN_TO_WORD("Dạng (d): Nghe phát âm → Viết cả từ")
}

/**
 * Một câu hỏi trong phần Kiểm tra cuối bài hoặc Ôn tập.
 */
data class QuizQuestion(
    val type: QuizQuestionType,
    /** Nếu là câu hỏi chữ đơn (a, b) */
    val targetTemplate: KanaStrokeTemplate? = null,
    /** Nếu là câu hỏi từ vựng (c, d) */
    val targetWord: KanaVocabWord? = null
)

/**
 * Thông tin một bài học theo hàng gojūon (ví dụ: あ行, か行).
 */
data class GojuonRowLesson(
    val rowId: String,
    val script: String,
    val rowName: String,
    val titleVi: String,
    val subtitleVi: String,
    val characters: List<KanaStrokeTemplate>,
    val isUnlocked: Boolean,
    val isCompleted: Boolean,
    val bestQuizScore: Int = 0
)
