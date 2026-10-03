package com.bhagavatam.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.SanskritScript
import com.bhagavatam.app.data.Verse
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.components.MiniPlayer
import com.bhagavatam.app.ui.components.SectionLabel
import com.bhagavatam.app.ui.components.Segmented
import com.bhagavatam.app.ui.components.ShlokaText
import com.bhagavatam.app.ui.components.SwitchRow
import com.bhagavatam.app.ui.components.VSpace
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.Literata
import com.bhagavatam.app.ui.theme.LocalReaderColors
import com.bhagavatam.app.ui.theme.ReaderTheme
import com.bhagavatam.app.ui.theme.TiroBangla
import com.bhagavatam.app.ui.theme.TiroHindi
import com.bhagavatam.app.ui.theme.TiroSanskrit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(state: AppState, skandha: Int, adhyaya: Int, onBack: () -> Unit, onOpenPlayer: () -> Unit) {
    val c = LocalReaderColors.current
    val s = state.strings
    val ui = state.uiLang
    val verses = remember(skandha, adhyaya) { SampleData.versesFor(skandha, adhyaya) }
    val title = SampleData.adhyayaTitle(skandha, adhyaya, state.titleLang, s)
    var showSheet by remember { mutableStateOf(false) }
    val peeked = remember { mutableStateListOf<String>() }
    val list = rememberLazyListState()
    val scale = state.textScale

    LaunchedEffect(skandha, adhyaya) {
        if (verses.isNotEmpty() && !(state.lastSkandha == skandha && state.lastAdhyaya == adhyaya)) state.markRead(skandha, adhyaya, 1)
    }
    // Keep the playing shloka in view.
    val playingHere = state.hasSession && state.current.skandha == skandha && state.current.adhyaya == adhyaya
    LaunchedEffect(state.index, playingHere) {
        if (playingHere) list.animateScrollToItem((state.index + 1).coerceAtMost(verses.size))
    }

    // Translation shown in book mode: the paath language, or the first chosen translation when paath is Sanskrit.
    val bookLang = if (state.readLang == Lang.SA) state.alongsideLayers().first() else state.readLang

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize()) {
            // Top bar
            Row(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, s.back, tint = c.accent) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1)
                    Text(
                        localDigits(if (skandha == 0) "${s.mahatmya} · ${s.adhyaya} $adhyaya" else "${s.skandha} $skandha · ${s.adhyaya} $adhyaya", ui),
                        fontSize = 12.sp, color = c.secondary,
                    )
                }
                Box(Modifier.size(48.dp).clip(CircleShape).clickable { showSheet = true }, contentAlignment = Alignment.Center) {
                    Text("Aa", fontFamily = Literata, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = c.accent)
                }
            }
            if (state.showSanskrit) {
                LayerChips(state)
            } else {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface)
                        .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(s.sanskritOff, Modifier.weight(1f), fontSize = 13.sp, lineHeight = 18.sp, color = c.secondary)
                    Box(
                        Modifier.clip(CircleShape).background(c.chipOn).clickable { state.updateShowSanskrit(true) }.padding(horizontal = 12.dp, vertical = 7.dp),
                    ) { Text(s.show, color = c.chipOnText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                }
            }

            LazyColumn(
                state = list,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 260.dp),
                verticalArrangement = Arrangement.spacedBy(if (state.showSanskrit) 8.dp else 16.dp),
            ) {
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (skandha == 1 && adhyaya == 1) Text("ॐ नमो भगवते वासुदेवाय", fontFamily = TiroSanskrit, fontSize = 16.sp, color = c.gold)
                        Text(localDigits("${s.adhyaya.uppercase()} $adhyaya", ui), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.gold, letterSpacing = 1.sp)
                        Text(title, fontFamily = readingFont(state.titleLang), fontSize = 24.sp, lineHeight = 32.sp,
                            fontWeight = FontWeight.Medium, color = c.ink, textAlign = TextAlign.Center)
                        Box(Modifier.width(40.dp).height(1.dp).background(c.gold.copy(alpha = 0.6f)))
                    }
                }
                if (verses.isEmpty()) {
                    item {
                        Text(s.sampleOnly, Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(18.dp),
                            fontSize = 15.sp, lineHeight = 22.sp, color = c.secondary)
                    }
                }
                items(verses, key = { it.ref }) { v ->
                    val isCurrent = playingHere && state.current.ref == v.ref
                    if (state.showSanskrit) {
                        VerseBlock(state, v, isCurrent, scale)
                    } else {
                        BookParagraph(state, v, bookLang, scale, v.ref in peeked) {
                            if (v.ref in peeked) peeked.remove(v.ref) else peeked.add(v.ref)
                        }
                    }
                }
            }
        }

        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 10.dp)) {
            if (state.hasSession) {
                MiniPlayer(state, onOpen = onOpenPlayer)
            } else if (verses.isNotEmpty()) {
                Row(
                    Modifier.clip(CircleShape).background(Brand.Kesari).clickable { state.playVerses(verses) }.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Color.White)
                    Text(s.listen, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }, containerColor = Brand.Paper) {
            ReaderSettings(state)
        }
    }
}

