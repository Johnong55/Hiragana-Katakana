package vn.kana.app.domain

enum class ConjugationFilter(val labelVi: String) {
    ALL("Tất cả"),
    MASU("Thể ます"),
    TE("Thể て"),
    NAI_TA("Thể ない / た"),
    ADJECTIVE("Tính từ い / な")
}

data class ConjugationRuleCard(
    val id: String,
    val titleVi: String,
    val subtitleVi: String,
    val rules: List<String>,
    val exampleJp: String,
    val exampleVi: String
)

data class ConjugationDrillItem(
    val id: String,
    val baseJp: String,
    val baseKana: String,
    val meaningVi: String,
    val groupBadgeVi: String,
    val filter: ConjugationFilter,
    val targetFormVi: String,
    val correctAnswer: String,
    val distractors: List<String>,
    val explanationVi: String
)

/**
 * Kho quy tắc & bài luyện Chia thể Động từ (ます / て / ない / た) và Tính từ (い / な) trình độ JLPT N5 (100% offline).
 */
object N5ConjugationCatalog {

    val ruleCards: List<ConjugationRuleCard> = listOf(
        ConjugationRuleCard(
            id = "rule_groups",
            titleVi = "1. Phân biệt 3 nhóm Động từ N5",
            subtitleVi = "Nắm chắc nhóm động từ để chia không bao giờ nhầm",
            rules = listOf(
                "• Nhóm 1 (Godan / đuôi -u): Kết thúc bằng cột う (く, ぐ, す, つ, ぬ, ぶ, む, う) và các từ đuôi る không thuộc nhóm 2 (vd: 帰る, 走る, 入る).",
                "• Nhóm 2 (Ichidan / đuôi -ru): Kết thúc bằng âm [i-ru] hoặc [e-ru] (vd: 食べる, 見る, 起きる, 寝る, 教える).",
                "• Nhóm 3 (Bất quy tắc): Chỉ có 2 động từ duy nhất: する (làm) và くる / 来る (đến)."
            ),
            exampleJp = "書く (N1) • 食べる (N2) • する / 来る (N3)",
            exampleVi = "Viết (N1) • Ăn (N2) • Làm / Đến (N3)"
        ),
        ConjugationRuleCard(
            id = "rule_masu",
            titleVi = "2. Chia sang Thể ます (Lịch sự)",
            subtitleVi = "Dùng trong giao tiếp hằng ngày lịch sự",
            rules = listOf(
                "• Nhóm 1: Đổi âm cuối cột う → cột い tương ứng + ます (vd: く→きます, む→みます, う→います).",
                "• Nhóm 2: Bỏ る + ます (vd: 食べる → 食べます, 見る → 見ます).",
                "• Nhóm 3: する → します  |  くる (来る) → きます (来ます)."
            ),
            exampleJp = "飲む → 飲みます  •  食べる → 食べます",
            exampleVi = "Uống → Uống (lịch sự) • Ăn → Ăn (lịch sự)"
        ),
        ConjugationRuleCard(
            id = "rule_te",
            titleVi = "3. Chia sang Thể て (Nối câu / V-てください)",
            subtitleVi = "Bài vè thần thánh cho Nhóm 1 — bắt buộc thuộc lòng!",
            rules = listOf(
                "• Nhóm 1: う・つ・る → って  |  む・ぶ・ぬ → んで",
                "• Nhóm 1: く → いて  |  ぐ → いで  |  す → して",
                "• NGOẠI LỆ DUY NHẤT: 行く (いく - đi) → 行って (いって), KHÔNG phải いいて!",
                "• Nhóm 2: Bỏ る + て (食べる → 食べて).",
                "• Nhóm 3: する → して  |  くる → きて."
            ),
            exampleJp = "待つ → 待って  •  飲む → 飲んで  •  書く → 書いて",
            exampleVi = "Chờ • Uống • Viết (thể て)"
        ),
        ConjugationRuleCard(
            id = "rule_nai",
            titleVi = "4. Chia sang Thể ない (Phủ định ngắn)",
            subtitleVi = "Dùng cho V-ないでください (xin đừng...) & thể ngắn",
            rules = listOf(
                "• Nhóm 1: Đổi âm cuối cột う → cột あ + ない (く→かない, む→まない, す→さない).",
                "• LƯU Ý CỰC QUAN TRỌNG: Đuôi う (như 買う, 会う) đổi thành わ + ない (買わない), KHÔNG phải 買あない!",
                "• Nhóm 2: Bỏ る + ない (食べる → 食べない).",
                "• Nhóm 3: する → しない  |  くる → こない (来ない)."
            ),
            exampleJp = "買う → 買わない  •  飲む → 飲まない  •  来る → こない",
            exampleVi = "Không mua • Không uống • Không đến"
        ),
        ConjugationRuleCard(
            id = "rule_ta",
            titleVi = "5. Chia sang Thể た (Quá khứ ngắn)",
            subtitleVi = "Giống hệt 100% quy tắc Thể て — chỉ đổi て→た, で→だ!",
            rules = listOf(
                "• Nhóm 1: って → った  |  んで → んだ  |  いて → いた  |  いで → いだ  |  して → した.",
                "• Ngoại lệ 行く: 行って → 行った (đã đi).",
                "• Nhóm 2: Bỏ る + た (食べる → 食べた).",
                "• Nhóm 3: する → した  |  くる → きた."
            ),
            exampleJp = "読む → 読んだ  •  書く → 書いた  •  食べる → 食べた",
            exampleVi = "Đã đọc • Đã viết • Đã ăn"
        ),
        ConjugationRuleCard(
            id = "rule_adj_i",
            titleVi = "6. Chia Tính từ đuôi い (い-Adjective)",
            subtitleVi = "Bỏ chữ い ở cuối rồi ghép đuôi tương ứng",
            rules = listOf(
                "• Phủ định hiện tại: bỏ い + くないです (高い → 高くないです: không đắt).",
                "• Quá khứ khẳng định: bỏ い + かったです (高い → 高かったです: đã đắt).",
                "• Quá khứ phủ định: bỏ い + くなかったです (高い → 高くなかったです).",
                "• NGOẠI LỆ DUY NHẤT: いい (tốt) đổi theo gốc よい → よくないです / よかったです."
            ),
            exampleJp = "暑い → 暑かった  •  いい → よくないです",
            exampleVi = "Đã nóng • Không tốt"
        ),
        ConjugationRuleCard(
            id = "rule_adj_na",
            titleVi = "7. Chia Tính từ đuôi な (な-Adjective)",
            subtitleVi = "Chia giống hệt Danh từ, cẩn thận きれい・有名 là đuôi な!",
            rules = listOf(
                "• Hiện tại khẳng định: A + です (静かです: yên tĩnh).",
                "• Hiện tại phủ định: A + じゃありません (静かじゃありません).",
                "• Quá khứ khẳng định: A + でした (静かでした: đã yên tĩnh).",
                "• Quá khứ phủ định: A + じゃありませんでした.",
                "• Bẫy hay gặp: きれい (đẹp), ゆうめい (nổi tiếng), げんき (khỏe) kết thúc bằng い nhưng là Tính từ đuôi な!"
            ),
            exampleJp = "きれい → きれいじゃありません  •  静か → 静かでした",
            exampleVi = "Không đẹp • Đã yên tĩnh"
        )
    )

