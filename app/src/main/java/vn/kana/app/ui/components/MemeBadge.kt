package vn.kana.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
        MemeReaction(R.drawable.meme_cat_surprised, "HẢ?! VIẾT GÌ LẠ VẬY BRO?!"),
        MemeReaction(R.drawable.meme_cat_grumpy, "CỐ TỎ RA ỔN NHƯNG SAI RỒI..."),
        MemeReaction(R.drawable.meme_cat_angry, "NÓNG MÁU À NHA! NHÌN KỸ LẠI NÈ!"),
        MemeReaction(R.drawable.meme_cat_reading, "ĐEO KÍNH VÀO SOI LẠI TỪNG NÉT NÈ!"),
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

@Composable
fun AnimatedMemeFeedbackBanner(
    message: String,
    isError: Boolean,
    seed: Int = message.hashCode(),
    modifier: Modifier = Modifier
) {
    val reaction = remember(seed, isError) {
        if (isError) MemeReactionCatalog.pickError(seed) else MemeReactionCatalog.pickPraise(seed)
    }
    val scaleAnim = remember(seed, isError) { Animatable(0.75f) }
    val tiltAnim = remember(seed, isError) { Animatable(0f) }

    LaunchedEffect(seed, isError) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }
    LaunchedEffect(seed, isError) {
        val angles = if (isError) {
            listOf(-14f, 14f, -10f, 10f, -5f, 5f, 0f)
        } else {
            listOf(-10f, 10f, -6f, 6f, 0f)
        }
        for (a in angles) {
            tiltAnim.animateTo(a, animationSpec = tween(durationMillis = 55))
        }
    }

    val accentColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scaleAnim.value)
            .border(2.dp, accentColor)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = reaction.resId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(46.dp)
                .graphicsLayer { rotationZ = tiltAnim.value }
                .clip(CircleShape)
                .border(2.dp, accentColor, CircleShape)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = reaction.headlineVi,
                style = MaterialTheme.typography.labelLarge,
                color = accentColor
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
