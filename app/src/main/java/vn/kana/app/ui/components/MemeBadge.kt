package vn.kana.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import vn.kana.app.R

data class MemeReaction(
    @DrawableRes val resId: Int,
    val headlineVi: String
)

object MemeReactionCatalog {

    val praiseReactions: List<MemeReaction> = listOf(
        MemeReaction(R.drawable.meme_hamster, "ĐỈNH NÓC KỊCH TRẦN!"),
        MemeReaction(R.drawable.meme_duck, "10 ĐIỂM KHÔNG CÓ NHƯNG!"),
        MemeReaction(R.drawable.meme_shiba, "SENSEI GẬT ĐẦU HÀI LÒNG!"),
        MemeReaction(R.drawable.meme_capybara, "MƯỢT NHƯ SUNSILK LUÔN!"),
        MemeReaction(R.drawable.meme_red_panda, "NGẦU ĐÉT LUÔN BRO!"),
        MemeReaction(R.drawable.meme_cat_blep, "DỄ ẸC ẤY MÀ HEHE!"),
        MemeReaction(R.drawable.meme_hamster, "CHUẨN KHÔNG CẦN CHỈNH!"),
        MemeReaction(R.drawable.meme_duck, "UY TÍN LUÔN BẠN ƠI!"),
        MemeReaction(R.drawable.meme_shiba, "NÉT CHỮ NẾT NGƯỜI ĐÂY RỒI!"),
        MemeReaction(R.drawable.meme_capybara, "QUÁ ĐẲNG CẤP LUÔN!")
    )

    val errorReactions: List<MemeReaction> = listOf(
        MemeReaction(R.drawable.meme_cat_surprised, "HẢ?! GÌ LẠ VẬY BRO?!"),
        MemeReaction(R.drawable.meme_cat_grumpy, "CỐ TỎ RA ỔN NHƯNG SAI RỒI..."),
        MemeReaction(R.drawable.meme_cat_angry, "NÓNG MÁU À NHA! NHÌN KỸ LẠI NÈ!"),
        MemeReaction(R.drawable.meme_cat_reading, "ĐEO KÍNH VÀO SOI LẠI NÈ!"),
        MemeReaction(R.drawable.meme_cat_surprised, "ỦA ALO?! LỘN TIỆM RỒI BẠN ƠI!"),
        MemeReaction(R.drawable.meme_cat_grumpy, "KHÓC MỘT DÒNG SÔNG, THỬ LẠI NHA!"),
        MemeReaction(R.drawable.meme_cat_reading, "SENSEI SOI THẤY SAI RỒI ĐÓ NHA!"),
        MemeReaction(R.drawable.meme_cat_angry, "BÌNH TĨNH LÀM LẠI PHÁT NỮA XEM NÀO!")
    )

    fun pickPraise(seed: Int): MemeReaction {
        val idx = (seed and 0x7fffffff) % praiseReactions.size
        return praiseReactions[idx]
    }

    fun pickError(seed: Int): MemeReaction {
        val idx = (seed and 0x7fffffff) % errorReactions.size
        return errorReactions[idx]
    }
}

@Composable
fun MemeBadge(
    @DrawableRes resId: Int,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
    )
}

/**
 * Thẻ Pop-up Meme Đúng/Sai chuẩn hoá đồng bộ cho TOÀN BỘ loại câu hỏi (Viết nét & Chọn đáp án).
 */
@Composable
fun MemeReactionPopupBox(
    message: String,
    isError: Boolean,
    seed: Int = message.hashCode(),
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val reaction = remember(seed, isError) {
        if (isError) MemeReactionCatalog.pickError(seed) else MemeReactionCatalog.pickPraise(seed)
    }
    val scaleAnim = remember(seed, isError) { Animatable(0.93f) }

    LaunchedEffect(seed, isError) {
        scaleAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 140)
        )
    }

    val containerBg = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val contentCol = if (isError) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .scale(scaleAnim.value)
            .background(containerBg)
            .border(2.dp, MaterialTheme.colorScheme.onBackground)
            .padding(horizontal = 22.dp, vertical = 16.dp)
    ) {
        Image(
            painter = painterResource(id = reaction.resId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .border(2.dp, contentCol, CircleShape)
        )
        Text(
            text = reaction.headlineVi,
            style = MaterialTheme.typography.titleLarge,
            color = contentCol,
            textAlign = TextAlign.Center
        )
        if (message.isNotBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentCol,
                textAlign = TextAlign.Center
            )
        }
        if (actionLabel != null && onActionClick != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .clickable { onActionClick() }
                    .padding(vertical = 8.dp, horizontal = 12.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

/**
 * Pop-up Meme nổi chính giữa màn hình cho mọi câu hỏi trắc nghiệm / chọn đáp án
 * (đồng bộ 100% giao diện với Pop-up trên ô viết nét).
 */
@Composable
fun AnimatedMemeFeedbackBanner(
    message: String,
    isError: Boolean,
    seed: Int = message.hashCode(),
    onContinueAction: (() -> Unit)? = null,
    autoDismissErrorMs: Long? = null,
    autoDismissSuccessMs: Long = 1300L,
    modifier: Modifier = Modifier
) {
    var dismissed by remember(seed, isError, message) { mutableStateOf(false) }

    if (autoDismissErrorMs != null && isError) {
        LaunchedEffect(seed, isError, message) {
            delay(autoDismissErrorMs)
            dismissed = true
        }
    } else if (!isError && onContinueAction == null) {
        LaunchedEffect(seed, isError, message) {
            delay(autoDismissSuccessMs)
            dismissed = true
        }
    }

    if (dismissed) return

    Dialog(
        onDismissRequest = {
            if (onContinueAction != null) {
                onContinueAction()
            } else {
                dismissed = true
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .clickable {
                    if (onContinueAction != null) {
                        onContinueAction()
                    } else {
                        dismissed = true
                    }
                }
        ) {
            MemeReactionPopupBox(
                message = message,
                isError = isError,
                seed = seed,
                actionLabel = if (isError) {
                    if (onContinueAction != null) "Tiếp theo →" else "Đã hiểu"
                } else null,
                onActionClick = {
                    if (onContinueAction != null) {
                        onContinueAction()
                    } else {
                        dismissed = true
                    }
                }
            )
        }
    }
}
