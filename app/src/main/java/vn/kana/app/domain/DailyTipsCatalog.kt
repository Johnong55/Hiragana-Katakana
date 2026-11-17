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
        ),
        DailyTipArticle(
            id = "tip_17",
            issueNumber = 17,
            category = "Mẹo nhớ chữ",
            titleVi = "Phân biệt ソ (so) và ン (n) cực nhanh",
            highlightJp = "ソ  vs  ン",
            highlightRomaji = "so  vs  n",
            bodyVi = "Cũng giống cặp シ/ツ: chữ ソ (so) có nét dài kéo TỪ TRÊN XUỐNG giống cột dọc; còn chữ ン (n) có nét dài hất TỪ DƯỚI LÊN giống hàng ngang.",
            funFactVi = "Mẹo: Đọc chữ 'ソ' (so) là 'sổ xuống', còn 'ン' (n) là 'ngang lên'!",
            speakText = "そ、ん"
        ),
        DailyTipArticle(
            id = "tip_18",
            issueNumber = 18,
            category = "Mẹo nhớ chữ",
            titleVi = "Bộ 3 anh em Hiragana dễ nhầm: わ・ね・れ",
            highlightJp = "わ  •  ね  •  れ",
            highlightRomaji = "wa  •  ne  •  re",
            bodyVi = "Cả 3 đều có cột thẳng bên trái. わ (wa) có bụng tròn trơn; ね (ne) có vòng thắt nút tròn ở đuôi; れ (re) đá hất nhọn lên bên phải.",
            funFactVi = "Mẹo nhớ: 'ね' (ne - con mèo) có cuộn len tròn ở đuôi; 'れ' (re - rẽ) đá vút sang phải!",
            speakText = "わ、ね、れ"
        ),
        DailyTipArticle(
            id = "tip_19",
            issueNumber = 19,
            category = "Mẹo nhớ chữ",
            titleVi = "Xúc âm 'っ' (tsu nhỏ) — nhịp nín thở 1 phách",
            highlightJp = "きって  vs  きて",
            highlightRomaji = "kitte (tem thư)  vs  kite (hãy đến)",
            bodyVi = "Chữ 'っ' nhỏ đứng giữa từ không đọc thành 'tsu', mà tạo 1 nhịp ngắt hơi rồi gấp đôi phụ âm đứng sau (kitte, kippu, zasshi). Thiếu 'っ' là đổi hẳn nghĩa từ!",
            funFactVi = "Ví dụ kinh điển: 'おと' (oto = âm thanh) vs 'おっと' (otto = chồng)!",
            speakText = "きって、きて"
        ),
        DailyTipArticle(
            id = "tip_20",
            issueNumber = 20,
            category = "Mẹo nhớ chữ",
            titleVi = "Phân biệt さ (sa) và ち (chi) không bao giờ lộn",
            highlightJp = "さ  vs  ち",
            highlightRomaji = "sa  vs  chi",
            bodyVi = "Chữ ち (chi) trông y hệt số 5 (chỉ có 2 nét), bụng mở sang bên phải. Còn chữ さ (sa) quay lưng ngược lại, bụng mở sang bên trái.",
            funFactVi = "Nhớ nhanh: ち (chi) = số 5 quay phải; き (ki) giống さ (sa) nhưng có tới 2 nét ngang!",
            speakText = "さ、ち"
        ),
        DailyTipArticle(
            id = "tip_21",
            issueNumber = 21,
            category = "Mẹo nhớ chữ",
            titleVi = "Tại sao 'おう' thường đọc kéo dài thành 'ō'?",
            highlightJp = "おはよう  •  がっこう",
            highlightRomaji = "ohayō  •  gakkō",
            bodyVi = "Trong Hiragana, khi chữ kết thúc bằng vần 'o' đi kèm với 'う' (như おう, こう, そう), người Nhật không đọc tách 'o-u' mà kéo dài thành âm 'ō' (ô dài).",
            funFactVi = "Ngoại lệ nhỏ: 'おおい' (nhiều), 'おおきい' (to lớn) thì lại kéo dài bằng chữ 'お' chứ không phải 'う'!",
            speakText = "おはようございます"
        ),
        DailyTipArticle(
            id = "tip_22",
            issueNumber = 22,
            category = "Kanji hình ảnh",
            titleVi = "Chữ 休 (Nghỉ ngơi) = Người (亻) tựa vào Cây (木)",
            highlightJp = "亻 + 木 = 休",
            highlightRomaji = "yasumu (nghỉ ngơi)",
            bodyVi = "Bộ nhân đứng 亻 (người) đứng tựa lưng vào chữ 木 (cây) tạo thành chữ 休 (Hưu) nghĩa là nghỉ ngơi dưới bóng cây.",
            funFactVi = "Từ quen thuộc: 'やすみ' (yasumi = ngày nghỉ), '夏休み' (natsuyasumi = nghỉ hè)!",
            speakText = "やすみ"
        ),
        DailyTipArticle(
            id = "tip_23",
            issueNumber = 23,
            category = "Kanji hình ảnh",
            titleVi = "Chữ 明 (Sáng) = Mặt trời (日) + Mặt trăng (月)",
            highlightJp = "日 + 月 = 明",
            highlightRomaji = "akarui (sáng sủa)  •  ashita (ngày mai)",
            bodyVi = "Ghép 日 (Nhật - mặt trời ban ngày) và 月 (Nguyệt - mặt trăng ban đêm) lại với nhau thành chữ 明 (Minh) nghĩa là sáng rõ, tươi sáng.",
            funFactVi = "Chữ 明日 có thể đọc là 'ashita' (ngày mai) hoặc trang trọng hơn là 'asu'!",
            speakText = "あかるい、あした"
        ),
        DailyTipArticle(
            id = "tip_24",
            issueNumber = 24,
            category = "Kanji hình ảnh",
            titleVi = "Từ 木 (Cây) đến 林 (Rừng thưa) và 森 (Rừng rậm)",
            highlightJp = "木  →  林  →  森",
            highlightRomaji = "ki  →  hayashi  →  mori",
            bodyVi = "1 cái cây là 木 (ki), 2 cái cây đứng cạnh nhau là 林 (hayashi - rừng thưa), còn 3 cái cây tụ lại thành 森 (mori - rừng rậm sâu thẳm)!",
            funFactVi = "Cũng theo logic đó: 1 người 人, 2 người đi theo nhau 从, 3 người tụ tập 衆 (đám đông)!",
            speakText = "き、はやし、もり"
        ),
        DailyTipArticle(
            id = "tip_25",
            issueNumber = 25,
            category = "Kanji hình ảnh",
            titleVi = "Chữ 好 (Thích) = Người mẹ (女) ôm con (子)",
            highlightJp = "女 + 子 = 好",
            highlightRomaji = "suki (thích)",
            bodyVi = "Ghép chữ 女 (Nữ - phụ nữ) và chữ 子 (Tử - đứa trẻ) thành chữ 好 (Hảo) mang nghĩa yêu thích, tốt đẹp.",
            funFactVi = "Trong tiếng Nhật, nói 'すきです' (suki desu) vừa có thể dùng cho đồ ăn, sở thích, vừa dùng để tỏ tình!",
            speakText = "すきです"
        ),
        DailyTipArticle(
            id = "tip_26",
            issueNumber = 26,
            category = "Kanji hình ảnh",
            titleVi = "Chữ 聞 (Nghe) = Đôi tai (耳) áp vào Cánh cửa (門)",
            highlightJp = "門 + 耳 = 聞",
            highlightRomaji = "kiku (nghe / hỏi)  •  shinbun (báo)",
            bodyVi = "Bên ngoài là chữ 門 (Môn - cánh cửa), bên trong là chữ 耳 (Nhĩ - cái tai). Hình ảnh ghé tai vào cánh cửa để nghe ngóng!",
            funFactVi = "Từ '新聞' (shinbun = tờ báo) ghép từ 新 (mới) + 聞 (nghe) — tức là 'những điều mới nghe được'!",
            speakText = "ききます、しんぶん"
        ),
        DailyTipArticle(
            id = "tip_27",
            issueNumber = 27,
            category = "Kanji hình ảnh",
            titleVi = "Chữ 電 (Điện) = Mưa (雨) + Tia chớp dưới đất",
            highlightJp = "雨 + 乚 = 電",
            highlightRomaji = "denki (điện)  •  densha (tàu điện)",
            bodyVi = "Phần đầu của chữ 電 là bộ 雨 (Vũ - mưa), phần dưới mô phỏng tia sét loằng ngoằng đánh xuống mặt đất khi trời mưa giông.",
            funFactVi = "Các từ hiện đại đều dùng chữ 電: 電話 (điện thoại), 電車 (tàu điện), 電気 (đèn điện)!",
            speakText = "でんき、でんしゃ"
        ),
        DailyTipArticle(
            id = "tip_28",
            issueNumber = 28,
            category = "Ngữ pháp vui",
            titleVi = "Bí mật đằng sau trợ từ 'は' (wa) và 'が' (ga)",
            highlightJp = "わたし は  vs  だれ が",
            highlightRomaji = "watashi wa  vs  dare ga",
            bodyVi = "'は' (wa) giới thiệu chủ đề chung đã biết ('Còn về tôi thì...'), còn 'が' (ga) nhấn mạnh vào chủ thể cụ thể hoặc thông tin mới ('Chính ai là người...').",
            funFactVi = "Từ để hỏi (だれ, なに, どこ) đứng làm chủ ngữ luôn đi với 'が', không bao giờ đi với 'は'!",
            speakText = "だれがきますか"
        ),
        DailyTipArticle(
            id = "tip_29",
            issueNumber = 29,
            category = "Ngữ pháp vui",
            titleVi = "Thể quá khứ của tính từ い: bỏ い thêm かった",
            highlightJp = "おいしい  →  おいしかった",
            highlightRomaji = "oishii  →  oishikatta",
            bodyVi = "Nhiều bạn mới hay nói nhầm 'おいしいでした', nhưng tính từ đuôi い phải bỏ い rồi thêm 'かったです' để thành quá khứ lịch sự!",
            funFactVi = "Ngoại lệ duy nhất cần nhớ: 'いい' (tốt) khi chia quá khứ phải biến thành 'よかったです'!",
            speakText = "おいしかったです"
        ),
        DailyTipArticle(
            id = "tip_30",
            issueNumber = 30,
            category = "Ngữ pháp vui",
            titleVi = "'あります' và 'います' — Đừng nhầm đồ vật với thú cưng!",
            highlightJp = "いぬが います  •  ほんが あります",
            highlightRomaji = "inu ga imasu  •  hon ga arimasu",
            bodyVi = "Cả hai đều có nghĩa là 'có / ở'. Tuy nhiên 'います' chỉ dùng cho người và động vật (di chuyển được), còn 'あります' dùng cho đồ vật, cây cối, sự kiện.",
            funFactVi = "Câu hỏi vui: Robot tự hành thì dùng います hay あります? Người Nhật thường dùng あります cho máy móc, trừ robot thú cưng!",
            speakText = "いぬがいます"
        ),
        DailyTipArticle(
            id = "tip_31",
            issueNumber = 31,
            category = "Ngữ pháp vui",
            titleVi = "'へ' (e) và 'に' (ni) khi chỉ hướng đi",
            highlightJp = "にほんへ いきます",
            highlightRomaji = "nihon e ikimasu",
            bodyVi = "Khi đi với động từ di chuyển (いきます, きます, かえります), 'へ' nhấn mạnh hướng đi tới đâu, còn 'に' nhấn mạnh điểm đến cuối cùng. Đều đọc là 'e' khi làm trợ từ!",
            funFactVi = "Trong bài thi JLPT N5, cả 'がっこうへ いきます' và 'がっこうに いきます' đều đúng ngữ pháp!",
            speakText = "にほんへいきます"
        ),
        DailyTipArticle(
            id = "tip_32",
            issueNumber = 32,
            category = "Ngữ pháp vui",
            titleVi = "Cách nói 'Không chỉ... mà còn...' siêu dễ với 'も'",
            highlightJp = "わたしも  •  これも",
            highlightRomaji = "watashi mo  •  kore mo",
            bodyVi = "Trợ từ 'も' (cũng) sẽ thay thế cho 'は', 'が', 'を'. Ví dụ: 'わたしは がくせいです。たなかさんも がくせいです。' (Tôi là học sinh. Anh Tanaka cũng là học sinh.)",
            funFactVi = "Mẹo thi N5: Không bao giờ viết 'はも' hay 'をも' — nhưng với 'へ' hoặc 'に' thì có thể ghép thành 'にも', 'へも'!",
            speakText = "わたしもがくせいです"
        ),
        DailyTipArticle(
            id = "tip_33",
            issueNumber = 33,
            category = "Ngữ pháp vui",
            titleVi = "Chữ 'に' đa năng: Lúc ở thời gian, lúc ở nơi chốn",
            highlightJp = "七じに  •  つくえの うえに",
            highlightRomaji = "shichiji ni  •  tsukue no ue ni",
            bodyVi = "Trợ từ 'に' đi sau mốc thời gian cụ thể (7じに, 日よう日に) — nhưng KHÔNG dùng với thời gian tương đối như きょう (hôm nay), あした (ngày mai), まいにち (mỗi ngày)!",
            funFactVi = "Trong đề N5 hay bẫy: 'あした に いきます' là SAI, phải là 'あした いきます'!",
            speakText = "あした、しちじにいきます"
        ),
        DailyTipArticle(
            id = "tip_34",
            issueNumber = 34,
            category = "Văn hoá Nhật",
            titleVi = "Vì sao người Nhật nói 'いただきます' trước khi ăn?",
            highlightJp = "いただきます",
            highlightRomaji = "itadakimasu",
            bodyVi = "Trước mỗi bữa ăn, người Nhật chắp tay nói 'いただきます' để bày tỏ lòng biết ơn với thiên nhiên, cây cỏ, động vật và người đã nấu bữa ăn đó.",
            funFactVi = "Ăn xong thì nói 'ごちそうさまでした' (gochisōsama deshita — Cảm ơn vì bữa ăn ngon)!",
            speakText = "いただきます"
        ),
        DailyTipArticle(
            id = "tip_35",
            issueNumber = 35,
            category = "Văn hoá Nhật",
            titleVi = "'すみません' — Câu nói quyền năng nhất ở Nhật",
            highlightJp = "すみません",
            highlightRomaji = "sumimasen",
            bodyVi = "Chỉ một từ 'すみません' có thể dùng trong 3 tình huống: Xin lỗi nhẹ ('Tôi xin lỗi'), Gọi người khác ('Xin lỗi, cho tôi hỏi/gọi món'), và Cảm ơn khi ai đó giúp đỡ mình!",
            funFactVi = "Khi vào quán ăn Nhật muốn gọi nhân viên, bạn chỉ cần giơ tay nhẹ và nói 'すみません！'.",
            speakText = "すみません"
        ),
        DailyTipArticle(
            id = "tip_36",
            issueNumber = 36,
            category = "Văn hoá Nhật",
            titleVi = "Văn hoá 'いってきます' và 'いってらっしゃい' mỗi sáng",
            highlightJp = "いってきます  ↔  いってらっしゃい",
            highlightRomaji = "ittekimasu  ↔  itterasshai",
            bodyVi = "Khi rời khỏi nhà, người đi nói 'いってきます' (Tôi đi rồi sẽ về), còn người ở nhà đáp 'いってらっしゃい' (Đi cẩn thận rồi về nhé).",
            funFactVi = "Khi về đến nhà, người về nói 'ただいま' (Tôi đã về), người ở nhà đáp 'おかえりなさい' (Mừng bạn đã về)!",
            speakText = "いってきます"
        ),
        DailyTipArticle(
            id = "tip_37",
            issueNumber = 37,
            category = "Văn hoá Nhật",
            titleVi = "Số 4 và số 9 thường bị né trong bệnh viện ở Nhật",
            highlightJp = "四 (し)  •  九 (く)",
            highlightRomaji = "shi (tử)  •  ku (khổ)",
            bodyVi = "Số 4 đọc là 'し' trùng âm với chữ 死 (tử - chết), và số 9 đọc là 'く' trùng âm với 苦 (khổ - đau khổ). Vì thế nhiều bệnh viện và khách sạn bỏ qua phòng số 4 và số 9.",
            funFactVi = "Đó cũng là lý do người Nhật thường ưu tiên đọc số 4 là 'よん' (yon) và số 9 là 'きゅう' (kyū) khi đếm!",
            speakText = "よん、きゅう"
        ),
        DailyTipArticle(
            id = "tip_38",
            issueNumber = 38,
            category = "Văn hoá Nhật",
            titleVi = "'おつかれさまです' — Lời chào nơi công sở Nhật",
            highlightJp = "おつかれさまです",
            highlightRomaji = "otsukaresama desu",
            bodyVi = "Nghĩa đen là 'Bạn đã vất vả rồi', câu này được dùng cả ngày ở công ty/trường học khi gặp đồng nghiệp, khi nhờ vả, và đặc biệt là khi tan làm ra về.",
            funFactVi = "Dùng với bạn bè thân thiết hoặc cấp dưới có thể rút gọn thành 'おつかれ！'.",
            speakText = "おつかれさまです"
        ),
        DailyTipArticle(
            id = "tip_39",
            issueNumber = 39,
            category = "Văn hoá Nhật",
            titleVi = "Đừng bao giờ cắm đũa thẳng đứng vào bát cơm!",
            highlightJp = "はし (お箸)",
            highlightRomaji = "hashi (đũa)",
            bodyVi = "Cắm đũa thẳng đứng vào bát cơm (tate-bashi) hoặc truyền thức ăn từ đũa sang đũa (hashi-watashi) là điều kiêng kỵ tuyệt đối vì gợi nhớ nghi thức tang lễ ở Nhật.",
            funFactVi = "Nếu chưa ăn tiếp, hãy đặt đũa nằm ngang lên gác đũa (hashioki) hoặc mép khay ăn.",
            speakText = "おはし"
        ),
        DailyTipArticle(
            id = "tip_40",
            issueNumber = 40,
            category = "Văn hoá Nhật",
            titleVi = "Bí quyết học 15 phút mỗi ngày hiệu quả hơn 3 tiếng cuối tuần",
            highlightJp = "まいにち 十五ふん",
            highlightRomaji = "mainichi jūgofun",
            bodyVi = "Não bộ ghi nhớ ngôn ngữ qua lặp lại ngắt quãng (Spaced Repetition). Ôn đều đặn 10–15 phút mỗi ngày giúp chuyển Kana & Kanji vào trí nhớ dài hạn nhanh gấp 3 lần học dồn.",
            funFactVi = "Hãy bật 'Nhắc học hằng ngày' trong Cài đặt và giữ chuỗi Streak 🔥 của bạn mỗi ngày nhé!",
            speakText = "まいにちがんばりましょう"
        )
    )

    fun getTipForEpochDay(epochDay: Long): DailyTipArticle {
        val idx = ((epochDay % articles.size) + articles.size).toInt() % articles.size
        return articles[idx]
    }
}
