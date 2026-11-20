package vn.kana.app.domain

import androidx.annotation.DrawableRes
import vn.kana.app.R
import vn.kana.app.ui.map.LessonMapUiState

data class OmikujiSlip(
    val id: String,
    val tierJa: String,
    val tierRomaji: String,
    val tierVi: String,
    val isWarning: Boolean = false,
    @DrawableRes val memeRes: Int,
    val proverbJa: String,
    val proverbRomaji: String,
    val proverbVi: String,
    val studyFortuneVi: String,
    val luckyChar: String,
    val funQuestVi: String
)

data class HankoStampSpec(
    val id: String,
    val sealKanji: String,
    val titleVi: String,
    val conditionVi: String,
    val flavorVi: String,
    @DrawableRes val memeRes: Int,
    val isUnlocked: (LessonMapUiState) -> Boolean
)

object OmikujiAndStampsCatalog {

    val omikujiSlips: List<OmikujiSlip> = listOf(
        OmikujiSlip(
            id = "omi_01",
            tierJa = "大吉",
            tierRomaji = "Daikichi",
            tierVi = "ĐẠI CÁT — ĐỎ NHƯ SON!",
            memeRes = R.drawable.meme_omikuji_king,
            proverbJa = "七転び八起き",
            proverbRomaji = "Nanakorobi yaoki",
            proverbVi = "Bảy lần vấp ngã, tám lần đứng dậy.",
            studyFortuneVi = "Hôm nay đầu óc cực kỳ thông suốt! Học chữ nào nhớ chữ đó, thi thử auto qua điểm liệt.",
            luckyChar = "幸 (Hạnh phúc)",
            funQuestVi = "Nhiệm vụ giữ vía: Vào Ôn tập làm đúng 5 câu liên tiếp."
        ),
        OmikujiSlip(
            id = "omi_02",
            tierJa = "大吉",
            tierRomaji = "Daikichi",
            tierVi = "ĐẠI CÁT — TRÙM CUỐI N5!",
            memeRes = R.drawable.meme_omikuji_boss,
            proverbJa = "継続は力なり",
            proverbRomaji = "Keizoku wa chikara nari",
            proverbVi = "Kiên trì chính là sức mạnh.",
            studyFortuneVi = "Khí chất ngút trời! Hôm nay viết Kanji nét nào ra nét đó, Sensei cũng phải gật gù.",
            luckyChar = "勝 (Chiến thắng)",
            funQuestVi = "Nhiệm vụ giữ vía: Tập viết 3 chữ Kanji mới trong Bảng chữ."
        ),
        OmikujiSlip(
            id = "omi_03",
            tierJa = "中吉",
            tierRomaji = "Chūkichi",
            tierVi = "TRUNG CÁT — ÊM ĐỀM TỐT LÀNH",
            memeRes = R.drawable.meme_omikuji_happy,
            proverbJa = "千里の道も一歩から",
            proverbRomaji = "Senri no michi mo ippo kara",
            proverbVi = "Đường vạn dặm bắt đầu từ một bước chân.",
            studyFortuneVi = "Mọi thứ đang đi đúng quỹ đạo. Đừng vội vàng, cứ chậm mà chắc là thắng lớn.",
            luckyChar = "歩 (Bước đi)",
            funQuestVi = "Nhiệm vụ hôm nay: Đọc 1 bài trong mục Đọc hiểu N5."
        ),
        OmikujiSlip(
            id = "omi_04",
            tierJa = "中吉",
            tierRomaji = "Chūkichi",
            tierVi = "TRUNG CÁT — MÈO ĐỘ HỘ!",
            memeRes = R.drawable.meme_shiba,
            proverbJa = "一期一会",
            proverbRomaji = "Ichigo ichie",
            proverbVi = "Mỗi cuộc gặp gỡ đều đáng trân quý như lần duy nhất.",
            studyFortuneVi = "Duyên với từ vựng hôm nay rất mạnh, dễ gặp lại đúng từ mình vừa ôn.",
            luckyChar = "友 (Bạn bè)",
            funQuestVi = "Nhiệm vụ hôm nay: Lướt nhanh 10 thẻ Flashcard N5."
        ),
        OmikujiSlip(
            id = "omi_05",
            tierJa = "小吉",
            tierRomaji = "Shōkichi",
            tierVi = "TIỂU CÁT — VUI NHẸ NHÀNG",
            memeRes = R.drawable.meme_omikuji_pray,
            proverbJa = "笑う門には福来る",
            proverbRomaji = "Warau kado ni wa fuku kitaru",
            proverbVi = "Cửa nhà hay cười thì phúc tự tìm đến.",
            studyFortuneVi = "Có vài chữ hơi dễ nhầm (như シ/ツ), nhưng cười một cái rồi viết lại là qua tuốt!",
            luckyChar = "笑 (Nụ cười)",
            funQuestVi = "Nhiệm vụ hôm nay: Luyện 1 vòng Chữ dễ nhầm."
        ),
        OmikujiSlip(
            id = "omi_06",
            tierJa = "吉",
            tierRomaji = "Kichi",
            tierVi = "CÁT — ỔN ÁP KHÔNG LO!",
            memeRes = R.drawable.meme_hamster,
            proverbJa = "案ずるより産むが易し",
            proverbRomaji = "Anzuru yori umu ga yasushi",
            proverbVi = "Bắt tay vào làm luôn dễ hơn ngồi lo lắng.",
            studyFortuneVi = "Kanji trông nhiều nét vậy thôi chứ viết 2 lần là quen tay ngay.",
            luckyChar = "力 (Sức lực)",
            funQuestVi = "Nhiệm vụ hôm nay: Nghe 10 câu trong Luyện nghe phản xạ."
        ),
        OmikujiSlip(
            id = "omi_07",
            tierJa = "末吉",
            tierRomaji = "Suekichi",
            tierVi = "MẠT CÁT — HẬU VẬN BÙNG NỔ",
            memeRes = R.drawable.meme_omikuji_silly,
            proverbJa = "石の上にも三年",
            proverbRomaji = "Ishi no ue ni mo sannen",
            proverbVi = "Ngồi trên tảng đá lạnh 3 năm rồi đá cũng ấm lên.",
            studyFortuneVi = "Đầu buổi hơi lú một xíu, nhưng càng ôn về sau càng vào guồng cực mượt!",
            luckyChar = "時 (Thời gian)",
            funQuestVi = "Nhiệm vụ hôm nay: Ôn lại bảng biến âm Số & Giờ."
        ),
        OmikujiSlip(
            id = "omi_08",
            tierJa = "凶",
            tierRomaji = "Kyō",
            tierVi = "HUNG — CẢNH BÁO LÚ NÉT!",
            isWarning = true,
            memeRes = R.drawable.meme_omikuji_shock,
            proverbJa = "失敗は成功のもと",
            proverbRomaji = "Shippai wa seikō no moto",
            proverbVi = "Thất bại là mẹ của thành công.",
            studyFortuneVi = "Ủa alo! Hôm nay dễ nhìn nhầm ぬ với め, hoặc đọc nhầm 4 giờ thành よんじ. Đọc kỹ đề nha bro!",
            luckyChar = "気 (Tinh thần)",
            funQuestVi = "Cách giải hạn: Vào Ôn tập làm xong 5 câu là tự động hoá giải thành Cát!"
        ),
        OmikujiSlip(
            id = "omi_09",
            tierJa = "凶",
            tierRomaji = "Kyō",
            tierVi = "HUNG NHẸ — ĐỪNG LƯỜI NHA!",
            isWarning = true,
            memeRes = R.drawable.meme_cat_grumpy,
            proverbJa = "思い立ったが吉日",
            proverbRomaji = "Omoitatta ga kichijitsu",
            proverbVi = "Ngày bạn quyết tâm hành động chính là ngày tốt nhất.",
            studyFortuneVi = "Cơn buồn ngủ đang rình rập chuỗi Streak của bạn! Đừng để mèo khóc vì đứt chuỗi nha.",
            luckyChar = "今 (Ngay bây giờ)",
            funQuestVi = "Cách giải hạn: Học ngay 5 phút để giữ lửa Streak hôm nay!"
        ),
        OmikujiSlip(
            id = "omi_10",
            tierJa = "大吉",
            tierRomaji = "Daikichi",
            tierVi = "ĐẠI CÁT — VÍA THI ĐỖ N5!",
            memeRes = R.drawable.meme_duck,
            proverbJa = "猿も木から落ちる",
            proverbRomaji = "Saru mo ki kara ochiru",
            proverbVi = "Khỉ giỏi leo cây cũng có lúc trượt tay (sai vài câu không sao cả!).",
            studyFortuneVi = "Quá uy tín! Hôm nay mà bấm vào Thi thử JLPT N5 là có điềm điểm cao chót vót.",
            luckyChar = "学 (Học tập)",
            funQuestVi = "Nhiệm vụ giữ vía: Thử sức ngay 1 đề Thi thử JLPT N5."
        )
    )

