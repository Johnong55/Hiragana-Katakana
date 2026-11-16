package vn.kana.app.domain

/**
 * Một bài học Ngữ pháp JLPT N5 (dùng cho Chặng 05 và Chặng 07).
 */
data class N5GrammarPoint(
    val id: String,
    val stageNumber: Int,
    val titleVi: String,
    val patternJp: String,
    val explanationVi: String,
    val examTipVi: String,
    val exampleJp: String,
    val exampleRomaji: String,
    val exampleVi: String,
    /** Câu hỏi bài tập điền khuyết (người dùng viết chữ Kana/Kanji còn thiếu trên Canvas!) */
    val quizPromptVi: String,
    val quizSentenceWithBlank: String,
    val quizAnswerChar: String,
    val quizAnswerRomaji: String
)

/**
 * Loại nội dung của từng Trạm dừng (Milestone 01..08) trên Con đường Chinh phục N5.
 */
enum class RoadmapStageType {
    HIRAGANA_BASIC,     // 01
    KATAKANA_BASIC,     // 02
    DAKUTEN_AND_YOON,   // 03
    N5_VOCAB_SENTENCES, // 04
    N5_GRAMMAR_1,       // 05
    N5_KANJI_1,         // 06
    N5_GRAMMAR_2,       // 07
    N5_KANJI_2_FINAL    // 08
}

/**
 * Một Trạm dừng (Milestone 01..08) trên bản đồ đường đua S-Curve chinh phục JLPT N5.
 */
data class RoadmapStageNode(
    val stageNumber: Int,           // 1..8 (hiển thị "01".."08")
    val type: RoadmapStageType,
    val titleVi: String,
    val subtitleVi: String,
    val badgeIconText: String,      // Biểu tượng chữ đặc trưng của trạm: "あ", "ア", "が", "語", "文", "漢", "て", "N5🚩"
    val totalItems: Int,
    val completedItems: Int,
    val isUnlocked: Boolean,
    val isCompleted: Boolean
) {
    val numberFormatted: String
        get() = "%02d".format(stageNumber)

    val progressFraction: Float
        get() = if (totalItems > 0) (completedItems.toFloat() / totalItems).coerceIn(0f, 1f) else 0f
}

/**
 * Kho dữ liệu Ngữ pháp JLPT N5 đóng gói sẵn 100% offline.
 */
object N5GrammarCatalog {

