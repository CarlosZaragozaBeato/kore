package com.kore.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val KoreOrange = Color(0xFFF2A33C)

private val LightColors = lightColorScheme(
    primary = KoreOrange,
    onPrimary = Color(0xFF111111),
    background = Color(0xFFF8F8F6),
    onBackground = Color(0xFF141414),
    surface = Color.White,
    onSurface = Color(0xFF141414),
    surfaceVariant = Color(0xFFEEEEEA),
    onSurfaceVariant = Color(0xFF5D5D57),
    outlineVariant = Color(0xFFDDDDD6),
)

private val DarkColors = darkColorScheme(
    primary = KoreOrange,
    onPrimary = Color(0xFF111111),
    background = Color(0xFF101418),
    onBackground = Color(0xFFF7F7F3),
    surface = Color(0xFF171C21),
    onSurface = Color(0xFFF7F7F3),
    surfaceVariant = Color(0xFF252B31),
    onSurfaceVariant = Color(0xFFBEC3C7),
    outlineVariant = Color(0xFF343B42),
)

private val KoreShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun KoreTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = KoreShapes,
        content = content,
    )
}