    val hankoStamps: List<HankoStampSpec> = listOf(
        HankoStampSpec(
            id = "stamp_first_step",
            sealKanji = "初",
            titleVi = "Khởi Hành N5",
            conditionVi = "Thuộc ít nhất 1 chữ cái bất kỳ",
            flavorVi = "Bước chân đầu tiên trên con đường chinh phục tiếng Nhật! Mèo tặng bạn 1 tràng pháo tay.",
            memeRes = R.drawable.meme_omikuji_pray,
            isUnlocked = { it.totalLearnedCount >= 1 }
        ),
        HankoStampSpec(
            id = "stamp_omikuji",
            sealKanji = "運",
            titleVi = "Tín Đồ Rút Quẻ",
            conditionVi = "Rút quẻ Omikuji Mèo ít nhất 1 lần",
            flavorVi = "Đã xin vía thành công tại đền thờ Mèo! Học hành từ nay cứ gọi là mượt.",
            memeRes = R.drawable.meme_omikuji_happy,
            isUnlocked = { it.settings.omikujiDrawCount >= 1 }
        ),
        HankoStampSpec(
            id = "stamp_hiragana",
            sealKanji = "あ",
            titleVi = "Bá Chủ Hiragana",
            conditionVi = "Thuộc trọn 46 chữ Hiragana cơ bản",
            flavorVi = "46 chữ mềm không còn làm khó được bạn nữa. Cười tươi như mèo Đại Cát!",
            memeRes = R.drawable.meme_omikuji_king,
            isUnlocked = { ui ->
                ui.basicHiragana.isNotEmpty() &&
                    ui.basicHiragana.count { ui.characterProgressByChar[it.char]?.learned == true } >= 46
            }
        ),
        HankoStampSpec(
            id = "stamp_katakana",
            sealKanji = "ア",
            titleVi = "Chiến Thần Katakana",
            conditionVi = "Thuộc trọn 46 chữ Katakana cơ bản",
            flavorVi = "Đọc menu tiếng Nhật & phân biệt シ/ツ như một vị thần đeo kính râm.",
            memeRes = R.drawable.meme_omikuji_boss,
            isUnlocked = { ui ->
                ui.basicKatakana.isNotEmpty() &&
                    ui.basicKatakana.count { ui.characterProgressByChar[it.char]?.learned == true } >= 46
            }
        ),
        HankoStampSpec(
            id = "stamp_streak_3",
            sealKanji = "火",
            titleVi = "Ngọn Lửa 3 Ngày",
            conditionVi = "Đạt chuỗi Streak 3 ngày liên tiếp",
            flavorVi = "Mèo bốc lửa đang cổ vũ bạn cháy hết mình, đừng để tắt bếp nha!",
            memeRes = R.drawable.meme_cat_angry,
            isUnlocked = { it.settings.streakDays >= 3 }
        ),
        HankoStampSpec(
            id = "stamp_streak_7",
            sealKanji = "神",
            titleVi = "Thần Đồng 7 Ngày",
            conditionVi = "Đạt chuỗi Streak 7 ngày liên tiếp",
            flavorVi = "Một tuần không nghỉ ngày nào — đeo kính Thug Life nhận danh hiệu cực ngầu!",
            memeRes = R.drawable.meme_hamster,
            isUnlocked = { it.settings.streakDays >= 7 }
        ),
        HankoStampSpec(
            id = "stamp_kanji_20",
            sealKanji = "漢",
            titleVi = "Thư Pháp Kanji",
            conditionVi = "Thuộc ít nhất 20 chữ Kanji N5",
            flavorVi = "Đã bắt đầu đọc hiểu biển hiệu và câu tiếng Nhật thực tế — đeo kính cận soi chữ cực nét!",
            memeRes = R.drawable.meme_cat_reading,
            isUnlocked = { ui ->
                ui.n5KanjiCharacters.count { ui.characterProgressByChar[it.char]?.learned == true } >= 20
            }
        ),
        HankoStampSpec(
            id = "stamp_mock_try",
            sealKanji = "試",
            titleVi = "Thí Sinh Can Đảm",
            conditionVi = "Hoàn thành ít nhất 1 đề Thi thử JLPT N5",
            flavorVi = "Dám bước vào phòng thi 24 câu là đã hơn 90% người mới rồi! Huh?! Quá dữ!",
            memeRes = R.drawable.meme_omikuji_shock,
            isUnlocked = { it.settings.mockTestAttemptCount >= 1 }
        ),
        HankoStampSpec(
            id = "stamp_mock_pass",
            sealKanji = "勝",
            titleVi = "Đỗ Chuẩn JLPT N5",
            conditionVi = "Đạt từ 60% trở lên trong Thi thử N5",
            flavorVi = "Bàn tay like khổng lồ chứng nhận: Bạn đã sẵn sàng đi thi thật JLPT N5!",
            memeRes = R.drawable.meme_duck,
            isUnlocked = { it.settings.bestMockScorePercent >= 60 }
        ),
        HankoStampSpec(
            id = "stamp_master_150",
            sealKanji = "王",
            titleVi = "Hoàng Thượng N5",
            conditionVi = "Thuộc từ 150 chữ trở lên trong giáo trình",
            flavorVi = "Đỉnh cao tối thượng của Sổ Tay Kana — không còn gì cản được bạn tiến lên N4!",
            memeRes = R.drawable.meme_red_panda,
            isUnlocked = { it.totalLearnedCount >= 150 }
        )
    )
}
