package com.bhagavatam.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.SanskritScript
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.state.PackState
import com.bhagavatam.app.state.PlayThrough
import com.bhagavatam.app.ui.components.Dot
import com.bhagavatam.app.ui.components.GroupCard
import com.bhagavatam.app.ui.components.LargeTitle
import com.bhagavatam.app.ui.components.NavBar
import com.bhagavatam.app.ui.components.Note
import com.bhagavatam.app.ui.components.RowDivider
import com.bhagavatam.app.ui.components.SectionLabel
import com.bhagavatam.app.ui.components.Segmented
import com.bhagavatam.app.ui.components.ShlokaText
import com.bhagavatam.app.ui.components.SwitchRow
import com.bhagavatam.app.ui.components.VSpace
import com.bhagavatam.app.ui.components.ValueRow
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.HindSiliguri
import com.bhagavatam.app.ui.theme.Jakarta
import com.bhagavatam.app.ui.theme.Literata
import com.bhagavatam.app.ui.theme.Mukta
import com.bhagavatam.app.ui.theme.ReaderTheme
import com.bhagavatam.app.ui.theme.TiroBangla
import com.bhagavatam.app.ui.theme.TiroHindi
import com.bhagavatam.app.ui.theme.TiroSanskrit

@Composable
fun DownloadsScreen(state: AppState) {
    val s = state.strings
    val ui = state.uiLang
    val done = state.packs.values.count { it == PackState.DONE }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = BottomRoom)) {
        item { LargeTitle(s.tabDownloads) }
        item {
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brand.Card).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Brand.TealTint), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Headphones, null, tint = Brand.Teal)
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(localDigits(s.sanskritAudioPacks.format(done), ui), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(s.downloadsNote, fontSize = 13.sp, lineHeight = 18.sp, color = Brand.Secondary)
                }
            }
        }
        item { Column { VSpace(20); SectionLabel(s.packs) } }
        item {
            GroupCard {
                SampleData.skandhas.forEachIndexed { i, sk ->
                    if (i > 0) RowDivider()
                    val st = state.packs[sk.num] ?: PackState.NONE
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 58.dp).padding(start = 16.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Dot(if (sk.num == 0) Brand.Gold else Brand.Skandha[sk.num - 1], 10)
                        Column(Modifier.weight(1f)) {
                            Text(if (sk.num == 0) s.mahatmya else localDigits("${s.skandha} ${sk.num}", ui), fontSize = 16.sp)
                            Text(
                                when (st) { PackState.DONE -> s.onPhone; PackState.DOWNLOADING -> s.downloading; PackState.NONE -> s.sanskritAudio },
                                fontSize = 13.sp, color = Brand.Secondary,
                            )
                        }
                        when (st) {
                            PackState.DONE -> Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Check, null, tint = Brand.Green)
                                IconButton(onClick = { state.removePack(sk.num) }) { Icon(Icons.Rounded.DeleteOutline, "Remove pack", tint = Brand.Tertiary) }
                            }
                            PackState.DOWNLOADING -> Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp), color = Brand.Teal, strokeWidth = 3.dp)
                            }
                            PackState.NONE -> IconButton(onClick = { state.download(sk.num) }) { Icon(Icons.Rounded.CloudDownload, "Download", tint = Brand.Kesari) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedScreen(state: AppState, onBack: () -> Unit, onOpen: (Int, Int) -> Unit) {
    val s = state.strings
    val ui = state.uiLang
    var filter by remember { mutableIntStateOf(0) }
    val refs = when (filter) {
        1 -> state.bookmarks.toList()
        2 -> state.highlights.keys.toList()
        else -> (state.bookmarks + state.highlights.keys).distinct()
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = BottomRoom), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { NavBar(s.tabMe, onBack) }
        item { Text(s.saved, Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.displaySmall) }
        item { Segmented(listOf(s.all, s.bookmarks, s.highlights), filter, { filter = it }) }
        items(refs) { ref ->
            val v = SampleData.verse(ref) ?: return@items
            val mark = state.highlights[ref]
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brand.Card)
                    .clickable { onOpen(v.skandha, v.adhyaya) }.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (mark != null) Box(Modifier.width(4.dp).height(64.dp).clip(CircleShape).background(Color(mark)))
                else Icon(Icons.Rounded.Bookmark, null, tint = Brand.Kesari, modifier = Modifier.size(18.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(localDigits(v.ref, ui), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Brand.Gold)
                    ShlokaText(v, state.script, Brand.Sindoor, 17f, lines = 1)
                    val tr = if (state.readLang == Lang.SA) state.alongsideLayers().first() else state.readLang
                    Text(v.translation(tr), fontFamily = readingFont(tr), fontSize = 14.sp, lineHeight = 20.sp, color = Brand.Secondary, maxLines = 2)
                }
            }
        }
    }
}

