package com.bhagavatam.app.ui.screens

import androidx.compose.ui.res.painterResource
import com.bhagavatam.app.ui.components.Ic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import com.bhagavatam.app.ui.theme.Motion
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import com.bhagavatam.app.ui.components.tappable
import com.bhagavatam.app.ui.components.opticallyCentred
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import com.bhagavatam.app.data.BENGALI_READY
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.SanskritScript
import com.bhagavatam.app.data.Verse
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.state.AppState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import com.bhagavatam.app.ui.components.AppSlider
import com.bhagavatam.app.ui.components.BottomScrim
import com.bhagavatam.app.ui.theme.Radius
import com.bhagavatam.app.ui.components.MiniPlayer
import com.bhagavatam.app.ui.components.MarkedText
import androidx.compose.ui.text.TextStyle
import com.bhagavatam.app.ui.components.SectionLabel
import com.bhagavatam.app.ui.components.Segmented
import com.bhagavatam.app.ui.components.ShlokaText
import com.bhagavatam.app.ui.components.SwitchRow
import com.bhagavatam.app.ui.components.VSpace
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.EnglishReading
import com.bhagavatam.app.ui.theme.LocalReaderColors
import com.bhagavatam.app.ui.theme.ReaderTheme
import com.bhagavatam.app.ui.theme.NotoSerifBengali
import com.bhagavatam.app.ui.theme.TiroHindi
import com.bhagavatam.app.ui.theme.NotoDevanagari

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(state: AppState, skandha: Int, adhyaya: Int, onBack: () -> Unit, onOpenPlayer: () -> Unit, onNextChapter: (Int, Int) -> Unit = { _, _ -> }, onPrevChapter: (Int, Int) -> Unit = { _, _ -> }) {
    val c = LocalReaderColors.current
    val s = state.strings
    val ui = state.uiLang
    val verses = remember(skandha, adhyaya) { SampleData.versesFor(skandha, adhyaya) }
    val title = SampleData.adhyayaTitle(skandha, adhyaya, state.titleLang, s)
    var showSheet by remember { mutableStateOf(false) }
    val peeked = remember { mutableStateListOf<String>() }
    val list = rememberLazyListState()
    val scale = state.textScale
    val prev = remember(skandha, adhyaya) { SampleData.neighbour(skandha, adhyaya, -1) }
    val next = remember(skandha, adhyaya) { SampleData.neighbour(skandha, adhyaya, 1) }
    val swipePx = with(LocalDensity.current) { 96.dp.toPx() }
    // The chapter title sits once, in the text. It moves up into the bar only after it has scrolled out of view.
    val titleInBar by remember { derivedStateOf { list.firstVisibleItemIndex >= 2 } }
    val reference = localDigits(if (skandha == 0) "${s.mahatmya} · ${s.adhyaya} $adhyaya" else "${s.skandha} $skandha · ${s.adhyaya} $adhyaya", ui)

    LaunchedEffect(skandha, adhyaya) {
        if (verses.isNotEmpty() && !(state.lastSkandha == skandha && state.lastAdhyaya == adhyaya)) state.markRead(skandha, adhyaya, 1)
    }
    // Reading carries on: scrolling puts the meaning card away.
    LaunchedEffect(list.isScrollInProgress) {
        if (list.isScrollInProgress && !state.showWordSheet) {
            state.lookup = null
            state.selBar?.let { it.clear(); state.selBar = null }
        }
    }
    // Keep the playing shloka in view.
    val playingHere = state.hasSession && state.current.skandha == skandha && state.current.adhyaya == adhyaya
    LaunchedEffect(state.index, playingHere) {
        if (playingHere) list.animateScrollToItem((state.index + 1).coerceAtMost(verses.size))
    }

    // Translation shown in book mode: the paath language, or the first chosen translation when paath is Sanskrit.
    val bookLang0 = if (state.readLang == Lang.SA) state.alongsideLayers().first() else state.readLang
    val bookLang = if (!BENGALI_READY && bookLang0 == Lang.BN) Lang.EN else bookLang0

    // Book mode: skip blocks whose translation is printed with the group, and label the carrier with the whole range.
    val bookRows = remember(verses, bookLang) {
        val out = ArrayList<BookRow>()
        var pending: Int? = null
        for (v in verses) {
            if (!v.hasText(bookLang)) {
                // A translation that sits on an earlier verse (English and Bengali joint verses): widen that row's label to cover this one.
                val from = when (bookLang) { Lang.EN -> v.enFrom; Lang.BN -> v.bnFrom; else -> null }
                val last = out.lastOrNull()
                if (from != null && from < v.num && last != null && last.verse.num == from) {
                    out[out.lastIndex] = last.copy(label = "$from-${v.numEnd}")
                    continue
                }
                if (pending == null) pending = v.num
                continue
            }
            out.add(BookRow(v, if (pending != null) "$pending-${v.numEnd}" else v.numLabel))
            pending = null
        }
        out
    }
    val rowVerses = remember(verses, bookRows, state.showSanskrit) { if (state.showSanskrit) verses else bookRows.map { it.verse } }

    // Resume where the reader stopped.
    LaunchedEffect(skandha, adhyaya) {
        if (state.lastSkandha == skandha && state.lastAdhyaya == adhyaya && state.lastVerse > 1 && rowVerses.isNotEmpty()) {
            val i = rowVerses.indexOfLast { it.num <= state.lastVerse }
            if (i > 0) list.scrollToItem(i + 1)
        }
    }
    // Save the first visible shloka as the reading position, and note the chapter finished once its end is reached.
    LaunchedEffect(skandha, adhyaya, rowVerses) {
        snapshotFlow { list.firstVisibleItemIndex to list.layoutInfo.visibleItemsInfo.lastOrNull()?.index }.collectLatest { (first, last) ->
            delay(700)
            if (rowVerses.isEmpty()) return@collectLatest
            if (!playingHere && first >= 1) state.markRead(skandha, adhyaya, rowVerses[(first - 1).coerceIn(0, rowVerses.lastIndex)].num)
            if (last != null && last > rowVerses.size) state.markFinished(skandha, adhyaya)
        }
    }

    Box(
        Modifier.fillMaxSize().background(c.bg)
            // Pinch with two fingers to resize the text. Only multi-touch is taken, so scrolling is untouched.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.size >= 2) {
                            val zoom = event.calculateZoom()
                            if (zoom != 1f) {
                                state.updateTextScale((state.textScale * zoom).coerceIn(0.8f, 1.6f))
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            // Swipe sideways for the next or previous chapter.
            .pointerInput(skandha, adhyaya) {
                var dx = 0f
                detectHorizontalDragGestures(onDragStart = { dx = 0f }, onDragCancel = { dx = 0f }, onDragEnd = {
                    if (dx < -swipePx) next?.let { onNextChapter(it.first, it.second) }
                    else if (dx > swipePx) prev?.let { onPrevChapter(it.first, it.second) }
                }) { _, d -> dx += d }
            },
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            // Top bar
            Row(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(painterResource(Ic.ArrowBackIos), s.back, tint = c.accent) }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    val barAlpha by animateFloatAsState(if (titleInBar) 1f else 0f, tween(Motion.sheet), label = "barTitle")
                    if (barAlpha > 0f) {
                        Column(Modifier.graphicsLayer { alpha = barAlpha; translationY = (1f - barAlpha) * 8.dp.toPx() }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(title, fontFamily = readingFont(state.titleLang), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(reference, fontSize = 12.sp, color = c.secondary, maxLines = 1)
                        }
                    }
                }
                Box(Modifier.size(48.dp).clip(CircleShape).clickable { showSheet = true }, contentAlignment = Alignment.Center) {
                    Text("Aa", fontFamily = EnglishReading, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = c.accent)
                }
            }
            Crossfade(state.showSanskrit, animationSpec = tween(Motion.sheet), label = "layerBar") { sanskritOn ->
            if (sanskritOn) {
                LayerChips(state)
            } else {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().clip(Radius.group).background(c.surface)
                        .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(s.sanskritOff, Modifier.weight(1f), fontSize = 13.sp, lineHeight = 18.sp, color = c.secondary)
                    Box(
                        Modifier.clip(CircleShape).background(c.chipOn).clickable { state.updateShowSanskrit(true) }.padding(horizontal = 12.dp, vertical = 7.dp),
                    ) { Text(s.show, color = c.chipOnText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
            }

            LazyColumn(
                state = list,
                modifier = Modifier.fillMaxHeight().widthIn(max = 600.dp).fillMaxWidth(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(if (state.showSanskrit) 0.dp else 20.dp),
            ) {
                item { com.bhagavatam.app.ui.components.ChapterBanner(skandha, adhyaya) }
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (skandha == 1 && adhyaya == 1) Text("ॐ नमो भगवते वासुदेवाय", fontFamily = NotoDevanagari, fontSize = 16.sp, color = c.gold)
                        Text(reference, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.gold, letterSpacing = 0.3.sp)
                        Text(title, fontFamily = readingFont(state.titleLang), fontSize = 24.sp, lineHeight = 32.sp,
                            fontWeight = FontWeight.Medium, color = c.ink, textAlign = TextAlign.Center)
                        val count = SampleData.verseCount(skandha, adhyaya)
                        if (count > 0) Text(localDigits("$count ${s.shlokas.lowercase()}", ui), fontSize = 13.sp, color = c.secondary)
                        Box(Modifier.padding(top = 4.dp).width(40.dp).height(1.dp).background(c.gold.copy(alpha = 0.6f)))
                    }
                }
                if (verses.isNotEmpty() && !state.showSanskrit && bookRows.isEmpty()) {
                    item {
                        Text(translationMissing(ui, bookLang), Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(18.dp),
                            fontSize = 15.sp, lineHeight = 22.sp, color = c.secondary)
                    }
                }
                if (verses.isEmpty()) {
                    item {
                        Text(s.sampleOnly, Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(18.dp),
                            fontSize = 15.sp, lineHeight = 22.sp, color = c.secondary)
                    }
                }
                if (state.showSanskrit) {
                    items(verses, key = { it.ref }) { v ->
                        VerseBlock(state, v, playingHere && state.current.ref == v.ref, scale)
                    }
                } else {
                    items(bookRows, key = { it.verse.ref }) { r ->
                        BookParagraph(state, r.verse, r.label, bookLang, scale, r.verse.ref in peeked) {
                            if (r.verse.ref in peeked) peeked.remove(r.verse.ref) else peeked.add(r.verse.ref)
                        }
                    }
                }
                if (verses.isNotEmpty()) item(key = "end") { ChapterEndBlock(state, skandha, adhyaya, prev, next, onPrevChapter, onNextChapter) }
            }
        }

        if (state.hasSession || verses.isNotEmpty()) BottomScrim(Modifier.align(Alignment.BottomCenter), height = if (state.hasSession) 120.dp else 100.dp)
        // Action bar while text is selected: sits just above the play controls.
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = if (state.hasSession) 96.dp else 76.dp).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MeaningCard(state)
            SelectionBar(state)
        }
        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 10.dp)) {
            Crossfade(state.hasSession, animationSpec = tween(Motion.sheet), label = "playControl") { session ->
            if (session) {
                MiniPlayer(state, onOpen = onOpenPlayer)
            } else if (verses.isNotEmpty()) {
                val (prevLabel, nextLabel) = chapterLabels(ui)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChapterStep(Ic.ArrowBackIos, prevLabel, prev != null) { prev?.let { onPrevChapter(it.first, it.second) } }
                    Row(
                        Modifier.shadow(10.dp, CircleShape, ambientColor = Color(0x331C1A17), spotColor = Color(0x331C1A17))
                            .clip(CircleShape).background(Brand.Kesari).clickable(role = Role.Button) { state.playVerses(verses) }
                            .heightIn(min = 52.dp).padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(painterResource(Ic.PlayArrow), null, tint = Brand.OnKesari)
                        Text(s.listen, color = Brand.OnKesari, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                    ChapterStep(Ic.KeyboardArrowRight, nextLabel, next != null) { next?.let { onNextChapter(it.first, it.second) } }
                }
            }
            }
        }
        WebPanel(state, Modifier.align(Alignment.BottomCenter))
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }, containerColor = Brand.Paper) {
            ReaderSettings(state)
        }
    }
    AnnotationSheet(state)
    WordSheet(state) { sk, a -> onNextChapter(sk, a) }
}