    val stage5Points: List<N5GrammarPoint> = listOf(
        N5GrammarPoint(
            id = "g5_01",
            stageNumber = 5,
            titleVi = "1. Trợ từ は (Chủ đề của câu)",
            patternJp = "Danh từ 1 + は + Danh từ 2 + です",
            explanationVi = "Trợ từ は (viết chữ 'ha' nhưng đọc là 'wa') đứng sau chủ đề chính của câu. です đứng cuối câu mang nghĩa 'là' một cách lịch sự.",
            examTipVi = "Lưu ý thi N5: Khi làm trợ từ chủ đề, bắt buộc viết là は (không bao giờ viết わ).",
            exampleJp = "わたし は がくせい です。",
            exampleRomaji = "watashi wa gakusei desu.",
            exampleVi = "Tôi là học sinh / sinh viên.",
            quizPromptVi = "Điền trợ từ chỉ chủ đề ('wa') vào chỗ trống:",
            quizSentenceWithBlank = "わたし [ ? ] がくせい です。",
            quizAnswerChar = "は",
            quizAnswerRomaji = "ha (đọc là wa)"
        ),
        N5GrammarPoint(
            id = "g5_02",
            stageNumber = 5,
            titleVi = "2. Trợ từ の (Sở hữu & Nối danh từ)",
            patternJp = "Danh từ 1 + の + Danh từ 2",
            explanationVi = "Trợ từ の nối 2 danh từ, mang nghĩa 'của' (Danh từ 2 thuộc về Danh từ 1).",
            examTipVi = "Trong tiếng Nhật, người/vật sở hữu đứng TRƯỚC の, vật được sở hữu đứng SAU の.",
            exampleJp = "これ は わたし の かさ です。",
            exampleRomaji = "kore wa watashi no kasa desu.",
            exampleVi = "Đây là cái ô của tôi.",
            quizPromptVi = "Điền trợ từ sở hữu ('của') vào chỗ trống:",
            quizSentenceWithBlank = "わたし [ ? ] ほん です。",
            quizAnswerChar = "の",
            quizAnswerRomaji = "no"
        ),
        N5GrammarPoint(
            id = "g5_03",
            stageNumber = 5,
            titleVi = "3. Trợ từ を (Tân ngữ của động từ)",
            patternJp = "Danh từ + を + Động từ",
            explanationVi = "Trợ từ を (đọc là 'o') đánh dấu đối tượng trực tiếp chịu tác động của hành động (ăn gì, uống gì, đọc gì, xem gì).",
            examTipVi = "Luôn dùng chữ を (ở hàng わ行), không dùng chữ お (ở hàng あ行).",
            exampleJp = "まいにち みず を のみます。",
            exampleRomaji = "mainichi mizu wo nomimasu.",
            exampleVi = "Mỗi ngày tôi uống nước.",
            quizPromptVi = "Điền trợ từ tân ngữ đứng trước động từ 'ăn/uống':",
            quizSentenceWithBlank = "すいか [ ? ] たべます。",
            quizAnswerChar = "を",
            quizAnswerRomaji = "wo"
        ),
        N5GrammarPoint(
            id = "g5_04",
            stageNumber = 5,
            titleVi = "4. Trợ từ へ & に (Hướng di chuyển / Đích đến)",
            patternJp = "Địa điểm + へ / に + いきます (đi) / きます (đến)",
            explanationVi = "Trợ từ へ (viết 'he' đọc là 'e') và に chỉ hướng hoặc đích đến của các động từ di chuyển như いく (đi), くる (đến), かえる (về).",
            examTipVi = "Chữ へ khi làm trợ từ chỉ hướng đọc là 'e'.",
            exampleJp = "あした えき へ いきます。",
            exampleRomaji = "ashita eki e ikimasu.",
            exampleVi = "Ngày mai tôi đi đến nhà ga.",
            quizPromptVi = "Điền trợ từ chỉ hướng đi ('e') vào chỗ trống:",
            quizSentenceWithBlank = "うみ [ ? ] いきます。",
            quizAnswerChar = "へ",
            quizAnswerRomaji = "he (đọc là e)"
        ),
        N5GrammarPoint(
            id = "g5_05",
            stageNumber = 5,
            titleVi = "5. Trợ từ で (Phương tiện & Nơi diễn ra hành động)",
            patternJp = "Phương tiện / Địa điểm + で + Động từ",
            explanationVi = "Trợ từ で dùng để chỉ: (1) Phương tiện đi lại/công cụ ('bằng xe gì'), hoặc (2) Nơi một hành động diễn ra ('làm gì ở đâu').",
            examTipVi = "Phân biệt: Có hành động xảy ra tại chỗ đó thì dùng で; chỉ tồn tại (いる/ある) thì dùng に.",
            exampleJp = "でんしゃ で がっこう へ いきます。",
            exampleRomaji = "densha de gakkou e ikimasu.",
            exampleVi = "Tôi đi đến trường bằng tàu điện.",
            quizPromptVi = "Điền trợ từ chỉ phương tiện ('bằng ô tô') vào chỗ trống:",
            quizSentenceWithBlank = "くるま [ ? ] いきます。",
            quizAnswerChar = "で",
            quizAnswerRomaji = "de"
        ),
        N5GrammarPoint(
            id = "g5_06",
            stageNumber = 5,
            titleVi = "6. Trợ từ が (Chỉ sự tồn tại いる / ある & Sở thích)",
            patternJp = "Danh từ + が + あります (đồ vật) / います (người, động vật) / すき です",
            explanationVi = "Trợ từ が đứng trước あります (có đồ vật), います (có người/con vật), hoặc すき (thích), わかります (hiểu).",
            examTipVi = "Câu kinh điển thi N5: Con vật/người dùng います, đồ vật/cây cối dùng あります.",
            exampleJp = "にわ に いぬ が います。",
            exampleRomaji = "niwa ni inu ga imasu.",
            exampleVi = "Trong vườn có chú chó.",
            quizPromptVi = "Điền trợ từ đứng trước 'います / すき です':",
            quizSentenceWithBlank = "ねこ [ ? ] すき です。",
            quizAnswerChar = "が",
            quizAnswerRomaji = "ga"
        )
    )

