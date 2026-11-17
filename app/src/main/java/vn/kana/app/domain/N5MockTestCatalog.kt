package vn.kana.app.domain

enum class MockTestSection(val titleVi: String, val badge: String) {
    VOCAB_KANJI("Phần 1: Từ vựng & Kanji", "文字・語彙"),
    GRAMMAR_READING("Phần 2: Ngữ pháp & Đọc hiểu", "文法・読解"),
    LISTENING("Phần 3: Nghe hiểu", "聴解")
}

data class N5MockQuestion(
    val id: String,
    val section: MockTestSection,
    val promptVi: String,
    val promptJa: String,
    val audioJa: String? = null,
    val options: List<String>,
    val correctIndex: Int,
    val explanationVi: String
)

object N5MockTestCatalog {

    val questions: List<N5MockQuestion> = listOf(
        // --- PHẦN 1: TỪ VỰNG & KANJI (8 câu) ---
        N5MockQuestion(
            id = "mt_v1",
            section = MockTestSection.VOCAB_KANJI,
            promptVi = "Chọn cách đọc đúng của chữ Kanji gạch chân:",
            promptJa = "まいにち 【学校】 へ いきます。",
            options = listOf("がっこう", "こうこう", "かいしゃ", "だいがく"),
            correctIndex = 0,
            explanationVi = "学校 (がっこう) = trường học."
        ),
        N5MockQuestion(
            id = "mt_v2",
            section = MockTestSection.VOCAB_KANJI,
            promptVi = "Chọn cách đọc đúng của chữ Kanji:",
            promptJa = "きのう 【友達】 と あいました。",
            options = listOf("かぞく", "せんせい", "ともだち", "がくせい"),
            correctIndex = 2,
            explanationVi = "友達 (ともだち) = bạn bè."
        ),
        N5MockQuestion(
            id = "mt_v3",
            section = MockTestSection.VOCAB_KANJI,
            promptVi = "Chọn chữ Kanji đúng cho từ trong ngoặc:",
            promptJa = "テーブルの 【うえ】 に ほんが あります。",
            options = listOf("下", "上", "中", "外"),
            correctIndex = 1,
            explanationVi = "上 (うえ) = ở trên; 下 (した) = ở dưới."
        ),
        N5MockQuestion(
            id = "mt_v4",
            section = MockTestSection.VOCAB_KANJI,
            promptVi = "Chọn cách đọc đúng của thời gian:",
            promptJa = "あさ 【七時】 に おきます。",
            options = listOf("しちじ", "ななじ", "はちじ", "くうじ"),
            correctIndex = 0,
            explanationVi = "7 giờ đọc là しちじ (shichiji), không đọc là ななじ."
        ),
        N5MockQuestion(
            id = "mt_v5",
            section = MockTestSection.VOCAB_KANJI,
            promptVi = "Chọn từ thích hợp điền vào chỗ trống:",
            promptJa = "あつい ですから、つめたい （　　） を のみたいです。",
            options = listOf("くつ", "みず", "しんぶん", "かばん"),
            correctIndex = 1,
            explanationVi = "Trời nóng nên muốn uống nước lạnh (つめたい みず)."
        ),
        N5MockQuestion(
            id = "mt_v6",
            section = MockTestSection.VOCAB_KANJI,
            promptVi = "Chọn nghĩa đúng của từ Katakana:",
            promptJa = "デパート で くつを かいました。",
            options = listOf("Bệnh viện", "Bưu điện", "Trung tâm thương mại", "Nhà ga"),
            correctIndex = 2,
            explanationVi = "デパート (Department store) = trung tâm thương mại / cửa hàng bách hóa."
        ),
        N5MockQuestion(
            id = "mt_v7",
            section = MockTestSection.VOCAB_KANJI,
            promptVi = "Chọn từ có nghĩa gần nhất với từ gạch chân:",
            promptJa = "この へやは 【しずか】 ですね。",
            options = listOf("うるさくない", "おおきい", "あたらしい", "たかい"),
            correctIndex = 0,
            explanationVi = "しずか (yên tĩnh) trái nghĩa với うるさい (ồn ào) = うるさくない."
        ),
        N5MockQuestion(
            id = "mt_v8",
            section = MockTestSection.VOCAB_KANJI,
            promptVi = "Chọn cách đọc đúng của chữ Kanji:",
            promptJa = "きょうは 【天気】 が いいですね。",
            options = listOf("でんき", "てんき", "げんき", "きぶん"),
            correctIndex = 1,
            explanationVi = "天気 (てんき) = thời tiết. 電気 (でんき) = điện."
        ),

        // --- PHẦN 2: NGỮ PHÁP & ĐỌC HIỂU (8 câu) ---
        N5MockQuestion(
            id = "mt_g1",
            section = MockTestSection.GRAMMAR_READING,
            promptVi = "Chọn trợ từ đúng:",
            promptJa = "わたしは バス （　　） かいしゃへ いきます。",
            options = listOf("を", "に", "で", "へ"),
            correctIndex = 2,
            explanationVi = "「で」chỉ phương tiện di chuyển (バスで = bằng xe buýt)."
        ),
        N5MockQuestion(
            id = "mt_g2",
            section = MockTestSection.GRAMMAR_READING,
            promptVi = "Chọn trợ từ đúng:",
            promptJa = "にほんご （　　） えいご、どちらが むずかしいですか。",
            options = listOf("と", "や", "も", "の"),
            correctIndex = 0,
            explanationVi = "So sánh 2 đối tượng A và B dùng 「A と B、どちらが〜」."
        ),
        N5MockQuestion(
            id = "mt_g3",
            section = MockTestSection.GRAMMAR_READING,
            promptVi = "Chọn dạng đúng của động từ:",
            promptJa = "ここに なまえを （　　） ください。",
            options = listOf("かく", "かいて", "かきます", "かいた"),
            correctIndex = 1,
            explanationVi = "Mẫu câu yêu cầu lịch sự: Động từ thể て + ください (かいて ください)."
        ),
        N5MockQuestion(
            id = "mt_g4",
            section = MockTestSection.GRAMMAR_READING,
            promptVi = "Chọn dạng quá khứ đúng của tính từ:",
            promptJa = "きのうの テストは （　　）。",
            options = listOf(
                "むずかしいでした",
                "むずかしかったです",
                "むずかしくでした",
                "むずかしいかったです"
            ),
            correctIndex = 1,
            explanationVi = "Tính từ đuôi い chuyển quá khứ: bỏ い thêm かったです → むずかしかったです."
        ),
        N5MockQuestion(
            id = "mt_g5",
            section = MockTestSection.GRAMMAR_READING,
            promptVi = "Chọn trợ từ đúng:",
            promptJa = "いぬが にわ （　　） います。",
            options = listOf("を", "で", "に", "へ"),
            correctIndex = 2,
            explanationVi = "Chỉ vị trí tồn tại với います/あります dùng trợ từ 「に」."
        ),
        N5MockQuestion(
            id = "mt_g6",
            section = MockTestSection.GRAMMAR_READING,
            promptVi = "Tìm vị trí ★ đúng trong câu: 「Tôi muốn ăn sushi ở Nhật」",
            promptJa = "わたしは [ 1 ] [ 2 ] [ ★ ] [ 4 ] です。",
            options = listOf(
                "にほん (1)",
                "で (2)",
                "すしを (★)",
                "たべたい (4)"
            ),
            correctIndex = 2,
            explanationVi = "Thứ tự đúng: にほんで すしを たべたい です → vị trí ★ là 「すしを」."
        ),
        N5MockQuestion(
            id = "mt_g7",
            section = MockTestSection.GRAMMAR_READING,
            promptVi = "Đọc đoạn ngắn và trả lời: Anh Yamada đến công ty bằng gì?",
            promptJa = "やまださん：「わたしは いつも でんしゃで かいしゃへ いきます。でも、きのうは あめでしたから、タクシーで いきました。」",
            options = listOf(
                "Hôm qua đi bằng tàu điện",
                "Thường ngày đi bằng tàu điện, hôm qua đi taxi",
                "Thường ngày đi bằng xe buýt",
                "Hôm qua đi bộ"
            ),
            correctIndex = 1,
            explanationVi = "「いつも でんしゃで」(thường đi tàu điện), 「きのうは ... タクシーで」(hôm qua đi taxi)."
        ),
        N5MockQuestion(
            id = "mt_g8",
            section = MockTestSection.GRAMMAR_READING,
            promptVi = "Đọc thông báo và trả lời: Thư viện đóng cửa vào thứ mấy?",
            promptJa = "「としょかん：あさ 9じから ゆうがた 5じまで。やすみは 月よう日です。」",
            options = listOf("Thứ Bảy", "Chủ Nhật", "Thứ Hai", "Thứ Tư"),
            correctIndex = 2,
            explanationVi = "「やすみは 月よう日です」→ Nghỉ vào Thứ Hai (げつようび)."
        ),

        // --- PHẦN 3: NGHE HIỂU (8 câu) ---
        N5MockQuestion(
            id = "mt_l1",
            section = MockTestSection.LISTENING,
            promptVi = "Nghe câu hỏi và chọn đáp án phù hợp nhất:",
            promptJa = "🔊 Bấm nghe câu tiếng Nhật",
            audioJa = "いま、なんじですか。",
            options = listOf(
                "はい、そうです。",
                "ごじはん です。",
                "わたしは がくせいです。",
                "あした いきます。"
            ),
            correctIndex = 1,
            explanationVi = "Câu hỏi: 「いま、なんじですか。」(Bây giờ là mấy giờ?) → Trả lời: 「ごじはん です。」(5 giờ rưỡi)."
        ),
        N5MockQuestion(
            id = "mt_l2",
            section = MockTestSection.LISTENING,
            promptVi = "Nghe đoạn hội thoại và chọn số tiền đúng:",
            promptJa = "🔊 Bấm nghe câu tiếng Nhật",
            audioJa = "すみません、この りんごは いくらですか。ひとつ ひゃくごじゅうえん です。",
            options = listOf("100 yên", "150 yên", "250 yên", "500 yên"),
            correctIndex = 1,
            explanationVi = "「ひとつ ひゃくごじゅうえん です」= Một quả giá 150 yên."
        ),
        N5MockQuestion(
            id = "mt_l3",
            section = MockTestSection.LISTENING,
            promptVi = "Nghe và cho biết người nói sẽ đi đâu vào ngày mai:",
            promptJa = "🔊 Bấm nghe câu tiếng Nhật",
            audioJa = "あしたは やすみですから、ともだちと びょういんではなくて、えいがかんへ いきます。",
            options = listOf("Bệnh viện", "Trường học", "Rạp chiếu phim", "Siêu thị"),
            correctIndex = 2,
            explanationVi = "Không phải bệnh viện (びょういんではなくて) mà đi rạp chiếu phim (えいがかん)."
        ),
        N5MockQuestion(
            id = "mt_l4",
            section = MockTestSection.LISTENING,
            promptVi = "Nghe câu hỏi và chọn câu trả lời tự nhiên nhất:",
            promptJa = "🔊 Bấm nghe câu tiếng Nhật",
            audioJa = "すみません、えきは どこですか。",
            options = listOf(
                "あそこ です。",
                "きのう いきました。",
                "でんしゃが すきです。",
                "ありがとうございます。"
            ),
            correctIndex = 0,
            explanationVi = "Hỏi đường 「えきは どこですか」(Nhà ga ở đâu?) → 「あそこ です」(Ở đằng kia)."
        ),
        N5MockQuestion(
            id = "mt_l5",
            section = MockTestSection.LISTENING,
            promptVi = "Nghe và cho biết người nói thích mùa nào nhất:",
            promptJa = "🔊 Bấm nghe câu tiếng Nhật",
            audioJa = "なつは あついですが、ふゆは さむいです。わたしは はるが いちばん すきです。",
            options = listOf("Mùa xuân", "Mùa hạ", "Mùa thu", "Mùa đông"),
            correctIndex = 0,
            explanationVi = "「わたしは はるが いちばん すきです」= Tôi thích mùa xuân (はる) nhất."
        ),
        N5MockQuestion(
            id = "mt_l6",
            section = MockTestSection.LISTENING,
            promptVi = "Nghe và chọn hành động được yêu cầu:",
            promptJa = "🔊 Bấm nghe câu tiếng Nhật",
            audioJa = "さむいですから、まどを しめて ください。",
            options = listOf(
                "Mở cửa sổ",
                "Đóng cửa sổ",
                "Bật đèn lên",
                "Ngồi xuống ghế"
            ),
            correctIndex = 1,
            explanationVi = "「まどを しめて ください」= Xin hãy đóng cửa sổ lại."
        ),
        N5MockQuestion(
            id = "mt_l7",
            section = MockTestSection.LISTENING,
            promptVi = "Nghe và cho biết có mấy người trong gia đình người nói:",
            promptJa = "🔊 Bấm nghe câu tiếng Nhật",
            audioJa = "わたしの かぞくは ちちと ははと いもうとと わたしの よにん です。",
            options = listOf("2 người", "3 người", "4 người", "5 người"),
            correctIndex = 2,
            explanationVi = "Bố, mẹ, em gái và tôi → 「よにん です」(4 người)."
        ),
        N5MockQuestion(
            id = "mt_l8",
            section = MockTestSection.LISTENING,
            promptVi = "Nghe câu chào và chọn lời đáp lại phù hợp:",
            promptJa = "🔊 Bấm nghe câu tiếng Nhật",
            audioJa = "きょうは ありがとうございました。さようなら。",
            options = listOf(
                "いただきます。",
                "こちらこそ。また あした。",
                "おやすみなさい。",
                "はじめまして。"
            ),
            correctIndex = 1,
            explanationVi = "Đáp lại lời cảm ơn & tạm biệt: 「こちらこそ。また あした。」(Tôi cũng cảm ơn bạn. Hẹn gặp ngày mai)."
        )
    )
}
