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
    secondaryContainer = YaVaSurfaceVariantDark,
    onSecondaryContainer = YaVaTextDark,
    tertiary = YaVaBlueInfo,
    onTertiary = Color.White,
    background = YaVaBlack,
    onBackground = YaVaTextDark,
    surface = YaVaSurfaceDark,
    onSurface = YaVaTextDark,
    surfaceVariant = YaVaSurfaceVariantDark,
    onSurfaceVariant = YaVaTextMutedDark,
    surfaceTint = YaVaYellowPrimary,
    outline = YaVaBorderDark,
    outlineVariant = Color(0xFF383844),
    error = YaVaRedAlert,
    onError = Color.White,
    errorContainer = YaVaRedContainer,
    onErrorContainer = Color(0xFFFFDAD6)
)

private val YaVaLightColorScheme = lightColorScheme(
    primary = YaVaYellowDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFFFF0B3),
    onPrimaryContainer = Color(0xFF3B2F00),
    secondary = YaVaBlack,
    onSecondary = Color.White,
    secondaryContainer = YaVaSurfaceVariantLight,
    onSecondaryContainer = YaVaTextLight,
    tertiary = YaVaBlueInfo,
    onTertiary = Color.White,
    background = YaVaGrayLight,
    onBackground = YaVaTextLight,
    surface = YaVaSurfaceLight,
    onSurface = YaVaTextLight,
    surfaceVariant = YaVaSurfaceVariantLight,
    onSurfaceVariant = YaVaTextMutedLight,
    surfaceTint = YaVaYellowDark,
    outline = YaVaBorderLight,
    outlineVariant = Color(0xFFD0D0D8),
    error = YaVaRedAlert,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A)
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
