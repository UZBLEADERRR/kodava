package uz.kodava.studio.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Kodava rang palitrasi — kinoteatr qorong'uligi va neon urg'ular. */
object Kodava {
    val Ink = Color(0xFF0A0912)
    val Surface = Color(0xFF15131F)
    val SurfaceHigh = Color(0xFF1F1B2E)
    val Line = Color(0x1FFFFFFF)
    val Violet = Color(0xFF8B5CFF)
    val Magenta = Color(0xFFFF4FD8)
    val Mint = Color(0xFF5BE7C4)
    val Amber = Color(0xFFFFC46B)
    val TextHigh = Color(0xFFF2EEFF)
    val TextMid = Color(0xFFA79FC4)
    val Danger = Color(0xFFFF7B7B)

    val accent: Brush get() = Brush.horizontalGradient(listOf(Violet, Magenta))
    val glow: Brush get() = Brush.verticalGradient(listOf(Color(0x338B5CFF), Color(0x00000000)))
    val scrim: Brush
        get() = Brush.verticalGradient(listOf(Color(0x00000000), Color(0x99000000), Color(0xE6000000)))
}

private val Scheme = darkColorScheme(
    primary = Kodava.Violet,
    onPrimary = Color.White,
    primaryContainer = Kodava.SurfaceHigh,
    onPrimaryContainer = Kodava.TextHigh,
    secondary = Kodava.Mint,
    onSecondary = Kodava.Ink,
    tertiary = Kodava.Magenta,
    background = Kodava.Ink,
    onBackground = Kodava.TextHigh,
    surface = Kodava.Surface,
    onSurface = Kodava.TextHigh,
    surfaceVariant = Kodava.SurfaceHigh,
    onSurfaceVariant = Kodava.TextMid,
    outline = Color(0xFF3A3350),
    error = Kodava.Danger,
    onError = Color.White
)

private val KodavaTypography = Typography(
    displaySmall = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
    bodyMedium = TextStyle(fontSize = 14.5.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
)

private val KodavaShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun KodavaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = KodavaTypography,
        shapes = KodavaShapes,
        content = content
    )
}