@Composable
private fun LayerChips(state: AppState) {
    val c = LocalReaderColors.current
    val chips = listOf(
        Triple("${state.strings.mool}", true, { state.updateShowSanskrit(false) }),
        Triple("IAST", state.showIast, { state.updateShowIast(!state.showIast) }),
        Triple("हिन्दी", state.showHi, { state.updateShowHi(!state.showHi) }),
        Triple("বাংলা", state.showBn, { state.updateShowBn(!state.showBn) }),
        Triple("English", state.showEn, { state.updateShowEn(!state.showEn) }),
    )
    val fonts = listOf(null, null, TiroHindi, TiroBangla, null)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEachIndexed { i, (label, on, click) ->
            Box(
                Modifier.clip(CircleShape)
                    .background(if (on) c.chipOn else Color.Transparent)
                    .border(1.dp, if (on) c.chipOn else c.separator, CircleShape)
                    .clickable(onClick = click).padding(horizontal = 13.dp, vertical = 7.dp),
            ) {
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = fonts[i], color = if (on) c.chipOnText else c.secondary)
            }
        }
    }
}

@Composable
private fun VerseBlock(state: AppState, v: Verse, isCurrent: Boolean, scale: Float) {
    val c = LocalReaderColors.current
    val ui = state.uiLang
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (isCurrent) c.playing else Color.Transparent)
            .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(localDigits(if (v.numEnd > v.num) "${v.ref}\u2013${v.numEnd}" else v.ref, ui), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.gold, letterSpacing = 0.5.sp)
            if (v.speaker != null) Text("  ·  ${v.speaker}", fontFamily = TiroSanskrit, fontSize = 13.sp, color = c.secondary)
            Box(Modifier.weight(1f))
            if (isCurrent) {
                Icon(Icons.Rounded.Headphones, null, tint = c.teal, modifier = Modifier.size(15.dp))
            }
            val marked = v.ref in state.bookmarks
            IconButton(onClick = { state.toggleBookmark(v.ref) }, modifier = Modifier.size(36.dp)) {
                Icon(if (marked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, "Bookmark", tint = if (marked) c.accent else c.secondary, modifier = Modifier.size(20.dp))
            }
        }
        Box(Modifier.padding(end = 8.dp)) { ShlokaText(v, state.script, c.shloka, 20f * scale) }
        if (state.showIast && state.script != SanskritScript.IAST) {
            Text(v.iast.joinToString("\n"), fontFamily = Literata, fontSize = (15 * scale).sp, lineHeight = (24 * scale).sp, color = c.secondary)
        }
        state.alongsideLayers().forEach { l ->
            Box(Modifier.fillMaxWidth().padding(end = 8.dp).height(1.dp).background(c.gold.copy(alpha = 0.25f)))
            Text(v.translation(l), fontFamily = readingFont(l), fontSize = (17 * scale).sp, lineHeight = (26 * scale).sp, color = c.ink,
                modifier = Modifier.padding(end = 8.dp))
        }
    }
}

@Composable
private fun BookParagraph(state: AppState, v: Verse, lang: Lang, scale: Float, peek: Boolean, onToggle: () -> Unit) {
    val c = LocalReaderColors.current
    val ui = state.uiLang
    Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = c.gold, fontWeight = FontWeight.Bold, fontSize = 12.sp, baselineShift = BaselineShift.Superscript)) {
                    append(localDigits(v.numLabel, ui) + "  ")
                }
                append(v.translation(lang))
            },
            modifier = Modifier.clickable(onClick = onToggle),
            fontFamily = readingFont(lang), fontSize = (18 * scale).sp, lineHeight = (31 * scale).sp, color = c.ink,
        )
        if (peek) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.playing).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(localDigits("${state.strings.shloka} ${v.ref}", ui), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.gold)
                val script = if (ui == Lang.BN && state.script == SanskritScript.DEVANAGARI) SanskritScript.BENGALI else state.script
                ShlokaText(v, script, c.shloka, 16f * scale)
            }
        }
    }
}

@Composable
fun ReaderSettings(state: AppState) {
    val s = state.strings
    Column(Modifier.fillMaxWidth().padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            ReaderTheme.entries.forEachIndexed { i, t ->
                val on = state.readerTheme == t
                Column(Modifier.clickable { state.updateTheme(t) }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape).background(t.colors.bg)
                            .border(if (on) 3.dp else 1.dp, if (on) Brand.Kesari else Brand.Separator, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("Aa", fontFamily = Literata, fontSize = 17.sp, color = t.colors.ink) }
                    Text(s.themes[i], fontSize = 12.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium, color = if (on) Brand.Ink else Brand.Secondary)
                }
            }
        }
        Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("A", fontFamily = Literata, fontSize = 14.sp, color = Brand.Secondary)
            Slider(
                value = state.textScale, onValueChange = state::updateTextScale, valueRange = 0.8f..1.6f, modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = Brand.Card, activeTrackColor = Brand.Kesari, inactiveTrackColor = Brand.Separator),
            )
            Text("A", fontFamily = Literata, fontSize = 22.sp, color = Brand.Secondary)
        }
        Column(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(14.dp)).background(Brand.Card)) {
            SwitchRow(s.showSanskrit, state.showSanskrit, state::updateShowSanskrit)
        }
        if (state.showSanskrit) {
            Column {
                SectionLabel(s.sanskritScript)
                Segmented(
                    listOf("देवनागरी", "বাংলা লিপি", "IAST"),
                    SanskritScript.entries.indexOf(state.script),
                    { state.updateScript(SanskritScript.entries[it]) },
                    fonts = listOf(TiroSanskrit, TiroBangla, Literata),
                )
            }
        }
        VSpace(4)
        HorizontalDivider(color = Color.Transparent)
    }
}
