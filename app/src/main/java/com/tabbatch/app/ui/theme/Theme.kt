package com.tabbatch.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary = Color(0xFF2962FF)
private val PrimaryDark = Color(0xFF82B1FF)

private val LightColors = lightColorScheme(
    primary = Primary,
    background = Color(0xFFFAFAFC),
    surface = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    background = Color(0xFF0F1115),
    surface = Color(0xFF16181D),
)

@Composable
fun TabBatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = TabBatchTypography,
        content = content,
    )
}