@Composable
private fun LayerChips(state: AppState) {
    val c = LocalReaderColors.current
    val all = listOf(
        Triple("${state.strings.mool}", true, { state.updateShowSanskrit(false) }),
        Triple("IAST", state.showIast, { state.updateShowIast(!state.showIast) }),
        Triple("हिन्दी", state.showHi, { state.updateShowHi(!state.showHi) }),
        Triple("বাংলা", state.showBn, { state.updateShowBn(!state.showBn) }),
        Triple("English", state.showEn, { state.updateShowEn(!state.showEn) }),
    )
    val allFonts = listOf(null, null, TiroHindi, NotoSerifBengali, null)
    val keep = all.indices.filter { BENGALI_READY || it != 3 }
    val chips = keep.map { all[it] }
    val fonts = keep.map { allFonts[it] }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEachIndexed { i, (label, on, click) ->
            val fill by animateColorAsState(if (on) c.chipOn else c.chipOn.copy(alpha = 0f), tween(Motion.press), label = "chipFill")
            val edge by animateColorAsState(if (on) c.chipOn else c.separator, tween(Motion.press), label = "chipEdge")
            Box(Modifier.heightIn(min = 48.dp).clip(CircleShape).clickable(onClick = click), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.clip(CircleShape)
                        .background(fill)
                        .border(1.dp, edge, CircleShape)
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                ) {
                    Text(label, Modifier.opticallyCentred(fonts[i]), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = fonts[i], color = if (on) c.chipOnText else c.secondary)
                }
            }
        }
    }
}

