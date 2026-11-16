package vn.kana.app.ui.roadmap

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as ComposeStroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.kana.app.domain.N5GrammarCatalog
import vn.kana.app.domain.N5GrammarPoint
import vn.kana.app.domain.RoadmapStageNode
import vn.kana.app.domain.RoadmapStageType
import vn.kana.app.ui.components.KanaDrawingCanvas
import vn.kana.app.ui.map.CurriculumTab
import vn.kana.app.ui.map.LessonMapViewModel
import vn.kana.recognizer.KanaRecognizer
import vn.kana.recognizer.KanaStrokeTemplate
import vn.kana.recognizer.RecognizerConfig
import vn.kana.recognizer.Stroke
import kotlin.math.roundToInt

private sealed interface RoadmapOverlayMode {
    data object None : RoadmapOverlayMode
    data class GrammarLesson(val stageNumber: Int, val points: List<N5GrammarPoint>) : RoadmapOverlayMode
    data class StageCheckpointTest(
        val stage: RoadmapStageNode,
        val questions: List<CheckpointQuestion>
    ) : RoadmapOverlayMode
    data class UnlockCelebration(val unlockedStageNumber: Int, val unlockedStageTitle: String) : RoadmapOverlayMode
}

private data class CheckpointQuestion(
    val promptVi: String,
    val subPromptVi: String,
    val targetTemplate: KanaStrokeTemplate
)

/**
 * Màn hình Lộ trình Chinh phục JLPT N5 (S-Curve Winding Road Map 01 -> 08):
 * - Thiết kế con đường nhựa uốn lượn chữ S với vạch kẻ đứt nét chuyển động (animated dashed road)
 *   và Cờ Đích N5 ở trạm 08 giống hình mẫu.
 * - Hoàn thành hoặc bấm "Kiểm tra Mở khoá 🔓" ở trạm trước sẽ kích hoạt hiệu ứng Mở khoá (Unlock Animation)
 *   và mở trạm tiếp theo trên lộ trình.
 */
