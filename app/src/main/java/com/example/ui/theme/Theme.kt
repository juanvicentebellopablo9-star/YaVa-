package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val YaVaDarkColorScheme = darkColorScheme(
    primary = YaVaYellowPrimary,
    onPrimary = Color.Black,
    primaryContainer = YaVaYellowContainer,
    onPrimaryContainer = YaVaYellowLight,
    secondary = YaVaYellowLight,
    onSecondary = Color.Black,
    background = YaVaBlack,
    onBackground = YaVaTextDark,
    surface = YaVaSurfaceDark,
    onSurface = YaVaTextDark,
    surfaceVariant = YaVaSurfaceVariantDark,
    onSurfaceVariant = Color.LightGray
)

private val YaVaLightColorScheme = lightColorScheme(
    primary = YaVaYellowDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFFFF1B3),
    onPrimaryContainer = Color(0xFF332B00),
    secondary = YaVaBlack,
    onSecondary = Color.White,
    background = YaVaGrayLight,
    onBackground = YaVaTextLight,
    surface = YaVaSurfaceLight,
    onSurface = YaVaTextLight,
    surfaceVariant = YaVaSurfaceVariantLight,
    onSurfaceVariant = Color.DarkGray
)

@Composable
fun YaVaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) YaVaDarkColorScheme else YaVaLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    YaVaTheme(darkTheme = darkTheme, content = content)
}
