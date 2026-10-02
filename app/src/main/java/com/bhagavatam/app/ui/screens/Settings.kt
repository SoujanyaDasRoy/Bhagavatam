package com.bhagavatam.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.BuildConfig
import com.bhagavatam.app.data.ContentDb
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.data.playerTextFor
import com.bhagavatam.app.data.settingsTextFor
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.state.PlayThrough
import com.bhagavatam.app.state.ThemeMode
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.ui.components.ConfirmDialog
import com.bhagavatam.app.ui.components.AppSlider
import com.bhagavatam.app.ui.components.GroupCard
import com.bhagavatam.app.ui.components.Ic
import com.bhagavatam.app.ui.components.LargeTitle
import com.bhagavatam.app.ui.components.NavBar
import com.bhagavatam.app.ui.components.Note
import com.bhagavatam.app.ui.components.RowDivider
import com.bhagavatam.app.ui.components.SectionLabel
import com.bhagavatam.app.ui.components.Segmented
import com.bhagavatam.app.ui.components.SwitchRow
import com.bhagavatam.app.ui.components.VSpace
import com.bhagavatam.app.ui.components.ValueRow
import com.bhagavatam.app.ui.theme.AppColors
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.EnglishReading
import com.bhagavatam.app.ui.theme.LocalAppColors
import com.bhagavatam.app.ui.theme.Motion
import com.bhagavatam.app.ui.theme.Radius
import com.bhagavatam.app.ui.theme.ReaderTheme
import com.bhagavatam.app.ui.theme.appColorsFor

private fun langName(l: Lang) = when (l) { Lang.SA -> "संस्कृत"; Lang.HI -> "हिन्दी"; Lang.BN -> "বাংলা"; Lang.EN -> "English" }

/**
 * Settings is a short hub: the theme switch first, then one row per area with its current value.
 * Each area opens its own page (Appearance, Reading, Languages, Listening), so nothing here scrolls for long.
 */
@Composable
fun SettingsScreen(
    state: AppState, onSaved: () -> Unit, onGlossary: () -> Unit, onLanguages: () -> Unit,
    onAppearance: () -> Unit, onReading: () -> Unit, onListening: () -> Unit,
) {
    val s = state.strings
    val t = settingsTextFor(state.uiLang)
    val ui = state.uiLang
    var pending by remember { mutableStateOf<DataAction?>(null) }
    val total = remember { SampleData.skandhas.sumOf { it.adhyayaCount } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        LargeTitle(s.tabMe)

        Column {
            SectionLabel(t.theme)
            ThemeSwitcher(state, t.modes)
            if (state.themeMode == ThemeMode.SYSTEM) Note(t.matchPhoneNote)
        }

        Column {
            SectionLabel(t.preferences)
            GroupCard {
                ValueRow(t.appearance, t.modes[state.themeMode.ordinal], Ic.Palette, onAppearance)
                RowDivider()
                ValueRow(t.readingSec, localDigits("${(state.textScale * 100).toInt()}%", ui), Ic.TextSize, onReading)
                RowDivider()
                ValueRow(s.languages, "${langName(state.uiLang)} · ${langName(state.readLang)}", Ic.Languages, onLanguages)
                RowDivider()
                ValueRow(t.listeningSec, "${state.speed}×", Ic.Headphones, onListening)
            }
        }

        Column {
            SectionLabel(t.librarySec)
            GroupCard {
                ValueRow(s.savedVerses, localDigits((state.bookmarks + state.highlights.keys).distinct().size.toString(), ui), Ic.Bookmark, onSaved)
                RowDivider()
                ValueRow(s.glossary, "", Ic.Scroll, onGlossary)
            }
        }

        Column {
            SectionLabel(t.dataSec)
            GroupCard {
                ValueRow(t.clearProgress, localDigits("${state.finished.size} / $total", ui), Ic.History) { pending = DataAction.PROGRESS }
                RowDivider()
                ValueRow(t.clearSearches, localDigits(state.recent.size.toString(), ui), Ic.Search) { pending = DataAction.SEARCHES }
                RowDivider()
                ValueRow(t.clearSaved, localDigits((state.bookmarks + state.highlights.keys).distinct().size.toString(), ui), Ic.DeleteOutline) { pending = DataAction.SAVED }
                RowDivider()
                ValueRow(t.resetSettings, "", Ic.Settings) { pending = DataAction.RESET }
            }
        }

        Column {
            SectionLabel(t.aboutSec)
            GroupCard {
                ValueRow(t.textsFrom, s.gitaPress); RowDivider()
                ValueRow(t.version, BuildConfig.VERSION_NAME); RowDivider()
                ValueRow(t.contentVersion, localDigits(ContentDb.openVersion.toString(), ui))
            }
        }
        VSpace(200)
    }
    pending?.let { action ->
        val (title, message, confirm) = when (action) {
            DataAction.PROGRESS -> Triple(t.clearProgress, t.clearProgressMsg, t.doClear)
            DataAction.SEARCHES -> Triple(t.clearSearches, t.clearSearchesMsg, t.doClear)
            DataAction.SAVED -> Triple(t.clearSaved, t.clearSavedMsg, t.doRemove)
            DataAction.RESET -> Triple(t.resetSettings, t.resetSettingsMsg, t.doReset)
        }
        ConfirmDialog(title, message, confirm, t.cancel, onDismiss = { pending = null }) {
            when (action) {
                DataAction.PROGRESS -> state.clearProgress()
                DataAction.SEARCHES -> state.clearRecent()
                DataAction.SAVED -> state.clearSaved()
                DataAction.RESET -> state.resetSettings()
            }
        }
    }
}