@Composable
fun N5RoadmapScreen(
    viewModel: LessonMapViewModel,
    onStartRowLesson: (rowName: String, script: String) -> Unit,
    onOpenTabWithFilter: (CurriculumTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val localView = LocalView.current

    val recognizer = remember(uiState.allCurriculumCharacters) {
        if (uiState.allCurriculumCharacters.isNotEmpty()) {
            KanaRecognizer(uiState.allCurriculumCharacters)
        } else {
            null
        }
    }
    val byChar = remember(uiState.allCurriculumCharacters) {
        uiState.allCurriculumCharacters.associateBy { it.char }
    }

    // Tính toán trạng thái Mở khoá & Tiến độ của 8 Chặng đường N5 (01 .. 08)
    val stages = remember(
        uiState.basicHiragana,
        uiState.basicKatakana,
        uiState.extendedCharacters,
        uiState.n5KanjiCharacters,
        uiState.characterProgressByChar,
        uiState.lessonProgressByRowId,
        uiState.settings.unlockAllRows
    ) {
        val learnedSet = uiState.characterProgressByChar.filter { it.value.learned }.keys
        val passedStageMap = (1..8).associateWith { sNum ->
            uiState.lessonProgressByRowId["roadmap_stage_$sNum"] == true
        }

        val hiraLearned = uiState.basicHiragana.count { it.char in learnedSet }
        val s1Done = passedStageMap[1] == true || (uiState.basicHiragana.isNotEmpty() && hiraLearned >= uiState.basicHiragana.size)

        val kataLearned = uiState.basicKatakana.count { it.char in learnedSet }
        val s2Unlocked = true // Luôn cho phép bắt đầu hoặc kiểm tra vượt chặng 1 -> 2
        val s2Done = passedStageMap[2] == true || (uiState.basicKatakana.isNotEmpty() && kataLearned >= uiState.basicKatakana.size)

        val extLearned = uiState.extendedCharacters.count { it.char in learnedSet }
        val s3Done = passedStageMap[3] == true || (uiState.extendedCharacters.isNotEmpty() && extLearned >= uiState.extendedCharacters.size)

        val s4Done = passedStageMap[4] == true
        val s5Done = passedStageMap[5] == true

        val kanjiPart1 = uiState.n5KanjiCharacters.take(38)
        val kanji1Learned = kanjiPart1.count { it.char in learnedSet }
        val s6Done = passedStageMap[6] == true || (kanjiPart1.isNotEmpty() && kanji1Learned >= kanjiPart1.size)

        val s7Done = passedStageMap[7] == true

        val kanjiPart2 = uiState.n5KanjiCharacters.drop(38)
        val kanji2Learned = kanjiPart2.count { it.char in learnedSet }
        val s8Done = passedStageMap[8] == true || (kanjiPart2.isNotEmpty() && kanji2Learned >= kanjiPart2.size)

        val unlockAll = uiState.settings.unlockAllRows

        listOf(
            RoadmapStageNode(
                stageNumber = 1,
                type = RoadmapStageType.HIRAGANA_BASIC,
                titleVi = "Hiragana Cơ bản",
                subtitleVi = "46 chữ mềm nền tảng (あ → ん) & 92 từ vựng",
                badgeIconText = "あ",
                totalItems = 46,
                completedItems = if (s1Done) 46 else hiraLearned,
                isUnlocked = true,
                isCompleted = s1Done
            ),
            RoadmapStageNode(
                stageNumber = 2,
                type = RoadmapStageType.KATAKANA_BASIC,
                titleVi = "Katakana Cơ bản",
                subtitleVi = "46 chữ cứng (ア → ン) & 92 từ mượn thông dụng",
                badgeIconText = "ア",
                totalItems = 46,
                completedItems = if (s2Done) 46 else kataLearned,
                isUnlocked = unlockAll || s1Done,
                isCompleted = s2Done
            ),
            RoadmapStageNode(
                stageNumber = 3,
                type = RoadmapStageType.DAKUTEN_AND_YOON,
                titleVi = "Âm đục & Âm ghép",
                subtitleVi = "Dakuten (が/パ), Yōon (きゃ/シャ), っ & ー",
                badgeIconText = "が",
                totalItems = uiState.extendedCharacters.size.coerceAtLeast(59),
                completedItems = if (s3Done) uiState.extendedCharacters.size else extLearned,
                isUnlocked = unlockAll || s2Done,
                isCompleted = s3Done
            ),
            RoadmapStageNode(
                stageNumber = 4,
                type = RoadmapStageType.N5_VOCAB_SENTENCES,
                titleVi = "Từ vựng & Câu N5",
                subtitleVi = "302 từ vựng & mẫu câu giao tiếp + Luyện phát âm",
                badgeIconText = "語",
                totalItems = 10,
                completedItems = if (s4Done) 10 else (hiraLearned + kataLearned).coerceAtMost(10),
                isUnlocked = unlockAll || s3Done,
                isCompleted = s4Done
            ),
            RoadmapStageNode(
                stageNumber = 5,
                type = RoadmapStageType.N5_GRAMMAR_1,
                titleVi = "Ngữ pháp N5 (Phần 1)",
                subtitleVi = "Trợ từ cốt lõi: は, の, を, へ, に, で, が & です",
                badgeIconText = "文",
                totalItems = N5GrammarCatalog.stage5Points.size,
                completedItems = if (s5Done) N5GrammarCatalog.stage5Points.size else 0,
                isUnlocked = unlockAll || s4Done,
                isCompleted = s5Done
            ),
            RoadmapStageNode(
                stageNumber = 6,
                type = RoadmapStageType.N5_KANJI_1,
                titleVi = "Kanji N5 (Cấp độ 1)",
                subtitleVi = "38 chữ Hán: Số đếm, Tự nhiên, Con người, Trường học",
                badgeIconText = "日",
                totalItems = kanjiPart1.size.coerceAtLeast(38),
                completedItems = if (s6Done) kanjiPart1.size else kanji1Learned,
                isUnlocked = unlockAll || s5Done,
                isCompleted = s6Done
            ),
            RoadmapStageNode(
                stageNumber = 7,
                type = RoadmapStageType.N5_GRAMMAR_2,
                titleVi = "Ngữ pháp N5 (Phần 2)",
                subtitleVi = "Câu hỏi か, と, も, V-たい, V-て ください, V-ないで",
                badgeIconText = "て",
                totalItems = N5GrammarCatalog.stage7Points.size,
                completedItems = if (s7Done) N5GrammarCatalog.stage7Points.size else 0,
                isUnlocked = unlockAll || s6Done,
                isCompleted = s7Done
            ),
            RoadmapStageNode(
                stageNumber = 8,
                type = RoadmapStageType.N5_KANJI_2_FINAL,
                titleVi = "Kanji N5 (Cấp độ 2) & Đích N5",
                subtitleVi = "42 chữ Hán: Thời gian, Hướng, Động/Tính từ & Thi tổng hợp",
                badgeIconText = "🚩",
                totalItems = kanjiPart2.size.coerceAtLeast(42),
                completedItems = if (s8Done) kanjiPart2.size else kanji2Learned,
                isUnlocked = unlockAll || s7Done,
                isCompleted = s8Done
            )
        )
    }

    var selectedStage by remember(stages) {
        mutableStateOf(
            stages.firstOrNull { it.isUnlocked && !it.isCompleted } ?: stages.first()
        )
    }
    var overlayMode by remember { mutableStateOf<RoadmapOverlayMode>(RoadmapOverlayMode.None) }

    // Tạo bộ câu hỏi Kiểm tra Mở khoá cho từng Chặng (Stage 01..08)
    fun buildCheckpointQuestions(stage: RoadmapStageNode): List<CheckpointQuestion> {
        val sampleChars: List<Pair<String, String>> = when (stage.type) {
            RoadmapStageType.HIRAGANA_BASIC -> listOf(
                "あ" to "Viết chữ Hiragana 'a' (あ)",
                "か" to "Viết chữ Hiragana 'ka' (か)",
                "さ" to "Viết chữ Hiragana 'sa' (さ)",
                "ん" to "Viết chữ Hiragana 'n' (ん)"
            )
            RoadmapStageType.KATAKANA_BASIC -> listOf(
                "ア" to "Viết chữ Katakana 'a' (ア)",
                "カ" to "Viết chữ Katakana 'ka' (カ)",
                "テレビ".take(1) to "Viết chữ Katakana 'te' (テ) trong テレビ",
                "ン" to "Viết chữ Katakana 'n' (ン)"
            )
            RoadmapStageType.DAKUTEN_AND_YOON -> listOf(
                "が" to "Viết chữ âm đục 'ga' (が)",
                "ぱ" to "Viết chữ âm nửa đục 'pa' (ぱ)",
                "ゃ" to "Viết chữ nhỏ 'ya' (ゃ)",
                "ー" to "Viết dấu trường âm (ー)"
            )
            RoadmapStageType.N5_VOCAB_SENTENCES -> listOf(
                "あ" to "Từ 'あめ' (cơn mưa): Viết chữ đầu 'a'",
                "い" to "Từ 'いぬ' (con chó): Viết chữ đầu 'i'",
                "う" to "Từ 'うみ' (biển): Viết chữ đầu 'u'",
                "え" to "Từ 'えき' (nhà ga): Viết chữ đầu 'e'"
            )
            RoadmapStageType.N5_GRAMMAR_1 -> listOf(
                "は" to "わたし [ ? ] がくせい です (Trợ từ chủ đề 'wa')",
                "の" to "わたし [ ? ] ほん です (Trợ từ sở hữu 'no')",
                "を" to "みず [ ? ] のみます (Trợ từ tân ngữ 'wo')",
                "で" to "でんしゃ [ ? ] いきます (Trợ từ phương tiện 'de')"
            )
            RoadmapStageType.N5_KANJI_1 -> listOf(
                "一" to "Viết chữ Kanji NHẤT - số 1 (ichi)",
                "日" to "Viết chữ Kanji NHẬT - mặt trời/ngày (nichi/hi)",
                "人" to "Viết chữ Kanji NHÂN - con người (jin/hito)",
                "本" to "Viết chữ Kanji BẢN - sách/Nhật Bản (hon)"
            )
            RoadmapStageType.N5_GRAMMAR_2 -> listOf(
                "か" to "なんじ です [ ? ] (Trợ từ cuối câu hỏi)",
                "と" to "ともだち [ ? ] いきます (Trợ từ 'cùng với')",
                "も" to "わたし [ ? ] がくせい です (Trợ từ 'cũng')",
                "て" to "これ を み [ ? ] ください (Động từ thể て)"
            )
            RoadmapStageType.N5_KANJI_2_FINAL -> listOf(
                "上" to "Viết chữ Kanji THƯỢNG - phía trên (ue)",
                "下" to "Viết chữ Kanji HẠ - phía dưới (shita)",
                "大" to "Viết chữ Kanji ĐẠI - to lớn (ookii)",
                "行" to "Viết chữ Kanji HÀNH - đi (iku)"
            )
        }
        return sampleChars.mapNotNull { (ch, prompt) ->
            val tmpl = byChar[ch] ?: return@mapNotNull null
            CheckpointQuestion(
                promptVi = prompt,
                subPromptVi = "Số nét chuẩn: ${tmpl.strokeCount} nét • Phiên âm: ${tmpl.romaji}",
                targetTemplate = tmpl
            )
        }
    }

    // Xử lý khi mở các chế độ toàn màn hình (Học ngữ pháp / Kiểm tra Mở khoá / Pháo hoa Mở khoá)
    when (val mode = overlayMode) {
        is RoadmapOverlayMode.GrammarLesson -> {
            N5GrammarInteractiveLessonView(
                stageNumber = mode.stageNumber,
                points = mode.points,
                byChar = byChar,
                recognizer = recognizer,
                strictnessLevel = uiState.settings.strictnessLevel,
                onSpeak = { text -> viewModel.speak(text, null) },
                onCompleteAllGrammar = {
                    viewModel.markRoadmapStagePassed(mode.stageNumber)
                    val nextStage = stages.firstOrNull { it.stageNumber == mode.stageNumber + 1 }
                    overlayMode = if (nextStage != null) {
                        RoadmapOverlayMode.UnlockCelebration(
                            unlockedStageNumber = nextStage.stageNumber,
                            unlockedStageTitle = nextStage.titleVi
                        )
                    } else {
                        RoadmapOverlayMode.None
                    }
                },
                onBack = { overlayMode = RoadmapOverlayMode.None },
                localView = localView,
                modifier = modifier
            )
            return
        }

        is RoadmapOverlayMode.StageCheckpointTest -> {
            StageCheckpointTestView(
                stage = mode.stage,
                questions = mode.questions,
                recognizer = recognizer,
                strictnessLevel = uiState.settings.strictnessLevel,
                onSpeak = { ch, ro -> viewModel.speak(ch, ro) },
                onPassedAll = {
                    val charsInStage = when (mode.stage.type) {
                        RoadmapStageType.HIRAGANA_BASIC -> uiState.basicHiragana
                        RoadmapStageType.KATAKANA_BASIC -> uiState.basicKatakana
                        RoadmapStageType.DAKUTEN_AND_YOON -> uiState.extendedCharacters
                        RoadmapStageType.N5_KANJI_1 -> uiState.n5KanjiCharacters.take(38)
                        RoadmapStageType.N5_KANJI_2_FINAL -> uiState.n5KanjiCharacters.drop(38)
                        else -> emptyList()
                    }
                    viewModel.markRoadmapStagePassed(mode.stage.stageNumber, charsInStage)
                    val nextStage = stages.firstOrNull { it.stageNumber == mode.stage.stageNumber + 1 }
                    overlayMode = if (nextStage != null) {
                        selectedStage = nextStage
                        RoadmapOverlayMode.UnlockCelebration(
                            unlockedStageNumber = nextStage.stageNumber,
                            unlockedStageTitle = nextStage.titleVi
                        )
                    } else {
                        RoadmapOverlayMode.UnlockCelebration(
                            unlockedStageNumber = 8,
                            unlockedStageTitle = "HOÀN THÀNH LỘ TRÌNH JLPT N5 🚩"
                        )
                    }
                },
                onCancel = { overlayMode = RoadmapOverlayMode.None },
                localView = localView,
                modifier = modifier
            )
            return
        }

        is RoadmapOverlayMode.UnlockCelebration -> {
            StageUnlockCelebrationDialog(
                unlockedStageNumber = mode.unlockedStageNumber,
                unlockedStageTitle = mode.unlockedStageTitle,
                onContinue = { overlayMode = RoadmapOverlayMode.None },
                modifier = modifier
            )
            return
        }

        RoadmapOverlayMode.None -> {}
    }

    val completedStagesCount = stages.count { it.isCompleted }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Header Lộ trình N5
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "LỘ TRÌNH CHINH PHỤC JLPT N5",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Hoàn thành từng chặng • Kiểm tra kiến thức để mở khoá chặng mới",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$completedStagesCount/8 Chặng",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        // 2. Bản đồ Con đường Uốn lượn Chữ S (S-Curve Winding Road Map 01 -> 08)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .verticalScroll(rememberScrollState())
        ) {
            SCurveInfographicRoadMap(
                stages = stages,
                selectedStageNumber = selectedStage.stageNumber,
                onSelectStage = { st -> selectedStage = st }
            )
        }

        // 3. Bảng điều khiển Trạm đang chọn (gọn gàng dưới đáy)
        val cur = stages.firstOrNull { it.stageNumber == selectedStage.stageNumber } ?: selectedStage
        SelectedStageActionCard(
            stage = cur,
            onEnterStageStudy = {
                when (cur.type) {
                    RoadmapStageType.HIRAGANA_BASIC -> {
                        val nextRow = uiState.allLessons.firstOrNull {
                            it.script == "hiragana" && !it.isCompleted && it.characters.firstOrNull()?.category == "basic"
                        } ?: uiState.allLessons.firstOrNull { it.script == "hiragana" }
                        if (nextRow != null) {
                            onStartRowLesson(nextRow.rowName, nextRow.script)
                        }
                    }

                    RoadmapStageType.KATAKANA_BASIC -> {
                        val nextRow = uiState.allLessons.firstOrNull {
                            it.script == "katakana" && !it.isCompleted && it.characters.firstOrNull()?.category == "basic"
                        } ?: uiState.allLessons.firstOrNull { it.script == "katakana" }
                        if (nextRow != null) {
                            onStartRowLesson(nextRow.rowName, nextRow.script)
                        }
                    }

                    RoadmapStageType.DAKUTEN_AND_YOON -> {
                        val nextRow = uiState.allLessons.firstOrNull {
                            it.script != "kanji" && it.characters.firstOrNull()?.category != "basic" && !it.isCompleted
                        } ?: uiState.allLessons.firstOrNull {
                            it.script != "kanji" && it.characters.firstOrNull()?.category != "basic"
                        }
                        if (nextRow != null) {
                            onStartRowLesson(nextRow.rowName, nextRow.script)
                        }
                    }

                    RoadmapStageType.N5_VOCAB_SENTENCES -> {
                        onOpenTabWithFilter(CurriculumTab.HIRAGANA_BASIC)
                    }

                    RoadmapStageType.N5_GRAMMAR_1 -> {
                        overlayMode = RoadmapOverlayMode.GrammarLesson(
                            stageNumber = 5,
                            points = N5GrammarCatalog.stage5Points
                        )
                    }

                    RoadmapStageType.N5_KANJI_1 -> {
                        val nextKanjiRow = uiState.allLessons.firstOrNull {
                            it.script == "kanji" && !it.isCompleted
                        } ?: uiState.allLessons.firstOrNull { it.script == "kanji" }
                        if (nextKanjiRow != null) {
                            onStartRowLesson(nextKanjiRow.rowName, nextKanjiRow.script)
                        }
                    }

                    RoadmapStageType.N5_GRAMMAR_2 -> {
                        overlayMode = RoadmapOverlayMode.GrammarLesson(
                            stageNumber = 7,
                            points = N5GrammarCatalog.stage7Points
                        )
                    }

                    RoadmapStageType.N5_KANJI_2_FINAL -> {
                        val kanjiPart2Rows = uiState.allLessons.filter { it.script == "kanji" }.drop(5)
                        val targetRow = kanjiPart2Rows.firstOrNull { !it.isCompleted }
                            ?: kanjiPart2Rows.firstOrNull()
                            ?: uiState.allLessons.lastOrNull { it.script == "kanji" }
                        if (targetRow != null) {
                            onStartRowLesson(targetRow.rowName, targetRow.script)
                        }
                    }
                }
            },
            onStartStageCheckpointTest = {
                val qs = buildCheckpointQuestions(cur)
                if (qs.isNotEmpty()) {
                    overlayMode = RoadmapOverlayMode.StageCheckpointTest(
                        stage = cur,
                        questions = qs
                    )
                }
            }
        )
    }
}

