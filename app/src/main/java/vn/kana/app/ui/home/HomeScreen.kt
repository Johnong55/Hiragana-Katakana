package vn.kana.app.ui.home

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.kana.app.domain.MasteryLevel
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
    onOpenReference: (initialChar: String?) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()

    val nextRowLesson = uiState.filteredLessons.firstOrNull { it.isUnlocked && !it.isCompleted }
        ?: uiState.allLessons.firstOrNull { it.isUnlocked && !it.isCompleted }
        ?: uiState.filteredLessons.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Header gọn gàng: Tên app + Streak bên trái, nút Cài đặt rộng rãi 1 dòng bên phải
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "KANA",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                Text(
                    text = "Streak: ${uiState.settings.streakDays} ngày • Đã thuộc: ${uiState.totalLearnedCount}/${uiState.allCurriculumCharacters.size} chữ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            TopNavButton(
                label = "Cài đặt",
                onClick = onOpenSettings
            )
        }

        // Thanh điều hướng 2 nút cân đối: Bản đồ bài học & Bảng tra cứu Kana
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .clickable(onClick = onOpenLessonMap)
                    .padding(vertical = 9.dp, horizontal = 8.dp)
            ) {
                Text(
                    text = "Bản đồ bài học",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .clickable { onOpenReference(null) }
                    .padding(vertical = 9.dp, horizontal = 8.dp)
            ) {
                Text(
                    text = "Bảng tra cứu Kana",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
            }
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 2. Hai thẻ hành động chính: "Ôn hôm nay" & "Học tiếp"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, MaterialTheme.colorScheme.onBackground)
                        .clickable(onClick = onStartReview)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "ÔN HÔM NAY",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                    Text(
                        text = "${uiState.dueSrsCardsCount} thẻ đến hạn",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                    Text(
                        text = if (uiState.dueSrsCardsCount > 0) {
                            "Ôn chữ & từ vựng →"
                        } else {
                            "Luyện tập thêm →"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(1.dp, MaterialTheme.colorScheme.onBackground)
                        .clickable {
                            if (nextRowLesson != null) {
                                onStartRowLesson(nextRowLesson.rowName, nextRowLesson.script)
                            } else {
                                onOpenLessonMap()
                            }
                        }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "HỌC TIẾP",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = nextRowLesson?.rowName ?: "Hoàn tất",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "Bắt đầu học ngay →",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1
                    )
                }
            }

            // 3. Thanh chuyển hệ chữ: Hiragana (46) | Katakana (46) | Dakuten • 拗音
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

            // 4. Bảng Kana thể hiện độ thuộc bằng 4 độ đậm nhạt của xám (không gradient)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BẢNG KANA • ${uiState.selectedTab.titleVi.uppercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Chạm chữ để xem nét",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Chú thích 4 mức độ xám
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
                            isDark = isDark,
                            onClickCharacter = { ch -> onOpenReference(ch) }
                        )
                    }

                    CurriculumTab.KATAKANA_BASIC -> {
                        GojuonMasteryGrid(
                            templates = uiState.basicKatakana,
                            rowsLayout = KATAKANA_GOJUON_LAYOUT,
                            masteryByChar = uiState.masteryByChar,
                            isDark = isDark,
                            onClickCharacter = { ch -> onOpenReference(ch) }
                        )
                    }

                    CurriculumTab.EXTENDED -> {
                        GojuonMasteryGrid(
                            templates = uiState.extendedCharacters,
                            rowsLayout = EXTENDED_KANA_LAYOUT,
                            masteryByChar = uiState.masteryByChar,
                            isDark = isDark,
                            onClickCharacter = { ch -> onOpenReference(ch) }
                        )
                    }
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

@Composable
private fun TopNavButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.onBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            softWrap = false
        )
    }
}

internal fun masteryColors(level: MasteryLevel, isDark: Boolean): Pair<Color, Color> {
    return if (!isDark) {
        when (level) {
            MasteryLevel.UNLEARNED -> Color(0xFFFFFFFF) to Color(0xFF757575)
            MasteryLevel.LEARNING -> Color(0xFFE2E2E2) to Color(0xFF111111)
            MasteryLevel.FAMILIAR -> Color(0xFF8C8C8C) to Color(0xFFFFFFFF)
            MasteryLevel.MASTERED -> Color(0xFF111111) to Color(0xFFFFFFFF)
        }
    } else {
        when (level) {
            MasteryLevel.UNLEARNED -> Color(0xFF161616) to Color(0xFF757575)
            MasteryLevel.LEARNING -> Color(0xFF333333) to Color(0xFFF5F5F5)
            MasteryLevel.FAMILIAR -> Color(0xFF757575) to Color(0xFF111111)
            MasteryLevel.MASTERED -> Color(0xFFF5F5F5) to Color(0xFF111111)
        }
    }
}

@Composable
private fun GojuonMasteryGrid(
    templates: List<KanaStrokeTemplate>,
    rowsLayout: List<Pair<String, List<String?>>>,
    masteryByChar: Map<String, MasteryLevel>,
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
                        val typedStrokes = remember(tmpl?.char) { tmpl?.toTypedStrokes().orEmpty() }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(bgColor)
                                .border(1.dp, MaterialTheme.colorScheme.outline)
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
