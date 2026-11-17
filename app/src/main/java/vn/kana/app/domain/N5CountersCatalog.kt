package vn.kana.app.domain

enum class CounterCategory(val labelVi: String, val badgeJa: String) {
    HOURS("Giờ (〜時)", "時"),
    MINUTES("Phút (〜分)", "分"),
    DAYS("Ngày (〜日)", "日"),
    THINGS("Đồ vật (〜つ)", "つ"),
    PEOPLE("Người (〜人)", "人"),
    LONG_OBJECTS("Vật dài (〜本)", "本")
}

data class CounterEntry(
    val numberLabel: String,
    val kanji: String,
    val kana: String,
    val romaji: String,
    val isIrregular: Boolean = false,
    val noteVi: String = ""
)

object N5CountersCatalog {

    val entriesByCategory: Map<CounterCategory, List<CounterEntry>> = mapOf(
        CounterCategory.HOURS to listOf(
            CounterEntry("1 giờ", "一時", "いちじ", "ichiji"),
            CounterEntry("2 giờ", "二時", "にじ", "niji"),
            CounterEntry("3 giờ", "三時", "さんじ", "sanji"),
            CounterEntry("4 giờ", "四時", "よじ", "yoji", isIrregular = true, noteVi = "Không đọc よんじ"),
            CounterEntry("5 giờ", "五時", "ごじ", "goji"),
            CounterEntry("6 giờ", "六時", "ろくじ", "rokuji"),
            CounterEntry("7 giờ", "七時", "しちじ", "shichiji", isIrregular = true, noteVi = "Không đọc ななじ"),
            CounterEntry("8 giờ", "八時", "はちじ", "hachiji"),
            CounterEntry("9 giờ", "九時", "くじ", "kuji", isIrregular = true, noteVi = "Không đọc きゅうじ"),
            CounterEntry("10 giờ", "十時", "じゅうじ", "juuji")
        ),
        CounterCategory.MINUTES to listOf(
            CounterEntry("1 phút", "一分", "いっぷん", "ippun", isIrregular = true, noteVi = "Biến âm っ + ぷん"),
            CounterEntry("2 phút", "二分", "にふん", "nifun"),
            CounterEntry("3 phút", "三分", "さんぷん", "sanpun", isIrregular = true, noteVi = "Biến âm ぷん"),
            CounterEntry("4 phút", "四分", "よんぷん", "yonpun", isIrregular = true, noteVi = "Biến âm ぷん"),
            CounterEntry("5 phút", "五分", "ごふん", "gofun"),
            CounterEntry("6 phút", "六分", "ろっぷん", "roppun", isIrregular = true, noteVi = "Biến âm っ + ぷん"),
            CounterEntry("7 phút", "七分", "ななふん", "nanafun"),
            CounterEntry("8 phút", "八分", "はっぷん", "happun", isIrregular = true, noteVi = "Biến âm っ + ぷん"),
            CounterEntry("9 phút", "九分", "きゅうふん", "kyuufun"),
            CounterEntry("10 phút", "十分", "じゅっぷん", "juppun", isIrregular = true, noteVi = "Biến âm っ + ぷん")
        ),
        CounterCategory.DAYS to listOf(
            CounterEntry("Mùng 1", "一日", "ついたち", "tsuitachi", isIrregular = true, noteVi = "Cách đọc đặc biệt"),
            CounterEntry("Mùng 2", "二日", "ふつか", "futsuka", isIrregular = true),
            CounterEntry("Mùng 3", "三日", "みっか", "mikka", isIrregular = true),
            CounterEntry("Mùng 4", "四日", "よっか", "yokka", isIrregular = true),
            CounterEntry("Mùng 5", "五日", "いつか", "itsuka", isIrregular = true),
            CounterEntry("Mùng 6", "六日", "むいか", "muika", isIrregular = true),
            CounterEntry("Mùng 7", "七日", "なのか", "nanoka", isIrregular = true),
            CounterEntry("Mùng 8", "八日", "ようか", "youka", isIrregular = true),
            CounterEntry("Mùng 9", "九日", "ここのか", "kokonoka", isIrregular = true),
            CounterEntry("Mùng 10", "十日", "とおか", "tooka", isIrregular = true),
            CounterEntry("Ngày 14", "十四日", "じゅうよっか", "juuyokka", isIrregular = true),
            CounterEntry("Ngày 20", "二十日", "はつか", "hatsuka", isIrregular = true, noteVi = "Cách đọc đặc biệt"),
            CounterEntry("Ngày 24", "二十四日", "にじゅうよっか", "nijuuyokka", isIrregular = true)
        ),
        CounterCategory.THINGS to listOf(
            CounterEntry("1 cái", "一つ", "ひとつ", "hitotsu"),
            CounterEntry("2 cái", "二つ", "ふたつ", "futatsu"),
            CounterEntry("3 cái", "三つ", "みっつ", "mittsu"),
            CounterEntry("4 cái", "四つ", "よっつ", "yottsu"),
            CounterEntry("5 cái", "五つ", "いつつ", "itsutsu"),
            CounterEntry("6 cái", "六つ", "むっつ", "muttsu"),
            CounterEntry("7 cái", "七つ", "ななつ", "nanatsu"),
            CounterEntry("8 cái", "八つ", "やっつ", "yattsu"),
            CounterEntry("9 cái", "九つ", "ここのつ", "kokonotsu"),
            CounterEntry("10 cái", "十", "とお", "too", isIrregular = true, noteVi = "Không có chữ つ")
        ),
        CounterCategory.PEOPLE to listOf(
            CounterEntry("1 người", "一人", "ひとり", "hitori", isIrregular = true, noteVi = "Đọc đặc biệt"),
            CounterEntry("2 người", "二人", "ふたり", "futari", isIrregular = true, noteVi = "Đọc đặc biệt"),
            CounterEntry("3 người", "三人", "さんにん", "sannin"),
            CounterEntry("4 người", "四人", "よにん", "yonin", isIrregular = true, noteVi = "Không đọc よんにん"),
            CounterEntry("5 người", "五人", "ごにん", "gonin"),
            CounterEntry("6 người", "六人", "ろくにん", "rokunin"),
            CounterEntry("7 người", "七人", "ななにん", "nananin"),
            CounterEntry("8 người", "八人", "はちにん", "hachinin"),
            CounterEntry("9 người", "九人", "きゅうにん", "kyuunin"),
            CounterEntry("10 người", "十人", "じゅうにん", "juunin")
        ),
        CounterCategory.LONG_OBJECTS to listOf(
            CounterEntry("1 cây/chai", "一本", "いっぽん", "ippon", isIrregular = true, noteVi = "Biến âm っぽん"),
            CounterEntry("2 cây/chai", "二本", "にほん", "nihon"),
            CounterEntry("3 cây/chai", "三本", "さんぼん", "sanbon", isIrregular = true, noteVi = "Biến âm ぼん"),
            CounterEntry("4 cây/chai", "四本", "よんほん", "yonhon"),
            CounterEntry("5 cây/chai", "五本", "ごほん", "gohon"),
            CounterEntry("6 cây/chai", "六本", "ろっぽん", "roppon", isIrregular = true, noteVi = "Biến âm っぽん"),
            CounterEntry("7 cây/chai", "七本", "ななほん", "nanahon"),
            CounterEntry("8 cây/chai", "八本", "はっぽん", "happon", isIrregular = true, noteVi = "Biến âm っぽん"),
            CounterEntry("9 cây/chai", "九本", "きゅうほん", "kyuuhon"),
            CounterEntry("10 cây/chai", "十本", "じゅっぽん", "juppon", isIrregular = true, noteVi = "Biến âm っぽん")
        )
    )
}
