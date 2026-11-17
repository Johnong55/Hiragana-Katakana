package vn.kana.app.domain

data class N5ReadingSentence(
    val japanese: String,
    val kanaOnly: String,
    val vietnamese: String
)

data class N5ReadingQuestion(
    val questionVi: String,
    val optionsVi: List<String>,
    val correctIndex: Int,
    val explanationVi: String
)

data class N5ReadingPassage(
    val id: String,
    val number: Int,
    val titleJa: String,
    val titleVi: String,
    val grammarFocus: String,
    val grammarNoteVi: String,
    val sentences: List<N5ReadingSentence>,
    val questions: List<N5ReadingQuestion>
)

data class N5SentenceBuilderItem(
    val id: String,
    val promptVi: String,
    val prefixJa: String,
    val suffixJa: String,
    val fragments: List<String>,
    val correctOrder: List<Int>,
    val starSlotIndex: Int,
    val fullSentenceJa: String,
    val explanationVi: String
)

object N5ReadingCatalog {

    val passages: List<N5ReadingPassage> = listOf(
        N5ReadingPassage(
            id = "read_01",
            number = 1,
            titleJa = "じこしょうかい",
            titleVi = "Giới thiệu bản thân",
            grammarFocus = "A は B です ・ の",
            grammarNoteVi = "「は」(đọc là wa) đánh dấu chủ đề câu. 「の」nối 2 danh từ (thuộc về / của).",
            sentences = listOf(
                N5ReadingSentence(
                    japanese = "はじめまして。わたしは ミンです。",
                    kanaOnly = "はじめまして。わたしは みんです。",
                    vietnamese = "Rất hân hạnh được gặp bạn. Tôi là Minh."
                ),
                N5ReadingSentence(
                    japanese = "ベトナムの だいがくせいです。",
                    kanaOnly = "べとなむの だいがくせいです。",
                    vietnamese = "Tôi là sinh viên đại học ở Việt Nam."
                ),
                N5ReadingSentence(
                    japanese = "いま にほんごを べんきょうして います。",
                    kanaOnly = "いま にほんごを べんきょうして います。",
                    vietnamese = "Hiện tại tôi đang học tiếng Nhật."
                ),
                N5ReadingSentence(
                    japanese = "どうぞ よろしく おねがいします。",
                    kanaOnly = "どうぞ よろしく おねがいします。",
                    vietnamese = "Rất mong nhận được sự giúp đỡ từ bạn."
                )
            ),
            questions = listOf(
                N5ReadingQuestion(
                    questionVi = "Minh hiện đang làm gì?",
                    optionsVi = listOf(
                        "Là nhân viên công ty ở Nhật",
                        "Là sinh viên đại học ở Việt Nam",
                        "Là giáo viên tiếng Nhật"
                    ),
                    correctIndex = 1,
                    explanationVi = "Câu 2 nói rõ: 「ベトナムの だいがくせいです」(Tôi là sinh viên đại học ở Việt Nam)."
                ),
                N5ReadingQuestion(
                    questionVi = "Hiện tại Minh đang học môn gì?",
                    optionsVi = listOf("Tiếng Anh", "Tiếng Nhật", "Tiếng Hàn"),
                    correctIndex = 1,
                    explanationVi = "Câu 3: 「いま にほんごを べんきょうして います」."
                )
            )
        ),
        N5ReadingPassage(
            id = "read_02",
            number = 2,
            titleJa = "わたしの一日",
            titleVi = "Một ngày của tôi",
            grammarFocus = "Thời gian に ・ 動詞ます",
            grammarNoteVi = "「に」đi sau mốc thời gian cụ thể (6じに). 「を」đánh dấu tân ngữ của hành động.",
            sentences = listOf(
                N5ReadingSentence(
                    japanese = "わたしは まいにち 六じに おきます。",
                    kanaOnly = "わたしは まいにち ろくじに おきます。",
                    vietnamese = "Mỗi ngày tôi thức dậy lúc 6 giờ."
                ),
                N5ReadingSentence(
                    japanese = "あさごはんは パンと たまごを たべます。",
                    kanaOnly = "あさごはんは ぱんと たまごを たべます。",
                    vietnamese = "Bữa sáng tôi ăn bánh mì và trứng."
                ),
                N5ReadingSentence(
                    japanese = "八じから 五じまで かいしゃで はたらきます。",
                    kanaOnly = "はちじから ごじまで かいしゃで はたらきます。",
                    vietnamese = "Tôi làm việc ở công ty từ 8 giờ đến 5 giờ."
                ),
                N5ReadingSentence(
                    japanese = "よるは 十じはんに ねます。",
                    kanaOnly = "よるは じゅうじはんに ねます。",
                    vietnamese = "Buổi tối tôi đi ngủ lúc 10 giờ rưỡi."
                )
            ),
            questions = listOf(
                N5ReadingQuestion(
                    questionVi = "Nhân vật tôi ăn sáng món gì?",
                    optionsVi = listOf(
                        "Cơm và cá",
                        "Bánh mì và trứng",
                        "Mì ramen"
                    ),
                    correctIndex = 1,
                    explanationVi = "「パンと たまごを たべます」nghĩa là ăn bánh mì và trứng."
                ),
                N5ReadingQuestion(
                    questionVi = "Nhân vật tôi làm việc đến mấy giờ?",
                    optionsVi = listOf("5 giờ", "6 giờ", "10 giờ rưỡi"),
                    correctIndex = 0,
                    explanationVi = "「八じから 五じまで」= từ 8 giờ đến 5 giờ."
                )
            )
        ),
        N5ReadingPassage(
            id = "read_03",
            number = 3,
            titleJa = "スーパーで かいもの",
            titleVi = "Mua sắm ở siêu thị",
            grammarFocus = "いくらですか ・ をください",
            grammarNoteVi = "「を ください」dùng khi gọi món hoặc mua đồ. 「と」nối các danh từ (và).",
            sentences = listOf(
                N5ReadingSentence(
                    japanese = "きょうの ゆうがた、スーパーへ いきました。",
                    kanaOnly = "きょうの ゆうがた、すーぱーへ いきました。",
                    vietnamese = "Chiều tối hôm nay tôi đã đi siêu thị."
                ),
                N5ReadingSentence(
                    japanese = "りんごを 三つと ぎゅうにゅうを 一本 かいました。",
                    kanaOnly = "りんごを みっつと ぎゅうにゅうを いっぽん かいました。",
                    vietnamese = "Tôi đã mua 3 quả táo và 1 chai sữa bò."
                ),
                N5ReadingSentence(
                    japanese = "りんごは ひとつ 百えんでした。",
                    kanaOnly = "りんごは ひとつ ひゃくえんでした。",
                    vietnamese = "Táo có giá 100 yên một quả."
                ),
                N5ReadingSentence(
                    japanese = "ぎゅうにゅうは 二百えんでした。",
                    kanaOnly = "ぎゅうにゅうは にひゃくえんでした。",
                    vietnamese = "Sữa bò có giá 200 yên."
                )
            ),
            questions = listOf(
                N5ReadingQuestion(
                    questionVi = "Tổng số tiền mua 3 quả táo và 1 chai sữa là bao nhiêu?",
                    optionsVi = listOf("300 yên", "400 yên", "500 yên"),
                    correctIndex = 2,
                    explanationVi = "3 quả táo x 100 yên = 300 yên, cộng 1 chai sữa 200 yên = 500 yên."
                ),
                N5ReadingQuestion(
                    questionVi = "Người viết đi siêu thị vào lúc nào?",
                    optionsVi = listOf("Sáng nay", "Trưa nay", "Chiều tối nay"),
                    correctIndex = 2,
                    explanationVi = "「きょうの ゆうがた」= chiều tối hôm nay."
                )
            )
        ),
        N5ReadingPassage(
            id = "read_04",
            number = 4,
            titleJa = "しゅうまつ",
            titleVi = "Cuối tuần",
            grammarFocus = "Địa điểm へ いきました ・ で",
            grammarNoteVi = "「へ」(đọc là e) chỉ hướng di chuyển. 「で」chỉ nơi diễn ra hành động.",
            sentences = listOf(
                N5ReadingSentence(
                    japanese = "きのうは 日よう日でした。",
                    kanaOnly = "きのうは にちようびでした。",
                    vietnamese = "Hôm qua là Chủ nhật."
                ),
                N5ReadingSentence(
                    japanese = "わたしは ともだちと おおきい こうえんへ いきました。",
                    kanaOnly = "わたしは ともだちと おおきい こうえんへ いきました。",
                    vietnamese = "Tôi đã đi đến công viên lớn cùng bạn."
                ),
                N5ReadingSentence(
                    japanese = "こうえんで しゃしんを とりました。",
                    kanaOnly = "こうえんで しゃしんを とりました。",
                    vietnamese = "Ở công viên chúng tôi đã chụp ảnh."
                ),
                N5ReadingSentence(
                    japanese = "とても たのしかったです。",
                    kanaOnly = "とても たのしかったです。",
                    vietnamese = "Rất là vui."
                )
            ),
            questions = listOf(
                N5ReadingQuestion(
                    questionVi = "Người viết đã đi công viên cùng ai?",
                    optionsVi = listOf("Đi một mình", "Đi với bạn", "Đi với gia đình"),
                    correctIndex = 1,
                    explanationVi = "「ともだちと ... いきました」= đã đi cùng bạn."
                ),
                N5ReadingQuestion(
                    questionVi = "Họ đã làm gì ở công viên?",
                    optionsVi = listOf("Chụp ảnh", "Đọc sách", "Ăn trưa"),
                    correctIndex = 0,
                    explanationVi = "「こうえんで しゃしんを とりました」= đã chụp ảnh ở công viên."
                )
            )
        ),
        N5ReadingPassage(
            id = "read_05",
            number = 5,
            titleJa = "かぞくの しゃしん",
            titleVi = "Bức ảnh gia đình",
            grammarFocus = "に います ・ に あります",
            grammarNoteVi = "「います」dùng cho người/động vật; 「あります」dùng cho đồ vật/cây cối.",
            sentences = listOf(
                N5ReadingSentence(
                    japanese = "これは わたしの かぞくの しゃしんです。",
                    kanaOnly = "これは わたしの かぞくの しゃしんです。",
                    vietnamese = "Đây là bức ảnh gia đình của tôi."
                ),
                N5ReadingSentence(
                    japanese = "ちちと ははと あねが います。",
                    kanaOnly = "ちちと ははと あねが います。",
                    vietnamese = "Có bố, mẹ và chị gái."
                ),
                N5ReadingSentence(
                    japanese = "しろい いぬも 一ぴき います。",
                    kanaOnly = "しろい いぬも いっぴき います。",
                    vietnamese = "Cũng có một chú chó màu trắng nữa."
                ),
                N5ReadingSentence(
                    japanese = "テーブルの うえに 花が あります。",
                    kanaOnly = "てーぶるの うえに はなが あります。",
                    vietnamese = "Trên bàn có hoa."
                )
            ),
            questions = listOf(
                N5ReadingQuestion(
                    questionVi = "Trong gia đình có chú chó màu gì?",
                    optionsVi = listOf("Màu đen", "Màu trắng", "Màu vàng"),
                    correctIndex = 1,
                    explanationVi = "「しろい いぬ」= chú chó trắng."
                ),
                N5ReadingQuestion(
                    questionVi = "Trên bàn có gì?",
                    optionsVi = listOf("Có hoa", "Có sách", "Có cốc nước"),
                    correctIndex = 0,
                    explanationVi = "「テーブルの うえに 花が あります」= trên bàn có hoa."
                )
            )
        ),
        N5ReadingPassage(
            id = "read_06",
            number = 6,
            titleJa = "すきな きっさてん",
            titleVi = "Quán cà phê yêu thích",
            grammarFocus = "Tính từ い / な ・ が すきです",
            grammarNoteVi = "Đối tượng thích/ghét đi với trợ từ 「が」: コーヒーが すきです.",
            sentences = listOf(
                N5ReadingSentence(
                    japanese = "えきの まえに ちいさい きっさてんが あります。",
                    kanaOnly = "えきの まえに ちいさい きっさてんが あります。",
                    vietnamese = "Trước nhà ga có một quán cà phê nhỏ."
                ),
                N5ReadingSentence(
                    japanese = "その みせは とても しずかです。",
                    kanaOnly = "その みせは とても しずかです。",
                    vietnamese = "Quán đó rất yên tĩnh."
                ),
                N5ReadingSentence(
                    japanese = "わたしは ここで まいしゅう 本を よみます。",
                    kanaOnly = "わたしは ここで まいしゅう ほんを よみます。",
                    vietnamese = "Mỗi tuần tôi đọc sách ở đây."
                ),
                N5ReadingSentence(
                    japanese = "あたたかい コーヒーが だいすきです。",
                    kanaOnly = "あたたかい こーひーが だいすきです。",
                    vietnamese = "Tôi rất thích cà phê ấm."
                )
            ),
            questions = listOf(
                N5ReadingQuestion(
                    questionVi = "Quán cà phê nằm ở đâu?",
                    optionsVi = listOf("Trong trường học", "Trước nhà ga", "Sau công viên"),
                    correctIndex = 1,
                    explanationVi = "「えきの まえに」= ở phía trước nhà ga."
                ),
                N5ReadingQuestion(
                    questionVi = "Quán cà phê có đặc điểm gì?",
                    optionsVi = listOf("Lớn và ồn ào", "Nhỏ và yên tĩnh", "Mới và đắt"),
                    correctIndex = 1,
                    explanationVi = "「ちいさい きっさてん」 (nhỏ) và 「とても しずかです」 (rất yên tĩnh)."
                )
            )
        ),
        N5ReadingPassage(
            id = "read_07",
            number = 7,
            titleJa = "がっこうへの みち",
            titleVi = "Đường đến trường",
            grammarFocus = "Phương tiện で ・ から〜まで",
            grammarNoteVi = "「でんしゃで」= bằng tàu điện. 「〜から 〜まで」= từ ... đến ...",
            sentences = listOf(
                N5ReadingSentence(
                    japanese = "わたしの いえから がっこうまで でんしゃで 三十分 かかります。",
                    kanaOnly = "わたしの いえから がっこうまで でんしゃで さんじゅっぷん かかります。",
                    vietnamese = "Từ nhà tôi đến trường mất 30 phút đi tàu điện."
                ),
                N5ReadingSentence(
                    japanese = "えきから がっこうまでは あるいて 五ふんです。",
                    kanaOnly = "えきから がっこうまでは あるいて ごふんです。",
                    vietnamese = "Từ ga đến trường đi bộ mất 5 phút."
                ),
                N5ReadingSentence(
                    japanese = "あめの日には バスで いきます。",
                    kanaOnly = "あめのひには ばすで いきます。",
                    vietnamese = "Vào ngày mưa tôi đi bằng xe buýt."
                )
            ),
            questions = listOf(
                N5ReadingQuestion(
                    questionVi = "Từ nhà ga đi bộ đến trường mất bao lâu?",
                    optionsVi = listOf("5 phút", "15 phút", "30 phút"),
                    correctIndex = 0,
                    explanationVi = "「えきから がっこうまでは あるいて 五ふんです」= 5 phút."
                ),
                N5ReadingQuestion(
                    questionVi = "Vào ngày mưa tác giả đi bằng gì?",
                    optionsVi = listOf("Đi bộ", "Xe đạp", "Xe buýt"),
                    correctIndex = 2,
                    explanationVi = "「あめの日には バスで いきます」= đi bằng xe buýt."
                )
            )
        ),
        N5ReadingPassage(
            id = "read_08",
            number = 8,
            titleJa = "にほんの きせつ",
            titleVi = "Các mùa ở Nhật Bản",
            grammarFocus = "がいちばん ・ 〜から (Vì)",
            grammarNoteVi = "「〜が いちばん 〜です」dùng để so sánh nhất. 「〜から」ở cuối mệnh đề nghĩa là \"vì...\".",
            sentences = listOf(
                N5ReadingSentence(
                    japanese = "にほんには 春と 夏と 秋と 冬が あります。",
                    kanaOnly = "にほんには はると なつと あきと ふゆが あります。",
                    vietnamese = "Ở Nhật Bản có mùa xuân, hạ, thu và đông."
                ),
                N5ReadingSentence(
                    japanese = "わたしは 秋が いちばん すきです。",
                    kanaOnly = "わたしは あきが いちばん すきです。",
                    vietnamese = "Tôi thích mùa thu nhất."
                ),
                N5ReadingSentence(
                    japanese = "すずしくて、たべものが おいしいですから。",
                    kanaOnly = "すずしくて、たべものが おいしいですから。",
                    vietnamese = "Vì trời mát mẻ và đồ ăn rất ngon."
                )
            ),
            questions = listOf(
                N5ReadingQuestion(
                    questionVi = "Người viết thích mùa nào nhất?",
                    optionsVi = listOf("Mùa xuân", "Mùa thu", "Mùa đông"),
                    correctIndex = 1,
                    explanationVi = "「わたしは 秋が いちばん すきです」= thích mùa thu nhất."
                ),
                N5ReadingQuestion(
                    questionVi = "Vì sao người viết thích mùa đó?",
                    optionsVi = listOf(
                        "Vì có hoa anh đào",
                        "Vì mát mẻ và đồ ăn ngon",
                        "Vì có tuyết rơi"
                    ),
                    correctIndex = 1,
                    explanationVi = "「すずしくて、たべものが おいしいですから」."
                )
            )
        )
    )

