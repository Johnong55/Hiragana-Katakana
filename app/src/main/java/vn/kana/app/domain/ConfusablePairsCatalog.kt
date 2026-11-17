package vn.kana.app.domain

data class ConfusableCharItem(
    val char: String,
    val romaji: String,
    val cueVi: String
)

data class ConfusableGroup(
    val id: String,
    val titleVi: String,
    val scriptLabel: String,
    val items: List<ConfusableCharItem>,
    val tipVi: String
)

object ConfusablePairsCatalog {

    val groups: List<ConfusableGroup> = listOf(
        ConfusableGroup(
            id = "kata_shi_tsu",
            titleVi = "シ (shi) vs ツ (tsu)",
            scriptLabel = "Katakana",
            items = listOf(
                ConfusableCharItem("シ", "shi", "2 chấm xếp dọc, nét hất TỪ DƯỚI LÊN"),
                ConfusableCharItem("ツ", "tsu", "2 chấm xếp ngang, nét hất TỪ TRÊN XUỐNG")
            ),
            tipVi = "Mẹo: Nhìn giống mặt cười — シ (shi) nhìn ngang qua phải; ツ (tsu) nhìn từ trên xuống."
        ),
        ConfusableGroup(
            id = "kata_so_n",
            titleVi = "ソ (so) vs ン (n)",
            scriptLabel = "Katakana",
            items = listOf(
                ConfusableCharItem("ソ", "so", "1 chấm trên, nét dài kéo TỪ TRÊN XUỐNG"),
                ConfusableCharItem("ン", "n", "1 chấm trên, nét dài hất TỪ DƯỚI LÊN")
            ),
            tipVi = "Giống quy tắc シ/ツ: ソ kéo xuống theo cột; ン hất lên theo hàng ngang."
        ),
        ConfusableGroup(
            id = "kata_ku_ta_ke",
            titleVi = "ク (ku) vs タ (ta) vs ケ (ke)",
            scriptLabel = "Katakana",
            items = listOf(
                ConfusableCharItem("ク", "ku", "2 nét, không có nét ngang cắt giữa"),
                ConfusableCharItem("タ", "ta", "3 nét, có 1 chấm ngắn cắt giữa như chữ T"),
                ConfusableCharItem("ケ", "ke", "Nét ngang dài thò ra bên trái")
            ),
            tipVi = "ク (ku) trống ruột; タ (ta) có chấm giữa; ケ (ke) mở rộng sang phải từ nét ngang."
        ),
        ConfusableGroup(
            id = "kata_wa_u_fu",
            titleVi = "ワ (wa) vs ウ (u) vs フ (fu)",
            scriptLabel = "Katakana",
            items = listOf(
                ConfusableCharItem("ワ", "wa", "Không có mũ trên đầu, nét dọc trái thẳng"),
                ConfusableCharItem("ウ", "u", "Có 1 chấm dọc nhỏ làm mũ trên đỉnh"),
                ConfusableCharItem("フ", "fu", "Chỉ 1 nét gập đơn giản, không có nét trái")
            ),
            tipVi = "フ = 1 nét; ワ = thêm vách trái; ウ = thêm chấm trên đỉnh."
        ),
        ConfusableGroup(
            id = "hira_nu_me",
            titleVi = "ぬ (nu) vs め (me)",
            scriptLabel = "Hiragana",
            items = listOf(
                ConfusableCharItem("ぬ", "nu", "Có vòng xoắn tròn ở đuôi (giống đuôi lợn)"),
                ConfusableCharItem("め", "me", "Đuôi kéo xuống mượt, KHÔNG có vòng tròn")
            ),
            tipVi = "ぬ (nu) có nút thắt ở cuối; め (me) trơn tru giống hình con mắt."
        ),
        ConfusableGroup(
            id = "hira_wa_ne_re",
            titleVi = "わ (wa) vs ね (ne) vs れ (re)",
            scriptLabel = "Hiragana",
            items = listOf(
                ConfusableCharItem("わ", "wa", "Vòng bụng tròn trơn sang phải"),
                ConfusableCharItem("ね", "ne", "Đuôi thắt nút tròn ở góc dưới phải"),
                ConfusableCharItem("れ", "re", "Đuôi đá hất nhọn lên bên phải")
            ),
            tipVi = "Cả 3 đều có trục dọc thẳng bên trái: わ tròn, ね thắt nút, れ đá lên."
        ),
        ConfusableGroup(
            id = "hira_sa_chi",
            titleVi = "さ (sa) vs ち (chi)",
            scriptLabel = "Hiragana",
            items = listOf(
                ConfusableCharItem("さ", "sa", "Nét xiên nghiêng sang phải, bụng mở sang trái"),
                ConfusableCharItem("ち", "chi", "Giống số 5, bụng tròn mở sang phải")
            ),
            tipVi = "ち (chi) giống số 5 (năm = go -> chi); さ (sa) ngược hướng lại."
        ),
        ConfusableGroup(
            id = "hira_ha_ho_ma",
            titleVi = "は (ha) vs ほ (ho) vs ま (ma)",
            scriptLabel = "Hiragana",
            items = listOf(
                ConfusableCharItem("は", "ha", "1 nét dọc trái + 1 nét ngang bên phải"),
                ConfusableCharItem("ほ", "ho", "1 nét dọc trái + 2 nét ngang song song bên phải"),
                ConfusableCharItem("ま", "ma", "Không có nét dọc trái, 2 nét ngang cắt trục giữa")
            ),
            tipVi = "は (1 ngang), ほ (2 ngang, đội mũ), ま (2 ngang, không có cột trái)."
        )
    )
}
