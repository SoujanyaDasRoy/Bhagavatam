package com.bhagavatam.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.BuildConfig
import com.bhagavatam.app.data.BENGALI_READY
import com.bhagavatam.app.data.ContentDb
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.PlayThrough
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.data.settingsTextFor
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.components.GroupCard
import com.bhagavatam.app.ui.components.LargeTitle
import com.bhagavatam.app.ui.components.Note
import com.bhagavatam.app.ui.components.RowDivider
import com.bhagavatam.app.ui.components.SectionLabel
import com.bhagavatam.app.ui.components.Segmented
import com.bhagavatam.app.ui.components.SwitchRow
import com.bhagavatam.app.ui.components.VSpace
import com.bhagavatam.app.ui.components.ValueRow
import com.bhagavatam.app.ui.theme.AccentChoice
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.EnglishReading
import com.bhagavatam.app.ui.theme.LocalAppColors
import com.bhagavatam.app.ui.theme.NotoDevanagari
import com.bhagavatam.app.ui.theme.ReaderTheme

/** One screen for every setting, grouped by what you are doing: how it looks, reading, listening, library, about. */
@Composable
fun SettingsScreen(state: AppState, onSaved: () -> Unit, onGlossary: () -> Unit, onLanguages: () -> Unit) {
    val s = state.strings
    val t = settingsTextFor(state.uiLang)
    val ui = state.uiLang
    val langName = { l: Lang -> when (l) { Lang.SA -> "संस्कृत"; Lang.HI -> "हिन्दी"; Lang.BN -> "বাংলা"; Lang.EN -> "English" } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        LargeTitle(s.tabMe)

        Column {
            SectionLabel(t.appearance)
            GroupCard {
                SwitchRow(t.matchPhone, state.followSystem, state::updateFollowSystem)
                RowDivider()
                if (state.followSystem) {
                    LookRow(t.lightLook, ReaderTheme.entries.filter { !it.colors.isDark }, state.lightTheme, s.themes) { state.updateLightTheme(it) }
                    RowDivider()
                    LookRow(t.darkLook, ReaderTheme.entries.filter { it.colors.isDark }, state.darkTheme, s.themes) { state.updateDarkTheme(it) }
                } else {
                    LookRow(t.look, ReaderTheme.entries.toList(), state.readerTheme, s.themes) { state.updateTheme(it) }
                }
                RowDivider()
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(t.accentColour, fontSize = 16.sp, color = Brand.Ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        AccentChoice.entries.forEachIndexed { i, a ->
                            val on = state.accent == a
                            val dark = LocalAppColors.current.isDark
                            Column(
                                Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.RadioButton, onClickLabel = t.accents[i]) { state.updateAccent(a) }.padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    Modifier.size(44.dp).clip(CircleShape).background(if (dark) a.dark else a.light)
                                        .border(if (on) 3.dp else 0.dp, Brand.Ink, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) { if (on) Icon(Icons.Rounded.Check, null, tint = LocalAppColors.current.onAccent) }
                                Text(t.accents[i], fontSize = 12.sp, color = if (on) Brand.Ink else Brand.Secondary, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
            if (state.followSystem) Note(t.matchPhoneNote)
        }

        Column {
            SectionLabel(s.reading)
            GroupCard {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t.textSize, Modifier.weight(1f), fontSize = 16.sp, color = Brand.Ink)
                        Text(localDigits("${(state.textScale * 100).toInt()}%", ui), fontSize = 14.sp, color = Brand.Secondary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("A", fontSize = 14.sp, color = Brand.Secondary)
                        Slider(
                            value = state.textScale, onValueChange = state::updateTextScale, valueRange = 0.8f..1.6f, modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = Brand.Kesari, activeTrackColor = Brand.Kesari, inactiveTrackColor = Brand.Fill),
                        )
                        Text("A", fontSize = 24.sp, color = Brand.Secondary)
                    }
                    Text(
                        t.sample, fontFamily = EnglishReading, fontSize = (17 * state.textScale).sp, lineHeight = (26 * state.textScale * state.lineScale).sp,
                        color = Brand.Ink, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Brand.Fill).padding(14.dp),
                    )
                }
                RowDivider()
                Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(t.lineSpacing, Modifier.padding(horizontal = 16.dp), fontSize = 16.sp, color = Brand.Ink)
                    val values = listOf(0.9f, 1.0f, 1.2f)
                    Segmented(t.spacings, values.indexOfFirst { it == state.lineScale }.coerceAtLeast(1), { state.updateLineScale(values[it]) })
                }
                RowDivider()
                ValueRow(s.languages, "${langName(state.uiLang)} · ${langName(state.readLang)}", onLanguages)
                RowDivider()
                SwitchRow(s.showSanskrit, state.showSanskrit, state::updateShowSanskrit)
            }
            Note(s.showSanskritNote)
        }

        Column {
            SectionLabel(t.homeSec)
            GroupCard { SwitchRow(t.showDaily, state.showDaily, state::updateShowDaily) }
        }

        Column {
            SectionLabel(s.listening)
            GroupCard {
                SwitchRow(s.keepPlaying, state.keepPlaying, state::updateKeepPlaying); RowDivider()
                ValueRow(s.playThrough, when (state.playThrough) { PlayThrough.ADHYAYA -> s.throughAdhyaya; PlayThrough.SKANDHA -> s.throughSkandha; PlayThrough.GRANTH -> s.throughGranth }) { state.cyclePlayThrough() }
                RowDivider()
                ValueRow(s.speed, "${state.speed}×") { state.cycleSpeed() }; RowDivider()
                SwitchRow(s.keepScreenOn, state.keepScreenOn, state::updateKeepScreenOn)
            }
            Note(s.backgroundNote)
        }

        Column {
            SectionLabel(t.librarySec)
            GroupCard {
                ValueRow(s.savedVerses, localDigits((state.bookmarks + state.highlights.keys).distinct().size.toString(), ui), onSaved); RowDivider()
                ValueRow(s.glossary, "", onGlossary)
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
}

/** A row of theme swatches: a coloured circle with "Aa", the name below, a ring on the chosen one. */
@Composable
private fun LookRow(title: String, themes: List<ReaderTheme>, selected: ReaderTheme, names: List<String>, onPick: (ReaderTheme) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, fontSize = 16.sp, color = Brand.Ink)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            themes.forEach { th ->
                val on = th == selected
                val name = names.getOrElse(ReaderTheme.entries.indexOf(th)) { th.name }
                Column(
                    Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.RadioButton, onClickLabel = name) { onPick(th) }.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(th.colors.bg).border(if (on) 3.dp else 1.dp, if (on) Brand.Kesari else Brand.Separator, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("Aa", fontFamily = EnglishReading, fontSize = 17.sp, color = th.colors.ink) }
                    Text(name, fontSize = 12.sp, color = if (on) Brand.Ink else Brand.Secondary, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}
