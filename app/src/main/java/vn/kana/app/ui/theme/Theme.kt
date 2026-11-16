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
    primary = KanaBlack,
    onPrimary = KanaWhite,
    primaryContainer = KanaGraySubtle,
    onPrimaryContainer = KanaBlack,
    secondary = KanaGrayDark,
    onSecondary = KanaWhite,
    secondaryContainer = KanaGraySubtle,
    onSecondaryContainer = KanaBlack,
    background = KanaWhite,
    onBackground = KanaBlack,
    surface = KanaWhite,
    onSurface = KanaBlack,
    surfaceVariant = KanaGraySubtle,
    onSurfaceVariant = KanaGrayDark,
    outline = KanaGrayLight,
    outlineVariant = KanaGrayMedium,
    error = KanaErrorRedLight,
    onError = KanaWhite
)

private val DarkColors = darkColorScheme(
    primary = KanaDarkInk,
    onPrimary = KanaDarkBg,
    primaryContainer = KanaDarkSurface,
    onPrimaryContainer = KanaDarkInk,
    secondary = KanaDarkGrayMedium,
    onSecondary = KanaDarkBg,
    secondaryContainer = KanaDarkSurface,
    onSecondaryContainer = KanaDarkInk,
    background = KanaDarkBg,
    onBackground = KanaDarkInk,
    surface = KanaDarkBg,
    onSurface = KanaDarkInk,
    surfaceVariant = KanaDarkSurface,
    onSurfaceVariant = KanaDarkGrayMedium,
    outline = KanaDarkBorder,
    outlineVariant = KanaDarkGrayMedium,
    error = KanaErrorRedDark,
    onError = KanaDarkBg
)

val KanaShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(2.dp),
    extraLarge = RoundedCornerShape(2.dp)
)

/**
 * Theme tối giản của ứng dụng Kana:
 * - Luôn tắt dynamicColor (dynamicColor = false).
 * - Nền trắng / chữ đen (hoặc dark mode theo hệ thống).
 * - Chỉ có 1 màu nhấn duy nhất cho trạng thái lỗi (error).
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
