package com.example.mcmovilcare.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF2A6D),         // Fucsia / rojo Dandadan
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF14F1C7),       // Cian neón
    onSecondary = Color(0xFF061A18),
    tertiary = Color(0xFFFFD166),        // Amarillo acento
    background = Color(0xFF120E20),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xD9120E20),         // Fondo barras semi-oscuro
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xE618132B),  // Tarjetas oscuras casi sólidas (texto 100% legible)
    onSurfaceVariant = Color(0xFFF0E8FF)
)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    secondary = PurpleSecondary,
    surfaceVariant = PurpleContainer,
    background = LightBackground
)

@Composable
fun MCMovilCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}