package vn.kana.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = KanaPrimaryVintage,
    onPrimary = KanaPaperLight,
    primaryContainer = KanaGraySubtle,
    onPrimaryContainer = KanaBlack,
    secondary = KanaGrayDark,
    onSecondary = KanaPaperLight,
    secondaryContainer = KanaGraySubtle,
    onSecondaryContainer = KanaBlack,
    background = KanaWhite,
    onBackground = KanaBlack,
    surface = KanaPaperLight,
    onSurface = KanaBlack,
    surfaceVariant = KanaGraySubtle,
    onSurfaceVariant = KanaGrayDark,
    outline = KanaGrayLight,
    outlineVariant = KanaGrayMedium,
    error = KanaErrorRedLight,
    onError = KanaPaperLight
)

private val DarkColors = darkColorScheme(
    primary = KanaDarkPrimary,
    onPrimary = KanaDarkBg,
    primaryContainer = KanaDarkSurfaceVariant,
    onPrimaryContainer = KanaDarkInk,
    secondary = KanaDarkGrayMedium,
    onSecondary = KanaDarkBg,
    secondaryContainer = KanaDarkSurfaceVariant,
    onSecondaryContainer = KanaDarkInk,
    background = KanaDarkBg,
    onBackground = KanaDarkInk,
    surface = KanaDarkSurface,
    onSurface = KanaDarkInk,
    surfaceVariant = KanaDarkSurfaceVariant,
    onSurfaceVariant = KanaDarkGrayMedium,
    outline = KanaDarkBorder,
    outlineVariant = KanaDarkGrayMedium,
    error = KanaErrorRedDark,
    onError = KanaDarkBg
)

val KanaShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(4.dp)
)

/**
 * Theme Sổ tay Retro / Vintage của ứng dụng Kana:
 * - Luôn tắt dynamicColor (dynamicColor = false).
 * - Giấy Washi kem ấm / chữ mực nâu sepia (hoặc chế độ đèn bàn ban đêm).
 * - Màu đỏ son con dấu Hanko cho trạng thái lỗi (error).
 */
@Composable
fun KanaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KanaTypography,
        shapes = KanaShapes,
        content = content
    )
}
