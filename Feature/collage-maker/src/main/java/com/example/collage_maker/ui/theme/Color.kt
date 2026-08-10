package com.example.collage_maker.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// =======================
// Primary Gradient Colors
// =======================

val GradientStart = Color(0xFF8B1A1A) // Dark Red
val GradientEnd = Color(0xFF5B1E8C)   // Deep Purple
val GradientAccent = Color(0xFFFF4D8D) // Pink Accent

// =======================
// Dark Theme Colors
// =======================

val DarkBackground = Color(0xFF0D0D1A)
val DarkSurface = Color(0xFF1A1010)
val DarkSurfaceVariant = Color(0xFF2A1515)
val DarkCard = Color(0xFF331818)
val ThemeDark = Color(0xFF111827)

// =======================
// Light Theme Colors
// =======================

val LightBackground = Color(0xFFF8F9FE)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F1F8)

// =======================
// Accent Colors
// =======================

val AccentPink = Color(0xFFFF4D8D)
val AccentPurple = Color(0xFF764BA2)
val AccentBlue = Color(0xFF667EEA)
val AccentCyan = Color(0xFF00D4FF)
val AccentGreen = Color(0xFF00D9A5)
val AccentViolet = Color(0xFFA855F7)

// =======================
// Text Colors
// =======================

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0B0C3)

val TextDark = Color(0xFF1A1A2E)
val TextDarkSecondary = Color(0xFF666688)

// =======================
// Border Colors
// =======================

val BorderWhite = Color(0xFFFFFFFF)
val BorderBlack = Color(0xFF000000)

// =======================
// Main App Gradient
// =======================

val PrimaryGradient = Brush.linearGradient(
    colors = listOf(
        GradientStart,
        GradientEnd,
        GradientAccent
    )
)

// =======================
// Card Gradient
// =======================

val CardGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF2A0F0F).copy(alpha = 0.95f),
        Color(0xFF3A1A1A).copy(alpha = 0.75f)
    )
)