    val drillItems: List<ConjugationDrillItem> = listOf(
        // === THỂ ます ===
        ConjugationDrillItem(
            id = "m1", baseJp = "書く", baseKana = "かく", meaningVi = "viết",
            groupBadgeVi = "Động từ Nhóm 1", filter = ConjugationFilter.MASU,
            targetFormVi = "Đổi sang Thể ます (Lịch sự)",
            correctAnswer = "かきます",
            distractors = listOf("かくます", "かます", "かいます"),
            explanationVi = "Nhóm 1 đuôi く → đổi sang cột い là き + ます ⇒ かきます (書きます)."
        ),
        ConjugationDrillItem(
            id = "m2", baseJp = "飲む", baseKana = "のむ", meaningVi = "uống",
            groupBadgeVi = "Động từ Nhóm 1", filter = ConjugationFilter.MASU,
            targetFormVi = "Đổi sang Thể ます (Lịch sự)",
            correctAnswer = "のみます",
            distractors = listOf("のむます", "のんでます", "のます"),
            explanationVi = "Nhóm 1 đuôi む → đổi sang cột い là み + ます ⇒ のみます (飲みます)."
        ),
        ConjugationDrillItem(
            id = "m3", baseJp = "食べる", baseKana = "たべる", meaningVi = "ăn",
            groupBadgeVi = "Động từ Nhóm 2", filter = ConjugationFilter.MASU,
            targetFormVi = "Đổi sang Thể ます (Lịch sự)",
            correctAnswer = "たべます",
            distractors = listOf("たべります", "たべいます", "たべれます"),
            explanationVi = "食べる là Động từ Nhóm 2 (đuôi -eru) → chỉ cần bỏ る + ます ⇒ たべます."
        ),
        ConjugationDrillItem(
            id = "m4", baseJp = "帰る", baseKana = "かえる", meaningVi = "trở về",
            groupBadgeVi = "Động từ Nhóm 1 (ngoại lệ -eru)", filter = ConjugationFilter.MASU,
            targetFormVi = "Đổi sang Thể ます (Lịch sự)",
            correctAnswer = "かえります",
            distractors = listOf("かえます", "かえいます", "かえって"),
            explanationVi = "Cẩn thận bẫy N5! 帰る (かえる) thuộc Nhóm 1 → る đổi thành り + ます ⇒ かえります."
        ),
        ConjugationDrillItem(
            id = "m5", baseJp = "来る", baseKana = "くる", meaningVi = "đến",
            groupBadgeVi = "Động từ Nhóm 3", filter = ConjugationFilter.MASU,
            targetFormVi = "Đổi sang Thể ます (Lịch sự)",
            correctAnswer = "きます",
            distractors = listOf("くます", "くります", "こます"),
            explanationVi = "来る (くる) là Nhóm 3 bất quy tắc: thể ます đọc là きます (来ます)."
        ),
        ConjugationDrillItem(
            id = "m6", baseJp = "する", baseKana = "する", meaningVi = "làm",
            groupBadgeVi = "Động từ Nhóm 3", filter = ConjugationFilter.MASU,
            targetFormVi = "Đổi sang Thể ます (Lịch sự)",
            correctAnswer = "します",
            distractors = listOf("すます", "すります", "せます"),
            explanationVi = "する là Nhóm 3 bất quy tắc: する → します."
        ),
        ConjugationDrillItem(
            id = "m7", baseJp = "待つ", baseKana = "まつ", meaningVi = "chờ đợi",
            groupBadgeVi = "Động từ Nhóm 1", filter = ConjugationFilter.MASU,
            targetFormVi = "Đổi sang Thể ます (Lịch sự)",
            correctAnswer = "まちます",
            distractors = listOf("まつます", "まてます", "まします"),
            explanationVi = "Nhóm 1 đuôi つ → cột い của hàng た là ち + ます ⇒ まちます (待ちます)."
        ),
        ConjugationDrillItem(
            id = "m8", baseJp = "見る", baseKana = "みる", meaningVi = "nhìn / xem",
            groupBadgeVi = "Động từ Nhóm 2", filter = ConjugationFilter.MASU,
            targetFormVi = "Đổi sang Thể ます (Lịch sự)",
            correctAnswer = "みます",
            distractors = listOf("みります", "みえます", "みいます"),
            explanationVi = "見る (みる) là Nhóm 2 (đuôi -iru) → bỏ る + ます ⇒ みます."
        ),

        // === THỂ て ===
        ConjugationDrillItem(
            id = "t1", baseJp = "買う", baseKana = "かう", meaningVi = "mua",
            groupBadgeVi = "Động từ Nhóm 1", filter = ConjugationFilter.TE,
            targetFormVi = "Đổi sang Thể て",
            correctAnswer = "かって",
            distractors = listOf("かいて", "かんで", "かて"),
            explanationVi = "Bài vè Nhóm 1: う・つ・る → って ⇒ 買う (かう) → かって (買って)."
        ),
        ConjugationDrillItem(
            id = "t2", baseJp = "待つ", baseKana = "まつ", meaningVi = "chờ đợi",
            groupBadgeVi = "Động từ Nhóm 1", filter = ConjugationFilter.TE,
            targetFormVi = "Đổi sang Thể て",
            correctAnswer = "まって",
            distractors = listOf("まちて", "まいて", "まんで"),
            explanationVi = "Bài vè Nhóm 1: う・つ・る → って ⇒ 待つ (まつ) → まって (待って)."
        ),
        ConjugationDrillItem(
            id = "t3", baseJp = "飲む", baseKana = "のむ", meaningVi = "uống",
            groupBadgeVi = "Động từ Nhóm 1", filter = ConjugationFilter.TE,
            targetFormVi = "Đổi sang Thể て",
            correctAnswer = "のんで",
            distractors = listOf("のって", "のみて", "のいて"),
            explanationVi = "Bài vè Nhóm 1: む・ぶ・ぬ → んで ⇒ 飲む (のむ) → のんで (飲んで)."
        ),
        ConjugationDrillItem(
            id = "t4", baseJp = "遊ぶ", baseKana = "あそぶ", meaningVi = "chơi",
            groupBadgeVi = "Động từ Nhóm 1", filter = ConjugationFilter.TE,
            targetFormVi = "Đổi sang Thể て",
            correctAnswer = "あそんで",
            distractors = listOf("あそって", "あそびて", "あそいで"),
            explanationVi = "Bài vè Nhóm 1: む・ぶ・ぬ → んで ⇒ 遊ぶ (あそぶ) → あそんで (遊んで)."
        ),
        ConjugationDrillItem(
            id = "t5", baseJp = "書く", baseKana = "かく", meaningVi = "viết",
            groupBadgeVi = "Động từ Nhóm 1", filter = ConjugationFilter.TE,
            targetFormVi = "Đổi sang Thể て",
            correctAnswer = "かいて",
            distractors = listOf("かって", "かいで", "かきして"),
            explanationVi = "Bài vè Nhóm 1: く → いて ⇒ 書く (かく) → かいて (書いて)."
        ),
        ConjugationDrillItem(
            id = "t6", baseJp = "泳ぐ", baseKana = "およぐ", meaningVi = "bơi",
            groupBadgeVi = "Động từ Nhóm 1", filter = ConjugationFilter.TE,
            targetFormVi = "Đổi sang Thể て",
            correctAnswer = "およいで",
            distractors = listOf("およいて", "およんで", "およって"),
            explanationVi = "Bài vè Nhóm 1: ぐ → いで (có âm đục!) ⇒ 泳ぐ → およいで (泳いで)."
        ),
        ConjugationDrillItem(
            id = "t7", baseJp = "話す", baseKana = "はなす", meaningVi = "nói chuyện",
            groupBadgeVi = "Động từ Nhóm 1", filter = ConjugationFilter.TE,
            targetFormVi = "Đổi sang Thể て",
            correctAnswer = "はなして",
            distractors = listOf("はなって", "はないて", "はなすて"),
            explanationVi = "Bài vè Nhóm 1: す → して ⇒ 話す (はなす) → はなして (話して)."
        ),
        ConjugationDrillItem(
            id = "t8", baseJp = "行く", baseKana = "いく", meaningVi = "đi",
            groupBadgeVi = "Động từ Nhóm 1 (NGOẠI LỆ)", filter = ConjugationFilter.TE,
            targetFormVi = "Đổi sang Thể て",
            correctAnswer = "いって",
            distractors = listOf("いいて", "いんで", "いきて"),
            explanationVi = "Ngoại lệ quan trọng nhất N5: 行く tuy kết thúc bằng く nhưng chia là いって (行って)!"
        ),
        ConjugationDrillItem(
            id = "t9", baseJp = "起きる", baseKana = "おきる", meaningVi = "thức dậy",
            groupBadgeVi = "Động từ Nhóm 2", filter = ConjugationFilter.TE,
            targetFormVi = "Đổi sang Thể て",
            correctAnswer = "おきて",
            distractors = listOf("おきって", "おきいて", "おきんで"),
            explanationVi = "起きる là Nhóm 2 → chỉ cần bỏ る + て ⇒ おきて (起きて)."
        ),
        ConjugationDrillItem(
            id = "t10", baseJp = "乗る", baseKana = "のる", meaningVi = "lên xe / tàu",
            groupBadgeVi = "Động từ Nhóm 1", filter = ConjugationFilter.TE,
            targetFormVi = "Đổi sang Thể て",
            correctAnswer = "のって",
            distractors = listOf("のて", "のんで", "のいて"),
            explanationVi = "乗る (のる) là Nhóm 1 đuôi る → って ⇒ のって (乗って)."
        ),

        // === THỂ ない & THỂ た ===
        ConjugationDrillItem(
            id = "n1", baseJp = "買う", baseKana = "かう", meaningVi = "mua",
            groupBadgeVi = "Động từ Nhóm 1", filter = ConjugationFilter.NAI_TA,
            targetFormVi = "Đổi sang Thể ない (Phủ định ngắn)",
            correctAnswer = "かわない",
            distractors = listOf("かあない", "かない", "かいない"),
            explanationVi = "Quy tắc vàng thể ない: Nhóm 1 đuôi う đổi thành わ + ない (không phải あ) ⇒ かわない!"
        ),
        ConjugationDrillItem(
            id = "n2", baseJp = "飲む", baseKana = "のむ", meaningVi = "uống",
            groupBadgeVi = "Động từ Nhóm 1", filter = ConjugationFilter.NAI_TA,
            targetFormVi = "Đổi sang Thể ない (Phủ định ngắn)",
            correctAnswer = "のまない",
            distractors = listOf("のみない", "のむない", "のわない"),
            explanationVi = "Nhóm 1 đuôi む → cột あ là ま + ない ⇒ のまない (飲まない)."
        ),
        ConjugationDrillItem(
            id = "n3", baseJp = "来る", baseKana = "くる", meaningVi = "đến",
            groupBadgeVi = "Động từ Nhóm 3", filter = ConjugationFilter.NAI_TA,
            targetFormVi = "Đổi sang Thể ない (Phủ định ngắn)",
            correctAnswer = "こない",
            distractors = listOf("きない", "くない", "くらない"),
            explanationVi = "Bất quy tắc Nhóm 3: 来る (くる) sang thể ない đọc là こない (来ない)."
        ),
        ConjugationDrillItem(
            id = "n4", baseJp = "寝る", baseKana = "ねる", meaningVi = "ngủ",
            groupBadgeVi = "Động từ Nhóm 2", filter = ConjugationFilter.NAI_TA,
            targetFormVi = "Đổi sang Thể ない (Phủ định ngắn)",
            correctAnswer = "ねない",
            distractors = listOf("ねらない", "ねわない", "ねいない"),
            explanationVi = "寝る (ねる) là Nhóm 2 → bỏ る + ない ⇒ ねない (寝ない)."
        ),
        ConjugationDrillItem(
            id = "n5", baseJp = "読む", baseKana = "よむ", meaningVi = "đọc",
            groupBadgeVi = "Động từ Nhóm 1", filter = ConjugationFilter.NAI_TA,
            targetFormVi = "Đổi sang Thể た (Quá khứ ngắn)",
            correctAnswer = "よんだ",
            distractors = listOf("よった", "よいた", "よみた"),
            explanationVi = "Thể て là よんで → đổi で thành だ ⇒ よんだ (読んだ: đã đọc)."
        ),
        ConjugationDrillItem(
            id = "n6", baseJp = "行く", baseKana = "いく", meaningVi = "đi",
            groupBadgeVi = "Động từ Nhóm 1 (NGOẠI LỆ)", filter = ConjugationFilter.NAI_TA,
            targetFormVi = "Đổi sang Thể た (Quá khứ ngắn)",
            correctAnswer = "いった",
            distractors = listOf("いいた", "いんだ", "いきた"),
            explanationVi = "Ngoại lệ 行く: thể て là いって → thể た là いった (行った: đã đi)."
        ),
        ConjugationDrillItem(
            id = "n7", baseJp = "作る", baseKana = "つくる", meaningVi = "làm / chế tạo",
            groupBadgeVi = "Động từ Nhóm 1", filter = ConjugationFilter.NAI_TA,
            targetFormVi = "Đổi sang Thể た (Quá khứ ngắn)",
            correctAnswer = "つくった",
            distractors = listOf("つくた", "つくんだ", "つくいた"),
            explanationVi = "作る (つくる) là Nhóm 1 đuôi る → って / った ⇒ つくった (作った)."
        ),
        ConjugationDrillItem(
            id = "n8", baseJp = "する", baseKana = "する", meaningVi = "làm",
            groupBadgeVi = "Động từ Nhóm 3", filter = ConjugationFilter.NAI_TA,
            targetFormVi = "Đổi sang Thể ない (Phủ định ngắn)",
            correctAnswer = "しない",
            distractors = listOf("さない", "すない", "すらない"),
            explanationVi = "Bất quy tắc Nhóm 3: する → しない (không làm)."
        ),

        // === TÍNH TỪ い & な ===
        ConjugationDrillItem(
            id = "a1", baseJp = "高い", baseKana = "たかい", meaningVi = "đắt / cao",
            groupBadgeVi = "Tính từ đuôi い", filter = ConjugationFilter.ADJECTIVE,
            targetFormVi = "Đổi sang Phủ định lịch sự",
            correctAnswer = "たかくないです",
            distractors = listOf("たかいじゃないです", "たかじゃないです", "たかかったないです"),
            explanationVi = "Tính từ đuôi い phủ định: bỏ い + くないです ⇒ たかくないです (高くないです)."
        ),
        ConjugationDrillItem(
            id = "a2", baseJp = "暑い", baseKana = "あつい", meaningVi = "nóng (thời tiết)",
            groupBadgeVi = "Tính từ đuôi い", filter = ConjugationFilter.ADJECTIVE,
            targetFormVi = "Đổi sang Quá khứ lịch sự",
            correctAnswer = "あつかったです",
            distractors = listOf("あついでした", "あつでした", "あつくないでした"),
            explanationVi = "Tính từ đuôi い quá khứ: bỏ い + かったです ⇒ あつかったです (暑かったです)."
        ),
        ConjugationDrillItem(
            id = "a3", baseJp = "いい", baseKana = "いい", meaningVi = "tốt",
            groupBadgeVi = "Tính từ đuôi い (NGOẠI LỆ)", filter = ConjugationFilter.ADJECTIVE,
            targetFormVi = "Đổi sang Phủ định lịch sự",
            correctAnswer = "よくないです",
            distractors = listOf("いくないです", "いいじゃないです", "いかったないです"),
            explanationVi = "Ngoại lệ duy nhất của tính từ đuôi い: いい phải đổi theo gốc よい ⇒ よくないです!"
        ),
        ConjugationDrillItem(
            id = "a4", baseJp = "いい", baseKana = "いい", meaningVi = "tốt",
            groupBadgeVi = "Tính từ đuôi い (NGOẠI LỆ)", filter = ConjugationFilter.ADJECTIVE,
            targetFormVi = "Đổi sang Quá khứ lịch sự",
            correctAnswer = "よかったです",
            distractors = listOf("いかったです", "いいでした", "よいでした"),
            explanationVi = "Ngoại lệ いい → quá khứ là よかったです (đã rất tốt / may quá!)."
        ),
        ConjugationDrillItem(
            id = "a5", baseJp = "安い", baseKana = "やすい", meaningVi = "rẻ",
            groupBadgeVi = "Tính từ đuôi い", filter = ConjugationFilter.ADJECTIVE,
            targetFormVi = "Đổi sang Quá khứ phủ định",
            correctAnswer = "やすくなかったです",
            distractors = listOf("やすいじゃなかったです", "やすかったないです", "やすくないでした"),
            explanationVi = "Quá khứ phủ định tính từ い: bỏ い + くなかったです ⇒ やすくなかったです."
        ),
        ConjugationDrillItem(
            id = "a6", baseJp = "静か", baseKana = "しずか", meaningVi = "yên tĩnh",
            groupBadgeVi = "Tính từ đuôi な", filter = ConjugationFilter.ADJECTIVE,
            targetFormVi = "Đổi sang Quá khứ lịch sự",
            correctAnswer = "しずかでした",
            distractors = listOf("しずかかったです", "しずかなでした", "しずかじゃありません"),
            explanationVi = "Tính từ đuôi な quá khứ: bỏ な + でした ⇒ しずかでした (静かでした)."
        ),
        ConjugationDrillItem(
            id = "a7", baseJp = "きれい", baseKana = "きれい", meaningVi = "đẹp / sạch sẽ",
            groupBadgeVi = "Tính từ đuôi な (Bẫy kết thúc bằng い!)", filter = ConjugationFilter.ADJECTIVE,
            targetFormVi = "Đổi sang Phủ định lịch sự",
            correctAnswer = "きれいじゃありません",
            distractors = listOf("きれくないです", "きれいくないです", "きれいじゃないかった"),
            explanationVi = "Bẫy kinh điển N5! きれい là Tính từ đuôi な nên phủ định là きれいじゃありません (KHÔNG phải きれくない!)."
        ),
        ConjugationDrillItem(
            id = "a8", baseJp = "有名", baseKana = "ゆうめい", meaningVi = "nổi tiếng",
            groupBadgeVi = "Tính từ đuôi な (Bẫy kết thúc bằng い!)", filter = ConjugationFilter.ADJECTIVE,
            targetFormVi = "Đổi sang Quá khứ lịch sự",
            correctAnswer = "ゆうめいでした",
            distractors = listOf("ゆうめかったです", "ゆうめいかったです", "ゆうめいくないです"),
            explanationVi = "有名 (ゆうめい) là Tính từ đuôi な → quá khứ là ゆうめいでした (有名でした)."
        ),
        ConjugationDrillItem(
            id = "a9", baseJp = "便利", baseKana = "べんり", meaningVi = "tiện lợi",
            groupBadgeVi = "Tính từ đuôi な", filter = ConjugationFilter.ADJECTIVE,
            targetFormVi = "Đổi sang Phủ định lịch sự",
            correctAnswer = "べんりじゃありません",
            distractors = listOf("べんりくないです", "べんりなかったです", "べんりじゃないでした"),
            explanationVi = "Tính từ đuôi な phủ định lịch sự: A + じゃありません ⇒ べんりじゃありません."
        ),
        ConjugationDrillItem(
            id = "a10", baseJp = "大きい", baseKana = "おおきい", meaningVi = "to lớn",
            groupBadgeVi = "Tính từ đuôi い", filter = ConjugationFilter.ADJECTIVE,
            targetFormVi = "Đổi sang Quá khứ lịch sự",
            correctAnswer = "おおきかったです",
            distractors = listOf("おおきいでした", "おおきでした", "おおきくないです"),
            explanationVi = "大きい là Tính từ đuôi い → bỏ い + かったです ⇒ おおきかったです."
        )
    )
}