private data class BookRow(val verse: Verse, val label: String)

/** Shown in book mode when the chosen reading language has nothing for this chapter yet. */
private fun translationMissing(ui: Lang, lang: Lang): String = when {
    lang == Lang.BN && ui == Lang.HI -> "इस अध्याय का बंगाली अनुवाद अभी उपलब्ध नहीं है। यह अध्याय-दर-अध्याय जोड़ा जा रहा है।"
    lang == Lang.BN && ui == Lang.BN -> "এই অধ্যায়ের বাংলা অনুবাদ এখনও পাওয়া যায়নি। অধ্যায় ধরে ধরে যোগ করা হচ্ছে।"
    lang == Lang.BN -> "The Bengali translation of this chapter is not available yet. It is being added chapter by chapter."
    else -> "This translation is not available for this chapter."
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VerseBlock(state: AppState, v: Verse, isCurrent: Boolean, scale: Float) {
    val c = LocalReaderColors.current
    val ui = state.uiLang
    val marked = v.ref in state.bookmarks
    val tint by animateColorAsState(if (isCurrent) c.playing else c.playing.copy(alpha = 0f), tween(Motion.sheet), label = "playingTint")
    // Layers whose translation is printed with another shloka of the group are left out here; the carrier shows it once.
    val layers = state.alongsideLayers().filter { v.hasText(it) }
    val showIast = state.showIast && state.script != SanskritScript.IAST
    Column(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().clip(Radius.group).background(tint)
                .combinedClickable(onClick = {}, onLongClickLabel = if (marked) "Remove bookmark" else "Bookmark this shloka", onLongClick = { state.toggleBookmark(v.ref) })
                .padding(horizontal = 14.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Reference on the left, speaker on the right.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(localDigits("${v.skandha}.${v.adhyaya}.${v.numLabel}", ui), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.gold)
                Box(Modifier.weight(1f))
                if (v.speaker != null) Text(v.speaker, fontFamily = NotoDevanagari, fontSize = 13.sp, color = c.secondary)
                if (isCurrent) Icon(painterResource(Ic.Headphones), null, tint = c.accent, modifier = Modifier.size(15.dp))
                // Press and hold anywhere on the shloka saves it; the mark only shows once saved.
                if (marked) {
                    IconButton(onClick = { state.toggleBookmark(v.ref) }, modifier = Modifier.size(40.dp)) {
                        Icon(painterResource(Ic.Bookmark), "Remove bookmark", tint = c.accent, modifier = Modifier.size(20.dp))
                    }
                }
            }
            // The shloka and its transliteration are centred, like a printed verse.
            ShlokaText(v, state.script, c.shloka, 20f * scale, center = true)
            if (showIast) {
                Text(v.iast.joinToString("\n"), Modifier.fillMaxWidth(), fontFamily = EnglishReading, fontStyle = FontStyle.Italic, fontSize = (15 * scale).sp,
                    lineHeight = (24 * scale * state.lineScale).sp, color = c.secondary, textAlign = TextAlign.Center)
            }
            if (layers.isNotEmpty()) Box(Modifier.align(Alignment.CenterHorizontally).width(28.dp).height(1.dp).background(c.gold.copy(alpha = 0.5f)))
            // Translations read left-aligned; each is named when more than one is shown.
            layers.forEach { l ->
                val big = l == Lang.HI || l == Lang.BN
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (layers.size > 1) Text(layerName(l), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = c.secondary)
                    val body = v.translation(l)
                    val style = TextStyle(fontFamily = readingFont(l), fontSize = ((if (big) 18 else 17) * scale).sp,
                        lineHeight = ((if (big) 32 else 27) * scale * state.lineScale).sp, color = c.ink)
                    // Pointers such as "translated together with verse 5" are not text to mark.
                    if (v.hasText(l)) MarkedText(state, v.ref, l, body, style, Modifier.fillMaxWidth())
                    else Text(body, Modifier.fillMaxWidth(), style = style)
                }
            }
        }
        HorizontalDivider(Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp, color = c.separator)
    }
}

