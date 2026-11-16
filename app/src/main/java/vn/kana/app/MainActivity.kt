package vn.kana.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import vn.kana.app.data.ThemeMode
import vn.kana.app.ui.custom.CustomStudyScreen
import vn.kana.app.ui.debug.RecognizerDebugScreen
import vn.kana.app.ui.debug.RecognizerDebugViewModel
import vn.kana.app.ui.home.HomeScreen
import vn.kana.app.ui.lesson.LessonScreen
import vn.kana.app.ui.lesson.LessonViewModel
import vn.kana.app.ui.license.LicenseScreen
import vn.kana.app.ui.map.LessonMapViewModel
import vn.kana.app.ui.reference.KanaReferenceScreen
import vn.kana.app.ui.review.ReviewScreen
import vn.kana.app.ui.review.ReviewViewModel
import vn.kana.app.ui.roadmap.N5RoadmapScreen
import vn.kana.app.ui.settings.SettingsScreen
import vn.kana.app.ui.theme.KanaTheme

private enum class AppDestination(val isMainTab: Boolean, val tabLabelVi: String = "") {
    HOME(isMainTab = true, tabLabelVi = "Trang chủ"),
    ROADMAP_N5(isMainTab = true, tabLabelVi = "Lộ trình N5"),
    CUSTOM_STUDY(isMainTab = true, tabLabelVi = "Chọn hàng"),
    REVIEW(isMainTab = true, tabLabelVi = "Ôn tập"),
    SETTINGS(isMainTab = true, tabLabelVi = "Cài đặt"),
    ACTIVE_LESSON(isMainTab = false),
    REFERENCE_TABLE(isMainTab = false),
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
                var destination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
                var selectedRowName by rememberSaveable { mutableStateOf("あ行") }
                var selectedScript by rememberSaveable { mutableStateOf("hiragana") }
                var startLessonInTestOutMode by rememberSaveable { mutableStateOf(false) }
                var lessonInitialFocusChar by rememberSaveable { mutableStateOf<String?>(null) }
                var referenceInitialChar by rememberSaveable { mutableStateOf<String?>(null) }
                var sessionCounter by rememberSaveable { mutableIntStateOf(0) }

                BackHandler(enabled = destination != AppDestination.HOME) {
                    destination = when (destination) {
                        AppDestination.DEBUG_RECOGNIZER,
                        AppDestination.LICENSES -> AppDestination.SETTINGS
                        else -> AppDestination.HOME
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
                            when (destination) {
                                AppDestination.HOME -> {
                                    HomeScreen(
                                        viewModel = mapViewModel,
                                        onStartReview = {
                                            sessionCounter++
                                            destination = AppDestination.REVIEW
                                        },
                                        onStartRowLesson = { rowName, script ->
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
                                            referenceInitialChar = char
                                            destination = AppDestination.REFERENCE_TABLE
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
                                            selectedRowName = rowName
                                            selectedScript = script
                                            startLessonInTestOutMode = false
                                            lessonInitialFocusChar = null
                                            sessionCounter++
                                            destination = AppDestination.ACTIVE_LESSON
                                        },
                                        onOpenTabWithFilter = { tab ->
                                            mapViewModel.selectTab(tab)
                                            destination = AppDestination.CUSTOM_STUDY
                                        }
                                    )
                                }

                                AppDestination.CUSTOM_STUDY -> {
                                    CustomStudyScreen(
                                        viewModel = mapViewModel,
                                        onStartRowLesson = { rowName, script ->
                                            selectedRowName = rowName
                                            selectedScript = script
                                            startLessonInTestOutMode = false
                                            lessonInitialFocusChar = null
                                            sessionCounter++
                                            destination = AppDestination.ACTIVE_LESSON
                                        },
                                        onBack = {
                                            destination = AppDestination.HOME
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
                                            destination = AppDestination.HOME
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
                                            destination = AppDestination.HOME
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
                                            destination = AppDestination.HOME
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
                                        onClearProgress = mapViewModel::clearAllProgress,
                                        onOpenDebugScreen = {
                                            destination = AppDestination.DEBUG_RECOGNIZER
                                        },
                                        onOpenLicenses = {
                                            destination = AppDestination.LICENSES
                                        },
                                        onBack = {
                                            destination = AppDestination.HOME
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
                            }
                        }

                        // Thanh điều hướng 4 Tab cố định dưới đáy màn hình (gọn gàng, không bao giờ bấm nhầm)
                        if (destination.isMainTab) {
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
        }
    }
}

@Composable
private fun MainBottomNavigationBar(
    currentDestination: AppDestination,
    onSelectTab: (AppDestination) -> Unit
) {
    val mainTabs = listOf(
        AppDestination.HOME,
        AppDestination.ROADMAP_N5,
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
