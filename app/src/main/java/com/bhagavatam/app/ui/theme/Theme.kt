package com.bhagavatam.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import android.app.Activity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.WindowCompat

/** The app palette that goes with a reader theme. */
fun appColorsFor(theme: ReaderTheme): AppColors = when (theme) {
    ReaderTheme.Prabhat -> AppLight
    ReaderTheme.Pothi -> AppPothi
    ReaderTheme.Sandhya -> AppDark
    ReaderTheme.Ratri -> AppRatri
}

@Composable
fun BhagavatamTheme(
    uiFont: FontFamily,
    reader: ReaderColors,
    app: AppColors,
    content: @Composable () -> Unit,
) {
    val scheme = if (app.isDark) darkColorScheme(
        primary = app.accent, onPrimary = app.onAccent, primaryContainer = app.accentTint, onPrimaryContainer = app.accent,
        secondary = app.link, background = app.bg, onBackground = app.ink, surface = app.surface, onSurface = app.ink,
        surfaceVariant = app.sunk, onSurfaceVariant = app.inkSecondary, outline = app.line,
    ) else lightColorScheme(
        primary = app.accent, onPrimary = app.onAccent, primaryContainer = app.accentTint, onPrimaryContainer = app.accent,
        secondary = app.link, background = app.bg, onBackground = app.ink, surface = app.surface, onSurface = app.ink,
        surfaceVariant = app.sunk, onSurfaceVariant = app.inkSecondary, outline = app.line,
    )
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val w = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(w, view).apply { isAppearanceLightStatusBars = !app.isDark; isAppearanceLightNavigationBars = !app.isDark }
    }
    // Text with no colour of its own follows the theme's ink, not Material's default black.
    CompositionLocalProvider(LocalReaderColors provides reader, LocalAppColors provides app, androidx.compose.material3.LocalContentColor provides app.ink) {
        MaterialTheme(colorScheme = scheme, typography = appTypography(uiFont), content = content)
    }
}
