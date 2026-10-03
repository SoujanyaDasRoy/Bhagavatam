package com.bhagavatam.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Fixed brand colours of the "Mayur" design system (Kesari Deep primary). */
object Brand {
    val Paper = Color(0xFFF7F4EE)
    val Card = Color(0xFFFFFFFF)
    val Ink = Color(0xFF1C1A17)
    val Secondary = Color(0xFF6B645A)
    val Tertiary = Color(0xFF736C62)
    val Separator = Color(0xFFE6E0D5)
    val Fill = Color(0xFFECE7DE)
    val Kesari = Color(0xFFA34E10)
    val KesariTint = Color(0xFFF6EADF)
    val Gold = Color(0xFF85621A)
    /** Chevrons and row arrows; 3:1 against white. */
    val Chevron = Color(0xFF8A8378)
    val Sindoor = Color(0xFF6E1B1B)
    val Teal = Color(0xFF0F7C80)
    val TealTint = Color(0xFFE7F3F3)
    val Green = Color(0xFF34A36B)
    val Lotus = Color(0xFFD9577E)

    /** Identity hue per Skandha (1..12). Index 0 = Skandha 1. */
    val Skandha = listOf(
        Color(0xFFE07A1F), Color(0xFFD4960F), Color(0xFFB8892A), Color(0xFF4E8A3E),
        Color(0xFF1F8A70), Color(0xFF0F7C80), Color(0xFFC8552B), Color(0xFF1D6FA5),
        Color(0xFFC98204), Color(0xFF2D3E8C), Color(0xFFD9577E), Color(0xFF6B4C9A),
    )
}

/** Colours of the reading surface; the four reader themes swap these. */
@Immutable
data class ReaderColors(
    val name: String,
    val bg: Color,
    val surface: Color,
    val ink: Color,
    val secondary: Color,
    val separator: Color,
    val accent: Color,
    val gold: Color,
    val shloka: Color,
    val teal: Color,
    val playing: Color,
    val track: Color,
    val chipOn: Color,
    val chipOnText: Color,
    val isDark: Boolean,
)

enum class ReaderTheme(val colors: ReaderColors) {
    Prabhat(
        ReaderColors("Prabhat", Color(0xFFF7F4EE), Color(0xFFFFFFFF), Color(0xFF1C1A17), Color(0xFF6B645A),
            Color(0xFFDDD6C9), Color(0xFFA34E10), Color(0xFF85621A), Color(0xFF6E1B1B), Color(0xFF0F7C80),
            Color(0x140F7C80), Color(0xFFE6E0D5), Color(0xFFA34E10), Color(0xFFFFFFFF), false)
    ),
    Pothi(
        ReaderColors("Pothi", Color(0xFFF3E7CF), Color(0xFFFAF0DC), Color(0xFF3B2F20), Color(0xFF6B5A43),
            Color(0xFFD9C8A6), Color(0xFF8F4410), Color(0xFF8F6515), Color(0xFF7A2412), Color(0xFF0F7075),
            Color(0x1A0F7075), Color(0xFFE3D4B6), Color(0xFF3B2F20), Color(0xFFFAF0DC), false)
    ),
    Sandhya(
        ReaderColors("Sandhya", Color(0xFF12141C), Color(0xFF1C2030), Color(0xFFE9E4D8), Color(0xFFA8A294),
            Color(0xFF2E3346), Color(0xFFF0A868), Color(0xFFD9B45E), Color(0xFFF2C38B), Color(0xFF6FD0D3),
            Color(0x1F4FC3C7), Color(0xFF2E3346), Color(0xFFF0A868), Color(0xFF12141C), true)
    ),
    Ratri(
        ReaderColors("Ratri", Color(0xFF000000), Color(0xFF0B0B0F), Color(0xFFD9D4C8), Color(0xFF8F8A7F),
            Color(0xFF26262C), Color(0xFFF0A868), Color(0xFFD9B45E), Color(0xFFE6B67E), Color(0xFF6FD0D3),
            Color(0x224FC3C7), Color(0xFF26262C), Color(0xFFF0A868), Color(0xFF000000), true)
    ),
}

val LocalReaderColors = staticCompositionLocalOf { ReaderTheme.Prabhat.colors }
