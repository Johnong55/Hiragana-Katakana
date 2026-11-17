package vn.kana.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import vn.kana.app.data.ThemeMode
import vn.kana.app.domain.MasteryLevel
import vn.kana.app.ui.components.KanaStrokeAnimatorCanvas
import vn.kana.app.ui.components.drawSmoothStrokePath
import vn.kana.app.ui.map.CurriculumTab
import vn.kana.app.ui.map.LessonMapViewModel
import vn.kana.recognizer.KanaStrokeTemplate

@Composable
fun HomeScreen(
    viewModel: LessonMapViewModel,
    onStartReview: () -> Unit,
    onStartRowLesson: (rowName: String, script: String) -> Unit,
    onOpenLessonMap: () -> Unit,
    onOpenCustomStudy: () -> Unit = {},
    onOpenReference: (initialChar: String?) -> Unit,
    onPracticeCharacter: (rowName: String, script: String, targetChar: String) -> Unit = { r, s, _ ->
        onStartRowLesson(r, s)
    },
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val isDark = when (uiState.settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }

    // Tìm bài học đầu tiên chưa hoàn thành và chữ cái đầu tiên chưa học trong bài đó
    val nextRowLesson = uiState.filteredLessons.firstOrNull { !it.isCompleted }
        ?: uiState.allLessons.firstOrNull { !it.isCompleted }
        ?: uiState.filteredLessons.firstOrNull()

    val nextUnlearnedChar = remember(nextRowLesson, uiState.characterProgressByChar) {
        nextRowLesson?.characters?.firstOrNull { ch ->
            uiState.characterProgressByChar[ch.char]?.learned != true
        } ?: nextRowLesson?.characters?.firstOrNull()
    }

    val byChar = remember(uiState.allCurriculumCharacters) {
        uiState.allCurriculumCharacters.associateBy { it.char }
    }
    var selectedChar by remember { mutableStateOf<String?>(null) }
    var replayTrigger by remember { mutableIntStateOf(0) }

    val selectedTemplate = selectedChar?.let { byChar[it] }
    var lastActiveTemplate by remember { mutableStateOf<KanaStrokeTemplate?>(null) }
    LaunchedEffect(selectedTemplate) {
        if (selectedTemplate != null) {
            lastActiveTemplate = selectedTemplate
        }
    }
    val displayTemplate = selectedTemplate ?: lastActiveTemplate
    val selectedStrokes = remember(displayTemplate?.char) {
        displayTemplate?.toTypedStrokes().orEmpty()
    }
    val selectedWords = uiState.wordsByChar[displayTemplate?.char.orEmpty()].orEmpty()

    var animStrokeIdx by remember { mutableIntStateOf(0) }
    var animStrokeProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(selectedTemplate?.char, replayTrigger, uiState.settings.animationSpeed) {
        if (selectedTemplate == null || selectedStrokes.isEmpty()) return@LaunchedEffect
        val speed = uiState.settings.animationSpeed.coerceIn(0.5f, 2.0f)
        while (true) {
            for (sIdx in selectedStrokes.indices) {
                animStrokeIdx = sIdx
                animStrokeProgress = 0f
                val dur = (660f / speed).toInt().coerceAtLeast(130)
                animate(0f, 1f, animationSpec = tween(dur, easing = LinearEasing)) { v, _ ->
                    animStrokeProgress = v
                }
                delay((180f / speed).toLong().coerceAtLeast(60L))
            }
            animStrokeIdx = selectedStrokes.size
            animStrokeProgress = 1f
            delay((850f / speed).toLong().coerceAtLeast(250L))
        }
    }

    fun onTapGridChar(ch: String) {
        if (selectedChar == ch) {
            selectedChar = null
        } else {
            selectedChar = ch
            replayTrigger++
            byChar[ch]?.let { viewModel.speak(it.char, it.romaji) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Header tối giản cho Tab Bảng chữ cái: Tiến độ + Nút Sáng/Tối
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "BẢNG CHỮ CÁI",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                Text(
                    text = "Đã thuộc: ${uiState.totalLearnedCount}/${uiState.allCurriculumCharacters.size} chữ • Streak: ${uiState.settings.streakDays} ngày",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .clickable { viewModel.toggleLightDarkQuick(isDark) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = if (isDark) "☀ Sáng" else "☾ Tối",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // 2. Khung Upper Animated ngay trên cùng khi bấm vào 1 chữ (thu/mở mượt, không khựng khi đóng)
        AnimatedVisibility(
            visible = selectedTemplate != null && displayTemplate != null,
            enter = expandVertically(animationSpec = tween(240)) + fadeIn(animationSpec = tween(220)),
            exit = shrinkVertically(animationSpec = tween(240)) + fadeOut(animationSpec = tween(200))
        ) {
            val activeTmpl = displayTemplate
            if (activeTmpl != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onBackground)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(124.dp)) {
                        KanaStrokeAnimatorCanvas(
                            strokes = selectedStrokes,
                            activeStrokeIndex = animStrokeIdx,
                            activeStrokeProgress = animStrokeProgress,
                            showGhostStrokes = true,
                            showStrokeNumbers = true,
                            showDirectionArrows = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${activeTmpl.char} (${activeTmpl.romaji})",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${activeTmpl.row} • ${activeTmpl.strokeCount} nét",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Nghe",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable {
                                            replayTrigger++
                                            viewModel.speak(activeTmpl.char, activeTmpl.romaji)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                                Text(
                                    text = "✕",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier
                                        .border(1.dp, MaterialTheme.colorScheme.outline)
                                        .clickable { selectedChar = null }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }

                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

                        for (w in selectedWords.take(2)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.speak(w.word, w.romaji) }
                                    .padding(vertical = 1.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${w.word} (${w.romaji})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1
                                )
                                Text(
                                    text = w.meaningVi,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    onPracticeCharacter(
                                        activeTmpl.row,
                                        activeTmpl.script,
                                        activeTmpl.char
                                    )
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = "✎ Luyện viết chữ '${activeTmpl.char}' →",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 3. Thanh chuyển hệ chữ: Hiragana | Katakana | Âm đục/ghép | Kanji N5
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (tab in CurriculumTab.entries) {
                val selected = uiState.selectedTab == tab
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        )
                        .border(1.dp, MaterialTheme.colorScheme.onBackground)
                        .clickable { viewModel.selectTab(tab) }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    Text(
                        text = tab.titleVi,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 4. Bảng Kana cuộn bên dưới (giữ nguyên screen khi xem animation phía trên)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "BẢNG CHỮ CÁI (CHẠM CHỮ ĐỂ XEM NÉT & NGHE)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (level in MasteryLevel.entries) {
                    val (bg, _) = masteryColors(level, isDark)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(bg)
                                .border(1.dp, MaterialTheme.colorScheme.onBackground)
                        )
                        Text(
                            text = level.labelVi,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1
                        )
                    }
                }
            }

            when (uiState.selectedTab) {
                CurriculumTab.HIRAGANA_BASIC -> {
                    GojuonMasteryGrid(
                        templates = uiState.basicHiragana,
                        rowsLayout = HIRAGANA_GOJUON_LAYOUT,
                        masteryByChar = uiState.masteryByChar,
                        selectedChar = selectedChar,
                        isDark = isDark,
                        onClickCharacter = ::onTapGridChar
                    )
                }

                CurriculumTab.KATAKANA_BASIC -> {
                    GojuonMasteryGrid(
                        templates = uiState.basicKatakana,
                        rowsLayout = KATAKANA_GOJUON_LAYOUT,
                        masteryByChar = uiState.masteryByChar,
                        selectedChar = selectedChar,
                        isDark = isDark,
                        onClickCharacter = ::onTapGridChar
                    )
                }

                CurriculumTab.EXTENDED -> {
                    GojuonMasteryGrid(
                        templates = uiState.extendedCharacters,
                        rowsLayout = EXTENDED_KANA_LAYOUT,
                        masteryByChar = uiState.masteryByChar,
                        selectedChar = selectedChar,
                        isDark = isDark,
                        onClickCharacter = ::onTapGridChar
                    )
                }

                CurriculumTab.KANJI_N5 -> {
                    GojuonMasteryGrid(
                        templates = uiState.n5KanjiCharacters,
                        rowsLayout = KANJI_N5_LAYOUT,
                        masteryByChar = uiState.masteryByChar,
                        selectedChar = selectedChar,
                        isDark = isDark,
                        onClickCharacter = ::onTapGridChar
                    )
                }
            }
        }
    }
}

internal val HIRAGANA_GOJUON_LAYOUT = listOf(
    "あ行" to listOf("あ", "い", "う", "え", "お"),
    "か行" to listOf("か", "き", "く", "け", "こ"),
    "さ行" to listOf("さ", "し", "す", "せ", "そ"),
    "た行" to listOf("た", "ち", "つ", "て", "と"),
    "な行" to listOf("な", "に", "ぬ", "ね", "の"),
    "は行" to listOf("は", "ひ", "ふ", "へ", "ほ"),
    "ま行" to listOf("ま", "み", "む", "め", "も"),
    "や行" to listOf("や", null, "ゆ", null, "よ"),
    "ら行" to listOf("ら", "り", "る", "れ", "ろ"),
    "わ行" to listOf("わ", null, "を", null, "ん")
)

internal val KATAKANA_GOJUON_LAYOUT = listOf(
    "ア行" to listOf("ア", "イ", "ウ", "エ", "オ"),
    "カ行" to listOf("カ", "キ", "ク", "ケ", "コ"),
    "サ行" to listOf("サ", "シ", "ス", "セ", "ソ"),
    "タ行" to listOf("タ", "チ", "ツ", "テ", "ト"),
    "ナ行" to listOf("ナ", "ニ", "ヌ", "ネ", "ノ"),
    "ハ行" to listOf("ハ", "ヒ", "フ", "ヘ", "ホ"),
    "マ行" to listOf("マ", "ミ", "ム", "メ", "モ"),
    "ヤ行" to listOf("ヤ", null, "ユ", null, "ヨ"),
    "ラ行" to listOf("ラ", "リ", "ル", "レ", "ロ"),
    "ワ行" to listOf("ワ", null, "ヲ", null, "ン")
)

internal val EXTENDED_KANA_LAYOUT = listOf(
    "が行" to listOf("が", "ぎ", "ぐ", "げ", "ご"),
    "ざ行" to listOf("ざ", "じ", "ず", "ぜ", "ぞ"),
    "だ行" to listOf("だ", "ぢ", "づ", "で", "ど"),
    "ば行" to listOf("ば", "び", "ぶ", "べ", "ぼ"),
    "ぱ行" to listOf("ぱ", "ぴ", "ぷ", "ぺ", "ぽ"),
    "ガ行" to listOf("ガ", "ギ", "グ", "ゲ", "ゴ"),
    "ザ行" to listOf("ザ", "ジ", "ズ", "ゼ", "ゾ"),
    "ダ行" to listOf("ダ", "ヂ", "ヅ", "デ", "ド"),
    "バ行" to listOf("バ", "ビ", "ブ", "ベ", "ボ"),
    "パ行" to listOf("パ", "ピ", "プ", "ペ", "ポ"),
    "拗・促" to listOf("ゃ", "ゅ", "ょ", "っ", null),
    "拗・長" to listOf("ャ", "ュ", "ョ", "ッ", "ー")
)

internal val KANJI_N5_LAYOUT = listOf(
    "Số 1" to listOf("一", "二", "三", "四", "五"),
    "Số 2" to listOf("六", "七", "八", "九", "十"),
    "Số lớn" to listOf("百", "千", "万", "円", "年"),
    "Tự nhiên" to listOf("日", "月", "火", "水", "木"),
    "Đất trời" to listOf("金", "土", "山", "川", "田"),
    "Người" to listOf("人", "子", "女", "男", "友"),
    "Gia đình" to listOf("父", "母", "先", "生", "名"),
    "Trường" to listOf("学", "校", "本", "語", "時"),
    "Giờ giấc" to listOf("分", "半", "今", "前", "後"),
    "Vị trí" to listOf("上", "下", "中", "外", "右"),
    "Hướng" to listOf("左", "北", "南", "東", "西"),
    "Động từ" to listOf("行", "来", "食", "飲", "見"),
    "Động 2" to listOf("聞", "読", "書", "話", "買"),
    "Động 3" to listOf("休", "出", "入", "車", "電"),
    "Tính từ" to listOf("大", "小", "高", "安", "新"),
    "Tính 2" to listOf("古", "多", "少", "白", "駅")
)

internal fun masteryColors(level: MasteryLevel, isDark: Boolean): Pair<Color, Color> {
    return if (!isDark) {
        when (level) {
            MasteryLevel.UNLEARNED -> Color(0xFFFBF7EC) to Color(0xFF7B634E)
            MasteryLevel.LEARNING -> Color(0xFFEAE0C8) to Color(0xFF2C1E14)
            MasteryLevel.FAMILIAR -> Color(0xFF8C6B4D) to Color(0xFFFBF7EC)
            MasteryLevel.MASTERED -> Color(0xFF5C3A21) to Color(0xFFFBF7EC)
        }
    } else {
        when (level) {
            MasteryLevel.UNLEARNED -> Color(0xFF282019) to Color(0xFFA6927A)
            MasteryLevel.LEARNING -> Color(0xFF3D3127) to Color(0xFFEFE3CE)
            MasteryLevel.FAMILIAR -> Color(0xFF8A6F52) to Color(0xFF1E1813)
            MasteryLevel.MASTERED -> Color(0xFFD9B382) to Color(0xFF1E1813)
        }
    }
}

@Composable
private fun GojuonMasteryGrid(
    templates: List<KanaStrokeTemplate>,
    rowsLayout: List<Pair<String, List<String?>>>,
    masteryByChar: Map<String, MasteryLevel>,
    selectedChar: String? = null,
    isDark: Boolean,
    onClickCharacter: (String) -> Unit
) {
    val byChar = remember(templates) { templates.associateBy { it.char } }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for ((rowLabel, slots) in rowsLayout) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rowLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(width = 42.dp, height = 20.dp)
                )
                for (ch in slots) {
                    if (ch == null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    } else {
                        val tmpl = byChar[ch]
                        val mastery = masteryByChar[ch] ?: MasteryLevel.UNLEARNED
                        val (bgColor, fgColor) = masteryColors(mastery, isDark)
                        val isCharSelected = ch == selectedChar
                        val typedStrokes = remember(tmpl?.char) { tmpl?.toTypedStrokes().orEmpty() }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(bgColor)
                                .border(
                                    width = if (isCharSelected) 2.dp else 1.dp,
                                    color = if (isCharSelected) MaterialTheme.colorScheme.onBackground
                                    else MaterialTheme.colorScheme.outline
                                )
                                .clickable { onClickCharacter(ch) }
                                .padding(4.dp)
                        ) {
                            if (typedStrokes.isNotEmpty()) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.minDimension * 0.075f
                                    for (s in typedStrokes) {
                                        drawSmoothStrokePath(
                                            points = s.points,
                                            progress = 1f,
                                            color = fgColor,
                                            strokeWidth = w
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = ch,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = fgColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