    val stage7Points: List<N5GrammarPoint> = listOf(
        N5GrammarPoint(
            id = "g7_01",
            stageNumber = 7,
            titleVi = "1. Câu hỏi trong tiếng Nhật với trợ từ か",
            patternJp = "Câu trần thuật + か。",
            explanationVi = "Đặt trợ từ か ở cuối câu với giọng cao lên để biến câu trần thuật thành câu hỏi (Có phải... không?).",
            examTipVi = "Trong tiếng Nhật chuẩn lịch sự, câu có か ở cuối thường kết thúc bằng dấu chấm (。) thay vì dấu chấm hỏi.",
            exampleJp = "これ は あなた の かさ ですか。",
            exampleRomaji = "kore wa anata no kasa desu ka.",
            exampleVi = "Đây có phải là cái ô của bạn không?",
            quizPromptVi = "Điền trợ từ đặt cuối câu để tạo câu hỏi:",
            quizSentenceWithBlank = "なんじ です [ ? ]。",
            quizAnswerChar = "か",
            quizAnswerRomaji = "ka"
        ),
        N5GrammarPoint(
            id = "g7_02",
            stageNumber = 7,
            titleVi = "2. Trợ từ と (Và / Cùng với ai đó)",
            patternJp = "Danh từ 1 + と + Danh từ 2 / Người + と + Động từ",
            explanationVi = "Trợ từ と nối 2 danh từ ('A và B'), hoặc chỉ người cùng thực hiện hành động ('làm cùng với ai').",
            examTipVi = "Khi đi một mình thì dùng ひとり で (không dùng と).",
            exampleJp = "かぞく と テレビ を みます。",
            exampleRomaji = "kazoku to terebi wo mimasu.",
            exampleVi = "Tôi xem ti vi cùng với gia đình.",
            quizPromptVi = "Điền trợ từ mang nghĩa 'cùng với / và':",
            quizSentenceWithBlank = "ともだち [ ? ] あそびます。",
            quizAnswerChar = "と",
            quizAnswerRomaji = "to"
        ),
        N5GrammarPoint(
            id = "g7_03",
            stageNumber = 7,
            titleVi = "3. Trợ từ も (Cũng)",
            patternJp = "Danh từ + も + です / Động từ",
            explanationVi = "Trợ từ も thay thế cho は, が, を để mang nghĩa 'cũng vậy'.",
            examTipVi = "Khi điểm tương đồng lặp lại với câu trước, dùng も thay cho は.",
            exampleJp = "わたし も にほんご を べんきょう します。",
            exampleRomaji = "watashi mo nihongo wo benkyou shimasu.",
            exampleVi = "Tôi cũng học tiếng Nhật.",
            quizPromptVi = "Điền trợ từ mang nghĩa 'cũng' vào chỗ trống:",
            quizSentenceWithBlank = "これ [ ? ] おいしい です。",
            quizAnswerChar = "も",
            quizAnswerRomaji = "mo"
        ),
        N5GrammarPoint(
            id = "g7_04",
            stageNumber = 7,
            titleVi = "4. Mẫu câu mong muốn: V-たい です (Muốn làm gì)",
            patternJp = "Động từ (bỏ ます) + たい です",
            explanationVi = "Bỏ đuôi ます của động từ rồi cộng thêm たい です để diễn tả mong muốn của bản thân ('Tôi muốn...').",
            examTipVi = "Ví dụ: いきます (đi) → いきたい (muốn đi); たべます (ăn) → たべたい (muốn ăn).",
            exampleJp = "にほん へ いきたい です。",
            exampleRomaji = "nihon e ikitai desu.",
            exampleVi = "Tôi muốn đi Nhật Bản.",
            quizPromptVi = "Điền chữ đầu của đuôi mong muốn '[ ? ]い です' (trong たべたい):",
            quizSentenceWithBlank = "すし を たべ [ ? ] い です。",
            quizAnswerChar = "た",
            quizAnswerRomaji = "ta (trong たい)"
        ),
        N5GrammarPoint(
            id = "g7_05",
            stageNumber = 7,
            titleVi = "5. Mẫu câu yêu cầu lịch sự: V-て ください (Hãy làm...)",
            patternJp = "Động từ thể て + ください",
            explanationVi = "Dùng động từ chia sang thể て kết hợp với ください để nhờ hoặc mời ai đó làm việc gì một cách lịch sự.",
            examTipVi = "Ví dụ: みます → みて ください (Xin hãy nhìn); ききます → きいて ください (Xin hãy nghe).",
            exampleJp = "ここ に なまえ を かいて ください。",
            exampleRomaji = "koko ni namae wo kaite kudasai.",
            exampleVi = "Xin hãy viết tên vào đây.",
            quizPromptVi = "Điền chữ 'て' trong mẫu câu 'み [ ? ] ください' (Xin hãy nhìn):",
            quizSentenceWithBlank = "これ を み [ ? ] ください。",
            quizAnswerChar = "て",
            quizAnswerRomaji = "te"
        ),
        N5GrammarPoint(
            id = "g7_06",
            stageNumber = 7,
            titleVi = "6. Mẫu câu khuyên nhủ: V-ないで ください (Xin đừng làm...)",
            patternJp = "Động từ thể ない + で ください",
            explanationVi = "Dùng động từ chia sang thể phủ định ない kết hợp với で ください để nhắc nhở ai đó không làm việc gì.",
            examTipVi = "Ví dụ: わすれます → わすれないで ください (Xin đừng quên).",
            exampleJp = "しゃしん を とらないで ください。",
            exampleRomaji = "shashin wo toranaide kudasai.",
            exampleVi = "Xin đừng chụp ảnh.",
            quizPromptVi = "Điền chữ đầu của thể phủ định '[ ? ]いで ください' (trong いかないで):",
            quizSentenceWithBlank = "いか [ ? ] いで ください。",
            quizAnswerChar = "な",
            quizAnswerRomaji = "na (trong ないで)"
        )
    )
}
