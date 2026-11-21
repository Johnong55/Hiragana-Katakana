package vn.kana.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import vn.kana.app.data.ThemeMode
import vn.kana.app.domain.DailyTipsCatalog
import vn.kana.app.ui.custom.CustomStudyScreen
import vn.kana.app.ui.debug.RecognizerDebugScreen
import vn.kana.app.ui.debug.RecognizerDebugViewModel
import vn.kana.app.ui.home.HomeScreen
import vn.kana.app.ui.counters.CountersDrillScreen
import vn.kana.app.ui.dictionary.OfflineDictionaryScreen
import vn.kana.app.ui.lesson.LessonScreen
import vn.kana.app.ui.lesson.LessonViewModel
import vn.kana.app.ui.license.LicenseScreen
import vn.kana.app.ui.listening.ListeningQuizScreen
import vn.kana.app.ui.map.LessonMapViewModel
import vn.kana.app.ui.mocktest.N5MockTestScreen
import vn.kana.app.ui.practice.ConfusableAndFlashcardScreen
import vn.kana.app.ui.reading.ReadingPracticeScreen
import vn.kana.app.ui.reference.KanaReferenceScreen
import vn.kana.app.ui.review.ReviewScreen
import vn.kana.app.ui.review.ReviewViewModel
import vn.kana.app.ui.roadmap.N5RoadmapScreen
import vn.kana.app.ui.conjugation.ConjugationDojoScreen
import vn.kana.app.ui.stats.ProgressStatsScreen
import vn.kana.app.ui.tips.DailyTipMagazinePopup
import vn.kana.app.ui.tips.TipsMagazineScreen
import vn.kana.app.ui.settings.SettingsScreen
import vn.kana.app.ui.theme.KanaTheme

private enum class AppDestination(val isMainTab: Boolean, val tabLabelVi: String = "") {
    ROADMAP_N5(isMainTab = true, tabLabelVi = "Trang chủ"),
    HOME(isMainTab = true, tabLabelVi = "Bảng chữ"),
    CUSTOM_STUDY(isMainTab = true, tabLabelVi = "Chọn hàng"),
    REVIEW(isMainTab = true, tabLabelVi = "Ôn tập"),
    SETTINGS(isMainTab = true, tabLabelVi = "Cài đặt"),
    ACTIVE_LESSON(isMainTab = false),
    REFERENCE_TABLE(isMainTab = false),
    TIPS_MAGAZINE(isMainTab = false),
    LISTENING_QUIZ(isMainTab = false),
    READING_PRACTICE(isMainTab = false),
    MOCK_TEST(isMainTab = false),
    FLASHCARDS_CONFUSABLES(isMainTab = false),
    DICTIONARY(isMainTab = false),
    COUNTERS_DRILL(isMainTab = false),
    CONJUGATION_DOJO(isMainTab = false),
    PROGRESS_STATS(isMainTab = false),
    DEBUG_RECOGNIZER(isMainTab = false),
    LICENSES(isMainTab = false)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as KanaApplication

