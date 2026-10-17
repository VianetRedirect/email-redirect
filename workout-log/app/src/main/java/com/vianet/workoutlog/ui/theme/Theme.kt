package com.vianet.workoutlog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal = Color(0xFF2EC4B6)
private val TealDark = Color(0xFF00897B)
private val Amber = Color(0xFFFFB74D)

private val DarkColors = darkColorScheme(
    primary = Teal,
    onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF0F3F3A),
    onPrimaryContainer = Color(0xFFB2F1EA),
    secondary = Amber,
    onSecondary = Color(0xFF2B1B00),
    background = Color(0xFF121417),
    onBackground = Color(0xFFE6E8EB),
    surface = Color(0xFF1A1D21),
    onSurface = Color(0xFFE6E8EB),
    surfaceVariant = Color(0xFF262A30),
    onSurfaceVariant = Color(0xFFB8BEC6),
    outline = Color(0xFF4A5059),
    error = Color(0xFFFF6B6B),
)

private val LightColors = lightColorScheme(
    primary = TealDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2F1EA),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFFEF6C00),
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF1A1D21),
    surface = Color.White,
    onSurface = Color(0xFF1A1D21),
    surfaceVariant = Color(0xFFE9ECEF),
    onSurfaceVariant = Color(0xFF4A5059),
    outline = Color(0xFFB8BEC6),
    error = Color(0xFFC62828),
)

@Composable
fun WorkoutLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
