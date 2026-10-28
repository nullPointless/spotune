package dev.nullpointless.canvasmusic.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nullpointless.canvasmusic.R

// Spotify-inspired palette
val Bg = Color(0xFF121212)
val CardBg = Color(0xFF242424)
val TileBg = Color(0xFF2A2A2A)
val ElevatedBg = Color(0xFF404040)
val Green = Color(0xFF1DB954)
val GreenBright = Color(0xFF1ED760)
val White = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB3B3B3)
val TextTertiary = Color(0xFF7A7A7A)
val Inactive = Color(0xFF616161)
val Divider = Color(0xFF323232)
val ErrorRed = Color(0xFFF15E6C)

@OptIn(ExperimentalTextApi::class)
val Montserrat = FontFamily(
    Font(
        R.font.montserrat_vf,
        weight = FontWeight(400),
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        R.font.montserrat_vf,
        weight = FontWeight(500),
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        R.font.montserrat_vf,
        weight = FontWeight(600),
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        R.font.montserrat_vf,
        weight = FontWeight(700),
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
    Font(
        R.font.montserrat_vf,
        weight = FontWeight(800),
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    )
)

val CmTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight(800),
        fontSize = 26.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight(700),
        fontSize = 22.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight(700),
        fontSize = 17.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight(500),
        fontSize = 16.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight(400),
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight(400),
        fontSize = 13.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight(700),
        fontSize = 13.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight(600),
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight(500),
        fontSize = 11.sp
    )
)

val CmShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

private val CmColors = darkColorScheme(
    primary = Green,
    onPrimary = Color(0xFF000000),
    secondary = GreenBright,
    onSecondary = Color(0xFF000000),
    background = Bg,
    onBackground = White,
    surface = Bg,
    onSurface = White,
    surfaceVariant = CardBg,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed
)

@Composable
fun CmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CmColors,
        typography = CmTypography,
        shapes = CmShapes,
        content = content
    )
}
