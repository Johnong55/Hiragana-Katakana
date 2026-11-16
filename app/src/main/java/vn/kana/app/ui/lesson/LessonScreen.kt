package vn.kana.app.ui.lesson

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import vn.kana.app.domain.LessonStepType
import vn.kana.app.domain.QuizQuestionType
import vn.kana.app.ui.components.GenkoYoshiWordBar
import vn.kana.app.ui.components.KanaDrawingCanvas
import vn.kana.app.ui.components.KanaStrokeAnimatorCanvas

@Composable
fun LessonScreen(
    viewModel: LessonViewModel,
    onExitLesson: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val localView = LocalView.current
    val context = LocalContext.current

    if (uiState.isLessonCompleted) {
        LessonCompletedContent(
            rowName = uiState.rowName,
            learnedChars = uiState.rowCharacters.map { it.char },
            passedFirstAttempt = uiState.quizFirstAttemptPassedCount,
            totalQuizQuestions = uiState.quizQuestions.size,
            isTestOutMode = uiState.isRowTestOutMode,
            onFinish = onExitLesson,
            modifier = modifier
        )
        return
    }

    if (uiState.isTestOutFailed) {
        TestOutFailedContent(
            rowName = uiState.rowName,
            passedFirstAttempt = uiState.quizFirstAttemptPassedCount,
            totalQuizQuestions = uiState.quizQuestions.size,
            onRetryTestOut = viewModel::jumpToRowTestOut,
            onLearnStepByStep = viewModel::switchFromTestOutToNormalLesson,
            onExit = onExitLesson,
            modifier = modifier
        )
        return
    }

    // Điều khiển animation cho Bước 1 (XEM) hoặc khi hiển thị đáp án sửa lỗi (showingCorrectionAnimation)
    val isShowingAnimator = uiState.stepType == LessonStepType.STEP_1_OBSERVE ||
        uiState.showingCorrectionAnimation

    val animatorTemplate = if (uiState.showingCorrectionAnimation) {
        uiState.correctionTemplate ?: uiState.currentCharacter
    } else {
        uiState.currentCharacter
    }
    val animatorStrokes = remember(animatorTemplate?.char) {
        animatorTemplate?.toTypedStrokes().orEmpty()
    }

    var animStrokeIndex by remember { mutableIntStateOf(0) }
    var animStrokeProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(
        isShowingAnimator,
        animatorTemplate?.char,
        uiState.animationRestartTrigger,
        uiState.isAnimationPlaying,
        uiState.isAnimationLooping,
        uiState.settings.animationSpeed,
        uiState.showingCorrectionAnimation
    ) {
        if (!isShowingAnimator || animatorStrokes.isEmpty()) return@LaunchedEffect
        val shouldPlay = if (uiState.showingCorrectionAnimation) true else uiState.isAnimationPlaying
        if (!shouldPlay) return@LaunchedEffect

        animStrokeIndex = 0
        animStrokeProgress = 0f

        val shouldLoop = if (uiState.showingCorrectionAnimation) true else uiState.isAnimationLooping
        val speed = uiState.settings.animationSpeed.coerceIn(0.5f, 2.0f)

        do {
            for (sIdx in animatorStrokes.indices) {
                animStrokeIndex = sIdx
                animStrokeProgress = 0f
                val durationMs = (680f / speed).toInt().coerceAtLeast(140)
                animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
                ) { value, _ ->
                    animStrokeProgress = value
                }
                delay((200f / speed).toLong().coerceAtLeast(60L))
            }
            animStrokeIndex = animatorStrokes.size
            animStrokeProgress = 1f

            if (shouldLoop) {
                delay((750f / speed).toLong().coerceAtLeast(250L))
                animStrokeIndex = 0
                animStrokeProgress = 0f
            } else {
                break
            }
        } while (shouldLoop)
    }

    // Bố cục cố định trên 1 màn hình (KHÔNG dùng verticalScroll) để tránh kẹt cảm ứng khi viết
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Thanh tiêu đề & Tiến độ hàng
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "← Bản đồ",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .clickable(onClick = onExitLesson)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )

            val progressTitle = when {
                uiState.isRowTestOutMode -> "${uiState.rowName} • Vượt bài (${uiState.currentQuizIndex + 1}/${uiState.quizQuestions.size})"
                uiState.stepType == LessonStepType.END_OF_ROW_QUIZ ->
                    "${uiState.rowName} • Kiểm tra (${uiState.currentQuizIndex + 1}/${uiState.quizQuestions.size})"
                else ->
                    "${uiState.rowName} • Chữ ${uiState.currentCharIndex + 1}/${uiState.rowCharacters.size}"
            }
            Text(
                text = progressTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (uiState.stepType != LessonStepType.END_OF_ROW_QUIZ) {
                Text(
                    text = "Vượt bài →",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary)
                        .border(1.dp, MaterialTheme.colorScheme.onBackground)
                        .clickable(onClick = viewModel::jumpToRowTestOut)
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )
            } else {
                // Dãy chữ trong hàng
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    uiState.rowCharacters.forEach { tmpl ->
                        Text(
                            text = tmpl.char,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // 2. Thanh bước hiện tại (1.XEM -> 2.ĐỒ -> 3.GỢI Ý -> 4.NHỚ -> 5.TỪ -> KIỂM TRA)
        StepIndicatorStrip(currentStep = uiState.stepType)

        // 3. Khung Đề bài / Hướng dẫn / Dãy ô vuông 原稿用紙
        PromptHeaderSection(
            uiState = uiState,
            onSpeak = viewModel::speakCurrentPrompt,
            onSpeakSentence = viewModel::speakCurrentExampleSentence,
            onSelectWordSlot = viewModel::selectWordSlot,
            onRevealRomajiFallback = viewModel::revealListenRomajiFallback,
            onFastTrackCharacter = viewModel::jumpToCharacterFastTest
        )

        // 4. Thông báo phản hồi / lỗi (nếu có)
        uiState.feedbackMessage?.let { msg ->
            val borderCol = if (uiState.isFeedbackError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onBackground
            }
            val textCol = if (uiState.isFeedbackError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onBackground
            }
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                color = textCol,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderCol)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        // 5. Ô vuông 1:1 trung tâm (chiếm phần không gian còn lại, tỉ lệ 1:1 chuẩn)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isShowingAnimator) {
                KanaStrokeAnimatorCanvas(
                    strokes = animatorStrokes,
                    activeStrokeIndex = animStrokeIndex,
                    activeStrokeProgress = animStrokeProgress,
                    showGhostStrokes = true,
                    showStrokeNumbers = true,
                    showDirectionArrows = true,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val activeStrokes = if (uiState.isMultiBoxMode) {
                    uiState.wordSlots.getOrNull(uiState.activeSlotIndex)?.userStrokes.orEmpty()
                } else {
                    uiState.singleCharStrokes
                }

                val activeTemplateStrokes = if (uiState.isMultiBoxMode) {
                    uiState.wordSlots.getOrNull(uiState.activeSlotIndex)?.templateStrokes.orEmpty()
                } else {
                    uiState.currentCharacter?.toTypedStrokes().orEmpty()
                }

                // Bước 2: Hiện nét mờ + gợi ý nét kế tiếp
                // Bước 3: Chỉ hiện số thứ tự + điểm bắt đầu của nét kế tiếp
                // Bước 4, 5 & Kiểm tra: KHÔNG gợi ý nét, KHÔNG animation
                val showGhost = (uiState.stepType == LessonStepType.STEP_2_TRACE)
                val nextHintIdx = if (
                    uiState.stepType == LessonStepType.STEP_2_TRACE ||
                    uiState.stepType == LessonStepType.STEP_3_GUIDED
                ) {
                    activeStrokes.size
                } else {
                    null
                }

                KanaDrawingCanvas(
                    userStrokes = activeStrokes,
                    onStrokeCompleted = viewModel::onStrokeCompleted,
                    onStrokeActivity = viewModel::onUserCanvasActivity,
                    templateStrokes = activeTemplateStrokes,
                    showGhostTemplate = showGhost,
                    showTemplateComparison = false,
                    nextHintStrokeIndex = nextHintIdx,
                    shakeTrigger = uiState.shakeTrigger,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 6. Cụm nút điều khiển dưới cùng
        when {
            uiState.showingCorrectionAnimation -> {
                // Đang hiển thị đáp án + animate nét sau khi trả lời sai
                ActionButton(
                    text = if (uiState.mustRewriteAfterError) {
                        "Đã xem nét đúng • Viết lại 1 lần"
                    } else {
                        "Đã xem nét đúng • Thử viết lại"
                    },
                    filled = true,
                    onClick = viewModel::dismissCorrectionAndStartRewrite,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            uiState.stepType == LessonStepType.STEP_1_OBSERVE -> {
                // Điều khiển Bước 1: Tốc độ + Lặp lại + Phát lại + Tiếp tục
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val speeds = listOf(0.5f, 1.0f, 1.5f, 2.0f)
                    for (spd in speeds) {
                        val selected = uiState.settings.animationSpeed == spd
                        Text(
                            text = "${spd}x",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(1.dp, MaterialTheme.colorScheme.onBackground)
                                .clickable { viewModel.setObserveSpeed(spd) }
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }
                    ActionButton(
                        text = if (uiState.isAnimationLooping) "Lặp: BẬT" else "Lặp: TẮT",
                        filled = false,
                        onClick = viewModel::toggleObserveLoop,
                        modifier = Modifier.weight(1f)
                    )
                    ActionButton(
                        text = "Xem lại",
                        filled = false,
                        onClick = viewModel::replayObserveAnimation,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionButton(
                        text = "Bước 2: Đồ theo →",
                        filled = true,
                        onClick = viewModel::advanceFromObserveToTrace,
                        modifier = Modifier.weight(1f)
                    )
                    ActionButton(
                        text = "Đã biết chữ này → Kiểm tra luôn",
                        filled = false,
                        onClick = viewModel::jumpToCharacterFastTest,
                        modifier = Modifier.weight(1.1f)
                    )
                }
            }

            else -> {
                // Bước 2, 3, 4, 5 & Kiểm tra cuối bài: Hoàn tác nét cuối, Xoá hết, Kiểm tra
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionButton(
                        text = "Hoàn tác nét cuối",
                        filled = false,
                        onClick = viewModel::undoLastStroke,
                        modifier = Modifier.weight(1f)
                    )
                    ActionButton(
                        text = "Xoá hết",
                        filled = false,
                        onClick = viewModel::clearCanvas,
                        modifier = Modifier.weight(0.75f)
                    )
                    ActionButton(
                        text = "Kiểm tra",
                        filled = true,
                        onClick = {
                            val passed = viewModel.checkCurrentSubmission()
                            if (!passed) {
                                localView.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                vibrateOnError(context)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepIndicatorStrip(currentStep: LessonStepType) {
    val steps = listOf(
        LessonStepType.STEP_1_OBSERVE to "1.Xem",
        LessonStepType.STEP_2_TRACE to "2.Đồ",
        LessonStepType.STEP_3_GUIDED to "3.Gợi ý",
        LessonStepType.STEP_4_RECALL to "4.Nhớ",
        LessonStepType.STEP_5_VOCAB to "5.Từ",
        LessonStepType.END_OF_ROW_QUIZ to "K.Tra"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for ((step, label) in steps) {
            val active = step == currentStep
            val completed = step.stepNumber < currentStep.stepNumber
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        1.dp,
                        if (active || completed) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.outline
                    )
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun PromptHeaderSection(
    uiState: LessonUiState,
    onSpeak: () -> Unit,
    onSpeakSentence: () -> Unit = {},
    onSelectWordSlot: (Int) -> Unit,
    onRevealRomajiFallback: () -> Unit,
    onFastTrackCharacter: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when (uiState.stepType) {
            LessonStepType.STEP_1_OBSERVE,
            LessonStepType.STEP_2_TRACE,
            LessonStepType.STEP_3_GUIDED -> {
                val ch = uiState.currentCharacter ?: return@Column
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = ch.char,
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Column {
                            val strokeProgressLabel = if (uiState.stepType == LessonStepType.STEP_1_OBSERVE) {
                                "${ch.strokeCount} nét"
                            } else {
                                "Đã vẽ: ${uiState.singleCharStrokes.size}/${ch.strokeCount} nét"
                            }
                            Text(
                                text = "${uiState.stepType.titleVi} • ${ch.romaji} ($strokeProgressLabel)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = uiState.stepType.instructionVi,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.stepType != LessonStepType.STEP_1_OBSERVE) {
                                Text(
                                    text = "Đã thuộc chữ '${ch.char}'? Bấm để kiểm tra luôn →",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .clickable(onClick = onFastTrackCharacter)
                                )
                            }
                        }
                    }
                    SpeakButton(onClick = onSpeak)
                }
            }

            LessonStepType.STEP_4_RECALL -> {
                val ch = uiState.currentCharacter ?: return@Column
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val headerText = if (uiState.showingCorrectionAnimation) {
                            "Đáp án chuẩn: ${ch.char} (${ch.romaji})"
                        } else if (uiState.mustRewriteAfterError) {
                            "Viết lại từ trí nhớ chữ có âm: \"${ch.romaji}\""
                        } else {
                            "Bước 4/5: Viết từ trí nhớ chữ có âm: \"${ch.romaji}\""
                        }
                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Không có gợi ý nét • Đã vẽ: ${uiState.singleCharStrokes.size}/${ch.strokeCount} nét",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SpeakButton(onClick = onSpeak)
                }
            }

            LessonStepType.STEP_5_VOCAB -> {
                val word = uiState.currentVocabWord ?: return@Column
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Từ vựng (${uiState.currentWordIndex + 1}/${uiState.currentCharWords.size}): ${word.meaningVi}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        val subText = if (uiState.showingCorrectionAnimation) {
                            "Đáp án: ${word.word} (${word.romaji})"
                        } else {
                            "Romaji: ${word.romaji} • Tự chuyển ô sau ${uiState.settings.autoAdvanceDelayMs}ms"
                        }
                        Text(
                            text = subText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (word.exampleSentence.isNotBlank()) {
                            Text(
                                text = "Câu mẫu: ${word.exampleSentence} (${word.exampleVi})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable(onClick = onSpeakSentence)
                            )
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        SpeakButton(onClick = onSpeak)
                        if (word.exampleSentence.isNotBlank()) {
                            Text(
                                text = "Nghe câu",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                                    .clickable(onClick = onSpeakSentence)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                GenkoYoshiWordBar(
                    slots = uiState.wordSlots,
                    activeSlotIndex = uiState.activeSlotIndex,
                    onSelectSlot = onSelectWordSlot
                )
            }

            LessonStepType.END_OF_ROW_QUIZ -> {
                val q = uiState.currentQuizQuestion ?: return@Column
                when (q.type) {
                    QuizQuestionType.ROMAJI_TO_KANA -> {
                        val t = q.targetTemplate ?: return@Column
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = q.type.labelVi,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (uiState.showingCorrectionAnimation) {
                                        "Đáp án: ${t.char} (Romaji: ${t.romaji})"
                                    } else {
                                        "Hãy viết chữ Kana có âm: \"${t.romaji}\""
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            SpeakButton(onClick = onSpeak)
                        }
                    }

                    QuizQuestionType.LISTEN_TO_KANA -> {
                        val t = q.targetTemplate ?: return@Column
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = q.type.labelVi,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val promptText = when {
                                    uiState.showingCorrectionAnimation -> "Đáp án: ${t.char} (${t.romaji})"
                                    uiState.showFallbackRomajiInListenMode -> "Nghe & viết chữ Kana (Romaji: ${t.romaji})"
                                    else -> "Bấm 'Phát âm' để nghe rồi viết chữ Kana tương ứng"
                                }
                                Text(
                                    text = promptText,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (!uiState.showFallbackRomajiInListenMode && !uiState.showingCorrectionAnimation) {
                                    Text(
                                        text = "Không nghe được âm thanh? Bấm để hiện Romaji",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.clickable(onClick = onRevealRomajiFallback)
                                    )
                                }
                            }
                            SpeakButton(onClick = onSpeak)
                        }
                    }

                    QuizQuestionType.MEANING_ROMAJI_TO_WORD -> {
                        val w = q.targetWord ?: return@Column
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = q.type.labelVi,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${w.meaningVi} • Romaji: ${w.romaji}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            SpeakButton(onClick = onSpeak)
                        }
                        GenkoYoshiWordBar(
                            slots = uiState.wordSlots,
                            activeSlotIndex = uiState.activeSlotIndex,
                            onSelectSlot = onSelectWordSlot
                        )
                    }

                    QuizQuestionType.LISTEN_TO_WORD -> {
                        val w = q.targetWord ?: return@Column
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = q.type.labelVi,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val title = when {
                                    uiState.showingCorrectionAnimation -> "Đáp án: ${w.word} (${w.romaji} - ${w.meaningVi})"
                                    uiState.showFallbackRomajiInListenMode -> "Nghe & viết từ: ${w.meaningVi} (${w.romaji})"
                                    else -> "Nghe phát âm & viết từ có nghĩa: \"${w.meaningVi}\""
                                }
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (!uiState.showFallbackRomajiInListenMode && !uiState.showingCorrectionAnimation) {
                                    Text(
                                        text = "Không nghe được? Bấm để hiện Romaji",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.clickable(onClick = onRevealRomajiFallback)
                                    )
                                }
                            }
                            SpeakButton(onClick = onSpeak)
                        }
                        GenkoYoshiWordBar(
                            slots = uiState.wordSlots,
                            activeSlotIndex = uiState.activeSlotIndex,
                            onSelectSlot = onSelectWordSlot
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeakButton(onClick: () -> Unit) {
    Text(
        text = "Phát âm",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun ActionButton(
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
            .padding(vertical = 11.dp, horizontal = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (filled) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun LessonCompletedContent(
    rowName: String,
    learnedChars: List<String>,
    passedFirstAttempt: Int,
    totalQuizQuestions: Int,
    isTestOutMode: Boolean = false,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                .border(1.dp, MaterialTheme.colorScheme.onBackground)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isTestOutMode) "VƯỢT QUA KIỂM TRA NHANH" else "HOÀN THÀNH BÀI HỌC",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Đã mở khoá chữ mới • Hàng $rowName",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
            Text(
                text = learnedChars.joinToString("   "),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Kết quả kiểm tra: đúng ngay lần đầu $passedFirstAttempt / $totalQuizQuestions câu",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            ActionButton(
                text = "Tiếp tục sang bài mới →",
                filled = true,
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TestOutFailedContent(
    rowName: String,
    passedFirstAttempt: Int,
    totalQuizQuestions: Int,
    onRetryTestOut: () -> Unit,
    onLearnStepByStep: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                .border(1.dp, MaterialTheme.colorScheme.error)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "CHƯA ĐẠT KIỂM TRA VƯỢT BÀI",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Hàng $rowName: Đúng ngay lần đầu $passedFirstAttempt / $totalQuizQuestions câu",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Để vượt bài nhanh và mở khoá chữ cái mới mà không cần học từng bước, bạn cần viết đúng hết 100% các chữ và từ vựng ngay lần đầu.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            ActionButton(
                text = "Thử kiểm tra lại lần nữa",
                filled = true,
                onClick = onRetryTestOut,
                modifier = Modifier.fillMaxWidth()
            )
            ActionButton(
                text = "Học từng chữ theo 5 bước",
                filled = false,
                onClick = onLearnStepByStep,
                modifier = Modifier.fillMaxWidth()
            )
            ActionButton(
                text = "Về Bản đồ bài học",
                filled = false,
                onClick = onExit,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun vibrateOnError(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(70L, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            v?.vibrate(VibrationEffect.createOneShot(70L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    } catch (_: Exception) {
    }
}
