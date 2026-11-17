package vn.kana.app.domain

/**
 * Một bài viết / mẹo học tiếng Nhật hằng ngày theo phong cách Tạp chí (Mini Magazine).
 */
data class DailyTipArticle(
    val id: String,
    val issueNumber: Int,
    val category: String,
    val titleVi: String,
    val highlightJp: String,
    val highlightRomaji: String,
    val bodyVi: String,
    val funFactVi: String,
    val speakText: String = ""
) {
    val issueLabel: String
        get() = "SỐ #%02d".format(issueNumber)
}

/**
 * Kho Tạp chí Tips học tiếng Nhật 100% offline (xoay vòng mỗi ngày theo epochDay).
 */
object DailyTipsCatalog {

    val categories: List<String> = listOf(
        "Tất cả",
        "Mẹo nhớ chữ",
        "Kanji hình ảnh",
        "Ngữ pháp vui",
        "Văn hoá Nhật"
    )

    val articles: List<DailyTipArticle> = listOf(
        DailyTipArticle(
            id = "tip_01",
            issueNumber = 1,
            category = "Mẹo nhớ chữ",
            titleVi = "Phân biệt シ (shi) và ツ (tsu) trong 3 giây",
            highlightJp = "シ  vs  ツ",
            highlightRomaji = "shi (nhìn ngang)  vs  tsu (nhìn xuống)",
            bodyVi = "Mẹo cực dễ: Hãy tưởng tượng đây là khuôn mặt cười! Chữ シ (shi) có 2 mắt xếp DỌC và nét dài hất từ DƯỚI LÊN trên — giống chữ 'し' (shi) viết nằm ngang. Còn chữ ツ (tsu) có 2 mắt xếp NGANG và nét dài kéo từ TRÊN XUỐNG — giống chữ 'つ' (tsu) đứng thẳng.",
            funFactVi = "Khi viết Katakana, trẻ em Nhật hay đọc thầm: 'shi đi lên, tsu đi xuống' để không bao giờ bị nhầm!",
            speakText = "し、つ"
        ),
        DailyTipArticle(
            id = "tip_02",
            issueNumber = 2,
            category = "Mẹo nhớ chữ",
            titleVi = "Bộ ba dễ lộn: わ (wa) • れ (re) • ね (ne)",
            highlightJp = "わ  •  れ  •  ね",
            highlightRomaji = "wa (tròn)  •  re (đá chân)  •  ne (thắt nơ)",
            bodyVi = "Cả 3 chữ đều bắt đầu bằng một nét sổ dọc giống nhau! Hãy nhớ đuôi của chúng: わ (wa) vòng tròn hiền hoà như quả bóng; れ (re) đá hất chân ra ngoài như người đang chạy rẽ; còn ね (ne) có một vòng xoắn thắt nơ ở đuôi — như đuôi con mèo (neko = ねこ)!",
            funFactVi = "Từ 'neko' (ねこ - con mèo) bắt đầu bằng chính chữ ね có cái đuôi cuộn tròn!",
            speakText = "わ、れ、ね、ねこ"
        ),
        DailyTipArticle(
            id = "tip_03",
            issueNumber = 3,
            category = "Kanji hình ảnh",
            titleVi = "Cách chữ 山 (Núi) và 川 (Sông) ra đời",
            highlightJp = "山  •  川",
            highlightRomaji = "yama (núi)  •  kawa (sông)",
            bodyVi = "Kanji không hề khó nếu bạn nhìn nó như một bức tranh! Chữ 山 (Sơn - núi) chính là hình 3 ngọn núi nhấp nhô với đỉnh cao nhất ở giữa. Chữ 川 (Xuyên - sông) là 3 dòng nước chảy song song từ trên xuống.",
            funFactVi = "Tên ngọn núi biểu tượng của Nhật Bản là 富士山 (Fuji-san), dùng chính chữ 山 này!",
            speakText = "やま、かわ"
        ),
        DailyTipArticle(
            id = "tip_04",
            issueNumber = 4,
            category = "Ngữ pháp vui",
            titleVi = "Tại sao trợ từ は lại đọc là 'wa'?",
            highlightJp = "わたし は 〜 です",
            highlightRomaji = "watashi wa ... desu",
            bodyVi = "Ngày xưa người Nhật đọc chữ は là 'fa/pa', sau đó biến âm dần thành 'wa' khi đứng giữa câu. Khi cải cách chữ viết hiện đại, người Nhật giữ nguyên mặt chữ は cho trợ từ chủ đề để dễ nhận biết câu trúc, còn trong từ vựng bình thường vẫn đọc là 'ha' (như はな - hana).",
            funFactVi = "Tương tự, trợ từ へ (hướng đi) đọc là 'e', và trợ từ を (tân ngữ) đọc là 'o'!",
            speakText = "わたしはがくせいです"
        ),
        DailyTipArticle(
            id = "tip_05",
            issueNumber = 5,
            category = "Mẹo nhớ chữ",
            titleVi = "Phân biệt ソ (so) và ン (n) không bao giờ sai",
            highlightJp = "ソ  vs  ン",
            highlightRomaji = "so (kéo xuống)  vs  n (hất lên)",
            bodyVi = "Giống hệt cặp シ/ツ: Chữ ソ (so) bắt đầu từ trên kéo xuống (cùng chiều với nét chữ 'そ'), nét chấm đứng cao hơn. Chữ ン (n) có nét chấm nằm thấp và nét dài hất từ dưới lên trên bên phải!",
            funFactVi = "Trong từ 'ソフト' (soft - kem tươi) và 'パン' (pan - bánh mì), hai chữ này xuất hiện cực kỳ nhiều.",
            speakText = "そ、ん"
        ),
        DailyTipArticle(
            id = "tip_06",
            issueNumber = 6,
            category = "Kanji hình ảnh",
            titleVi = "Nghỉ ngơi (休) là Người (人) tựa vào Cây (木)",
            highlightJp = "人 + 木 = 休",
            highlightRomaji = "yasumu (nghỉ ngơi)",
            bodyVi = "Chữ 休 (Hưu - nghỉ ngơi) là chữ hội ý kinh điển của tiếng Nhật: bên trái là bộ Nhân đứng 亻 (người), bên phải là chữ 木 (cây). Hình ảnh một người đang tựa lưng vào gốc cây để nghỉ mát!",
            funFactVi = "Ngày nghỉ cuối tuần hay nghỉ lễ ở Nhật gọi là 休み (yasumi).",
            speakText = "やすむ"
        ),
        DailyTipArticle(
            id = "tip_07",
            issueNumber = 7,
            category = "Mẹo nhớ chữ",
            titleVi = "Chữ あ (a) và お (o): Khác nhau ở cái dấu chấm!",
            highlightJp = "あ  vs  お",
            highlightRomaji = "a (3 nét giao nhau)  vs  o (có chấm bay trên)",
            bodyVi = "Chữ あ (a) có nét thứ 3 uốn từ trên xuống cắt qua rồi vòng tròn lớn bên dưới. Chữ お (o) thì nét thứ 2 đi thẳng xuống rồi vòng luôn, và có thêm một dấu chấm nhỏ tách rời ở góc trên bên phải.",
            funFactVi = "Mẹo vui: 'お' có dấu chấm giống như vương miện của Ông hoàng (O)!",
            speakText = "あ、お"
        ),
        DailyTipArticle(
            id = "tip_08",
            issueNumber = 8,
            category = "Văn hoá Nhật",
            titleVi = "Âm ngắt っ (Sokuon) — Nhịp dừng bí mật",
            highlightJp = "きって  vs  きて",
            highlightRomaji = "kitte (con tem)  vs  kite (hãy đến)",
            bodyVi = "Chữ っ nhỏ không đọc thành âm mà tạo ra một nhịp ngắt (nín hơi 1 phách) trước phụ âm đứng sau nó. Đọc thiếu nhịp ngắt này có thể biến 'con tem' (kitte) thành 'hãy đến' (kite)!",
            funFactVi = "Khi gõ bàn phím tiếng Nhật, chỉ cần gõ gấp đôi phụ âm (ví dụ 'kitte') là máy tự hiện chữ っ!",
            speakText = "きって"
        ),
        DailyTipArticle(
            id = "tip_09",
            issueNumber = 9,
            category = "Kanji hình ảnh",
            titleVi = "7 ngày trong tuần theo Ngũ hành Nhật Bản",
            highlightJp = "日月火水木金土",
            highlightRomaji = "Chủ nhật → Thứ bảy",
            bodyVi = "Tên các thứ trong tuần tiếng Nhật cực dễ nhớ vì ghép từ 7 chữ Kanji cơ bản: 日 (Mặt trời - CN), 月 (Mặt trăng - T2), 火 (Hoả - T3), 水 (Thuỷ - T4), 木 (Mộc - T5), 金 (Kim - T6), 土 (Thổ - T7) + 曜日 (youbi).",
            funFactVi = "Học thuộc 7 chữ Kanji này là bạn đọc được lịch Nhật Bản cả năm luôn!",
            speakText = "にちようび、げつようび"
        ),
        DailyTipArticle(
            id = "tip_10",
            issueNumber = 10,
            category = "Ngữ pháp vui",
            titleVi = "Phân biệt います và あります trong 1 nốt nhạc",
            highlightJp = "います  vs  あります",
            highlightRomaji = "imasu (sống)  vs  arimasu (đồ vật)",
            bodyVi = "Cả hai đều có nghĩa là 'có / ở'. Quy tắc vàng thi JLPT N5: Nếu chủ thể biết tự di chuyển (người, chó, mèo, cá...) thì dùng います. Nếu là đồ vật, cây cối, nhà cửa... thì dùng あります!",
            funFactVi = "Ví dụ: 'いぬ が います' (Có con chó) nhưng 'ほん が あります' (Có quyển sách).",
            speakText = "いぬがいます。ほんがあります。"
        ),
        DailyTipArticle(
            id = "tip_11",
            issueNumber = 11,
            category = "Mẹo nhớ chữ",
            titleVi = "Cặp さ (sa) và き (ki): 1 gạch hay 2 gạch?",
            highlightJp = "さ  vs  き",
            highlightRomaji = "sa (1 gạch ngang)  vs  ki (2 gạch ngang)",
            bodyVi = "Hai chữ này có phần thân và nét vòng dưới gần như giống hệt nhau! Điểm khác biệt duy nhất nằm ở phần đầu: さ (sa) chỉ có 1 nét ngang, còn き (ki) có tới 2 nét ngang song song như cái chìa khoá (key) có 2 răng!",
            funFactVi = "Đáng yêu hơn nữa: Trong tiếng Nhật, 'cái chìa khoá' đọc là 'kagi' (かぎ), còn tiếng Anh là 'key' (ki)!",
            speakText = "さ、き"
        ),
        DailyTipArticle(
            id = "tip_12",
            issueNumber = 12,
            category = "Văn hoá Nhật",
            titleVi = "Quy tắc vàng viết nét Kanji & Kana",
            highlightJp = "Trên → Dưới  •  Trái → Phải",
            highlightRomaji = "Thứ tự nét chuẩn",
            bodyVi = "Dù chữ phức tạp đến đâu, 95% chữ Nhật tuân theo 2 quy tắc: (1) Viết từ TRÊN xuống DƯỚI, (2) Viết từ TRÁI sang PHẢI. Riêng nét ngang và nét dọc cắt nhau (như chữ 十, き, あ) thì viết nét NGANG trước, nét DỌC sau.",
            funFactVi = "Viết đúng thứ tự nét giúp chữ cân đối và bộ nhận dạng chữ viết tay chấm điểm cao hơn hẳn!",
            speakText = "じゅう"
        ),
        DailyTipArticle(
            id = "tip_13",
            issueNumber = 13,
            category = "Mẹo nhớ chữ",
            titleVi = "め (me - Đôi mắt) và ぬ (nu - Mì udon)",
            highlightJp = "め  vs  ぬ",
            highlightRomaji = "me (không vòng)  vs  nu (có vòng nhỏ ở đuôi)",
            bodyVi = "Chữ め (me - cũng là từ 'đôi mắt') nét cuối buông mở tự nhiên. Chữ ぬ (nu) thì ở cuối nét thứ 2 phải cuộn lại thành một vòng tròn nhỏ — giống như sợi mì cuộn tròn trên đầu đũa!",
            funFactVi = "Cặp は (ha) và ほ (ho) cũng tương tự: は 1 gạch ngang (3 nét), ほ 2 gạch ngang (4 nét) và nét dọc không nhô lên trên!",
            speakText = "め、ぬ"
        ),
        DailyTipArticle(
            id = "tip_14",
            issueNumber = 14,
            category = "Ngữ pháp vui",
            titleVi = "Bí quyết thêm 'ください' để nói lịch sự tức thì",
            highlightJp = "〜 を ください",
            highlightRomaji = "... wo kudasai",
            bodyVi = "Khi đi du lịch Nhật Bản hay mua đồ trong quán ăn, bạn chỉ cần ghép tên món đồ + を ください ('Cho tôi xin...'). Còn muốn nhờ ai làm gì thì ghép Động từ thể て + ください ('Xin hãy...').",
            funFactVi = "Ví dụ: 'コーヒー を ください' (Cho tôi xin một ly cà phê)!",
            speakText = "コーヒーをください"
        ),
        DailyTipArticle(
            id = "tip_15",
            issueNumber = 15,
            category = "Kanji hình ảnh",
            titleVi = "Chữ 男 (Nam) = Ruộng (田) + Sức lực (力)",
            highlightJp = "田 + 力 = 男",
            highlightRomaji = "otoko (đàn ông)",
            bodyVi = "Chữ 男 (Nam) ghép từ chữ 田 (Điền - ruộng đồng) ở trên và chữ 力 (Lực - sức mạnh) ở dưới. Ngày xưa người đàn ông là lao động chính dùng sức lực trên đồng ruộng!",
            funFactVi = "Còn chữ 安 (An - rẻ/bình yên) là mái nhà 宀 che chở cho người phụ nữ 女 ở bên dưới.",
            speakText = "おとこ"
        ),
        DailyTipArticle(
            id = "tip_16",
            issueNumber = 16,
            category = "Văn hoá Nhật",
            titleVi = "Dấu trường âm (ー) trong Katakana có tác dụng gì?",
            highlightJp = "ケーキ  •  コーヒー",
            highlightRomaji = "keeki (bánh kem)  •  koohii (cà phê)",
            bodyVi = "Dấu gạch ngang 'ー' (chōonpu) trong Katakana dùng để kéo dài nguyên âm đứng ngay trước nó thêm 1 nhịp. Ví dụ 'ケ' đọc là 'ke', thêm 'ー' thành 'kē' (keeki).",
            funFactVi = "Khi viết dọc từ trên xuống (kiểu sách truyện Nhật), dấu 'ー' sẽ xoay dọc thành '|'!",
            speakText = "ケーキ、コーヒー"
        )
    )

    fun getTipForEpochDay(epochDay: Long): DailyTipArticle {
        val idx = ((epochDay % articles.size) + articles.size).toInt() % articles.size
        return articles[idx]
    }
}