@Composable
fun GlossaryScreen(state: AppState, onBack: () -> Unit) {
    val s = state.strings
    var q by remember { mutableStateOf("") }
    val variants = Finder.variants(q)
    val terms = SampleData.glossary.filter { variants.isEmpty() || Finder.find(it.term + " " + it.dev + " " + it.meaning, variants) != null }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = BottomRoom), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { NavBar(s.tabMe, onBack) }
        item { Text(s.glossary, Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.displaySmall) }
        item {
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(42.dp).clip(RoundedCornerShape(11.dp)).background(Brand.Fill).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Search, null, tint = Brand.Secondary, modifier = Modifier.size(18.dp))
                Box(Modifier.weight(1f)) {
                    if (q.isEmpty()) Text(s.glossaryHint, color = Brand.Tertiary, fontSize = 16.sp)
                    BasicTextField(q, { q = it }, singleLine = true, textStyle = TextStyle(fontSize = 16.sp, color = Brand.Ink),
                        cursorBrush = SolidColor(Brand.Kesari), modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item {
            GroupCard {
                terms.forEachIndexed { i, g ->
                    if (i > 0) RowDivider()
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(g.term, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(g.dev, fontFamily = TiroSanskrit, fontSize = 15.sp, color = Brand.Sindoor)
                        }
                        Text(g.meaning, fontSize = 14.sp, lineHeight = 19.sp, color = Brand.Secondary)
                    }
                }
            }
        }
    }
}

@Composable
fun MeScreen(state: AppState, onSaved: () -> Unit, onGlossary: () -> Unit, onLanguages: () -> Unit) {
    val s = state.strings
    val ui = state.uiLang
    val langName = { l: Lang -> when (l) { Lang.SA -> "संस्कृत"; Lang.HI -> "हिन्दी"; Lang.BN -> "বাংলা"; Lang.EN -> "English" } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        LargeTitle(s.tabMe)
        GroupCard {
            ValueRow(s.savedVerses, localDigits((state.bookmarks + state.highlights.keys).distinct().size.toString(), ui), onSaved); RowDivider()
            ValueRow(s.glossary, "", onGlossary)
        }
        GroupCard { ValueRow(s.languages, "${langName(ui)} · ${langName(state.readLang)}", onLanguages) }
        Column {
            SectionLabel(s.reading)
            GroupCard {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ReaderTheme.entries.forEachIndexed { i, t ->
                        val on = state.readerTheme == t
                        Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable { state.updateTheme(t) }.padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(44.dp).clip(CircleShape).background(t.colors.bg)
                                .border(if (on) 3.dp else 1.dp, if (on) Brand.Kesari else Brand.Separator, CircleShape), contentAlignment = Alignment.Center) {
                                Text("Aa", fontFamily = Literata, fontSize = 16.sp, color = t.colors.ink)
                            }
                            Text(s.themes[i], fontSize = 12.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium, color = if (on) Brand.Ink else Brand.Secondary)
                        }
                    }
                }
                RowDivider()
                SwitchRow(s.showSanskrit, state.showSanskrit, state::updateShowSanskrit)
            }
            Note(s.showSanskritNote)
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
        GroupCard { ValueRow(s.texts, s.gitaPress) }
        VSpace(230)
    }
}

@Composable
fun LanguagesScreen(state: AppState, onBack: () -> Unit) {
    val s = state.strings
    Column(Modifier.fillMaxSize().background(Brand.Paper).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        NavBar(s.tabMe, onBack)
        Text(s.languages, Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.displaySmall)
        Column {
            SectionLabel(s.appLanguage)
            GroupCard {
                listOf(Triple(Lang.EN, "English", Jakarta), Triple(Lang.HI, "हिन्दी", Mukta), Triple(Lang.BN, "বাংলা", HindSiliguri)).forEachIndexed { i, (l, n, f) ->
                    if (i > 0) RowDivider()
                    LanguageOption(n, mapOf(Lang.EN to "English", Lang.HI to "Hindi", Lang.BN to "Bengali").getValue(l), f, state.uiLang == l) { state.updateUiLang(l) }
                }
            }
            Note(s.appLanguageNote)
        }
        Column {
            SectionLabel(s.bhagLanguage)
            GroupCard {
                listOf(
                    Triple(Lang.SA, "संस्कृत" to s.readSa, TiroSanskrit), Triple(Lang.HI, "हिन्दी" to s.readHi, TiroHindi),
                    Triple(Lang.BN, "বাংলা" to s.readBn, TiroBangla), Triple(Lang.EN, "English" to s.readEn, Literata),
                ).forEachIndexed { i, (l, names, f) ->
                    if (i > 0) RowDivider()
                    LanguageOption(names.first, names.second, f, state.readLang == l) { state.updateReadLang(l) }
                }
            }
            Note(s.bhagLanguageNote)
        }
        if (state.readLang == Lang.SA) {
            Column {
                SectionLabel(s.sanskritScript)
                Segmented(listOf("देवनागरी", "বাংলা লিপি", "IAST"), SanskritScript.entries.indexOf(state.script),
                    { state.updateScript(SanskritScript.entries[it]) }, fonts = listOf(TiroSanskrit, TiroBangla, Literata))
            }
            Column {
                SectionLabel(s.showAlongside)
                GroupCard {
                    SwitchRow(s.transliteration, state.showIast, state::updateShowIast); RowDivider()
                    SwitchRow(s.hindiTr, state.showHi, state::updateShowHi); RowDivider()
                    SwitchRow(s.bengaliTr, state.showBn, state::updateShowBn); RowDivider()
                    SwitchRow(s.englishTr, state.showEn, state::updateShowEn)
                }
            }
        }
        VSpace(40)
    }
}
