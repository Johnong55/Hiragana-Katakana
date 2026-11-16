#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script khởi tạo đầy đủ dữ liệu từ vựng tiếng Nhật cho toàn bộ 4 chặng của ứng dụng Kana:
1. 46 Hiragana cơ bản (92 từ)
2. 46 Katakana cơ bản - dùng từ mượn quen thuộc (92 từ)
3. Dakuten / Handakuten Hiragana & Katakana (100 từ)
4. Yōon (ゃゅょ / ャュョ), Sokuon (っ / ッ), Chōonpu (ー) (18 từ)
Tổng cộng: 302 từ vựng, xuất ra app/src/main/assets/vocab/kana_vocab.json và cập nhật REVIEW.md.
"""

import json
from pathlib import Path
from typing import List, Dict, Any

# 1. 46 Hiragana cơ bản (2 từ/chữ = 92 từ)
HIRAGANA_BASIC_VOCAB = [
    # あ行
    {"word": "あめ", "romaji": "ame", "meaning_vi": "cơn mưa / cái kẹo", "chars": ["あ", "め"], "introduces": "あ", "review_note": "あめ vừa có nghĩa là 'cơn mưa' (雨) vừa là 'cái kẹo' (飴)."},
    {"word": "あり", "romaji": "ari", "meaning_vi": "con kiến", "chars": ["あ", "り"], "introduces": "あ"},
    {"word": "いぬ", "romaji": "inu", "meaning_vi": "con chó", "chars": ["い", "ぬ"], "introduces": "い"},
    {"word": "いす", "romaji": "isu", "meaning_vi": "cái ghế", "chars": ["い", "す"], "introduces": "い"},
    {"word": "うみ", "romaji": "umi", "meaning_vi": "biển", "chars": ["う", "み"], "introduces": "う"},
    {"word": "うし", "romaji": "ushi", "meaning_vi": "con bò", "chars": ["う", "し"], "introduces": "う"},
    {"word": "うえ", "romaji": "ue", "meaning_vi": "phía trên", "chars": ["う", "え"], "introduces": "え"},
    {"word": "えび", "romaji": "ebi", "meaning_vi": "con tôm", "chars": ["え", "び"], "introduces": "え"},
    {"word": "あおい", "romaji": "aoi", "meaning_vi": "màu xanh dương", "chars": ["あ", "お", "い"], "introduces": "お"},
    {"word": "おに", "romaji": "oni", "meaning_vi": "con quỷ (truyện cổ tích)", "chars": ["お", "に"], "introduces": "お"},
    # か行
    {"word": "かさ", "romaji": "kasa", "meaning_vi": "cái ô (dù)", "chars": ["か", "さ"], "introduces": "か"},
    {"word": "かお", "romaji": "kao", "meaning_vi": "khuôn mặt", "chars": ["か", "お"], "introduces": "か"},
    {"word": "えき", "romaji": "eki", "meaning_vi": "nhà ga", "chars": ["え", "き"], "introduces": "き"},
    {"word": "きつね", "romaji": "kitsune", "meaning_vi": "con cáo", "chars": ["き", "つ", "ね"], "introduces": "き"},
    {"word": "くつ", "romaji": "kutsu", "meaning_vi": "đôi giày", "chars": ["く", "つ"], "introduces": "く"},
    {"word": "きく", "romaji": "kiku", "meaning_vi": "hoa cúc", "chars": ["き", "く"], "introduces": "く"},
    {"word": "いけ", "romaji": "ike", "meaning_vi": "cái ao", "chars": ["い", "け"], "introduces": "け"},
    {"word": "けむし", "romaji": "kemushi", "meaning_vi": "con sâu róm", "chars": ["け", "む", "し"], "introduces": "け"},
    {"word": "こい", "romaji": "koi", "meaning_vi": "cá chép", "chars": ["こ", "い"], "introduces": "こ"},
    {"word": "こま", "romaji": "koma", "meaning_vi": "con quay (đồ chơi)", "chars": ["こ", "ま"], "introduces": "こ"},
    # さ行
    {"word": "さかな", "romaji": "sakana", "meaning_vi": "con cá", "chars": ["さ", "か", "な"], "introduces": "さ"},
    {"word": "さる", "romaji": "saru", "meaning_vi": "con khỉ", "chars": ["さ", "る"], "introduces": "さ"},
    {"word": "しか", "romaji": "shika", "meaning_vi": "con hươu / nai", "chars": ["し", "か"], "introduces": "し"},
    {"word": "あし", "romaji": "ashi", "meaning_vi": "đôi chân", "chars": ["あ", "し"], "introduces": "し"},
    {"word": "すいか", "romaji": "suika", "meaning_vi": "quả dưa hấu", "chars": ["す", "い", "か"], "introduces": "す"},
    {"word": "すし", "romaji": "sushi", "meaning_vi": "món sushi", "chars": ["す", "し"], "introduces": "す"},
    {"word": "せみ", "romaji": "semi", "meaning_vi": "con ve sầu", "chars": ["せ", "み"], "introduces": "せ"},
    {"word": "あせ", "romaji": "ase", "meaning_vi": "mồ hôi", "chars": ["あ", "せ"], "introduces": "せ"},
    {"word": "そら", "romaji": "sora", "meaning_vi": "bầu trời", "chars": ["そ", "ら"], "introduces": "そ"},
    {"word": "うそ", "romaji": "uso", "meaning_vi": "lời nói dối", "chars": ["う", "そ"], "introduces": "そ"},
    # た行
    {"word": "たこ", "romaji": "tako", "meaning_vi": "bạch tuộc / cánh diều", "chars": ["た", "こ"], "introduces": "た", "review_note": "たこ vừa là 'bạch tuộc' vừa là 'cánh diều'."},
    {"word": "うた", "romaji": "uta", "meaning_vi": "bài hát", "chars": ["う", "た"], "introduces": "た"},
    {"word": "くち", "romaji": "kuchi", "meaning_vi": "cái miệng", "chars": ["く", "ち"], "introduces": "ち"},
    {"word": "いち", "romaji": "ichi", "meaning_vi": "số một (1)", "chars": ["い", "ち"], "introduces": "ち"},
    {"word": "つき", "romaji": "tsuki", "meaning_vi": "mặt trăng", "chars": ["つ", "き"], "introduces": "つ"},
    {"word": "つくえ", "romaji": "tsukue", "meaning_vi": "cái bàn học", "chars": ["つ", "く", "え"], "introduces": "つ"},
    {"word": "て", "romaji": "te", "meaning_vi": "bàn tay", "chars": ["て"], "introduces": "て"},
    {"word": "てがみ", "romaji": "tegami", "meaning_vi": "lá thư", "chars": ["て", "が", "み"], "introduces": "て"},
    {"word": "とけい", "romaji": "tokei", "meaning_vi": "đồng hồ", "chars": ["と", "け", "い"], "introduces": "と"},
    {"word": "とり", "romaji": "tori", "meaning_vi": "con chim", "chars": ["と", "り"], "introduces": "と"},
    # な行
    {"word": "なつ", "romaji": "natsu", "meaning_vi": "mùa hè", "chars": ["な", "つ"], "introduces": "な"},
    {"word": "なし", "romaji": "nashi", "meaning_vi": "quả lê", "chars": ["な", "し"], "introduces": "な"},
    {"word": "にく", "romaji": "niku", "meaning_vi": "thịt", "chars": ["に", "く"], "introduces": "に"},
    {"word": "かに", "romaji": "kani", "meaning_vi": "con cua", "chars": ["か", "に"], "introduces": "に"},
    {"word": "ぬの", "romaji": "nuno", "meaning_vi": "tấm vải", "chars": ["ぬ", "の"], "introduces": "ぬ"},
    {"word": "たぬき", "romaji": "tanuki", "meaning_vi": "con lửng chó (tanuki)", "chars": ["た", "ぬ", "き"], "introduces": "ぬ"},
    {"word": "ねこ", "romaji": "neko", "meaning_vi": "con mèo", "chars": ["ね", "こ"], "introduces": "ね"},
    {"word": "ふね", "romaji": "fune", "meaning_vi": "con thuyền", "chars": ["ふ", "ね"], "introduces": "ね"},
    {"word": "つの", "romaji": "tsuno", "meaning_vi": "cái sừng", "chars": ["つ", "の"], "introduces": "の"},
    {"word": "きのこ", "romaji": "kinoko", "meaning_vi": "cây nấm", "chars": ["き", "の", "こ"], "introduces": "の"},
    # は行
    {"word": "はな", "romaji": "hana", "meaning_vi": "bông hoa / cái mũi", "chars": ["は", "な"], "introduces": "は"},
    {"word": "はこ", "romaji": "hako", "meaning_vi": "cái hộp", "chars": ["は", "こ"], "introduces": "は"},
    {"word": "ひと", "romaji": "hito", "meaning_vi": "con người", "chars": ["ひ", "と"], "introduces": "ひ"},
    {"word": "ひこうき", "romaji": "hikouki", "meaning_vi": "máy bay", "chars": ["ひ", "こ", "う", "き"], "introduces": "ひ"},
    {"word": "ふく", "romaji": "fuku", "meaning_vi": "quần áo", "chars": ["ふ", "く"], "introduces": "ふ"},
    {"word": "ふえ", "romaji": "fue", "meaning_vi": "cái sáo / còi", "chars": ["ふ", "え"], "introduces": "ふ"},
    {"word": "へび", "romaji": "hebi", "meaning_vi": "con rắn", "chars": ["へ", "び"], "introduces": "へ"},
    {"word": "へそ", "romaji": "heso", "meaning_vi": "cái rốn", "chars": ["へ", "そ"], "introduces": "へ"},
    {"word": "ほし", "romaji": "hoshi", "meaning_vi": "ngôi sao", "chars": ["ほ", "し"], "introduces": "ほ"},
    {"word": "ほね", "romaji": "hone", "meaning_vi": "cái xương", "chars": ["ほ", "ね"], "introduces": "ほ"},
    # ま行
    {"word": "まくら", "romaji": "makura", "meaning_vi": "cái gối", "chars": ["ま", "く", "ら"], "introduces": "ま"},
    {"word": "まめ", "romaji": "mame", "meaning_vi": "hạt đậu", "chars": ["ま", "め"], "introduces": "ま"},
    {"word": "みみ", "romaji": "mimi", "meaning_vi": "đôi tai", "chars": ["み", "み"], "introduces": "み"},
    {"word": "みかん", "romaji": "mikan", "meaning_vi": "quả quýt", "chars": ["み", "か", "ん"], "introduces": "み"},
    {"word": "むし", "romaji": "mushi", "meaning_vi": "côn trùng / sâu bọ", "chars": ["む", "し"], "introduces": "む"},
    {"word": "むね", "romaji": "mune", "meaning_vi": "lồng ngực", "chars": ["む", "ね"], "introduces": "む"},
    {"word": "め", "romaji": "me", "meaning_vi": "đôi mắt", "chars": ["め"], "introduces": "め"},
    {"word": "めがね", "romaji": "megane", "meaning_vi": "cặp kính mắt", "chars": ["め", "が", "ね"], "introduces": "め"},
    {"word": "もも", "romaji": "momo", "meaning_vi": "quả đào", "chars": ["も", "も"], "introduces": "も"},
    {"word": "くも", "romaji": "kumo", "meaning_vi": "đám mây / con nhện", "chars": ["く", "も"], "introduces": "も"},
    # や行
    {"word": "やま", "romaji": "yama", "meaning_vi": "ngọn núi", "chars": ["や", "ま"], "introduces": "や"},
    {"word": "やさい", "romaji": "yasai", "meaning_vi": "rau củ", "chars": ["や", "さ", "い"], "introduces": "や"},
    {"word": "ゆき", "romaji": "yuki", "meaning_vi": "tuyết", "chars": ["ゆ", "き"], "introduces": "ゆ"},
    {"word": "ゆび", "romaji": "yubi", "meaning_vi": "ngón tay", "chars": ["ゆ", "び"], "introduces": "ゆ"},
    {"word": "よる", "romaji": "yoru", "meaning_vi": "ban đêm", "chars": ["よ", "る"], "introduces": "よ"},
    {"word": "ひよこ", "romaji": "hiyoko", "meaning_vi": "gà con", "chars": ["ひ", "よ", "こ"], "introduces": "よ"},
    # ら行
    {"word": "さくら", "romaji": "sakura", "meaning_vi": "hoa anh đào", "chars": ["さ", "く", "ら"], "introduces": "ら"},
    {"word": "とら", "romaji": "tora", "meaning_vi": "con hổ", "chars": ["と", "ら"], "introduces": "ら"},
    {"word": "りす", "romaji": "risu", "meaning_vi": "con sóc", "chars": ["り", "す"], "introduces": "り"},
    {"word": "りんご", "romaji": "ringo", "meaning_vi": "quả táo", "chars": ["り", "ん", "ご"], "introduces": "り"},
    {"word": "くるま", "romaji": "kuruma", "meaning_vi": "xe ô tô", "chars": ["く", "る", "ま"], "introduces": "る"},
    {"word": "かえる", "romaji": "kaeru", "meaning_vi": "con ếch", "chars": ["か", "え", "る"], "introduces": "る"},
    {"word": "れもん", "romaji": "remon", "meaning_vi": "quả chanh vàng", "chars": ["れ", "も", "ん"], "introduces": "れ"},
    {"word": "すみれ", "romaji": "sumire", "meaning_vi": "hoa tím (violet)", "chars": ["す", "み", "れ"], "introduces": "れ"},
    {"word": "しろ", "romaji": "shiro", "meaning_vi": "màu trắng", "chars": ["し", "ろ"], "introduces": "ろ"},
    {"word": "くろ", "romaji": "kuro", "meaning_vi": "màu đen", "chars": ["く", "ろ"], "introduces": "ろ"},
    # わ行 + ん
    {"word": "わに", "romaji": "wani", "meaning_vi": "con cá sấu", "chars": ["わ", "に"], "introduces": "わ"},
    {"word": "かわ", "romaji": "kawa", "meaning_vi": "dòng sông", "chars": ["か", "わ"], "introduces": "わ"},
    {"word": "ほんをよむ", "romaji": "hon wo yomu", "meaning_vi": "đọc sách (trợ từ を)", "chars": ["ほ", "ん", "を", "よ", "む"], "introduces": "を", "review_note": "Cụm động từ ngắn dùng trợ từ を."},
    {"word": "えをかく", "romaji": "e wo kaku", "meaning_vi": "vẽ tranh (trợ từ を)", "chars": ["え", "を", "か", "く"], "introduces": "を"},
    {"word": "ほん", "romaji": "hon", "meaning_vi": "quyển sách", "chars": ["ほ", "ん"], "introduces": "ん"},
    {"word": "きりん", "romaji": "kirin", "meaning_vi": "hươu cao cổ", "chars": ["き", "り", "ん"], "introduces": "ん"},
]

# 2. 46 Katakana cơ bản - dùng từ mượn quen thuộc (2 từ/chữ = 92 từ)
KATAKANA_BASIC_VOCAB = [
    # ア行
    {"word": "アイス", "romaji": "aisu", "meaning_vi": "kem (ice cream)", "chars": ["ア", "イ", "ス"], "introduces": "ア"},
    {"word": "アニメ", "romaji": "anime", "meaning_vi": "phim hoạt hình", "chars": ["ア", "ニ", "メ"], "introduces": "ア"},
    {"word": "インク", "romaji": "inku", "meaning_vi": "mực viết", "chars": ["イ", "ン", "ク"], "introduces": "イ"},
    {"word": "イチゴ", "romaji": "ichigo", "meaning_vi": "quả dâu tây", "chars": ["イ", "チ", "ゴ"], "introduces": "イ"},
    {"word": "ウサギ", "romaji": "usagi", "meaning_vi": "con thỏ", "chars": ["ウ", "サ", "ギ"], "introduces": "ウ"},
    {"word": "ウール", "romaji": "uuru", "meaning_vi": "len (wool)", "chars": ["ウ", "ー", "ル"], "introduces": "ウ"},
    {"word": "エアコン", "romaji": "eakon", "meaning_vi": "máy điều hoà", "chars": ["エ", "ア", "コ", "ン"], "introduces": "エ"},
    {"word": "エプロン", "romaji": "epuron", "meaning_vi": "tạp dề", "chars": ["エ", "プ", "ロ", "ン"], "introduces": "エ"},
    {"word": "オレンジ", "romaji": "orenji", "meaning_vi": "quả cam", "chars": ["オ", "レ", "ン", "ジ"], "introduces": "オ"},
    {"word": "タオル", "romaji": "taoru", "meaning_vi": "khăn bông", "chars": ["タ", "オ", "ル"], "introduces": "オ"},
    # カ行
    {"word": "カメラ", "romaji": "kamera", "meaning_vi": "máy ảnh", "chars": ["カ", "メ", "ラ"], "introduces": "カ"},
    {"word": "カレー", "romaji": "karee", "meaning_vi": "món cà ri", "chars": ["カ", "レ", "ー"], "introduces": "カ"},
    {"word": "キウイ", "romaji": "kiui", "meaning_vi": "quả kiwi", "chars": ["キ", "ウ", "イ"], "introduces": "キ"},
    {"word": "スキー", "romaji": "sukii", "meaning_vi": "trượt tuyết", "chars": ["ス", "キ", "ー"], "introduces": "キ"},
    {"word": "クラス", "romaji": "kurasu", "meaning_vi": "lớp học", "chars": ["ク", "ラ", "ス"], "introduces": "ク"},
    {"word": "マイク", "romaji": "maiku", "meaning_vi": "chiếc micro", "chars": ["マ", "イ", "ク"], "introduces": "ク"},
    {"word": "ケーキ", "romaji": "keeki", "meaning_vi": "bánh kem", "chars": ["ケ", "ー", "キ"], "introduces": "ケ"},
    {"word": "バケツ", "romaji": "baketsu", "meaning_vi": "cái xô", "chars": ["バ", "ケ", "ツ"], "introduces": "ケ"},
    {"word": "コーヒー", "romaji": "koohii", "meaning_vi": "cà phê", "chars": ["コ", "ー", "ヒ", "ー"], "introduces": "コ"},
    {"word": "コアラ", "romaji": "koara", "meaning_vi": "gấu túi koala", "chars": ["コ", "ア", "ラ"], "introduces": "コ"},
    # サ行
    {"word": "サラダ", "romaji": "sarada", "meaning_vi": "món salad", "chars": ["サ", "ラ", "ダ"], "introduces": "サ"},
    {"word": "サッカー", "romaji": "sakkaa", "meaning_vi": "bóng đá", "chars": ["サ", "ッ", "カ", "ー"], "introduces": "サ"},
    {"word": "シャツ", "romaji": "shatsu", "meaning_vi": "áo sơ mi", "chars": ["シ", "ャ", "ツ"], "introduces": "シ"},
    {"word": "タクシー", "romaji": "takushii", "meaning_vi": "xe taxi", "chars": ["タ", "ク", "シ", "ー"], "introduces": "シ"},
    {"word": "スプーン", "romaji": "supuun", "meaning_vi": "cái thìa (muỗng)", "chars": ["ス", "プ", "ー", "ン"], "introduces": "ス"},
    {"word": "バス", "romaji": "basu", "meaning_vi": "xe buýt", "chars": ["バ", "ス"], "introduces": "ス"},
    {"word": "セーター", "romaji": "seetaa", "meaning_vi": "áo len", "chars": ["セ", "ー", "タ", "ー"], "introduces": "セ"},
    {"word": "セット", "romaji": "setto", "meaning_vi": "bộ (set)", "chars": ["セ", "ッ", "ト"], "introduces": "セ"},
    {"word": "ソファ", "romaji": "sofa", "meaning_vi": "ghế sofa", "chars": ["ソ", "フ", "ァ"], "introduces": "ソ"},
    {"word": "ソース", "romaji": "soosu", "meaning_vi": "nước sốt", "chars": ["ソ", "ー", "ス"], "introduces": "ソ"},
    # タ行
    {"word": "タイヤ", "romaji": "taiya", "meaning_vi": "lốp xe", "chars": ["タ", "イ", "ヤ"], "introduces": "タ"},
    {"word": "ギター", "romaji": "gitaa", "meaning_vi": "đàn ghi-ta", "chars": ["ギ", "タ", "ー"], "introduces": "タ"},
    {"word": "チーズ", "romaji": "chiizu", "meaning_vi": "phô mai", "chars": ["チ", "ー", "ズ"], "introduces": "チ"},
    {"word": "チキン", "romaji": "chikin", "meaning_vi": "thịt gà", "chars": ["チ", "キ", "ン"], "introduces": "チ"},
    {"word": "ツアー", "romaji": "tsuaa", "meaning_vi": "chuyến du lịch (tour)", "chars": ["ツ", "ア", "ー"], "introduces": "ツ"},
    {"word": "スーツ", "romaji": "suutsu", "meaning_vi": "bộ đồ vest", "chars": ["ス", "ー", "ツ"], "introduces": "ツ"},
    {"word": "テレビ", "romaji": "terebi", "meaning_vi": "ti vi", "chars": ["テ", "レ", "ビ"], "introduces": "テ"},
    {"word": "テント", "romaji": "tento", "meaning_vi": "cái lều", "chars": ["テ", "ン", "ト"], "introduces": "テ"},
    {"word": "トマト", "romaji": "tomato", "meaning_vi": "quả cà chua", "chars": ["ト", "マ", "ト"], "introduces": "ト"},
    {"word": "ノート", "romaji": "nooto", "meaning_vi": "quyển vở", "chars": ["ノ", "ー", "ト"], "introduces": "ト"},
    # ナ行
    {"word": "ナイフ", "romaji": "naifu", "meaning_vi": "con dao", "chars": ["ナ", "イ", "フ"], "introduces": "ナ"},
    {"word": "バナナ", "romaji": "banana", "meaning_vi": "quả chuối", "chars": ["バ", "ナ", "ナ"], "introduces": "ナ"},
    {"word": "テニス", "romaji": "tenisu", "meaning_vi": "môn quần vợt", "chars": ["テ", "ニ", "ス"], "introduces": "ニ"},
    {"word": "メニュー", "romaji": "menyuu", "meaning_vi": "thực đơn", "chars": ["メ", "ニ", "ュ", "ー"], "introduces": "ニ"},
    {"word": "カヌー", "romaji": "kanuu", "meaning_vi": "thuyền ca-nô", "chars": ["カ", "ヌ", "ー"], "introduces": "ヌ"},
    {"word": "ヌードル", "romaji": "nuudoru", "meaning_vi": "mì sợi (noodle)", "chars": ["ヌ", "ー", "ド", "ル"], "introduces": "ヌ"},
    {"word": "ネクタイ", "romaji": "nekutai", "meaning_vi": "cà vạt", "chars": ["ネ", "ク", "タ", "イ"], "introduces": "ネ"},
    {"word": "ネット", "romaji": "netto", "meaning_vi": "tấm lưới / mạng", "chars": ["ネ", "ッ", "ト"], "introduces": "ネ"},
    {"word": "ピアノ", "romaji": "piano", "meaning_vi": "đàn dương cầm", "chars": ["ピ", "ア", "ノ"], "introduces": "ノ"},
    {"word": "カジノ", "romaji": "kajino", "meaning_vi": "sòng bài", "chars": ["カ", "ジ", "ノ"], "introduces": "ノ", "review_note": "Có thể đổi sang ノック (gõ cửa) nếu muốn gần gũi hơn với trẻ em."},
    # ハ行
    {"word": "ハム", "romaji": "hamu", "meaning_vi": "thịt nguội (ham)", "chars": ["ハ", "ム"], "introduces": "ハ"},
    {"word": "ハンカチ", "romaji": "hankachi", "meaning_vi": "khăn tay", "chars": ["ハ", "ン", "カ", "チ"], "introduces": "ハ"},
    {"word": "ヒーロー", "romaji": "hiiroo", "meaning_vi": "anh hùng", "chars": ["ヒ", "ー", "ロ", "ー"], "introduces": "ヒ"},
    {"word": "ヒント", "romaji": "hinto", "meaning_vi": "gợi ý (hint)", "chars": ["ヒ", "ン", "ト"], "introduces": "ヒ"},
    {"word": "フォーク", "romaji": "fooku", "meaning_vi": "cái nĩa", "chars": ["フ", "ォ", "ー", "ク"], "introduces": "フ"},
    {"word": "フルーツ", "romaji": "furuutsu", "meaning_vi": "trái cây", "chars": ["フ", "ル", "ー", "ツ"], "introduces": "フ"},
    {"word": "ヘルメット", "romaji": "herumetto", "meaning_vi": "mũ bảo hiểm", "chars": ["ヘ", "ル", "メ", "ッ", "ト"], "introduces": "ヘ"},
    {"word": "ヘリ", "romaji": "heri", "meaning_vi": "trực thăng (viết tắt)", "chars": ["ヘ", "リ"], "introduces": "ヘ"},
    {"word": "ホテル", "romaji": "hoteru", "meaning_vi": "khách sạn", "chars": ["ホ", "テ", "ル"], "introduces": "ホ"},
    {"word": "ボタン", "romaji": "botan", "meaning_vi": "cúc áo / nút bấm", "chars": ["ボ", "タ", "ン"], "introduces": "ホ", "review_note": "ボタン dùng ボ (biến thể dakuten của ホ); từ thứ 2 có ホ nguyên bản là ホース (vòi nước)."},
    # マ行
    {"word": "マスク", "romaji": "masuku", "meaning_vi": "khẩu trang", "chars": ["マ", "ス", "ク"], "introduces": "マ"},
    {"word": "マッチ", "romaji": "macchi", "meaning_vi": "que diêm", "chars": ["マ", "ッ", "チ"], "introduces": "マ"},
    {"word": "ミルク", "romaji": "miruku", "meaning_vi": "sữa", "chars": ["ミ", "ル", "ク"], "introduces": "ミ"},
    {"word": "ミシン", "romaji": "mishin", "meaning_vi": "máy khâu", "chars": ["ミ", "シ", "ン"], "introduces": "ミ"},
    {"word": "ガム", "romaji": "gamu", "meaning_vi": "kẹo cao su", "chars": ["ガ", "ム"], "introduces": "ム"},
    {"word": "チーム", "romaji": "chiimu", "meaning_vi": "đội / nhóm", "chars": ["チ", "ー", "ム"], "introduces": "ム"},
    {"word": "メロン", "romaji": "meron", "meaning_vi": "dưa lưới", "chars": ["メ", "ロ", "ン"], "introduces": "メ"},
    {"word": "メモ", "romaji": "memo", "meaning_vi": "ghi chú", "chars": ["メ", "モ"], "introduces": "メ"},
    {"word": "モデル", "romaji": "moderu", "meaning_vi": "mô hình / người mẫu", "chars": ["モ", "デ", "ル"], "introduces": "モ"},
    {"word": "モニター", "romaji": "monitaa", "meaning_vi": "màn hình", "chars": ["モ", "ニ", "タ", "ー"], "introduces": "モ"},
    # ヤ行
    {"word": "ヤギ", "romaji": "yagi", "meaning_vi": "con dê", "chars": ["ヤ", "ギ"], "introduces": "ヤ"},
    {"word": "ダイヤ", "romaji": "daiya", "meaning_vi": "kim cương", "chars": ["ダ", "イ", "ヤ"], "introduces": "ヤ"},
    {"word": "ユニフォーム", "romaji": "yunifoomu", "meaning_vi": "đồng phục", "chars": ["ユ", "ニ", "フ", "ォ", "ー", "ム"], "introduces": "ユ"},
    {"word": "ユーモア", "romaji": "yuumoa", "meaning_vi": "sự hài hước", "chars": ["ユ", "ー", "モ", "ア"], "introduces": "ユ"},
    {"word": "ヨット", "romaji": "yotto", "meaning_vi": "thuyền buồm", "chars": ["ヨ", "ッ", "ト"], "introduces": "ヨ"},
    {"word": "クレヨン", "romaji": "kureyon", "meaning_vi": "bút sáp màu", "chars": ["ク", "レ", "ヨ", "ン"], "introduces": "ヨ"},
    # ラ行
    {"word": "ラジオ", "romaji": "rajio", "meaning_vi": "đài radio", "chars": ["ラ", "ジ", "オ"], "introduces": "ラ"},
    {"word": "ラーメン", "romaji": "raamen", "meaning_vi": "mì ramen", "chars": ["ラ", "ー", "メ", "ン"], "introduces": "ラ"},
    {"word": "リボン", "romaji": "ribon", "meaning_vi": "dải ruy-băng", "chars": ["リ", "ボ", "ン"], "introduces": "リ"},
    {"word": "スリッパ", "romaji": "surippa", "meaning_vi": "dép đi trong nhà", "chars": ["ス", "リ", "ッ", "パ"], "introduces": "リ"},
    {"word": "ルール", "romaji": "ruuru", "meaning_vi": "quy tắc / luật", "chars": ["ル", "ー", "ル"], "introduces": "ル"},
    {"word": "ボール", "romaji": "booru", "meaning_vi": "quả bóng", "chars": ["ボ", "ー", "ル"], "introduces": "ル"},
    {"word": "レモン", "romaji": "remon", "meaning_vi": "quả chanh vàng", "chars": ["レ", "モ", "ン"], "introduces": "レ"},
    {"word": "トイレ", "romaji": "toire", "meaning_vi": "nhà vệ sinh", "chars": ["ト", "イ", "レ"], "introduces": "レ"},
    {"word": "ロボット", "romaji": "robotto", "meaning_vi": "người máy (robot)", "chars": ["ロ", "ボ", "ッ", "ト"], "introduces": "ロ"},
    {"word": "キロ", "romaji": "kiro", "meaning_vi": "ki-lô (kg / km)", "chars": ["キ", "ロ"], "introduces": "ロ"},
    # ワ行 + ン
    {"word": "ワイン", "romaji": "wain", "meaning_vi": "rượu vang", "chars": ["ワ", "イ", "ン"], "introduces": "ワ"},
    {"word": "シャツワンピ", "romaji": "shatsuwanpi", "meaning_vi": "váy liền sơ mi", "chars": ["シ", "ャ", "ツ", "ワ", "ン", "ピ"], "introduces": "ワ"},
    {"word": "ヲタク", "romaji": "wotaku", "meaning_vi": "người đam mê (otaku)", "chars": ["ヲ", "タ", "ク"], "introduces": "ヲ", "review_note": "Chữ ヲ rất hiếm dùng trong tiếng Nhật hiện đại."},
    {"word": "ビデオヲミル", "romaji": "bideo wo miru", "meaning_vi": "xem video (trợ từ ヲ)", "chars": ["ビ", "デ", "オ", "ヲ", "ミ", "ル"], "introduces": "ヲ"},
    {"word": "パン", "romaji": "pan", "meaning_vi": "bánh mì", "chars": ["パ", "ン"], "introduces": "ン"},
    {"word": "ライオン", "romaji": "raion", "meaning_vi": "sư tử", "chars": ["ラ", "イ", "オ", "ン"], "introduces": "ン"},
]

# Chỉnh lại カジノ -> ノック, ボタン -> ホース, シャツワンピ -> タワー để mọi từ đều chứa đúng chữ introduces và ngắn gọn
for entry in KATAKANA_BASIC_VOCAB:
    if entry["word"] == "カジノ":
        entry["word"] = "ノック"
        entry["romaji"] = "nokku"
        entry["meaning_vi"] = "tiếng gõ cửa"
        entry["chars"] = ["ノ", "ッ", "ク"]
    if entry["word"] == "ボタン" and entry["introduces"] == "ホ":
        entry["word"] = "ホース"
        entry["romaji"] = "hoosu"
        entry["meaning_vi"] = "vòi nước"
        entry["chars"] = ["ホ", "ー", "ス"]
    if entry["word"] == "シャツワンピ":
        entry["word"] = "タワー"
        entry["romaji"] = "tawaa"
        entry["meaning_vi"] = "ngọn tháp"
        entry["chars"] = ["タ", "ワ", "ー"]

# 3. Dakuten / Handakuten (Hiragana & Katakana) + Yōon / Sokuon / Chōonpu
HIRAGANA_DAKUTEN_SPECS = [
    ("が", [("かぎ", "kagi", "chìa khoá"), ("めがね", "megane", "kính mắt")]),  # Sửa かぎ -> がっこう, めがね
    ("ぎ", [("かぎ", "kagi", "chìa khoá"), ("うさぎ", "usagi", "con thỏ")]),
    ("ぐ", [("かぐ", "kagu", "đồ nội thất"), ("どんぐり", "donguri", "quả sồi")]),
    ("げ", [("ひげ", "hige", "râu"), ("かげ", "kage", "cái bóng")]),
    ("ご", [("りんご", "ringo", "quả táo"), ("ごはん", "gohan", "cơm")]),
    ("ざ", [("ひざ", "hiza", "đầu gối"), ("ざぶとん", "zabuton", "đệm ngồi")]),
    ("じ", [("にじ", "niji", "cầu vồng"), ("くじら", "kujira", "cá voi")]),
    ("ず", [("みず", "mizu", "nước"), ("すずめ", "suzume", "chim sẻ")]),
    ("ぜ", [("かぜ", "kaze", "cơn gió / cảm lạnh"), ("ぜんぶ", "zenbu", "tất cả")]),
    ("ぞ", [("ぞう", "zou", "con voi"), ("かぞく", "kazoku", "gia đình")]),
    ("だ", [("えだ", "eda", "cành cây"), ("なみだ", "namida", "nước mắt")]),
    ("ぢ", [("はなぢ", "hanaji", "chảy máu cam"), ("ちぢむ", "chijimu", "co lại")]),
    ("づ", [("つづく", "tsuzuku", "tiếp tục"), ("みかづき", "mikazuki", "trăng lưỡi liềm")]),
    ("で", [("でんわ", "denwa", "điện thoại"), ("うで", "ude", "cánh tay")]),
    ("ど", [("まど", "mado", "cửa sổ"), ("ぶどう", "budou", "quả nho")]),
    ("ば", [("かばん", "kaban", "cặp sách"), ("つばめ", "tsubame", "chim én")]),
    ("び", [("えび", "ebi", "con tôm"), ("ゆび", "yubi", "ngón tay")]),
    ("ぶ", [("ぶた", "buta", "con lợn (heo)"), ("ぶどう", "budou", "quả nho")]),
    ("べ", [("かべ", "kabe", "bức tường"), ("なべ", "nabe", "cái nồi")]),
    ("ぼ", [("ぼうし", "boushi", "cái mũ"), ("とんぼ", "tonbo", "chuồn chuồn")]),
    ("ぱ", [("かっぱ", "kappa", "áo mưa"), ("らっぱ", "rappa", "cái kèn")]),
    ("ぴ", [("えんぴつ", "enpitsu", "bút chì"), ("ぴかぴか", "pikapika", "lấp lánh")]),
    ("ぷ", [("てんぷら", "tenpura", "món tempura"), ("きっぷ", "kippu", "vé tàu")]),
    ("ぺ", [("ほっぺ", "hoppe", "má (khuôn mặt)"), ("ぺこぺこ", "pekopeko", "đói cồn cào")]),
    ("ぽ", [("しっぽ", "shippo", "cái đuôi"), ("さんぽ", "sanpo", "đi dạo")]),
]
# Đổi từ đầu tiên của が thành がっこう
HIRAGANA_DAKUTEN_SPECS[0] = ("が", [("がっこう", "gakkou", "trường học"), ("めがね", "megane", "kính mắt")])

KATAKANA_DAKUTEN_SPECS = [
    ("ガ", [("ガラス", "garasu", "kính / thuỷ tinh"), ("ガム", "gamu", "kẹo cao su")]),
    ("ギ", [("ギター", "gitaa", "đàn ghi-ta"), ("カギ", "kagi", "chìa khoá")]),
    ("グ", [("グラス", "gurasu", "cốc thuỷ tinh"), ("グループ", "guruupu", "nhóm")]),
    ("ゲ", [("ゲーム", "geemu", "trò chơi"), ("ゲスト", "gesuto", "khách mời")]),
    ("ゴ", [("ゴリラ", "gorira", "khỉ đột"), ("イチゴ", "ichigo", "dâu tây")]),
    ("ザ", [("ピザ", "piza", "bánh pizza"), ("デザート", "dezaato", "món tráng miệng")]),
    ("ジ", [("ジュース", "juusu", "nước trái cây"), ("ページ", "peeji", "trang sách")]),
    ("ズ", [("ズボン", "zubon", "quần dài"), ("チーズ", "chiizu", "phô mai")]),
    ("ゼ", [("ゼリー", "zerii", "thạch rau câu"), ("ゼロ", "zero", "số không (0)")]),
    ("ゾ", [("ゾーン", "zoon", "khu vực"), ("ナゾ", "nazo", "câu đố")]),
    ("ダ", [("ダンス", "dansu", "nhảy múa"), ("パンダ", "panda", "gấu trúc")]),
    ("ヂ", [("ヂーゼル", "jiizeru", "động cơ diesel"), ("ハナヂ", "hanaji", "máu cam")]),
    ("ヅ", [("ツヅキ", "tsuzuki", "phần tiếp theo"), ("ミカヅキ", "mikazuki", "trăng lưỡi liềm")]),
    ("デ", [("デスク", "desuku", "bàn làm việc"), ("ビデオ", "bideo", "video")]),
    ("ド", [("ドア", "doa", "cánh cửa"), ("ベッド", "beddo", "cái giường")]),
    ("バ", [("バス", "basu", "xe buýt"), ("バナナ", "banana", "quả chuối")]),
    ("ビ", [("テレビ", "terebi", "ti vi"), ("ビル", "biru", "toà nhà")]),
    ("ブ", [("ブラシ", "burashi", "bàn chải"), ("テーブル", "teeburu", "cái bàn")]),
    ("ベ", [("ベッド", "beddo", "cái giường"), ("ベル", "beru", "cái chuông")]),
    ("ボ", [("ボール", "booru", "quả bóng"), ("ボタン", "botan", "cúc áo")]),
    ("パ", [("パン", "pan", "bánh mì"), ("パパ", "papa", "bố / ba")]),
    ("ピ", [("ピアノ", "piano", "đàn dương cầm"), ("ピンク", "pinku", "màu hồng")]),
    ("プ", [("プール", "puuru", "hồ bơi"), ("コップ", "koppu", "cái cốc")]),
    ("ペ", [("ペン", "pen", "cây bút"), ("ペット", "petto", "thú cưng")]),
    ("ポ", [("ポスト", "posuto", "hòm thư"), ("ポケット", "poketto", "túi áo")]),
]

SMALL_AND_CHOON_SPECS = [
    # Hiragana Yōon & Sokuon
    ("ゃ", "hiragana", [("しゃしん", "shashin", "bức ảnh"), ("おちゃ", "ocha", "trà xanh")]),
    ("ゅ", "hiragana", [("きゅうり", "kyuuri", "dưa chuột"), ("きんぎょ", "kingyo", "cá vàng")]),
    ("ょ", "hiragana", [("きょう", "kyou", "hôm nay"), ("としょかん", "toshokan", "thư viện")]),
    ("っ", "hiragana", [("きって", "kitte", "con tem"), ("ねっこ", "nekko", "rễ cây")]),
    # Katakana Yōon, Sokuon & Chōonpu
    ("ャ", "katakana", [("シャツ", "shatsu", "áo sơ mi"), ("キャンプ", "kyanpu", "cắm trại")]),
    ("ュ", "katakana", [("ジュース", "juusu", "nước ép"), ("メニュー", "menyuu", "thực đơn")]),
    ("ョ", "katakana", [("チョコ", "choko", "sô-cô-la"), ("ヨット", "yotto", "thuyền buồm")]),
    ("ッ", "katakana", [("コップ", "koppu", "cái cốc"), ("ベッド", "beddo", "cái giường")]),
    ("ー", "katakana", [("ケーキ", "keeki", "bánh kem"), ("コーヒー", "koohii", "cà phê")]),
]
# Chỉnh từ thứ 2 của ゅ và ョ để luôn chứa đúng chữ introduces
SMALL_AND_CHOON_SPECS[1] = ("ゅ", "hiragana", [("きゅうり", "kyuuri", "dưa chuột"), ("しゅくだい", "shukudai", "bài tập về nhà")])
SMALL_AND_CHOON_SPECS[6] = ("ョ", "katakana", [("チョコ", "choko", "sô-cô-la"), ("ショップ", "shoppu", "cửa hàng")])


def build_example_sentence(word: str, romaji: str, meaning_vi: str, script: str) -> Dict[str, str]:
    custom = {
        "あめ": ("あめ が ふる。", "ame ga furu.", "Trời mưa."),
        "あり": ("ちいさい あり が あるく。", "chiisai ari ga aruku.", "Con kiến nhỏ đang đi bộ."),
        "いぬ": ("かわいい いぬ が はしる。", "kawaii inu ga hashiru.", "Chú chó dễ thương đang chạy."),
        "いす": ("いす に すわる。", "isu ni suwaru.", "Ngồi lên chiếc ghế."),
        "うみ": ("あおい うみ へ いく。", "aoi umi e iku.", "Đi ra biển xanh."),
        "うし": ("うし が くさ を たべる。", "ushi ga kusa wo taberu.", "Con bò ăn cỏ."),
        "うえ": ("つくえ の うえ に ある。", "tsukue no ue ni aru.", "Ở phía trên cái bàn."),
        "えび": ("おいしい えび を たべる。", "oishii ebi wo taberu.", "Ăn món tôm ngon."),
        "あおい": ("そら が あおい です。", "sora ga aoi desu.", "Bầu trời màu xanh dương."),
        "おに": ("あかい おに が くる。", "akai oni ga kuru.", "Con quỷ đỏ đang đến."),
        "かさ": ("あかい かさ を さす。", "akai kasa wo sasu.", "Che chiếc ô màu đỏ."),
        "かお": ("あさ かお を あらう。", "asa kao wo arau.", "Buổi sáng rửa mặt."),
        "えき": ("でんしゃ で えき へ いく。", "densha de eki e iku.", "Đi tàu điện đến nhà ga."),
        "きつね": ("やま に きつね が いる。", "yama ni kitsune ga iru.", "Trên núi có con cáo."),
        "くつ": ("あたらしい くつ を はく。", "atarashii kutsu wo haku.", "Đi đôi giày mới."),
        "きく": ("きいろい きく が さく。", "kiiroi kiku ga saku.", "Hoa cúc vàng nở."),
        "いけ": ("いけ に こい が およぐ。", "ike ni koi ga oyogu.", "Cá chép bơi trong ao."),
        "けむし": ("はっぱ に けむし が いる。", "happa ni kemushi ga iru.", "Trên lá có con sâu róm."),
        "こい": ("おおきい こい を みる。", "ookii koi wo miru.", "Ngắm con cá chép lớn."),
        "こま": ("こま を まわして あそぶ。", "koma wo mawashite asobu.", "Quay con vụ để chơi."),
        "さかな": ("かわ で さかな を つる。", "kawa de sakana wo tsuru.", "Câu cá ở trên sông."),
        "さる": ("さる が き に のぼる。", "saru ga ki ni noboru.", "Con khỉ trèo lên cây."),
        "しか": ("こうえん に しか が いる。", "kouen ni shika ga iru.", "Trong công viên có con hươu."),
        "あし": ("あし が はやい です。", "ashi ga hayai desu.", "Đôi chân chạy rất nhanh."),
        "すいか": ("なつ に すいか を たべる。", "natsu ni suika wo taberu.", "Mùa hè ăn dưa hấu."),
        "すし": ("わたし は すし が すき です。", "watashi wa sushi ga suki desu.", "Tôi thích món sushi."),
        "せみ": ("き で せみ が なく。", "ki de semi ga naku.", "Ve sầu kêu trên cây."),
        "あせ": ("あつい ので あせ が でる。", "atsui node ase ga deru.", "Trời nóng nên đổ mồ hôi."),
        "そら": ("ひろい そら を みあげる。", "hiroi sora wo miageru.", "Ngước nhìn bầu trời rộng lớn."),
        "うそ": ("うそ を ついて は いけない。", "uso wo tsuite wa ikenai.", "Không được nói dối."),
        "たこ": ("うみ に たこ が いる。", "umi ni tako ga iru.", "Dưới biển có con bạch tuộc."),
        "うた": ("たのしい うた を うたう。", "tanoshii uta wo utau.", "Hát bài ca vui vẻ."),
        "くち": ("おおきく くち を あける。", "ookiku kuchi wo akeru.", "Há miệng thật to."),
        "いち": ("いち から かぞえる。", "ichi kara kazoeru.", "Đếm từ số một."),
        "つき": ("よる に まるい つき が でる。", "yoru ni marui tsuki ga deru.", "Đêm xuống mặt trăng tròn hiện lên."),
        "つくえ": ("つくえ で べんきょう する。", "tsukue de benkyou suru.", "Học bài trên bàn học."),
        "て": ("きれい に て を あらう。", "kirei ni te wo arau.", "Rửa tay thật sạch."),
        "てがみ": ("ともだち に てがみ を かく。", "tomodachi ni tegami wo kaku.", "Viết thư cho bạn bè."),
        "とけい": ("とけい の はり を みる。", "tokei no hari wo miru.", "Nhìn kim đồng hồ."),
        "とり": ("とり が そら を とぶ。", "tori ga sora wo tobu.", "Con chim bay trên bầu trời."),
        "なつ": ("あつい なつ が きた。", "atsui natsu ga kita.", "Mùa hè nóng bức đã đến."),
        "なし": ("あまい なし を たべる。", "amai nashi wo taberu.", "Ăn quả lê ngọt."),
        "にく": ("おいしい にく を やく。", "oishii niku wo yaku.", "Nướng miếng thịt ngon."),
        "かに": ("すなはま に かに が いる。", "sunahama ni kani ga iru.", "Trên bãi cát có con cua."),
        "ぬの": ("しろい ぬの で ふく を ぬう。", "shiroi nuno de fuku wo nuu.", "May áo bằng tấm vải trắng."),
        "ねこ": ("ねこ が ひるね を する。", "neko ga hirune wo suru.", "Con mèo đang ngủ trưa."),
        "ふね": ("ふね に のって わたる。", "fune ni notte wataru.", "Đi thuyền qua sông."),
        "はな": ("にわ に きれい な はな が さく。", "niwa ni kirei na hana ga saku.", "Hoa đẹp nở trong vườn."),
        "はこ": ("はこ の なか を あける。", "hako no naka wo akeru.", "Mở bên trong chiếc hộp ra."),
        "ひと": ("やさしい ひと に あう。", "yasashii hito ni au.", "Gặp người tốt bụng."),
        "ひこうき": ("ひこうき が たかく とぶ。", "hikouki ga takaku tobu.", "Máy bay bay thật cao."),
        "ふく": ("あたたかい ふく を きる。", "atatakai fuku wo kiru.", "Mặc quần áo ấm."),
        "ほし": ("よぞら に ほし が ひかる。", "yozora ni hoshi ga hikaru.", "Ngôi sao lấp lánh trên bầu trời đêm."),
        "みみ": ("うさぎ は みみ が ながい。", "usagi wa mimi ga nagai.", "Con thỏ có đôi tai dài."),
        "みかん": ("ふゆ に みかん を たべる。", "fuyu ni mikan wo taberu.", "Mùa đông ăn quả quýt."),
        "め": ("め を とじて やすむ。", "me wo tojite yasumu.", "Nhắm mắt lại và nghỉ ngơi."),
        "めがね": ("あたらしい めがね を かける。", "atarashii megane wo kakeru.", "Đeo cặp kính mắt mới."),
        "もも": ("あまい もも を たべる。", "amai momo wo taberu.", "Ăn quả đào ngọt."),
        "やま": ("たかい やま に のぼる。", "takai yama ni noboru.", "Leo lên ngọn núi cao."),
        "ゆき": ("しろい ゆき が ふる。", "shiroi yuki ga furu.", "Tuyết trắng rơi."),
        "よる": ("しずか な よる に ねむる。", "shizuka na yoru ni nemuru.", "Ngủ trong đêm yên tĩnh."),
        "さくら": ("はる に さくら が さく。", "haru ni sakura ga saku.", "Mùa xuân hoa anh đào nở."),
        "くるま": ("くるま に のって でかける。", "kuruma ni notte dekakeru.", "Lên xe ô tô đi dạo."),
        "ほん": ("まいにち ほん を よむ。", "mainichi hon wo yomu.", "Mỗi ngày đều đọc sách."),
        "アイス": ("つめたい アイス を たべる。", "tsumetai aisu wo taberu.", "Ăn cây kem mát lạnh."),
        "テレビ": ("かぞく と テレビ を みる。", "kazoku to terebi wo miru.", "Xem ti vi cùng gia đình."),
        "コーヒー": ("あたたかい コーヒー を のむ。", "atatakai koohii wo nomu.", "Uống tách cà phê ấm."),
        "カメラ": ("カメラ で しゃしん を とる。", "kamera de shashin wo toru.", "Chụp ảnh bằng máy ảnh."),
        "ケーキ": ("たんじょうび に ケーキ を たべる。", "tanjoubi ni keeki wo taberu.", "Ăn bánh kem vào ngày sinh nhật."),
        "パン": ("あさごはん に パン を たべる。", "asagohan ni pan wo taberu.", "Ăn bánh mì vào bữa sáng."),
    }
    if word in custom:
        jp, ro, vi = custom[word]
        return {
            "example_sentence": jp,
            "example_romaji": ro,
            "example_vi": vi,
        }
    return {
        "example_sentence": f"これ は {word} です。",
        "example_romaji": f"kore wa {romaji} desu.",
        "example_vi": f"Đây là {meaning_vi}.",
    }


def main() -> None:
    repo_root = Path(__file__).resolve().parent.parent
    out_json = repo_root / "app" / "src" / "main" / "assets" / "vocab" / "kana_vocab.json"
    out_json.parent.mkdir(parents=True, exist_ok=True)

    all_words: List[Dict[str, Any]] = []
    counter = 1

    for entry in HIRAGANA_BASIC_VOCAB:
        ex = build_example_sentence(entry["word"], entry["romaji"], entry["meaning_vi"], "hiragana")
        all_words.append({
            "id": f"vocab_{counter:03d}",
            "word": entry["word"],
            "romaji": entry["romaji"],
            "meaning_vi": entry["meaning_vi"],
            "chars": list(entry["word"]),
            "introduces": entry["introduces"],
            "script": "hiragana",
            **ex,
        })
        counter += 1

    for entry in KATAKANA_BASIC_VOCAB:
        ex = build_example_sentence(entry["word"], entry["romaji"], entry["meaning_vi"], "katakana")
        all_words.append({
            "id": f"vocab_{counter:03d}",
            "word": entry["word"],
            "romaji": entry["romaji"],
            "meaning_vi": entry["meaning_vi"],
            "chars": list(entry["word"]),
            "introduces": entry["introduces"],
            "script": "katakana",
            **ex,
        })
        counter += 1

    for ch, pairs in HIRAGANA_DAKUTEN_SPECS:
        for w, romaji, vi in pairs:
            ex = build_example_sentence(w, romaji, vi, "hiragana")
            all_words.append({
                "id": f"vocab_{counter:03d}",
                "word": w,
                "romaji": romaji,
                "meaning_vi": vi,
                "chars": list(w),
                "introduces": ch,
                "script": "hiragana",
                **ex,
            })
            counter += 1

    for ch, pairs in KATAKANA_DAKUTEN_SPECS:
        for w, romaji, vi in pairs:
            ex = build_example_sentence(w, romaji, vi, "katakana")
            all_words.append({
                "id": f"vocab_{counter:03d}",
                "word": w,
                "romaji": romaji,
                "meaning_vi": vi,
                "chars": list(w),
                "introduces": ch,
                "script": "katakana",
                **ex,
            })
            counter += 1

    for ch, script_type, pairs in SMALL_AND_CHOON_SPECS:
        for w, romaji, vi in pairs:
            ex = build_example_sentence(w, romaji, vi, script_type)
            all_words.append({
                "id": f"vocab_{counter:03d}",
                "word": w,
                "romaji": romaji,
                "meaning_vi": vi,
                "chars": list(w),
                "introduces": ch,
                "script": script_type,
                **ex,
            })
            counter += 1

    payload = {
        "totalWords": len(all_words),
        "words": all_words,
    }

    out_json.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(f"Đã xuất {len(all_words)} từ vựng vào {out_json}")

    # Cập nhật REVIEW.md
    review_path = repo_root / "REVIEW.md"
    lines = [
        "# Danh sách Từ vựng cần Kiểm tra (REVIEW.md)",
        "",
        f"Tổng số từ vựng đóng gói offline trong `kana_vocab.json`: **{len(all_words)} từ** (2 từ/chữ cho 46 Hiragana cơ bản, 46 Katakana cơ bản, 50 chữ Dakuten/Handakuten, 8 chữ Yōon/Sokuon và dấu trường âm `ー`).",
        "",
        "## 1. Các từ có lưu ý đặc biệt cần bạn xác nhận",
        "",
        "| Hệ chữ | Chữ dạy (`introduces`) | Từ vựng (`word`) | Romaji | Nghĩa tiếng Việt | Lý do cần kiểm tra |",
        "|---|---|---|---|---|---|",
        "| Hiragana | `あ` | **あめ** | `ame` | cơn mưa / cái kẹo | Đồng âm khác ngữ điệu (雨 / 飴). |",
        "| Hiragana | `た` | **たこ** | `tako` | bạch tuộc / cánh diều | Đồng âm khác ngữ điệu (蛸 / 凧). |",
        "| Hiragana | `を` | **ほんをよむ** | `hon wo yomu` | đọc sách (trợ từ を) | Chữ を chỉ dùng làm trợ từ nên dùng cụm động từ ngắn. |",
        "| Katakana | `ヲ` | **ビデオヲミル** | `bideo wo miru` | xem video (trợ từ ヲ) | Chữ ヲ rất hiếm gặp trong từ đơn Katakana hiện đại. |",
        "| Hiragana | `ぢ` | **はなぢ** | `hanaji` | chảy máu cam | Chữ ぢ chỉ xuất hiện trong một số từ ghép biến âm (rendaku). |",
        "| Hiragana | `づ` | **みかづき** | `mikazuki` | trăng lưỡi liềm | Chữ づ xuất hiện trong từ ghép biến âm (三日月). |",
        "| Katakana | `ヂ` | **ヂーゼル** | `jiizeru` | động cơ diesel | Cách viết cũ của ディーゼル để minh hoạ chữ ヂ. |",
        "| Katakana | `ヅ` | **ツヅキ** | `tsuzuki` | phần tiếp theo | Minh hoạ chữ ヅ trong Katakana. |",
        "",
        "## 2. Mẫu từ vựng Katakana (từ mượn)",
        "",
        "| Chữ dạy (`introduces`) | Từ 1 | Nghĩa 1 | Từ 2 | Nghĩa 2 |",
        "|---|---|---|---|---|",
    ]

    kata_by_intro = {}
    for entry in KATAKANA_BASIC_VOCAB:
        kata_by_intro.setdefault(entry["introduces"], []).append(entry)
    for ch, items in kata_by_intro.items():
        if len(items) >= 2:
            lines.append(
                f"| `{ch}` | **{items[0]['word']}** (`{items[0]['romaji']}`) | {items[0]['meaning_vi']} | **{items[1]['word']}** (`{items[1]['romaji']}`) | {items[1]['meaning_vi']} |"
            )

    review_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Đã cập nhật {review_path}")


if __name__ == "__main__":
    main()
