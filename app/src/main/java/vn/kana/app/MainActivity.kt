package vn.kana.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.kana.app.data.ThemeMode
import vn.kana.app.ui.custom.CustomStudyScreen
import vn.kana.app.ui.debug.RecognizerDebugScreen
import vn.kana.app.ui.debug.RecognizerDebugViewModel
import vn.kana.app.ui.home.HomeScreen
import vn.kana.app.ui.lesson.LessonScreen
import vn.kana.app.ui.lesson.LessonViewModel
import vn.kana.app.ui.license.LicenseScreen
import vn.kana.app.ui.map.LessonMapScreen
import vn.kana.app.ui.map.LessonMapViewModel
import vn.kana.app.ui.reference.KanaReferenceScreen
import vn.kana.app.ui.review.ReviewScreen
import vn.kana.app.ui.review.ReviewViewModel
import vn.kana.app.ui.settings.SettingsScreen
import vn.kana.app.ui.theme.KanaTheme

private enum class AppDestination {
    HOME,
    LESSON_MAP,
    CUSTOM_STUDY,
    ACTIVE_LESSON,
    REVIEW,
    REFERENCE_TABLE,
    SETTINGS,
    DEBUG_RECOGNIZER,
    LICENSES
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
                        AppDestination.DEBUG_RECOGNIZER -> AppDestination.SETTINGS
                        AppDestination.ACTIVE_LESSON -> AppDestination.LESSON_MAP
                        else -> AppDestination.HOME
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
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
                                    destination = AppDestination.LESSON_MAP
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

                        AppDestination.LESSON_MAP -> {
                            LessonMapScreen(
                                viewModel = mapViewModel,
                                onStartRowLesson = { rowName, script ->
                                    selectedRowName = rowName
                                    selectedScript = script
                                    startLessonInTestOutMode = false
                                    lessonInitialFocusChar = null
                                    sessionCounter++
                                    destination = AppDestination.ACTIVE_LESSON
                                },
                                onTestOutRowLesson = { rowName, script ->
                                    selectedRowName = rowName
                                    selectedScript = script
                                    startLessonInTestOutMode = true
                                    lessonInitialFocusChar = null
                                    sessionCounter++
                                    destination = AppDestination.ACTIVE_LESSON
                                },
                                onOpenCustomStudy = {
                                    destination = AppDestination.CUSTOM_STUDY
                                },
                                onBackHome = {
                                    destination = AppDestination.HOME
                                },
                                onOpenSettings = {
                                    destination = AppDestination.SETTINGS
                                },
                                onOpenLicenses = {
                                    destination = AppDestination.LICENSES
                                }
                            )
                        }

                        AppDestination.CUSTOM_STUDY -> {
                            CustomStudyScreen(
                                viewModel = mapViewModel,
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
                                    destination = AppDestination.LESSON_MAP
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
                                    destination = AppDestination.HOME
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
