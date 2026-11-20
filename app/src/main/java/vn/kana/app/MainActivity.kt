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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import vn.kana.app.ui.shrine.OmikujiAndStampsScreen
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
    OMIKUJI_AND_STAMPS(isMainTab = false),
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

                BackHandler(enabled = destination != AppDestination.ROADMAP_N5) {
                    destination = when (destination) {
                        AppDestination.DEBUG_RECOGNIZER,
                        AppDestination.LICENSES -> AppDestination.SETTINGS
                        AppDestination.ACTIVE_LESSON,
                        AppDestination.REFERENCE_TABLE,
                        AppDestination.TIPS_MAGAZINE,
                        AppDestination.LISTENING_QUIZ,
                        AppDestination.PROGRESS_STATS -> returnTab
                        else -> AppDestination.ROADMAP_N5
                    }
                }

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
                                        onOpenOmikujiAndStamps = {
                                            returnTab = AppDestination.ROADMAP_N5
                                            destination = AppDestination.OMIKUJI_AND_STAMPS
                                        },
                                        onOpenProgressStats = {
                                            returnTab = AppDestination.ROADMAP_N5
                                            destination = AppDestination.PROGRESS_STATS
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

                                AppDestination.OMIKUJI_AND_STAMPS -> {
                                    OmikujiAndStampsScreen(
                                        uiState = mapState,
                                        onSpeak = { text -> mapViewModel.speak(text, null) },
                                        onRecordOmikujiDraw = { mapViewModel.recordOmikujiDraw() },
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
                        }

                        // Thanh điều hướng 4 Tab cố định dưới đáy màn hình (trượt mượt mà)
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
