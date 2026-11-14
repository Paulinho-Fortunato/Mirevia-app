package ao.mirevia.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Accent = Color(0xFFFF7185)
val Ink = Color(0xFF09090D)
val Muted = Color(0xFFAAA7B2)
val Gold = Color(0xFFEBCB91)
val LocalReduceMotion = staticCompositionLocalOf { false }
val LocalNetworkAvailable = staticCompositionLocalOf { true }

private val Colors =
    darkColorScheme(
        primary = Color(0xFFF5F2EE),
        onPrimary = Color(0xFF161318),
        primaryContainer = Color(0xFF3C202B),
        onPrimaryContainer = Color(0xFFFFD9E0),
        secondary = Accent,
        onSecondary = Color(0xFF350C1A),
        secondaryContainer = Color(0xFF30202A),
        onSecondaryContainer = Color(0xFFFFD9E0),
        tertiary = Gold,
        background = Ink,
        onBackground = Color(0xFFF5F2EE),
        surface = Color(0xFF141419),
        onSurface = Color(0xFFF5F2EE),
        surfaceVariant = Color(0xFF24232B),
        onSurfaceVariant = Color(0xFFBCB8C4),
        outline = Color(0xFF625D6B),
        outlineVariant = Color(0xFF302D38),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
    )
private val Type =
    Typography(
        displaySmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 40.sp,
                lineHeight = 45.sp,
                letterSpacing = (-1.4).sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 37.sp,
                letterSpacing = (-0.8).sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 27.sp,
                lineHeight = 33.sp,
                letterSpacing = (-0.6).sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = (-0.4).sp,
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
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.7.sp,
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
        MaterialTheme(
            colorScheme = Colors,
            typography = Type,
            shapes =
                Shapes(
                    extraSmall = RoundedCornerShape(6.dp),
                    small = RoundedCornerShape(10.dp),
                    medium = RoundedCornerShape(16.dp),
                    large = RoundedCornerShape(24.dp),
                    extraLarge = RoundedCornerShape(30.dp),
                ),
            content = content,
        )
    }
}