    val sentenceBuilders: List<N5SentenceBuilderItem> = listOf(
        N5SentenceBuilderItem(
            id = "sb_01",
            promptVi = "Tôi là học sinh trường tiếng Nhật.",
            prefixJa = "わたしは",
            suffixJa = "。",
            fragments = listOf("がくせいです", "の", "にほんご", "がっこう"),
            correctOrder = listOf(2, 3, 1, 0),
            starSlotIndex = 2,
            fullSentenceJa = "わたしは にほんご がっこう の がくせいです。",
            explanationVi = "「にほんご がっこう」(trường tiếng Nhật) + 「の」 + 「がくせいです」(là học sinh)."
        ),
        N5SentenceBuilderItem(
            id = "sb_02",
            promptVi = "Mỗi sáng tôi uống cà phê lúc 7 giờ.",
            prefixJa = "まいあさ",
            suffixJa = "。",
            fragments = listOf("のみます", "コーヒーを", "七じに", "わたしは"),
            correctOrder = listOf(3, 2, 1, 0),
            starSlotIndex = 1,
            fullSentenceJa = "まいあさ わたしは 七じに コーヒーを のみます。",
            explanationVi = "Chủ ngữ 「わたしは」 → thời gian 「七じに」 → tân ngữ 「コーヒーを」 → động từ 「のみます」."
        ),
        N5SentenceBuilderItem(
            id = "sb_03",
            promptVi = "Hôm qua tôi đã mua sách ở nhà ga.",
            prefixJa = "きのう",
            suffixJa = "。",
            fragments = listOf("かいました", "えきで", "本を", "わたしは"),
            correctOrder = listOf(3, 1, 2, 0),
            starSlotIndex = 2,
            fullSentenceJa = "きのう わたしは えきで 本を かいました。",
            explanationVi = "Nơi chốn diễn ra hành động dùng 「で」 (えきで), trước tân ngữ 「本を」 và động từ quá khứ 「かいました」."
        ),
        N5SentenceBuilderItem(
            id = "sb_04",
            promptVi = "Quyển từ điển này là của anh Tanaka.",
            prefixJa = "この",
            suffixJa = "。",
            fragments = listOf("の", "たなかさん", "じしょは", "です"),
            correctOrder = listOf(2, 1, 0, 3),
            starSlotIndex = 1,
            fullSentenceJa = "この じしょは たなかさん の です。",
            explanationVi = "「この じしょは」 (quyển từ điển này) + 「たなかさん の です」 (là của anh Tanaka)."
        ),
        N5SentenceBuilderItem(
            id = "sb_05",
            promptVi = "Trong phòng có một cái bàn nhỏ.",
            prefixJa = "へやの",
            suffixJa = "。",
            fragments = listOf("あります", "ちいさい", "なかに", "つくえが"),
            correctOrder = listOf(2, 1, 3, 0),
            starSlotIndex = 2,
            fullSentenceJa = "へやの なかに ちいさい つくえが あります。",
            explanationVi = "「へやの なかに」(trong phòng) + 「ちいさい つくえが」(cái bàn nhỏ) + 「あります」(có)."
        ),
        N5SentenceBuilderItem(
            id = "sb_06",
            promptVi = "Chủ nhật tuần sau tôi sẽ đi Kyoto bằng tàu điện.",
            prefixJa = "らいしゅうの 日よう日に",
            suffixJa = "。",
            fragments = listOf("いきます", "きょうとへ", "で", "でんしゃ"),
            correctOrder = listOf(3, 2, 1, 0),
            starSlotIndex = 1,
            fullSentenceJa = "らいしゅうの 日よう日に でんしゃ で きょうとへ いきます。",
            explanationVi = "Phương tiện 「でんしゃ で」 đi trước địa điểm đích 「きょうとへ」 và động từ 「いきます」."
        ),
        N5SentenceBuilderItem(
            id = "sb_07",
            promptVi = "Vì hôm nay trời lạnh nên tôi uống trà nóng.",
            prefixJa = "きょうは",
            suffixJa = "。",
            fragments = listOf("あつい", "おちゃを", "さむいですから", "のみます"),
            correctOrder = listOf(2, 0, 1, 3),
            starSlotIndex = 2,
            fullSentenceJa = "きょうは さむいですから あつい おちゃを のみます。",
            explanationVi = "Mệnh đề nguyên nhân 「さむいですから」 đứng trước kết quả 「あつい おちゃを のみます」."
        ),
        N5SentenceBuilderItem(
            id = "sb_08",
            promptVi = "Trong các môn thể thao, tôi thích bóng đá nhất.",
            prefixJa = "スポーツの なかで",
            suffixJa = "。",
            fragments = listOf("すきです", "いちばん", "が", "サッカー"),
            correctOrder = listOf(3, 2, 1, 0),
            starSlotIndex = 1,
            fullSentenceJa = "スポーツの なかで サッカー が いちばん すきです。",
            explanationVi = "Cấu trúc so sánh nhất: 「Danh từ + が + いちばん + Tính từ です」."
        ),
        N5SentenceBuilderItem(
            id = "sb_09",
            promptVi = "Xin hãy viết tên bằng bút chì.",
            prefixJa = "えんぴつで",
            suffixJa = "。",
            fragments = listOf("を", "かいて", "なまえ", "ください"),
            correctOrder = listOf(2, 0, 1, 3),
            starSlotIndex = 1,
            fullSentenceJa = "えんぴつで なまえ を かいて ください。",
            explanationVi = "「なまえ を」(tên) + 「かいて ください」(xin hãy viết - thể てください)."
        ),
        N5SentenceBuilderItem(
            id = "sb_10",
            promptVi = "Từ nhà tôi đến công ty mất khoảng 20 phút.",
            prefixJa = "いえから",
            suffixJa = "。",
            fragments = listOf("二十ぷん", "かいしゃまで", "かかります", "ぐらい"),
            correctOrder = listOf(1, 0, 3, 2),
            starSlotIndex = 2,
            fullSentenceJa = "いえから かいしゃまで 二十ぷん ぐらい かかります。",
            explanationVi = "「〜から 〜まで」 (từ... đến...) + 「二十ぷん ぐらい」(khoảng 20 phút) + 「かかります」(mất thời gian)."
        )
    )
}
