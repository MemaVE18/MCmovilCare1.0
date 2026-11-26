package com.example.mcmovilcare.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.mcmovilcare.R

@Composable
fun CyberBackground(
    isDashboard: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val bgDrawable = if (isDark) R.drawable.bg_ghost else R.drawable.bg_akira

    val topOverlay = if (isDark) {
        Color(0xFF0A0D14).copy(alpha = if (isDashboard) 0.68f else 0.48f)
    } else {
        Color(0xFFF8FAFC).copy(alpha = if (isDashboard) 0.74f else 0.45f)
    }

    val bottomOverlay = if (isDark) {
        Color(0xFF0D121F).copy(alpha = if (isDashboard) 0.78f else 0.72f)
    } else {
        Color(0xFFF1F5F9).copy(alpha = if (isDashboard) 0.82f else 0.68f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = bgDrawable),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(topOverlay, bottomOverlay)
                    )
                )
        )
        content()
    }
}