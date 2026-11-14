package ao.mirevia.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Mint = Color(0xFFA5EFCA)
val Ink = Color(0xFF0B0E13)
val Muted = Color(0xFF9AA5AD)
val LocalReduceMotion = staticCompositionLocalOf { false }
val LocalNetworkAvailable = staticCompositionLocalOf { true }

private val Colors =
    darkColorScheme(
        primary = Mint,
        onPrimary = Color(0xFF10251D),
        primaryContainer = Color(0xFF213A30),
        onPrimaryContainer = Color(0xFFCBF8E0),
        secondary = Color(0xFFADC8F3),
        onSecondary = Color(0xFF13283E),
        tertiary = Color(0xFFDFC58F),
        background = Ink,
        onBackground = Color(0xFFF2F4F5),
        surface = Color(0xFF13191F),
        onSurface = Color(0xFFF2F4F5),
        surfaceVariant = Color(0xFF222B32),
        onSurfaceVariant = Color(0xFFADB7BF),
        outline = Color(0xFF56626C),
        outlineVariant = Color(0xFF2A343D),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
    )
private val Type =
    Typography(
        displaySmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp,
                lineHeight = 43.sp,
                letterSpacing = (-1).sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
                lineHeight = 32.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 21.sp,
                lineHeight = 27.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
        bodyLarge =
            TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 25.sp),
        bodyMedium =
            TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 22.sp),
        labelSmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
            ),
    )

@Composable
fun MireviaTheme(
    reduceMotion: Boolean = false,
    networkAvailable: Boolean = true,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalReduceMotion provides reduceMotion,
        LocalNetworkAvailable provides networkAvailable,
    ) {
        MaterialTheme(colorScheme = Colors, typography = Type, content = content)
    }
}
