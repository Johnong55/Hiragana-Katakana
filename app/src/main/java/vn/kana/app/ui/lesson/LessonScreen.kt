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
import androidx.compose.ui.text.style.TextOverflow
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
    onContinueNextRow: () -> Unit = onExitLesson,
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
            onContinueNextRow = onContinueNextRow,
            onBackHome = onExitLesson,
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
        uiState.settings.animationSpeed,
        uiState.showingCorrectionAnimation
    ) {
        if (!isShowingAnimator || animatorStrokes.isEmpty()) return@LaunchedEffect
        animStrokeIndex = 0
        animStrokeProgress = 0f
        val speed = uiState.settings.animationSpeed.coerceIn(0.5f, 2.0f)

        while (true) {
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
            delay((750f / speed).toLong().coerceAtLeast(250L))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Thanh tiêu đề tối giản: [✕ Đóng] • Tiến độ • [Vượt cả bài]
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "✕ Đóng",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .clickable(onClick = onExitLesson)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            )

            val progressTitle = when {
                uiState.isRowTestOutMode ->
                    "${uiState.rowName} • Vượt bài (${uiState.currentQuizIndex + 1}/${uiState.quizQuestions.size})"
                uiState.stepType == LessonStepType.END_OF_ROW_QUIZ ->
                    "${uiState.rowName} • Kiểm tra (${uiState.currentQuizIndex + 1}/${uiState.quizQuestions.size})"
                else ->
                    "${uiState.rowName} • Chữ ${uiState.currentCharIndex + 1}/${uiState.rowCharacters.size}"
            }
            Text(
                text = progressTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )

            if (uiState.stepType != LessonStepType.END_OF_ROW_QUIZ) {
                Text(
                    text = "Vượt bài",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.onBackground)
                        .clickable(onClick = viewModel::jumpToRowTestOut)
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                )
            } else {
                Spacer(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        // 2. Khung Đề bài gọn gàng
        PromptHeaderSection(
            uiState = uiState,
            onSpeak = viewModel::speakCurrentPrompt,
            onSpeakSentence = viewModel::speakCurrentExampleSentence,
            onSelectWordSlot = viewModel::selectWordSlot,
            onRevealRomajiFallback = viewModel::revealListenRomajiFallback
        )

        // 3. Thông báo phản hồi (nếu có)
        uiState.feedbackMessage?.let { msg ->
            val col = if (uiState.isFeedbackError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onBackground
            }
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

        // 4. Ô vuông 1:1 trung tâm
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
                    successOverlayText = uiState.successOverlayText,
                    enabled = uiState.successOverlayText == null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 5. Cụm nút điều khiển dưới cùng — ĐƠN GIẢN, RÕ RÀNG
        when {
            uiState.showingCorrectionAnimation -> {
                ActionButton(
                    text = "Đã xem nét đúng • Viết lại →",
                    filled = true,
                    onClick = viewModel::dismissCorrectionAndStartRewrite,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            uiState.stepType == LessonStepType.STEP_1_OBSERVE -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionButton(
                        text = "Đã biết • Kiểm tra luôn",
                        filled = false,
                        onClick = viewModel::jumpToCharacterFastTest,
                        modifier = Modifier.weight(1f)
                    )
                    ActionButton(
                        text = "Bắt đầu tập viết →",
                        filled = true,
                        onClick = viewModel::advanceFromObserveToTrace,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            else -> {
                // Nếu đang ở Bước 2 hoặc 3, vẫn có nút nhỏ gọn để nhảy qua Kiểm tra luôn nếu đã thuộc
                if (uiState.stepType == LessonStepType.STEP_2_TRACE ||
                    uiState.stepType == LessonStepType.STEP_3_GUIDED
                ) {
                    Text(
                        text = "Đã thuộc chữ này? Bấm để kiểm tra 1 lần rồi qua chữ mới →",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable(onClick = viewModel::jumpToCharacterFastTest)
                            .padding(vertical = 2.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionButton(
                        text = "Hoàn tác",
                        filled = false,
                        onClick = viewModel::undoLastStroke,
                        modifier = Modifier.weight(0.85f)
                    )
                    ActionButton(
                        text = "Xoá",
                        filled = false,
                        onClick = viewModel::clearCanvas,
                        modifier = Modifier.weight(0.65f)
                    )
                    ActionButton(
                        text = "Kiểm tra →",
                        filled = true,
                        onClick = {
                            if (uiState.successOverlayText == null) {
                                val passed = viewModel.checkCurrentSubmission()
                                if (passed) {
                                    localView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                } else {
                                    localView.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                    vibrateOnError(context)
                                }
                            }
                        },
                        modifier = Modifier.weight(1.2f)
                    )
                }
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
    onRevealRomajiFallback: () -> Unit
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
                            val strokeLabel = if (uiState.stepType == LessonStepType.STEP_1_OBSERVE) {
                                "${ch.strokeCount} nét"
                            } else {
                                "Đã vẽ: ${uiState.singleCharStrokes.size}/${ch.strokeCount} nét"
                            }
                            Text(
                                text = "${uiState.stepType.titleVi} • Âm: ${ch.romaji} ($strokeLabel)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = uiState.stepType.instructionVi,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
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
                        val headerText = when {
                            uiState.showingCorrectionAnimation -> "Đáp án chuẩn: ${ch.char} (${ch.romaji})"
                            uiState.isCharQuickCheckMode -> "Kiểm tra nhanh chữ \"${ch.romaji}\""
                            else -> "Bước 4: Tự viết chữ có âm \"${ch.romaji}\""
                        }
                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Đã vẽ: ${uiState.singleCharStrokes.size}/${ch.strokeCount} nét",
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
                            text = "Từ vựng: ${word.meaningVi} (${word.romaji})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (word.exampleSentence.isNotBlank()) {
                            Text(
                                text = "Câu: ${word.exampleSentence} (${word.exampleVi})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable(onClick = onSpeakSentence)
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (uiState.showingCorrectionAnimation) {
                                        "Đáp án: ${t.char} (${t.romaji})"
                                    } else {
                                        "Hãy viết chữ Kana có âm: \"${t.romaji}\""
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Đã vẽ: ${uiState.singleCharStrokes.size}/${t.strokeCount} nét",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                val promptText = when {
                                    uiState.showingCorrectionAnimation -> "Đáp án: ${t.char} (${t.romaji})"
                                    uiState.showFallbackRomajiInListenMode -> "Viết chữ Kana (Âm: ${t.romaji})"
                                    else -> "Nghe phát âm & viết chữ Kana (${t.strokeCount} nét)"
                                }
                                Text(
                                    text = promptText,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (!uiState.showFallbackRomajiInListenMode && !uiState.showingCorrectionAnimation) {
                                    Text(
                                        text = "Không nghe được? Bấm hiện phiên âm",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.clickable(onClick = onRevealRomajiFallback)
                                    )
                                }
                            }
                            SpeakButton(onClick = onSpeak)
                        }
                    }

                    QuizQuestionType.MEANING_ROMAJI_TO_WORD,
                    QuizQuestionType.LISTEN_TO_WORD -> {
                        val w = q.targetWord ?: return@Column
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${w.meaningVi} • ${w.romaji}",
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
                }
            }
        }
    }
}

@Composable
private fun SpeakButton(onClick: () -> Unit) {
    Text(
        text = "Nghe",
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
            .padding(vertical = 12.dp, horizontal = 8.dp)
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

@Composable
private fun LessonCompletedContent(
    rowName: String,
    learnedChars: List<String>,
    passedFirstAttempt: Int,
    totalQuizQuestions: Int,
    isTestOutMode: Boolean = false,
    onContinueNextRow: () -> Unit,
    onBackHome: () -> Unit,
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
                text = if (isTestOutMode) "VƯỢT BÀI THÀNH CÔNG" else "HOÀN THÀNH HÀNG $rowName",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
            Text(
                text = learnedChars.joinToString("   "),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Đã lưu tiến độ ($passedFirstAttempt/$totalQuizQuestions) và mở khoá hàng tiếp theo!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            ActionButton(
                text = "Học tiếp hàng mới →",
                filled = true,
                onClick = onContinueNextRow,
                modifier = Modifier.fillMaxWidth()
            )
            ActionButton(
                text = "Về Trang chủ",
                filled = false,
                onClick = onBackHome,
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
                text = "Hàng $rowName: Đúng $passedFirstAttempt / $totalQuizQuestions chữ ngay lần đầu",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            ActionButton(
                text = "Thử kiểm tra lại",
                filled = true,
                onClick = onRetryTestOut,
                modifier = Modifier.fillMaxWidth()
            )
            ActionButton(
                text = "Học từng chữ từ đầu",
                filled = false,
                onClick = onLearnStepByStep,
                modifier = Modifier.fillMaxWidth()
            )
            ActionButton(
                text = "Đóng",
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
