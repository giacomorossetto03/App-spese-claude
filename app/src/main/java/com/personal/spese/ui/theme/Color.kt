package com.personal.spese.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Palette sobria, un solo accento (verde neutro).
private val Accent = Color(0xFF2E7D64)
private val AccentDark = Color(0xFF6FD0AF)

val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = Color(0xFF4F6459),
    background = Color(0xFFFBFBF9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDF1EE),
    error = Color(0xFFB3261E)
)

val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = Color(0xFF00382A),
    secondary = Color(0xFFB4CCC0),
    background = Color(0xFF111412),
    surface = Color(0xFF181C1A),
    surfaceVariant = Color(0xFF232926),
    error = Color(0xFFF2B8B5)
)
