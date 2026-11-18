package vn.kana.app.ui.review

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
import vn.kana.app.R
import vn.kana.app.domain.QuizQuestionType
import vn.kana.app.ui.components.AnimatedMemeFeedbackBanner
import vn.kana.app.ui.components.GenkoYoshiWordBar
import vn.kana.app.ui.components.KanaDrawingCanvas
import vn.kana.app.ui.components.KanaStrokeAnimatorCanvas
import vn.kana.app.ui.components.MemeBadge

@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel,
    onExitReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val localView = LocalView.current
    val context = LocalContext.current

    if (uiState.isSessionCompleted) {
        ReviewCompletedContent(
            totalReviewed = uiState.questions.size,
            firstAttemptCorrect = uiState.firstAttemptCorrectCount,
            averageScore = uiState.averageScore,
            onReviewMore = viewModel::buildNewReviewSession,
            onBackHome = onExitReview,
            modifier = modifier
        )
        return
    }

    val correctionStrokes = remember(uiState.correctionTemplate?.char) {
        uiState.correctionTemplate?.toTypedStrokes().orEmpty()
    }
    var animStrokeIdx by remember { mutableIntStateOf(0) }
    var animStrokeProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(uiState.showingCorrectionAnimation, uiState.correctionTemplate?.char) {
        if (!uiState.showingCorrectionAnimation || correctionStrokes.isEmpty()) return@LaunchedEffect
        val speed = uiState.settings.animationSpeed.coerceIn(0.5f, 2.0f)
        while (uiState.showingCorrectionAnimation) {
            for (sIdx in correctionStrokes.indices) {
                animStrokeIdx = sIdx
                animStrokeProgress = 0f
                val dur = (660f / speed).toInt().coerceAtLeast(130)
                animate(0f, 1f, animationSpec = tween(dur, easing = LinearEasing)) { v, _ ->
                    animStrokeProgress = v
                }
                delay((180f / speed).toLong().coerceAtLeast(60L))
            }
            animStrokeIdx = correctionStrokes.size
            animStrokeProgress = 1f
            delay((750f / speed).toLong().coerceAtLeast(250L))
        }
    }

    // Giao diện Ôn tập cố định trên 1 màn hình (không dùng verticalScroll) để vẽ mượt tuyệt đối
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "← Trang chủ",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .clickable(onClick = onExitReview)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
            Text(
                text = "ÔN TẬP (${uiState.currentIndex + 1}/${uiState.questions.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .clickable(onClick = viewModel::toggleSessionFilter)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                MemeBadge(
                    resId = if (uiState.sessionFilter == ReviewSessionFilter.MIXED) {
                        R.drawable.meme_cat_blep
                    } else {
                        R.drawable.meme_shiba
                    },
                    size = 20.dp
                )
                Text(
                    text = uiState.sessionFilter.labelVi,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        // Khung đề bài
        val q = uiState.currentQuestion
        if (q != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val scriptBadge = when (q.targetTemplate?.script ?: q.targetWord?.script) {
                            "katakana" -> "Katakana"
                            "kanji" -> "Kanji N5"
                            else -> "Hiragana"
                        }
                        Text(
                            text = "$scriptBadge • ${uiState.currentDrillStyle.labelVi}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val title = when (uiState.currentDrillStyle) {
                            ReviewDrillStyle.PICK_CHAR -> {
                                q.targetTemplate?.let { "Chọn đúng chữ cho âm: \"${it.romaji}\"" }
                                    ?: q.targetWord?.let { "Chọn từ có nghĩa: \"${it.meaningVi}\"" }
                                    ?: ""
                            }

                            ReviewDrillStyle.PICK_READING -> {
                                q.targetTemplate?.let { "Chữ 「${it.char}」 đọc là gì?" }
                                    ?: q.targetWord?.let { "Từ 「${it.word}」 có nghĩa là gì?" }
                                    ?: ""
                            }

                            ReviewDrillStyle.WRITE -> when (q.type) {
                                QuizQuestionType.ROMAJI_TO_KANA -> {
                                    val t = q.targetTemplate
                                    if (uiState.showingCorrectionAnimation) {
                                        "Đáp án: ${t?.char} (${t?.romaji})"
                                    } else {
                                        "Viết chữ Kana có âm: \"${t?.romaji}\""
                                    }
                                }

                                QuizQuestionType.LISTEN_TO_KANA -> {
                                    val t = q.targetTemplate
                                    when {
                                        uiState.showingCorrectionAnimation -> "Đáp án: ${t?.char} (${t?.romaji})"
                                        uiState.showFallbackRomajiInListenMode -> "Nghe & viết chữ Kana (${t?.romaji})"
                                        else -> "Nghe phát âm & viết chữ Kana"
                                    }
                                }

                                QuizQuestionType.MEANING_ROMAJI_TO_WORD -> {
                                    val w = q.targetWord
                                    "${w?.meaningVi} • Romaji: ${w?.romaji}"
                                }

                                QuizQuestionType.LISTEN_TO_WORD -> {
                                    val w = q.targetWord
                                    when {
                                        uiState.showingCorrectionAnimation -> "Đáp án: ${w?.word} (${w?.romaji})"
                                        uiState.showFallbackRomajiInListenMode -> "${w?.meaningVi} (${w?.romaji})"
                                        else -> "Nghe phát âm & viết từ: \"${w?.meaningVi}\""
                                    }
                                }
                            }
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        if ((q.type == QuizQuestionType.LISTEN_TO_KANA || q.type == QuizQuestionType.LISTEN_TO_WORD) &&
                            !uiState.showFallbackRomajiInListenMode &&
                            !uiState.showingCorrectionAnimation
                        ) {
                            Text(
                                text = "Không nghe được? Bấm để hiện Romaji",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable(onClick = viewModel::revealRomajiFallback)
                            )
                        }
                    }

                    Text(
                        text = "Phát âm",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(onClick = viewModel::speakCurrentPrompt)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                if (uiState.isMultiBoxMode && uiState.currentDrillStyle == ReviewDrillStyle.WRITE) {
                    GenkoYoshiWordBar(
                        slots = uiState.wordSlots,
                        activeSlotIndex = uiState.activeSlotIndex,
                        onSelectSlot = viewModel::selectWordSlot
                    )
                }
            }
        }

        // Phản hồi meme khen / báo sai sống động
        uiState.feedbackMessage?.let { msg ->
            AnimatedMemeFeedbackBanner(
                message = msg,
                isError = uiState.isFeedbackError,
                seed = uiState.currentIndex + uiState.shakeTrigger * 17
            )
        }

        if (uiState.currentDrillStyle != ReviewDrillStyle.WRITE) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                uiState.currentChoiceOptions.forEachIndexed { idx, optText ->
                    val chosen = uiState.selectedChoiceIndex
                    val isChosen = chosen == idx
                    val isCorrect = idx == uiState.currentCorrectChoiceIndex
                    val borderColor = when {
                        chosen == null -> MaterialTheme.colorScheme.outline
                        isCorrect -> MaterialTheme.colorScheme.primary
                        isChosen -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outline
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (chosen != null && (isCorrect || isChosen)) 2.dp else 1.dp,
                                color = borderColor
                            )
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(enabled = chosen == null) {
                                viewModel.submitChoiceAnswer(idx)
                                if (isCorrect) {
                                    localView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                } else {
                                    localView.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                    vibrateReviewError(context)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${('A' + idx)}. $optText",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (chosen != null && isCorrect) {
                            Text("✓", color = MaterialTheme.colorScheme.primary)
                        } else if (chosen != null && isChosen) {
                            Text("✗", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (uiState.selectedChoiceIndex != null && uiState.selectedChoiceIndex != uiState.currentCorrectChoiceIndex) {
                ReviewActionButton(
                    text = "Tiếp theo →",
                    filled = true,
                    onClick = viewModel::continueAfterWrongChoice,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // Vùng Canvas 1:1 trung tâm
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.showingCorrectionAnimation) {
                    KanaStrokeAnimatorCanvas(
                        strokes = correctionStrokes,
                        activeStrokeIndex = animStrokeIdx,
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

                    KanaDrawingCanvas(
                        userStrokes = activeStrokes,
                        onStrokeCompleted = viewModel::onStrokeCompleted,
                        onStrokeActivity = viewModel::onUserCanvasActivity,
                        showGhostTemplate = false,
                        showTemplateComparison = false,
                        nextHintStrokeIndex = null,
                        shakeTrigger = uiState.shakeTrigger,
                        successOverlayText = uiState.successOverlayText,
                        enabled = uiState.successOverlayText == null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Cụm nút hành động dưới cùng
            if (uiState.showingCorrectionAnimation) {
                ReviewActionButton(
                    text = "Đã xem đáp án • Viết lại 1 lần",
                    filled = true,
                    onClick = viewModel::dismissCorrectionAndStartRewrite,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReviewActionButton(
                        text = "Hoàn tác",
                        filled = false,
                        onClick = viewModel::undoLastStroke,
                        modifier = Modifier.weight(0.9f)
                    )
                    ReviewActionButton(
                        text = "Xoá",
                        filled = false,
                        onClick = viewModel::clearCanvas,
                        modifier = Modifier.weight(0.6f)
                    )
                    ReviewActionButton(
                        text = "Bỏ qua",
                        filled = false,
                        onClick = viewModel::skipCurrentQuestion,
                        modifier = Modifier.weight(0.75f)
                    )
                    ReviewActionButton(
                        text = "Kiểm tra",
                        filled = true,
                        onClick = {
                            if (uiState.successOverlayText == null) {
                                val passed = viewModel.checkCurrentSubmission()
                                if (passed) {
                                    localView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                } else {
                                    localView.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                    vibrateReviewError(context)
                                }
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
private fun ReviewCompletedContent(
    totalReviewed: Int,
    firstAttemptCorrect: Int,
    averageScore: Int,
    onReviewMore: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "HOÀN THÀNH PHIÊN ÔN TẬP",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
            Text(
                text = "Đúng ngay lần đầu: $firstAttemptCorrect / $totalReviewed thẻ",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Điểm nét trung bình: $averageScore / 100",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Lịch ôn tập SM-2 của từng chữ và từ đã được cập nhật vào cơ sở dữ liệu offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReviewActionButton(
                    text = "Ôn tập tiếp",
                    filled = false,
                    onClick = onReviewMore,
                    modifier = Modifier.weight(1f)
                )
                ReviewActionButton(
                    text = "Về Trang chủ",
                    filled = true,
                    onClick = onBackHome,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ReviewActionButton(
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

private fun vibrateReviewError(context: Context) {
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
