package com.example.cdn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Red,
    onPrimary = White,
    secondary = DarkRed,
    onSecondary = White,
    background = Black,
    onBackground = White,
    surface = DarkGray,
    onSurface = White,
    error = Red,
    onError = White
)

private val LightColorScheme = lightColorScheme(
    primary = Red,
    onPrimary = White,
    secondary = DarkRed,
    onSecondary = Red,
    background = White,
    onBackground = Black,
    surface = Color(0xFFF0F0F0),
    onSurface = Black,
    error = Red,
    onError = White
)

@Composable
fun CDNTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
