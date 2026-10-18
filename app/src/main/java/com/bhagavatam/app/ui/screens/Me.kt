package com.bhagavatam.app.ui.screens

import androidx.compose.ui.res.painterResource
import com.bhagavatam.app.ui.components.Ic
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import com.bhagavatam.app.data.tr
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
import com.bhagavatam.app.data.BENGALI_READY
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
import com.bhagavatam.app.ui.theme.Radius
import com.bhagavatam.app.ui.theme.HindSiliguri
import com.bhagavatam.app.ui.theme.Jakarta
import com.bhagavatam.app.ui.theme.EnglishReading
import com.bhagavatam.app.ui.theme.Mukta
import com.bhagavatam.app.ui.theme.ReaderTheme
import com.bhagavatam.app.ui.theme.NotoSerifBengali
import com.bhagavatam.app.ui.theme.TiroHindi
import com.bhagavatam.app.ui.theme.NotoDevanagari

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
                    Icon(painterResource(Ic.Headphones), null, tint = Brand.Teal)
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(localDigits(s.sanskritAudioPacks.format(done), ui), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(s.downloadsNote, fontSize = 15.sp, lineHeight = 21.sp, color = Brand.Secondary)
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
                            Text(if (sk.num == 0) state.bookStrings.mahatmya else "${localDigits("${state.bookStrings.skandha} ${sk.num}", state.titleLang)} · ${sk.title(state.contentLang)}", fontSize = 16.sp)
                            Text(
                                when (st) { PackState.DONE -> s.onPhone; PackState.DOWNLOADING -> s.downloading; PackState.NONE -> s.sanskritAudio },
                                fontSize = 15.sp, color = Brand.Secondary,
                            )
                        }
                        when (st) {
                            PackState.DONE -> Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(painterResource(Ic.Check), null, tint = Brand.Green)
                                IconButton(onClick = { state.removePack(sk.num) }) { Icon(painterResource(Ic.DeleteOutline), tr(ui, "Remove pack", "पैक हटाएं", "প্যাক মুছুন", "ପ୍ୟାକ୍ ହଟାନ୍ତୁ"), tint = Brand.Tertiary) }
                            }
                            PackState.DOWNLOADING -> Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp), color = Brand.Teal, strokeWidth = 3.dp)
                            }
                            PackState.NONE -> IconButton(onClick = { state.download(sk.num) }) { Icon(painterResource(Ic.CloudDownload), tr(ui, "Download", "डाउनलोड करें", "ডাউনলোড করুন", "ଡାଉନଲୋଡ୍"), tint = Brand.Kesari) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    // Passage marks and notes made in the reader: all of them, passage bookmarks only, or highlights and notes; then the search and colour chip.
    var query by remember { mutableStateOf("") }
    var colourChip by remember { mutableIntStateOf(0) }
    val kindMarks = state.annotations.filter { a ->
        when (filter) { 1 -> a.kind == com.bhagavatam.app.data.AnnKind.BOOKMARK; 2 -> a.kind != com.bhagavatam.app.data.AnnKind.BOOKMARK; else -> true }
    }
    val marks = remember(kindMarks, query, colourChip) { filterMarks(kindMarks, query, colourChip) }
    val markGroups = remember(marks) { groupMarks(marks) }
    Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = BottomRoom), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { NavBar(s.tabMe, onBack) }
        item { Text(s.saved, Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.displaySmall) }
        item { Segmented(listOf(s.all, s.bookmarks, s.highlights), filter, { filter = it }) }
        if (kindMarks.isNotEmpty()) item { NotebookControls(state, query, { query = it }, colourChip, { colourChip = it }, marks) }
        if (marks.isNotEmpty()) {
            item { Text(tr(ui, "Marks and notes", "निशान और नोट", "চিহ্ন ও নোট", "ଚିହ୍ନ ଓ ନୋଟ୍"), Modifier.padding(horizontal = 20.dp, vertical = 4.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Brand.Secondary) }
            notebookMarks(state, markGroups, onOpen)
        } else if (kindMarks.isNotEmpty()) item {
            Text(tr(ui, "No note matches.", "कोई नोट नहीं मिला।", "কোনো নোট মেলেনি।", "କୌଣସି ନୋଟ୍ ମେଳ ଖାଉନାହିଁ।"), Modifier.padding(horizontal = 20.dp, vertical = 12.dp), fontSize = 15.sp, color = Brand.Secondary)
        }
        if (refs.isEmpty() && kindMarks.isEmpty()) item {
            val t = com.bhagavatam.app.data.settingsTextFor(ui)
            Column(Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painterResource(Ic.Bookmark), null, tint = Brand.Secondary, modifier = Modifier.size(32.dp))
                Text(t.nothingSaved, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Brand.Ink)
                Text(t.savedHint, fontSize = 15.sp, lineHeight = 20.sp, color = Brand.Secondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        items(refs, key = { it }) { ref ->
            val v = SampleData.verse(ref) ?: return@items
            val mark = state.highlights[ref]
            // Swipe a bookmarked verse to the left to remove it. Highlights are not removed from here.
            val removable = ref in state.bookmarks
            // Keyed on removable so a row that stays (it is also a highlight) comes back closed.
            androidx.compose.runtime.key(removable) {
            val dismiss = rememberSwipeToDismissBoxState(confirmValueChange = { to ->
                if (to == SwipeToDismissBoxValue.EndToStart && removable) { state.toggleBookmark(ref); true } else false
            })
            SwipeToDismissBox(
                state = dismiss, modifier = Modifier.animateItem().padding(horizontal = 16.dp),
                enableDismissFromStartToEnd = false, enableDismissFromEndToStart = removable,
                backgroundContent = {
                    Box(Modifier.fillMaxSize().clip(Radius.group).background(Brand.Kesari), contentAlignment = Alignment.CenterEnd) {
                        Icon(painterResource(Ic.DeleteOutline), tr(ui, "Remove bookmark", "बुकमार्क हटाएँ", "বুকমার্ক সরান", "ବୁକମାର୍କ ହଟାନ୍ତୁ"), tint = Brand.OnKesari, modifier = Modifier.padding(end = 22.dp))
                    }
                },
            ) {
            Row(
                Modifier.fillMaxWidth().clip(Radius.group).background(Brand.Card)
                    .clickable { onOpen(v.skandha, v.adhyaya) }.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (mark != null) Box(Modifier.width(4.dp).height(64.dp).clip(CircleShape).background(Color(mark)))
                else Icon(painterResource(Ic.Bookmark), null, tint = Brand.Kesari, modifier = Modifier.size(18.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(localDigits(v.ref, ui), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Brand.Gold)
                    ShlokaText(v, state.script, Brand.Sindoor, 17f, lines = 1)
                    val tr = if (state.readLang == Lang.SA) state.alongsideLayers().first() else state.readLang
                    Text(v.translation(tr), fontFamily = readingFont(tr), fontSize = 15.sp, lineHeight = 24.sp, color = Brand.Secondary, maxLines = 2)
                }
            }
            }
            }
        }
    }
    AnnotationSheet(state)
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
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(48.dp).clip(Radius.field).background(Brand.Fill).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(painterResource(Ic.Search), null, tint = Brand.Secondary, modifier = Modifier.size(18.dp))
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
                            Text(g.dev, fontFamily = NotoDevanagari, fontSize = 15.sp, color = Brand.Sindoor)
                        }
                        Text(g.meaning, fontSize = 15.sp, lineHeight = 24.sp, color = Brand.Secondary)
                    }
                }
            }
        }
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
                listOf(Triple(Lang.EN, "English", Jakarta), Triple(Lang.HI, "हिन्दी", Mukta), Triple(Lang.BN, "বাংলা", HindSiliguri), Triple(Lang.OR, "ଓଡ଼ିଆ", readingFont(Lang.OR))).forEachIndexed { i, (l, n, f) ->
                    if (i > 0) RowDivider()
                    LanguageOption(n, nameIn(state.uiLang, l), f, state.uiLang == l) { state.updateUiLang(l) }
                }
            }
            Note(s.appLanguageNote)
        }
        Column {
            SectionLabel(s.bhagLanguage)
            GroupCard {
                listOf(
                    Triple(Lang.SA, "संस्कृत" to s.readSa, NotoDevanagari), Triple(Lang.HI, "हिन्दी" to s.readHi, TiroHindi),
                    Triple(Lang.BN, "বাংলা" to s.readBn, NotoSerifBengali), Triple(Lang.EN, "English" to s.readEn, EnglishReading),
                    Triple(Lang.OR, "ଓଡ଼ିଆ" to tr(state.uiLang, "Odia", "ओड़िया", "ওড়িয়া", "ଓଡ଼ିଆ"), com.bhagavatam.app.ui.theme.NotoSerifOriya),
                ).filter { (BENGALI_READY || it.first != Lang.BN) && (com.bhagavatam.app.data.ODIA_READY || it.first != Lang.OR) }.forEachIndexed { i, (l, names, f) ->
                    if (i > 0) RowDivider()
                    LanguageOption(names.first, names.second, f, state.readLang == l) { state.updateReadLang(l) }
                }
            }
            Note(s.bhagLanguageNote)
        }
        if (state.readLang == Lang.SA) {
            Column {
                SectionLabel(s.sanskritScript)
                Segmented(listOf("देवनागरी", "বাংলা লিপি", "English (Roman)"), SanskritScript.entries.indexOf(state.script),
                    { state.updateScript(SanskritScript.entries[it]) }, fonts = listOf(NotoDevanagari, NotoSerifBengali, EnglishReading))
            }
            Column {
                SectionLabel(s.showAlongside)
                GroupCard {
                    SwitchRow(s.transliteration, state.showIast, state::updateShowIast); RowDivider()
                    SwitchRow(s.hindiTr, state.showHi, state::updateShowHi); RowDivider()
                    if (BENGALI_READY) { SwitchRow(s.bengaliTr, state.showBn, state::updateShowBn); RowDivider() }
                    SwitchRow(s.englishTr, state.showEn, state::updateShowEn)
                    if (com.bhagavatam.app.data.ODIA_READY) { RowDivider(); SwitchRow(tr(state.uiLang, "Odia translation", "ओड़िया अनुवाद", "ওড়িয়া অনুবাদ", "ଓଡ଼ିଆ ଅନୁବାଦ"), state.showOr, state::updateShowOr) }
                }
            }
        }
        VSpace(40)
    }
}