        setContent {
            val mapViewModel: LessonMapViewModel = viewModel(
                factory = LessonMapViewModel.provideFactory(
                    strokeRepository = app.strokeRepository,
                    vocabRepository = app.vocabRepository,
                    settingsRepository = app.settingsRepository,
                    kanaDao = app.database.kanaDao(),
                    audioProvider = app.audioProvider
                )
            )
            val mapState by mapViewModel.uiState.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (mapState.settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
            }

            KanaTheme(darkTheme = useDarkTheme) {
                // Đặt Lộ trình N5 làm Trang chủ mặc định khi mở app
                var destination by rememberSaveable { mutableStateOf(AppDestination.ROADMAP_N5) }
                var returnTab by rememberSaveable { mutableStateOf(AppDestination.ROADMAP_N5) }
                var selectedRowName by rememberSaveable { mutableStateOf("あ行") }
                var selectedScript by rememberSaveable { mutableStateOf("hiragana") }
                var startLessonInTestOutMode by rememberSaveable { mutableStateOf(false) }
                var lessonInitialFocusChar by rememberSaveable { mutableStateOf<String?>(null) }
                var referenceInitialChar by rememberSaveable { mutableStateOf<String?>(null) }
                var sessionCounter by rememberSaveable { mutableIntStateOf(0) }
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val drawerScope = rememberCoroutineScope()

                BackHandler(enabled = drawerState.isOpen) {
                    drawerScope.launch { drawerState.close() }
                }

                BackHandler(enabled = !drawerState.isOpen && destination != AppDestination.ROADMAP_N5) {
                    destination = when (destination) {
                        AppDestination.DEBUG_RECOGNIZER,
                        AppDestination.LICENSES -> AppDestination.SETTINGS
                        AppDestination.ACTIVE_LESSON,
                        AppDestination.REFERENCE_TABLE,
                        AppDestination.TIPS_MAGAZINE,
                        AppDestination.LISTENING_QUIZ,
                        AppDestination.READING_PRACTICE,
                        AppDestination.MOCK_TEST,
                        AppDestination.FLASHCARDS_CONFUSABLES,
                        AppDestination.DICTIONARY,
                        AppDestination.COUNTERS_DRILL,
                        AppDestination.CONJUGATION_DOJO,
                        AppDestination.PROGRESS_STATS -> returnTab
                        else -> AppDestination.ROADMAP_N5
                    }
                }

                fun openFromDrawer(target: AppDestination) {
                    returnTab = destination.takeIf { it.isMainTab } ?: AppDestination.ROADMAP_N5
                    drawerScope.launch { drawerState.close() }
                    destination = target
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = destination.isMainTab,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerShape = RoundedCornerShape(0.dp),
                            drawerContainerColor = MaterialTheme.colorScheme.background,
                            modifier = Modifier
                                .width(296.dp)
                                .safeDrawingPadding()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.meme_shiba),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                        )
                                        Column {
                                            Text(
                                                text = "LUYỆN TẬP JLPT N5",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Text(
                                                text = "${mapState.settings.streakDays} ngày • ${mapState.totalLearnedCount} chữ",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = "✕",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier
                                            .border(1.dp, MaterialTheme.colorScheme.outline)
                                            .clickable { drawerScope.launch { drawerState.close() } }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                                data class GlobalDrawerItem(
                                    val iconRes: Int,
                                    val title: String,
                                    val subtitle: String,
                                    val target: AppDestination
                                )

                                val items = listOf(
                                    GlobalDrawerItem(R.drawable.meme_cat_reading, "Võ đường Chia thể N5", "Động từ ます/て/ない/た & Tính từ い/な", AppDestination.CONJUGATION_DOJO),
                                    GlobalDrawerItem(R.drawable.meme_capybara, "Đọc hiểu & Ghép câu", "8 bài đọc có Kana/dịch & 10 câu ★", AppDestination.READING_PRACTICE),
                                    GlobalDrawerItem(R.drawable.meme_cat_surprised, "Thi thử JLPT N5", "Đề 24 câu (15 phút) đủ 3 phần", AppDestination.MOCK_TEST),
                                    GlobalDrawerItem(R.drawable.meme_duck, "Số đếm, Giờ & Trợ số từ", "Biến âm 〜時, 〜分, 〜日, 〜本", AppDestination.COUNTERS_DRILL),
                                    GlobalDrawerItem(R.drawable.meme_red_panda, "Thẻ nhanh & Chữ dễ nhầm", "Flashcard vuốt & phân biệt シ/ツ", AppDestination.FLASHCARDS_CONFUSABLES),
                                    GlobalDrawerItem(R.drawable.meme_hamster, "Từ điển Offline", "Tra nhanh chữ, Kanji, từ vựng, ngữ pháp", AppDestination.DICTIONARY),
                                    GlobalDrawerItem(R.drawable.meme_shiba, "Luyện nghe phản xạ", "Nghe TTS & chọn đáp án nhanh", AppDestination.LISTENING_QUIZ),
                                    GlobalDrawerItem(R.drawable.meme_capybara, "Tạp chí Tips N5", "40 mẹo nhớ chữ & văn hoá Nhật", AppDestination.TIPS_MAGAZINE),
                                    GlobalDrawerItem(R.drawable.meme_cat_surprised, "Tiến độ & Chữ hay sai", "Thống kê chi tiết & luyện chữ yếu", AppDestination.PROGRESS_STATS)
                                )

                                items.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, MaterialTheme.colorScheme.outline)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .clickable { openFromDrawer(item.target) }
                                            .padding(horizontal = 10.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = painterResource(id = item.iconRes),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                        )
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Text(
                                                text = item.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            AnimatedContent(
                                targetState = destination,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(140))
                                        .togetherWith(fadeOut(animationSpec = tween(100)))
                                },
                                label = "screen_transition"
                            ) { activeDestination ->
                                when (activeDestination) {
                                AppDestination.HOME -> {
                                    HomeScreen(
                                        viewModel = mapViewModel,
                                        onStartReview = {
                                            sessionCounter++
                                            destination = AppDestination.REVIEW
                                        },
                                        onStartRowLesson = { rowName, script ->
                                            returnTab = AppDestination.HOME
                                            selectedRowName = rowName
                                            selectedScript = script
                                            startLessonInTestOutMode = false
                                            lessonInitialFocusChar = null
                                            sessionCounter++
                                            destination = AppDestination.ACTIVE_LESSON
                                        },
                                        onOpenLessonMap = {
                                            destination = AppDestination.ROADMAP_N5
                                        },
                                        onOpenCustomStudy = {
                                            destination = AppDestination.CUSTOM_STUDY
                                        },
                                        onOpenReference = { char ->
                                            returnTab = AppDestination.HOME
                                            referenceInitialChar = char
                                            destination = AppDestination.REFERENCE_TABLE
                                        },
                                        onPracticeCharacter = { rowName, script, targetChar ->
                                            returnTab = AppDestination.HOME
                                            selectedRowName = rowName
                                            selectedScript = script
                                            startLessonInTestOutMode = false
                                            lessonInitialFocusChar = targetChar
                                            sessionCounter++
                                            destination = AppDestination.ACTIVE_LESSON
                                        },
                                        onOpenSettings = {
                                            destination = AppDestination.SETTINGS
                                        }
                                    )
                                }

                                AppDestination.ROADMAP_N5 -> {
                                    N5RoadmapScreen(
                                        viewModel = mapViewModel,
                                        onStartRowLesson = { rowName, script ->
                                            returnTab = AppDestination.ROADMAP_N5
                                            selectedRowName = rowName
                                            selectedScript = script
                                            startLessonInTestOutMode = false
                                            lessonInitialFocusChar = null
                                            sessionCounter++
                                            destination = AppDestination.ACTIVE_LESSON
                                        },
                                        onPracticeCharacter = { rowName, script, targetChar ->
                                            returnTab = AppDestination.ROADMAP_N5
                                            selectedRowName = rowName
                                            selectedScript = script
                                            startLessonInTestOutMode = false
                                            lessonInitialFocusChar = targetChar
                                            sessionCounter++
                                            destination = AppDestination.ACTIVE_LESSON
                                        },
                                        onOpenTabWithFilter = { tab ->
                                            mapViewModel.selectTab(tab)
                                            destination = AppDestination.CUSTOM_STUDY
                                        },
                                        onOpenTipsMagazine = {
                                            returnTab = AppDestination.ROADMAP_N5
                                            destination = AppDestination.TIPS_MAGAZINE
                                        },
                                        onOpenListeningQuiz = {
                                            returnTab = AppDestination.ROADMAP_N5
                                            destination = AppDestination.LISTENING_QUIZ
                                        },
                                        onOpenReadingPractice = {
                                            returnTab = AppDestination.ROADMAP_N5
                                            destination = AppDestination.READING_PRACTICE
                                        },
                                        onOpenMockTest = {
                                            returnTab = AppDestination.ROADMAP_N5
                                            destination = AppDestination.MOCK_TEST
                                        },
                                        onOpenFlashcardsAndConfusables = {
                                            returnTab = AppDestination.ROADMAP_N5
                                            destination = AppDestination.FLASHCARDS_CONFUSABLES
                                        },
                                        onOpenDictionary = {
                                            returnTab = AppDestination.ROADMAP_N5
                                            destination = AppDestination.DICTIONARY
                                        },
                                        onOpenCountersDrill = {
                                            returnTab = AppDestination.ROADMAP_N5
                                            destination = AppDestination.COUNTERS_DRILL
                                        },
                                        onOpenProgressStats = {
                                            returnTab = AppDestination.ROADMAP_N5
                                            destination = AppDestination.PROGRESS_STATS
                                        },
                                        onOpenMenu = {
                                            drawerScope.launch { drawerState.open() }
                                        }
                                    )
                                }

                                AppDestination.CUSTOM_STUDY -> {
                                    CustomStudyScreen(
                                        viewModel = mapViewModel,
                                        onStartRowLesson = { rowName, script ->
                                            returnTab = AppDestination.CUSTOM_STUDY
                                            selectedRowName = rowName
                                            selectedScript = script
                                            startLessonInTestOutMode = false
                                            lessonInitialFocusChar = null
                                            sessionCounter++
                                            destination = AppDestination.ACTIVE_LESSON
                                        },
                                        onBack = {
                                            destination = AppDestination.ROADMAP_N5
                                        }
                                    )
                                }

                                AppDestination.ACTIVE_LESSON -> {
                                    val lessonViewModel: LessonViewModel = viewModel(
                                        key = "lesson_${selectedScript}_${selectedRowName}_${startLessonInTestOutMode}_${lessonInitialFocusChar}_$sessionCounter",
                                        factory = LessonViewModel.provideFactory(
                                            rowName = selectedRowName,
                                            script = selectedScript,
                                            startInTestOutMode = startLessonInTestOutMode,
                                            initialFocusChar = lessonInitialFocusChar,
                                            strokeRepository = app.strokeRepository,
                                            vocabRepository = app.vocabRepository,
                                            settingsRepository = app.settingsRepository,
                                            kanaDao = app.database.kanaDao(),
                                            audioProvider = app.audioProvider
                                        )
                                    )
                                    LessonScreen(
                                        viewModel = lessonViewModel,
                                        onExitLesson = {
                                            destination = returnTab
                                        },
                                        onContinueNextRow = {
                                            // Tìm hàng tiếp theo ngay sau hàng vừa hoàn thành để học tiếp không bị lặp lại
                                            val allRows = mapState.allLessons
                                            val currentIdx = allRows.indexOfFirst {
                                                it.script == selectedScript && it.rowName == selectedRowName
                                            }
                                            val nextLesson = if (currentIdx != -1 && currentIdx + 1 < allRows.size) {
                                                allRows[currentIdx + 1]
                                            } else {
                                                allRows.firstOrNull { !it.isCompleted }
                                            }
                                            if (nextLesson != null) {
                                                selectedRowName = nextLesson.rowName
                                                selectedScript = nextLesson.script
                                                startLessonInTestOutMode = false
                                                lessonInitialFocusChar = null
                                                sessionCounter++
                                                destination = AppDestination.ACTIVE_LESSON
                                            } else {
                                                destination = AppDestination.HOME
                                            }
                                        }
                                    )
                                }

                                AppDestination.REVIEW -> {
                                    val reviewViewModel: ReviewViewModel = viewModel(
                                        key = "review_$sessionCounter",
                                        factory = ReviewViewModel.provideFactory(
                                            strokeRepository = app.strokeRepository,
                                            vocabRepository = app.vocabRepository,
                                            settingsRepository = app.settingsRepository,
                                            kanaDao = app.database.kanaDao(),
                                            audioProvider = app.audioProvider
                                        )
                                    )
                                    ReviewScreen(
                                        viewModel = reviewViewModel,
                                        onExitReview = {
                                            destination = AppDestination.ROADMAP_N5
                                        }
                                    )
                                }

                                AppDestination.REFERENCE_TABLE -> {
                                    KanaReferenceScreen(
                                        viewModel = mapViewModel,
                                        initialChar = referenceInitialChar,
                                        onPracticeCharacter = { rowName, script, targetChar ->
                                            selectedRowName = rowName
                                            selectedScript = script
                                            startLessonInTestOutMode = false
                                            lessonInitialFocusChar = targetChar
                                            sessionCounter++
                                            destination = AppDestination.ACTIVE_LESSON
                                        },
                                        onBack = {
                                            destination = returnTab
                                        }
                                    )
                                }

                                AppDestination.SETTINGS -> {
                                    SettingsScreen(
                                        settings = mapState.settings,
                                        onChangeStrictness = mapViewModel::updateStrictness,
                                        onChangeAnimationSpeed = mapViewModel::updateAnimationSpeed,
                                        onChangeAutoAdvanceDelay = mapViewModel::updateAutoAdvanceDelay,
                                        onChangeSoundEnabled = mapViewModel::updateSoundEnabled,
                                        onChangeUnlockAllRows = mapViewModel::updateUnlockAllRows,
                                        onChangeThemeMode = mapViewModel::updateThemeMode,
                                        onChangeDailyReminder = mapViewModel::updateDailyReminder,
                                        onClearProgress = mapViewModel::clearAllProgress,
                                        onOpenDebugScreen = {
                                            destination = AppDestination.DEBUG_RECOGNIZER
                                        },
                                        onOpenLicenses = {
                                            destination = AppDestination.LICENSES
                                        },
                                        onBack = {
                                            destination = returnTab
                                        }
                                    )
                                }

                                AppDestination.DEBUG_RECOGNIZER -> {
                                    val debugViewModel: RecognizerDebugViewModel = viewModel(
                                        factory = RecognizerDebugViewModel.provideFactory(
                                            strokeRepository = app.strokeRepository,
                                            sampleStorage = app.debugSampleStorage,
                                            audioProvider = app.audioProvider
                                        )
                                    )
                                    RecognizerDebugScreen(
                                        viewModel = debugViewModel,
                                        onBack = {
                                            destination = AppDestination.SETTINGS
                                        }
                                    )
                                }

                                AppDestination.LICENSES -> {
                                    LicenseScreen(
                                        onBack = {
                                            destination = AppDestination.SETTINGS
                                        }
                                    )
                                }

                                AppDestination.TIPS_MAGAZINE -> {
                                    val todayTip = DailyTipsCatalog.getTipForEpochDay(LocalDate.now().toEpochDay())
                                    TipsMagazineScreen(
                                        todayTip = todayTip,
                                        onSpeak = { text -> mapViewModel.speak(text, null) },
                                        onBack = { destination = returnTab }
                                    )
                                }

                                AppDestination.LISTENING_QUIZ -> {
                                    ListeningQuizScreen(
                                        basicHiragana = mapState.basicHiragana,
                                        basicKatakana = mapState.basicKatakana,
                                        extendedCharacters = mapState.extendedCharacters,
                                        n5KanjiCharacters = mapState.n5KanjiCharacters,
                                        autoAdvanceDelayMs = mapState.settings.autoAdvanceDelayMs,
                                        onSpeak = { text, romaji -> mapViewModel.speak(text, romaji) },
                                        onRecordStudyActivity = { mapViewModel.recordStudyActivityToday() },
                                        onBack = { destination = returnTab }
                                    )
                                }

                                AppDestination.READING_PRACTICE -> {
                                    ReadingPracticeScreen(
                                        onSpeak = { text -> mapViewModel.speak(text, null) },
                                        onRecordStudyActivity = { mapViewModel.recordStudyActivityToday() },
                                        onBack = { destination = returnTab }
                                    )
                                }

                                AppDestination.MOCK_TEST -> {
                                    N5MockTestScreen(
                                        bestScorePercent = mapState.settings.bestMockScorePercent,
                                        attemptCount = mapState.settings.mockTestAttemptCount,
                                        onSpeak = { text -> mapViewModel.speak(text, null) },
                                        onSaveMockResult = { score -> mapViewModel.recordMockTestResult(score) },
                                        onRecordStudyActivity = { mapViewModel.recordStudyActivityToday() },
                                        onBack = { destination = returnTab }
                                    )
                                }

                                AppDestination.FLASHCARDS_CONFUSABLES -> {
                                    ConfusableAndFlashcardScreen(
                                        n5KanjiCharacters = mapState.n5KanjiCharacters,
                                        wordsByChar = mapState.wordsByChar,
                                        autoAdvanceDelayMs = mapState.settings.autoAdvanceDelayMs,
                                        onSpeak = { text, romaji -> mapViewModel.speak(text, romaji) },
                                        onRecordStudyActivity = { mapViewModel.recordStudyActivityToday() },
                                        onBack = { destination = returnTab }
                                    )
                                }

                                AppDestination.DICTIONARY -> {
                                    OfflineDictionaryScreen(
                                        allCharacters = mapState.allCurriculumCharacters,
                                        wordsByChar = mapState.wordsByChar,
                                        savedVocabWords = mapState.settings.savedVocabWords,
                                        onToggleSaveWord = { word -> mapViewModel.toggleSavedVocabWord(word) },
                                        onSpeak = { text, romaji -> mapViewModel.speak(text, romaji) },
                                        onPracticeCharacter = { rowName, script, targetChar ->
                                            returnTab = AppDestination.DICTIONARY
                                            selectedRowName = rowName
                                            selectedScript = script
                                            startLessonInTestOutMode = false
                                            lessonInitialFocusChar = targetChar
                                            sessionCounter++
                                            destination = AppDestination.ACTIVE_LESSON
                                        },
                                        onBack = { destination = returnTab }
                                    )
                                }

                                AppDestination.COUNTERS_DRILL -> {
                                    CountersDrillScreen(
                                        autoAdvanceDelayMs = mapState.settings.autoAdvanceDelayMs,
                                        onSpeak = { text, romaji -> mapViewModel.speak(text, romaji) },
                                        onRecordStudyActivity = { mapViewModel.recordStudyActivityToday() },
                                        onBack = { destination = returnTab }
                                    )
                                }

                                AppDestination.CONJUGATION_DOJO -> {
                                    ConjugationDojoScreen(
                                        onSpeak = { text -> mapViewModel.speak(text, null) },
                                        onBack = { destination = returnTab }
                                    )
                                }

                                AppDestination.PROGRESS_STATS -> {
                                    ProgressStatsScreen(
                                        uiState = mapState,
                                        onPracticeCharacter = { rowName, script, targetChar ->
                                            returnTab = AppDestination.PROGRESS_STATS
                                            selectedRowName = rowName
                                            selectedScript = script
                                            startLessonInTestOutMode = false
                                            lessonInitialFocusChar = targetChar
                                            sessionCounter++
                                            destination = AppDestination.ACTIVE_LESSON
                                        },
                                        onStartReview = {
                                            sessionCounter++
                                            destination = AppDestination.REVIEW
                                        },
                                        onBack = { destination = returnTab }
                                    )
                                }
                            }
                            }

                            // Vùng nhận cử chỉ vuốt từ cạnh trái sang phải ở mọi tab chính để mở nhanh Menu trượt bên trái
                            if (destination.isMainTab && drawerState.isClosed) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .fillMaxHeight()
                                        .width(28.dp)
                                        .pointerInput(destination) {
                                            var totalDrag = 0f
                                            var triggered = false
                                            detectHorizontalDragGestures(
                                                onDragStart = {
                                                    totalDrag = 0f
                                                    triggered = false
                                                },
                                                onHorizontalDrag = { _, dragAmount ->
                                                    if (dragAmount > 0f) {
                                                        totalDrag += dragAmount
                                                        if (!triggered && totalDrag > 24f) {
                                                            triggered = true
                                                            drawerScope.launch { drawerState.open() }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                )
                            }
                        }

                        // Thanh điều hướng 5 Tab cố định dưới đáy màn hình (gọn gàng, không có nút thừa)
                        AnimatedVisibility(
                            visible = destination.isMainTab,
                            enter = slideInVertically(animationSpec = tween(220)) { it } + fadeIn(tween(200)),
                            exit = slideOutVertically(animationSpec = tween(160)) { it } + fadeOut(tween(140))
                        ) {
                            MainBottomNavigationBar(
                                currentDestination = destination,
                                onSelectTab = { tab ->
                                    if (tab == AppDestination.REVIEW) {
                                        sessionCounter++
                                    }
                                    destination = tab
                                }
                            )
                        }
                    }
                }
                }

                val todayEpochDay = LocalDate.now().toEpochDay()
                if (!mapState.isLoading && mapState.settings.lastTipEpochDay != todayEpochDay) {
                    val todayTip = DailyTipsCatalog.getTipForEpochDay(todayEpochDay)
                    DailyTipMagazinePopup(
                        article = todayTip,
                        onSpeak = { text -> mapViewModel.speak(text, null) },
                        onReadMoreInMagazine = {
                            mapViewModel.markDailyTipSeenToday()
                            returnTab = destination.takeIf { it.isMainTab } ?: AppDestination.ROADMAP_N5
                            destination = AppDestination.TIPS_MAGAZINE
                        },
                        onDismiss = {
                            mapViewModel.markDailyTipSeenToday()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainBottomNavigationBar(
    currentDestination: AppDestination,
    onSelectTab: (AppDestination) -> Unit
) {
    val mainTabs = listOf(
        AppDestination.ROADMAP_N5,
        AppDestination.HOME,
        AppDestination.CUSTOM_STUDY,
        AppDestination.REVIEW,
        AppDestination.SETTINGS
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onBackground)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            for (tab in mainTabs) {
                val selected = currentDestination == tab
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        )
                        .border(0.5.dp, MaterialTheme.colorScheme.outline)
                        .clickable { onSelectTab(tab) }
                        .padding(vertical = 13.dp, horizontal = 4.dp)
                ) {
                    Text(
                        text = tab.tabLabelVi,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
