package com.bhagavatam.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontFamily

@Composable
fun BhagavatamTheme(
    uiFont: FontFamily,
    reader: ReaderColors,
    content: @Composable () -> Unit,
) {
    val scheme = lightColorScheme(
        primary = Brand.Kesari,
        onPrimary = Brand.Card,
        primaryContainer = Brand.KesariTint,
        onPrimaryContainer = Brand.Kesari,
        secondary = Brand.Teal,
        background = Brand.Paper,
        onBackground = Brand.Ink,
        surface = Brand.Card,
        onSurface = Brand.Ink,
        surfaceVariant = Brand.Fill,
        onSurfaceVariant = Brand.Secondary,
        outline = Brand.Separator,
    )
    CompositionLocalProvider(LocalReaderColors provides reader) {
        MaterialTheme(colorScheme = scheme, typography = appTypography(uiFont), content = content)
    }
}
