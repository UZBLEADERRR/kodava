package uz.kodava.studio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Purple = Color(0xFFC6A0FF)
private val Mint = Color(0xFF7AE7C7)
private val Ink = Color(0xFF0E0B16)
private val Surface1 = Color(0xFF181327)
private val Surface2 = Color(0xFF221B38)

private val DarkScheme = darkColorScheme(
    primary = Purple,
    onPrimary = Ink,
    primaryContainer = Surface2,
    onPrimaryContainer = Color.White,
    secondary = Mint,
    onSecondary = Ink,
    background = Ink,
    onBackground = Color(0xFFEDE9F7),
    surface = Surface1,
    onSurface = Color(0xFFEDE9F7),
    surfaceVariant = Surface2,
    onSurfaceVariant = Color(0xFFBFB6D6),
    outline = Color(0xFF463A66),
    error = Color(0xFFFF8A80)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF6B3FCE),
    secondary = Color(0xFF00876A),
    background = Color(0xFFF7F5FC),
    surface = Color.White
)

private val KodavaTypography = Typography(
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
)

@Composable
fun KodavaTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = KodavaTypography,
        content = content
    )
}
