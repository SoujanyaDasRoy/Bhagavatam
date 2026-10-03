package com.bhagavatam.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * Bhagavatam design system tokens. Light = Prabhat, dark = Sandhya.
 * Ratri (OLED) and Pothi (sepia) are variants below. Fonts: see Type.kt. See DESIGN_SYSTEM.md.
 * Text tokens reach 4.5:1 against bg in every mode; `line` is decorative only.
 */
@Immutable
data class AppColors(
    val bg: Color,
    val surface: Color,
    val sunk: Color,
    val ink: Color,
    val inkSecondary: Color,
    val line: Color,
    val accent: Color,
    val onAccent: Color,
    val accentTint: Color,
    val gold: Color,
    val shloka: Color,
    val link: Color,
    val success: Color,
    val danger: Color,
    val isDark: Boolean,
)

val AppLight = AppColors(
    bg = Color(0xFFF7F4EE),
    surface = Color(0xFFFFFFFF),
    sunk = Color(0xFFECE7DE),
    ink = Color(0xFF1C1A17),
    inkSecondary = Color(0xFF6B645A),
    line = Color(0xFFDDD6C9),
    accent = Color(0xFFA34E10),
    onAccent = Color(0xFFFFFFFF),
    accentTint = Color(0xFFF6EADF),
    gold = Color(0xFF85621A),
    shloka = Color(0xFF6E1B1B),
    link = Color(0xFF0F7C80),
    success = Color(0xFF1F7A4D),
    danger = Color(0xFFB3261E),
    isDark = false,
)

val AppDark = AppColors(
    bg = Color(0xFF12141C),
    surface = Color(0xFF1C2030),
    sunk = Color(0xFF262B3D),
    ink = Color(0xFFE9E4D8),
    inkSecondary = Color(0xFFA8A294),
    line = Color(0xFF2E3346),
    accent = Color(0xFFF0A868),
    onAccent = Color(0xFF12141C),
    accentTint = Color(0xFF2C2530),
    gold = Color(0xFFD9B45E),
    shloka = Color(0xFFF2C38B),
    link = Color(0xFF6FD0D3),
    success = Color(0xFF6CCB9A),
    danger = Color(0xFFFFB4AB),
    isDark = true,
)

/** True black for OLED screens. */
val AppRatri = AppDark.copy(bg = Color(0xFF000000), surface = Color(0xFF0B0B0F), sunk = Color(0xFF1A1A20), line = Color(0xFF26262C))

/** Sepia paper. */
val AppPothi = AppLight.copy(bg = Color(0xFFF3E7CF), surface = Color(0xFFFAF0DC), sunk = Color(0xFFE3D4B6), ink = Color(0xFF3B2F20), inkSecondary = Color(0xFF6B5A43), line = Color(0xFFD9C8A6))

val LocalAppColors = staticCompositionLocalOf { AppLight }

/** Identity hue per Skandha (1..12). */
val SkandhaHues = listOf(
    Color(0xFFE07A1F), Color(0xFFD4960F), Color(0xFFB8892A), Color(0xFF4E8A3E), Color(0xFF1F8A70), Color(0xFF0F7C80),
    Color(0xFFC8552B), Color(0xFF1D6FA5), Color(0xFFC98204), Color(0xFF2D3E8C), Color(0xFFD9577E), Color(0xFF6B4C9A),
)
val MahatmyaHue = Color(0xFF9A6A12)

/** Flat Skandha fill for tiles, chips and headers. White text stays above 4.5:1 on all twelve. */
fun skandhaFill(hue: Color, dark: Boolean): Color = lerp(hue, Color.Black, if (dark) 0.50f else 0.25f)

object Space { val xs = 4.dp; val s = 8.dp; val m = 12.dp; val l = 16.dp; val xl = 20.dp; val xxl = 24.dp; val huge = 32.dp }
object Radius {
    val field = RoundedCornerShape(12.dp); val group = RoundedCornerShape(16.dp)
    val card = RoundedCornerShape(22.dp); val large = RoundedCornerShape(24.dp); val bar = RoundedCornerShape(32.dp)
}
object Touch { val min = 48.dp }
object Motion { const val press = 120; const val sheet = 200; const val screen = 300 }
