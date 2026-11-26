package com.example.mcmovilcare.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF081421),
    primaryContainer = Color(0xFF0F2942),
    onPrimaryContainer = CyberCyan,
    secondary = MotokoViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF282654),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = MotokoMagenta,
    onTertiary = Color.White,
    background = Section9Bg,
    onBackground = TextPrimaryDark,
    surface = Section9Surface,
    onSurface = TextPrimaryDark,
    surfaceVariant = Section9SurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = Section9Border
)

private val LightColorScheme = lightColorScheme(
    primary = KanedaRed,
    onPrimary = Color.White,
    primaryContainer = KanedaRedContainer,
    onPrimaryContainer = Color(0xFF410004),
    secondary = CapsuleBlue,
    onSecondary = Color.White,
    secondaryContainer = CapsuleBlueContainer,
    onSecondaryContainer = Color(0xFF001E2F),
    tertiary = AkiraAsphalt,
    onTertiary = Color.White,
    background = AkiraBg,
    onBackground = TextPrimaryLight,
    surface = AkiraSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = AkiraSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = AkiraBorder
)

private val GhostShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

private val AkiraShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp)
)

@Composable
fun MCMovilCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val typography = if (darkTheme) GhostTypography else AkiraTypography
    val shapes = if (darkTheme) GhostShapes else AkiraShapes

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes,
        content = content
    )
}