private fun layerName(l: Lang) = when (l) { Lang.HI -> "हिन्दी"; Lang.BN -> "বাংলা"; Lang.SA -> "संस्कृत"; Lang.EN -> "English" }

private fun chapterLabels(ui: Lang) = when (ui) {
    Lang.HI -> "पिछला अध्याय" to "अगला अध्याय"
    Lang.BN -> "আগের অধ্যায়" to "পরের অধ্যায়"
    else -> "Previous chapter" to "Next chapter"
}

/** A round previous or next chapter button; dimmed and inert at either end of the book. */
@Composable
private fun ChapterStep(icon: Int, label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = LocalReaderColors.current
    Box(
        Modifier.size(48.dp).shadow(6.dp, CircleShape, ambientColor = Color(0x331C1A17), spotColor = Color(0x331C1A17))
            .clip(CircleShape).background(c.surface).border(1.dp, c.separator, CircleShape)
            .clickable(enabled = enabled, onClickLabel = label, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(painterResource(icon), label, tint = if (enabled) c.ink else c.secondary.copy(alpha = 0.4f), modifier = Modifier.size(22.dp)) }
}

@Composable
private fun BookParagraph(state: AppState, v: Verse, label: String, lang: Lang, scale: Float, peek: Boolean, onToggle: () -> Unit) {
    val c = LocalReaderColors.current
    val ui = state.uiLang
    val big = lang == Lang.HI || lang == Lang.BN
    Column(Modifier.animateContentSize(tween(Motion.sheet)).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Tap a word for its meaning, tap the verse number for the shloka, press and hold to select and mark.
        MarkedText(
            state, v.ref, lang, v.translation(lang),
            TextStyle(fontFamily = readingFont(lang), fontSize = ((if (big) 19 else 18) * scale).sp, lineHeight = ((if (big) 36 else 31) * scale * state.lineScale).sp, color = c.ink),
            Modifier.fillMaxWidth(),
            prefix = buildAnnotatedString {
                withStyle(SpanStyle(color = c.gold, fontWeight = FontWeight.Bold, fontSize = 12.sp, baselineShift = BaselineShift.Superscript)) {
                    append(localDigits(label, ui) + "  ")
                }
            },
            onPrefixTap = onToggle,
        )
        if (peek) {
            Column(
                Modifier.fillMaxWidth().clip(Radius.group).background(c.playing).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(localDigits("${state.strings.shloka} ${v.skandha}.${v.adhyaya}.${label}", ui), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.gold)
                val script = if (ui == Lang.BN && state.script == SanskritScript.DEVANAGARI) SanskritScript.BENGALI else state.script
                ShlokaText(v, script, c.shloka, 16f * scale)
            }
        }
    }
}

/** After the last shloka: the "Thus ends ..." colophon, the Gita Press notes (collapsed), then previous and next chapter. */
@Composable
private fun ChapterEndBlock(
    state: AppState, skandha: Int, adhyaya: Int, prev: Pair<Int, Int>?, next: Pair<Int, Int>?,
    onPrev: (Int, Int) -> Unit, onNext: (Int, Int) -> Unit,
) {
    val c = LocalReaderColors.current
    val ui = state.uiLang
    val lang = state.titleLang
    val end = remember(skandha, adhyaya, lang) { SampleData.chapterEnd(skandha, adhyaya, lang) }
    val font = if (lang == Lang.HI) TiroHindi else EnglishReading
    var showNotes by remember(skandha, adhyaya) { mutableStateOf(false) }
    val notesLabel = when (ui) { Lang.HI -> "\u091f\u093f\u092a\u094d\u092a\u0923\u093f\u092f\u093e\u0901"; Lang.BN -> "\u099f\u09c0\u0995\u09be"; else -> "Notes" }
    val (prevLabel, nextLabel) = chapterLabels(ui)
    Column(Modifier.fillMaxWidth().animateContentSize(tween(Motion.sheet)).padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.width(40.dp).height(1.dp).background(c.gold.copy(alpha = 0.6f)))
        if (end.colophon.isNotEmpty()) {
            Text(end.colophon, Modifier.padding(horizontal = 8.dp), fontFamily = font, fontSize = (14 * state.textScale).sp, lineHeight = (22 * state.textScale).sp,
                color = c.secondary, textAlign = TextAlign.Center)
        }
        if (end.notes.isNotEmpty()) {
            Row(
                Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).clickable { showNotes = !showNotes }.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(notesLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.accent)
                Text(if (showNotes) "\u25b4" else "\u25be", fontSize = 14.sp, color = c.accent)
            }
            if (showNotes) {
                Text(end.notes, Modifier.fillMaxWidth().clip(Radius.group).background(c.surface).padding(14.dp),
                    fontFamily = font, fontSize = (14 * state.textScale).sp, lineHeight = (22 * state.textScale).sp, color = c.secondary)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChapterButton(state, prevLabel, prev, filled = false, back = true, modifier = Modifier.weight(1f)) { prev?.let { onPrev(it.first, it.second) } }
            ChapterButton(state, nextLabel, next, filled = true, back = false, modifier = Modifier.weight(1f)) { next?.let { onNext(it.first, it.second) } }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Previous or next chapter at the end of the text: what it is, and the chapter's own title. */
@Composable
private fun ChapterButton(state: AppState, label: String, target: Pair<Int, Int>?, filled: Boolean, back: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalReaderColors.current
    val enabled = target != null
    val ink = if (filled) Brand.OnKesari else c.ink
    val sub = if (filled) Brand.OnKesari else c.secondary
    Row(
        modifier.heightIn(min = 64.dp)
            .then(if (enabled) Modifier.tappable(Radius.group, label, onClick = onClick) else Modifier.clip(Radius.group))
            .background(if (filled) Brand.Kesari else c.surface)
            .border(1.dp, if (filled) Brand.Kesari else c.separator, Radius.group)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .alpha(if (enabled) 1f else 0.4f),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (back) Icon(painterResource(Ic.ArrowBackIos), null, tint = ink, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f), horizontalAlignment = if (back) Alignment.Start else Alignment.End) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ink, maxLines = 1)
            if (target != null) Text(
                SampleData.adhyayaTitle(target.first, target.second, state.titleLang, state.strings),
                fontFamily = readingFont(state.titleLang), fontSize = 12.sp, color = sub, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (!back) Icon(painterResource(Ic.KeyboardArrowRight), null, tint = ink, modifier = Modifier.size(20.dp))
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
                    ) { Text("Aa", fontFamily = EnglishReading, fontSize = 17.sp, color = t.colors.ink) }
                    Text(s.themes[i], fontSize = 12.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium, color = if (on) Brand.Ink else Brand.Secondary)
                }
            }
        }
        Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("A", fontFamily = EnglishReading, fontSize = 14.sp, color = Brand.Secondary)
            AppSlider(value = state.textScale, onValueChange = state::updateTextScale, valueRange = 0.8f..1.6f, modifier = Modifier.weight(1f))
            Text("A", fontFamily = EnglishReading, fontSize = 22.sp, color = Brand.Secondary)
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
                    fonts = listOf(NotoDevanagari, NotoSerifBengali, EnglishReading),
                )
            }
        }
        VSpace(4)
        HorizontalDivider(color = Color.Transparent)
    }
}
