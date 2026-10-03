package com.bhagavatam.app.ui.screens

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.BuildConfig
import com.bhagavatam.app.data.AnnKind
import com.bhagavatam.app.data.ContentDb
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.data.playerTextFor
import com.bhagavatam.app.data.settingsTextFor
import com.bhagavatam.app.data.tr
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.state.PlayThrough
import com.bhagavatam.app.state.ThemeMode
import com.bhagavatam.app.ui.components.AppSlider
import com.bhagavatam.app.ui.components.ConfirmDialog
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


/** System, Light and Dark as three small previews of the app, with a ring on the chosen one. */
@Composable
internal fun ThemeSwitcher(state: AppState, labels: List<String>) {
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
internal fun MiniScreen(c: AppColors, modifier: Modifier) {
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