private enum class DataAction { PROGRESS, SEARCHES, SAVED, RESET }

/** A settings page with a back link, a title, and its own scroll. */
@Composable
private fun SubPage(title: String, backLabel: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column {
            NavBar(backLabel, onBack)
            Text(title, Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.displaySmall, color = Brand.Ink)
        }
        content()
        VSpace(200)
    }
}

@Composable
fun AppearanceSettings(state: AppState, onBack: () -> Unit) {
    val t = settingsTextFor(state.uiLang)
    SubPage(t.appearance, state.strings.tabMe, onBack) {
        Column {
            SectionLabel(t.look)
            GroupCard(Modifier.animateContentSize()) {
                when (state.themeMode) {
                    ThemeMode.SYSTEM -> {
                        LookRow(t.lightLook, ReaderTheme.entries.filter { !it.colors.isDark }, state.lightTheme, state.strings.themes) { state.updateLightTheme(it) }
                        RowDivider()
                        LookRow(t.darkLook, ReaderTheme.entries.filter { it.colors.isDark }, state.darkTheme, state.strings.themes) { state.updateDarkTheme(it) }
                    }
                    ThemeMode.LIGHT -> LookRow(t.look, ReaderTheme.entries.filter { !it.colors.isDark }, state.readerTheme, state.strings.themes) { state.updateTheme(it) }
                    ThemeMode.DARK -> LookRow(t.look, ReaderTheme.entries.filter { it.colors.isDark }, state.readerTheme, state.strings.themes) { state.updateTheme(it) }
                }
            }
        }
        Column {
            SectionLabel(t.homeSec)
            GroupCard { SwitchRow(t.showDaily, state.showDaily, state::updateShowDaily) }
        }
    }
}

@Composable
fun ReadingSettings(state: AppState, onBack: () -> Unit) {
    val s = state.strings
    val t = settingsTextFor(state.uiLang)
    val ui = state.uiLang
    SubPage(t.readingSec, s.tabMe, onBack) {
        Column {
            SectionLabel(t.textSize)
            GroupCard {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t.textSize, Modifier.weight(1f), fontSize = 16.sp, color = Brand.Ink)
                        Text(localDigits("${(state.textScale * 100).toInt()}%", ui), fontSize = 14.sp, color = Brand.Secondary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("A", fontSize = 14.sp, color = Brand.Secondary)
                        AppSlider(value = state.textScale, onValueChange = state::updateTextScale, valueRange = 0.8f..1.6f, modifier = Modifier.weight(1f))
                        Text("A", fontSize = 24.sp, color = Brand.Secondary)
                    }
                    Text(
                        t.sample, fontFamily = EnglishReading, fontSize = (17 * state.textScale).sp, lineHeight = (26 * state.textScale * state.lineScale).sp,
                        color = Brand.Ink, modifier = Modifier.fillMaxWidth().animateContentSize().clip(Radius.field).background(Brand.Fill).padding(14.dp),
                    )
                }
            }
            Note(t.pinchHint)
        }
        Column {
            SectionLabel(t.lineSpacing)
            GroupCard {
                Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    val values = listOf(0.9f, 1.0f, 1.2f)
                    Segmented(t.spacings, values.indexOfFirst { it == state.lineScale }.coerceAtLeast(1), { state.updateLineScale(values[it]) })
                }
            }
        }
        Column {
            SectionLabel(s.reading)
            GroupCard { SwitchRow(s.showSanskrit, state.showSanskrit, state::updateShowSanskrit) }
            Note(s.showSanskritNote)
        }
    }
}