/**
 * Vẽ Con đường nhựa uốn lượn chữ S (S-Curve Road Infographic) nối 8 Trạm 01 -> 08
 * kèm hiệu ứng vạch kẻ đường chuyển động và vòng sáng radar tại trạm đang học.
 */
@Composable
private fun SCurveInfographicRoadMap(
    stages: List<RoadmapStageNode>,
    selectedStageNumber: Int,
    onSelectStage: (RoadmapStageNode) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "road_anim")
    val dashPhase by infiniteTransition.animateFloat(
        initialValue = 48f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dash_phase"
    )
    val pulseRadiusRatio by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.48f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_ring"
    )

    val roadColorActive = MaterialTheme.colorScheme.onBackground
    val roadColorLocked = MaterialTheme.colorScheme.outline
    val centerLineColor = MaterialTheme.colorScheme.background
    val flagRedColor = MaterialTheme.colorScheme.error

    val totalHeightDp = 940.dp
    val density = LocalDensity.current

    // Toạ độ chuẩn hoá (xRatio, yRatio) cho 8 trạm uốn lượn chữ S giống hệt hình mẫu
    val stationPositions = remember {
        listOf(
            0.22f to 0.07f, // 01: Trái trên (Bắt đầu)
            0.78f to 0.16f, // 02: Phải trên
            0.20f to 0.30f, // 03: Uốn sang trái
            0.48f to 0.41f, // 04: Giữa
            0.80f to 0.51f, // 05: Uốn sang phải
            0.52f to 0.65f, // 06: Giữa phải
            0.22f to 0.78f, // 07: Uốn sang trái dưới
            0.74f to 0.91f  // 08: Phải dưới (Đích N5 🚩)
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeightDp)
            .padding(vertical = 8.dp)
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { totalHeightDp.toPx() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val pts = stationPositions.map { (xr, yr) ->
                Offset(xr * size.width, yr * size.height)
            }

            val roadWidth = 32.dp.toPx()
            val borderWidth = 38.dp.toPx()

            // Vẽ từng chặng đường nối Trạm i -> Trạm i+1 bằng đường cong mượt Cubic Bezier
            for (i in 0 until pts.size - 1) {
                val p0 = pts[i]
                val p1 = pts[i + 1]
                val midY = (p0.y + p1.y) / 2f

                val segPath = Path().apply {
                    moveTo(p0.x, p0.y)
                    cubicTo(
                        p0.x, midY,
                        p1.x, midY,
                        p1.x, p1.y
                    )
                }

                val isSegmentUnlocked = stages.getOrNull(i + 1)?.isUnlocked == true ||
                    stages.getOrNull(i)?.isCompleted == true

                val trackColor = if (isSegmentUnlocked) roadColorActive else roadColorLocked

                // Viền mép đường
                drawPath(
                    path = segPath,
                    color = trackColor,
                    style = ComposeStroke(
                        width = borderWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                // Mặt đường nhựa
                drawPath(
                    path = segPath,
                    color = trackColor,
                    style = ComposeStroke(
                        width = roadWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                // Vạch kẻ đứt nét ở tâm đường (chuyển động liên tục trên các đoạn đã mở khoá!)
                drawPath(
                    path = segPath,
                    color = centerLineColor,
                    style = ComposeStroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(18f, 16f),
                            phase = if (isSegmentUnlocked) dashPhase else 0f
                        )
                    )
                )
            }

            // Vòng sáng radar nhấp nháy quanh Trạm đang được chọn / đang học
            val activeIdx = (selectedStageNumber - 1).coerceIn(0, pts.lastIndex)
            val activePt = pts[activeIdx]
            drawCircle(
                color = roadColorActive.copy(alpha = 0.22f),
                radius = 28.dp.toPx() * pulseRadiusRatio,
                center = activePt
            )

            // Cờ đích màu đỏ ở Trạm 08 (giống hình mẫu)
            val finishPt = pts.last()
            val poleTop = Offset(finishPt.x + 28.dp.toPx(), finishPt.y - 44.dp.toPx())
            val poleBottom = Offset(finishPt.x + 28.dp.toPx(), finishPt.y - 4.dp.toPx())
            drawLine(
                color = roadColorActive,
                start = poleBottom,
                end = poleTop,
                strokeWidth = 3.dp.toPx()
            )
            val flagPath = Path().apply {
                moveTo(poleTop.x, poleTop.y)
                lineTo(poleTop.x + 24.dp.toPx(), poleTop.y + 10.dp.toPx())
                lineTo(poleTop.x, poleTop.y + 20.dp.toPx())
                close()
            }
            drawPath(path = flagPath, color = flagRedColor)
        }

        // Đặt 8 Trạm dừng (01..08) kèm thẻ thông tin cạnh con đường
        stages.forEachIndexed { idx, stage ->
            val (xr, yr) = stationPositions[idx]
            val nodeSizeDp = 50.dp
            val nodeX = (xr * widthPx) - with(density) { (nodeSizeDp / 2).toPx() }
            val nodeY = (yr * heightPx) - with(density) { (nodeSizeDp / 2).toPx() }
            val isSelected = stage.stageNumber == selectedStageNumber

            // Nút tròn Trạm dừng nằm ngay trên mặt đường
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset { IntOffset(nodeX.roundToInt(), nodeY.roundToInt()) }
                    .size(nodeSizeDp)
                    .clip(CircleShape)
                    .background(
                        when {
                            stage.isCompleted -> MaterialTheme.colorScheme.primary
                            stage.isUnlocked -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .border(
                        width = if (isSelected) 3.dp else 2.dp,
                        color = if (stage.isUnlocked) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .clickable { onSelectStage(stage) }
            ) {
                Text(
                    text = when {
                        stage.isCompleted -> "✓"
                        !stage.isUnlocked -> "🔒"
                        else -> stage.badgeIconText
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = when {
                        stage.isCompleted -> MaterialTheme.colorScheme.onPrimary
                        stage.isUnlocked -> MaterialTheme.colorScheme.onBackground
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Thẻ nhãn số thứ tự lớn "01".."08" + Tên chặng (đặt lệch sang bên trống của khúc cua)
            val placeCardOnRight = xr < 0.45f
            val cardWidthDp = 195.dp
            val cardX = if (placeCardOnRight) {
                nodeX + with(density) { 58.dp.toPx() }
            } else {
                (nodeX - with(density) { (cardWidthDp + 8.dp).toPx() }).coerceAtLeast(with(density) { 8.dp.toPx() })
            }
            val cardY = (nodeY - with(density) { 10.dp.toPx() }).coerceAtLeast(with(density) { 6.dp.toPx() })

            Column(
                modifier = Modifier
                    .offset { IntOffset(cardX.roundToInt(), cardY.roundToInt()) }
                    .width(cardWidthDp)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected || stage.isUnlocked) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.outline
                    )
                    .clickable { onSelectStage(stage) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stage.numberFormatted,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onBackground
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stage.titleVi,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                stage.isCompleted -> "Đã hoàn thành ✓"
                                stage.isUnlocked -> "Đang mở (${stage.completedItems}/${stage.totalItems})"
                                else -> "Bấm để kiểm tra mở khoá 🔓"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Text(
                    text = stage.subtitleVi,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SelectedStageActionCard(
    stage: RoadmapStageNode,
    onEnterStageStudy: () -> Unit,
    onStartStageCheckpointTest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onBackground)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CHẶNG ${stage.numberFormatted}: ${stage.titleVi.uppercase()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stage.subtitleVi,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${stage.completedItems}/${stage.totalItems}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .clickable(onClick = onStartStageCheckpointTest)
                    .padding(vertical = 10.dp, horizontal = 6.dp)
            ) {
                Text(
                    text = if (stage.isCompleted) "Kiểm tra lại chặng" else "Kiểm tra Mở khoá 🔓",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1.1f)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .clickable(onClick = onEnterStageStudy)
                    .padding(vertical = 10.dp, horizontal = 6.dp)
            ) {
                Text(
                    text = "Vào học Chặng ${stage.numberFormatted} →",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Màn hình Học & Thực hành Ngữ pháp JLPT N5 (Chặng 05 & Chặng 07):
 * - Hiển thị cấu trúc, giải thích tiếng Việt, mẹo thi N5 và câu ví dụ có phát âm.
 * - Tích hợp bài tập viết chữ Kana/Trợ từ còn thiếu vào chỗ trống [ ? ] trực tiếp trên Canvas 1:1.
 */
@Composable
private fun N5GrammarInteractiveLessonView(
    stageNumber: Int,
    points: List<N5GrammarPoint>,
    byChar: Map<String, KanaStrokeTemplate>,
    recognizer: KanaRecognizer?,
    strictnessLevel: vn.kana.recognizer.StrictnessLevel,
    onSpeak: (String) -> Unit,
    onCompleteAllGrammar: () -> Unit,
    onBack: () -> Unit,
    localView: android.view.View,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var currentIndex by remember { mutableIntStateOf(0) }
    val point = points.getOrNull(currentIndex) ?: return

    var userStrokes by remember(point.id) { mutableStateOf<List<Stroke>>(emptyList()) }
    var feedbackText by remember(point.id) { mutableStateOf<String?>(null) }
    var isFeedbackError by remember(point.id) { mutableStateOf(false) }
    var shakeTrigger by remember { mutableIntStateOf(0) }
    var successOverlayText by remember(point.id) { mutableStateOf<String?>(null) }

    val targetTemplate = byChar[point.quizAnswerChar]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "← Lộ trình N5",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
            Text(
                text = "NGỮ PHÁP N5 • BÀI ${currentIndex + 1}/${points.size}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (currentIndex > 0) {
                    Text(
                        text = "‹ Trước",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .clickable { currentIndex -= 1 }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
                Text(
                    text = "Sau ›",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .clickable {
                            if (currentIndex + 1 < points.size) currentIndex += 1
                            else onCompleteAllGrammar()
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }

        // Thẻ Lý thuyết Ngữ pháp + Ví dụ + Câu hỏi điền khuyết
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.onBackground)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = point.titleVi,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Công thức: ${point.patternJp}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = point.explanationVi,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ví dụ: ${point.exampleJp}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${point.exampleRomaji} — ${point.exampleVi}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "🔊 Nghe",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onSpeak(point.exampleJp) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

            Text(
                text = "BÀI TẬP: ${point.quizPromptVi}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = point.quizSentenceWithBlank,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        feedbackText?.let { msg ->
            val col = if (isFeedbackError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
            Text(
                text = msg,
                style = MaterialTheme.typography.labelMedium,
                color = col,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, col)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        // Canvas 1:1 để viết trợ từ đáp án
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            KanaDrawingCanvas(
                userStrokes = userStrokes,
                onStrokeCompleted = { stroke ->
                    userStrokes = userStrokes + stroke
                    feedbackText = null
                },
                templateStrokes = targetTemplate?.toTypedStrokes().orEmpty(),
                showGhostTemplate = false,
                shakeTrigger = shakeTrigger,
                successOverlayText = successOverlayText,
                enabled = successOverlayText == null,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3 nút điều khiển
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RoadmapActionBtn(
                text = "Hoàn tác",
                filled = false,
                onClick = {
                    if (userStrokes.isNotEmpty()) userStrokes = userStrokes.dropLast(1)
                },
                modifier = Modifier.weight(0.85f)
            )
            RoadmapActionBtn(
                text = "Xoá",
                filled = false,
                onClick = {
                    userStrokes = emptyList()
                    feedbackText = null
                },
                modifier = Modifier.weight(0.65f)
            )
            RoadmapActionBtn(
                text = "Kiểm tra →",
                filled = true,
                onClick = {
                    val engine = recognizer ?: return@RoadmapActionBtn
                    val tmpl = targetTemplate ?: return@RoadmapActionBtn
                    val cfg = RecognizerConfig.forLevel(strictnessLevel)
                    val res = engine.recognize(userStrokes, tmpl.char, cfg)
                    if (res.passed) {
                        isFeedbackError = false
                        localView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        onSpeak(point.exampleJp)
                        scope.launch {
                            successOverlayText = "Đáp án: ${tmpl.char} (${point.quizAnswerRomaji})"
                            delay(700L)
                            successOverlayText = null
                            if (currentIndex + 1 < points.size) {
                                currentIndex += 1
                            } else {
                                onCompleteAllGrammar()
                            }
                        }
                    } else {
                        isFeedbackError = true
                        shakeTrigger++
                        localView.performHapticFeedback(HapticFeedbackConstants.REJECT)
                        feedbackText = "ối zồi ôi làm được không đấy — Đáp án đúng là chữ '${tmpl.char}' (${point.quizAnswerRomaji}, ${tmpl.strokeCount} nét). Hãy viết lại nhé!"
                    }
                },
                modifier = Modifier.weight(1.2f)
            )
        }
    }
}

/**
 * Màn hình Kiểm tra Mở khoá Chặng (Stage Checkpoint Test):
 * Người dùng viết 4 chữ/câu hỏi đại diện của Chặng để mở khoá Chặng tiếp theo trên Con đường N5.
 */
@Composable
private fun StageCheckpointTestView(
    stage: RoadmapStageNode,
    questions: List<CheckpointQuestion>,
    recognizer: KanaRecognizer?,
    strictnessLevel: vn.kana.recognizer.StrictnessLevel,
    onSpeak: (String, String) -> Unit,
    onPassedAll: () -> Unit,
    onCancel: () -> Unit,
    localView: android.view.View,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var qIndex by remember { mutableIntStateOf(0) }
    val question = questions.getOrNull(qIndex) ?: return

    var userStrokes by remember(qIndex) { mutableStateOf<List<Stroke>>(emptyList()) }
    var feedbackText by remember(qIndex) { mutableStateOf<String?>(null) }
    var isFeedbackError by remember(qIndex) { mutableStateOf(false) }
    var shakeTrigger by remember { mutableIntStateOf(0) }
    var successOverlayText by remember(qIndex) { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoadmapActionBtn(text = "✕ Đóng", filled = false, onClick = onCancel)
            Text(
                text = "KIỂM TRA CHẶNG ${stage.numberFormatted} (${qIndex + 1}/${questions.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            RoadmapActionBtn(
                text = "🔊 Nghe",
                filled = true,
                onClick = { onSpeak(question.targetTemplate.char, question.targetTemplate.romaji) }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.onBackground)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = question.promptVi,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${question.subPromptVi} • Đã vẽ: ${userStrokes.size}/${question.targetTemplate.strokeCount} nét",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        feedbackText?.let { msg ->
            val col = if (isFeedbackError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                color = col,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, col)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            KanaDrawingCanvas(
                userStrokes = userStrokes,
                onStrokeCompleted = { stroke ->
                    userStrokes = userStrokes + stroke
                    feedbackText = null
                },
                templateStrokes = question.targetTemplate.toTypedStrokes(),
                showGhostTemplate = false,
                shakeTrigger = shakeTrigger,
                successOverlayText = successOverlayText,
                enabled = successOverlayText == null,
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RoadmapActionBtn(
                text = "Hoàn tác",
                filled = false,
                onClick = { if (userStrokes.isNotEmpty()) userStrokes = userStrokes.dropLast(1) },
                modifier = Modifier.weight(0.85f)
            )
            RoadmapActionBtn(
                text = "Xoá",
                filled = false,
                onClick = {
                    userStrokes = emptyList()
                    feedbackText = null
                },
                modifier = Modifier.weight(0.65f)
            )
            RoadmapActionBtn(
                text = "Kiểm tra →",
                filled = true,
                onClick = {
                    val engine = recognizer ?: return@RoadmapActionBtn
                    val tmpl = question.targetTemplate
                    val cfg = RecognizerConfig.forLevel(strictnessLevel)
                    val res = engine.recognize(userStrokes, tmpl.char, cfg)
                    if (res.passed) {
                        isFeedbackError = false
                        localView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        onSpeak(tmpl.char, tmpl.romaji)
                        scope.launch {
                            successOverlayText = "${tmpl.char} • ${res.score} điểm"
                            delay(650L)
                            successOverlayText = null
                            if (qIndex + 1 < questions.size) {
                                qIndex += 1
                            } else {
                                onPassedAll()
                            }
                        }
                    } else {
                        isFeedbackError = true
                        shakeTrigger++
                        localView.performHapticFeedback(HapticFeedbackConstants.REJECT)
                        feedbackText = "ối zồi ôi làm được không đấy — Đáp án là '${tmpl.char}' (${tmpl.strokeCount} nét). Hãy viết lại nhé!"
                    }
                },
                modifier = Modifier.weight(1.2f)
            )
        }
    }
}

/**
 * Hiệu ứng Mở khoá Chặng mới (Unlock Celebration Animation) khi hoàn thành bài kiểm tra trạm.
 */
@Composable
private fun StageUnlockCelebrationDialog(
    unlockedStageNumber: Int,
    unlockedStageTitle: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember { Animatable(0.55f) }
    LaunchedEffect(Unit) {
        scaleAnim.animateTo(1.0f, animationSpec = tween(durationMillis = 360))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scaleAnim.value)
                .border(2.dp, MaterialTheme.colorScheme.onBackground)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
            ) {
                Text(
                    text = "%02d".format(unlockedStageNumber),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Text(
                text = "🔓 MỞ KHOÁ CHẶNG MỚI!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

            Text(
                text = "Chặng %02d: %s".format(unlockedStageNumber, unlockedStageTitle),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "✓ Hay bạn! Bạn đã vượt qua bài kiểm tra kiến thức và mở khoá chặng tiếp theo trên con đường chinh phục JLPT N5.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            RoadmapActionBtn(
                text = "Tiếp tục trên Lộ trình N5 →",
                filled = true,
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RoadmapActionBtn(
    text: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                if (filled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .border(1.dp, MaterialTheme.colorScheme.onBackground)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (filled) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            softWrap = false
        )
    }
}