@Composable
fun ListeningSettings(state: AppState, onBack: () -> Unit) {
    val s = state.strings
    val t = settingsTextFor(state.uiLang)
    var showVoices by remember { mutableStateOf(false) }
    SubPage(t.listeningSec, s.tabMe, onBack) {
        Column {
            GroupCard {
                ValueRow(playerTextFor(state.uiLang).voiceTitle, "", Ic.Headphones) { showVoices = true }; RowDivider()
                SwitchRow(s.keepPlaying, state.keepPlaying, state::updateKeepPlaying); RowDivider()
                ValueRow(s.playThrough, when (state.playThrough) { PlayThrough.ADHYAYA -> s.throughAdhyaya; PlayThrough.SKANDHA -> s.throughSkandha; PlayThrough.GRANTH -> s.throughGranth }) { state.cyclePlayThrough() }
                RowDivider()
                ValueRow(s.speed, "${state.speed}×") { state.cycleSpeed() }; RowDivider()
                SwitchRow(s.keepScreenOn, state.keepScreenOn, state::updateKeepScreenOn)
            }
            Note(s.backgroundNote)
        }
    }
    if (showVoices) VoiceSheet(state) { showVoices = false }
}

/** System, Light and Dark as three small previews of the app, with a ring on the chosen one. */
@Composable
private fun ThemeSwitcher(state: AppState, labels: List<String>) {
    val light = appColorsFor(state.lightTheme)
    val dark = appColorsFor(state.darkTheme)
    Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ThemeMode.entries.forEachIndexed { i, mode ->
            val on = state.themeMode == mode
            val ring by animateColorAsState(if (on) Brand.Kesari else Brand.Separator, tween(Motion.sheet), label = "themeRing")
            Column(
                Modifier.weight(1f).clip(Radius.group).selectable(selected = on, role = Role.RadioButton) { state.setThemeMode(mode) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(92.dp).clip(Radius.field).border(if (on) 2.dp else 1.dp, ring, Radius.field)) {
                    when (mode) {
                        ThemeMode.LIGHT -> MiniScreen(light, Modifier.fillMaxSize())
                        ThemeMode.DARK -> MiniScreen(dark, Modifier.fillMaxSize())
                        ThemeMode.SYSTEM -> Row(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(1f).fillMaxHeight()) { MiniScreen(light, Modifier.fillMaxSize()) }
                            Box(Modifier.weight(1f).fillMaxHeight()) { MiniScreen(dark, Modifier.fillMaxSize()) }
                        }
                    }
                }
                Text(labels[i], fontSize = 14.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium, color = if (on) Brand.Ink else Brand.Secondary, maxLines = 1)
            }
        }
    }
}

/** A thumbnail of the app in one look: a heading bar, two text lines and the accent dot. */
@Composable
private fun MiniScreen(c: AppColors, modifier: Modifier) {
    Box(modifier.background(c.bg)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.width(30.dp).height(6.dp).clip(CircleShape).background(c.ink))
            Box(Modifier.width(44.dp).height(4.dp).clip(CircleShape).background(c.inkSecondary.copy(alpha = 0.7f)))
            Box(Modifier.width(34.dp).height(4.dp).clip(CircleShape).background(c.inkSecondary.copy(alpha = 0.7f)))
            Spacer(Modifier.height(2.dp))
            Box(Modifier.size(14.dp).clip(CircleShape).background(c.accent))
        }
    }
}

/** A row of look swatches: a coloured circle with "Aa", the name below, a ring on the chosen one. */
@Composable
private fun LookRow(title: String, themes: List<ReaderTheme>, selected: ReaderTheme, names: List<String>, onPick: (ReaderTheme) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, fontSize = 16.sp, color = Brand.Ink)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            themes.forEach { th ->
                val on = th == selected
                val name = names.getOrElse(ReaderTheme.entries.indexOf(th)) { th.name }
                val ring by animateColorAsState(if (on) Brand.Kesari else Brand.Separator, tween(Motion.press), label = "lookRing")
                Column(
                    Modifier.clip(RoundedCornerShape(12.dp)).selectable(selected = on, role = Role.RadioButton) { onPick(th) }.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(th.colors.bg).border(if (on) 3.dp else 1.dp, ring, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("Aa", fontFamily = EnglishReading, fontSize = 17.sp, color = th.colors.ink) }
                    Text(name, fontSize = 12.sp, color = if (on) Brand.Ink else Brand.Secondary, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}
