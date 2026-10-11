package com.bhagavatam.app.ui.screens

import android.app.SearchManager
import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.AnnDraft
import com.bhagavatam.app.data.AnnKind
import com.bhagavatam.app.data.Annotation
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.data.occurrencesOf
import com.bhagavatam.app.data.tr
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.components.Ic
import com.bhagavatam.app.ui.theme.LocalReaderColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The four highlight colours. */
val MarkColours = listOf(0xFFE8B931L, 0xFF5FAE6BL, 0xFF4F9BD9L, 0xFFD9577EL)

@Composable
internal fun TextAction(label: String, onClick: () -> Unit) {
    val c = LocalReaderColors.current
    Box(
        Modifier
            .heightIn(min = 34.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1)
    }
}

/** Shown above reader controls when text is selected: highlight colours, note, bookmark, meaning, copy. */
@Composable
fun SelectionBar(state: AppState, modifier: Modifier = Modifier, framed: Boolean = true) {
    val sel = state.selBar ?: return
    val c = LocalReaderColors.current
    val ui = state.uiLang
    val clipboard = LocalClipboardManager.current
    fun add(kind: AnnKind, colour: Long) {
        state.addAnnotation(Annotation(0, sel.ref, sel.lang.code, sel.start, sel.end, sel.quote, kind, colour, "", System.currentTimeMillis()))
        state.selBar = null
        sel.clear()
    }
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier
            .then(if (framed) Modifier.shadow(6.dp, shape, ambientColor = Color(0x331C1A17), spotColor = Color(0x331C1A17)).clip(shape).background(c.surface).border(1.dp, c.separator, shape) else Modifier)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        MarkColours.forEach { col ->
            Box(
                Modifier
                    .size(34.dp)
                    .clickable(role = Role.Button, onClickLabel = tr(ui, "Highlight", "हाइलाइट", "হাইলাইট")) { add(AnnKind.HIGHLIGHT, col) },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(18.dp).clip(CircleShape).background(Color(col)))
            }
        }
        Box(Modifier.width(1.dp).height(16.dp).background(c.separator))
        TextAction(tr(ui, "Note", "नोट", "নোট")) {
            state.annDraft = AnnDraft(null, sel.ref, sel.lang.code, sel.start, sel.end, sel.quote)
            state.selBar = null
            sel.clear()
        }
        TextAction(tr(ui, "Bookmark", "बुकमार्क", "বুকমার্ক")) { add(AnnKind.BOOKMARK, 0L) }
        TextAction(tr(ui, "Meaning", "अर्थ", "অর্থ")) {
            state.lookup = com.bhagavatam.app.data.WordLookup(sel.quote.trim().take(80), sel.lang, sel.ref)
            state.selBar = null
            sel.clear()
        }
        TextAction(tr(ui, "Copy", "कॉपी", "কপি")) {
            clipboard.setText(AnnotatedString(sel.quote))
            state.selBar = null
            sel.clear()
        }
    }
}

/** Edit or create a note: the quoted text, a text box, the colour, and delete when it already exists. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnotationSheet(state: AppState) {
    val d = state.annDraft ?: return
    val c = LocalReaderColors.current
    val ui = state.uiLang
    val existing = d.existing
    var note by remember(d) { mutableStateOf(existing?.note.orEmpty()) }
    var colour by remember(d) { mutableStateOf(existing?.colour?.takeIf { it != 0L } ?: MarkColours[state.defaultMark.coerceIn(0, MarkColours.lastIndex)]) }
    val close = { state.annDraft = null }
    fun save() {
        val kind = existing?.kind ?: AnnKind.NOTE
        val a = (existing ?: Annotation(0, d.ref, d.layer, d.start, d.end, d.quote, kind, colour, "", System.currentTimeMillis()))
            .copy(note = note.trim(), colour = if (kind == AnnKind.BOOKMARK) 0L else colour)
        if (existing == null) { if (a.note.isNotEmpty()) state.addAnnotation(a) } else state.updateAnnotation(a)
        close()
    }
    ModalBottomSheet(onDismissRequest = { save() }, containerColor = c.bg) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(localDigits(d.ref, ui), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.gold)
            Text(d.quote, fontSize = 16.sp, lineHeight = 24.sp, fontStyle = FontStyle.Italic, color = c.secondary, maxLines = 5)
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                placeholder = { Text(tr(ui, "Your note (stays on this phone)", "आपका नोट (केवल इसी फ़ोन में रहता है)", "আপনার নোট (শুধু এই ফোনে থাকে)")) },
            )
            if (existing?.kind != AnnKind.BOOKMARK) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MarkColours.forEach { col ->
                    Box(Modifier.size(44.dp).clickable(role = Role.RadioButton) { colour = col }, contentAlignment = Alignment.Center) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(Color(col)).then(if (col == colour) Modifier.border(2.dp, c.ink, CircleShape) else Modifier))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextAction(tr(ui, "Save", "सहेजें", "সংরক্ষণ")) { save() }
                if (existing != null) TextAction(tr(ui, "Delete", "हटाएँ", "মুছুন")) { state.removeAnnotation(existing.id); close() }
            }
        }
    }
}

/**
 * Fast, compact, offline-first card above the reading controls for a pressed or tapped word.
 * Shows the book's own glossary entry if there is one, then the offline dictionary, then online Wiktionary.
 */
@Composable
fun MeaningCard(
    state: AppState,
    modifier: Modifier = Modifier,
    framed: Boolean = true,
    onOpenDictionary: (() -> Unit)? = null
) {
    val w = state.lookup ?: return
    val c = LocalReaderColors.current
    val ui = state.uiLang
    val clipboard = LocalClipboardManager.current
    val term = remember(w) { SampleData.glossary.firstOrNull { it.term.equals(w.word, ignoreCase = true) || it.dev == w.word } }
    var retryKey by remember { mutableStateOf(0) }

    // 1. Offline dictionary lookup
    val offlineEntries by produceState<List<com.bhagavatam.app.data.DictEntry>>(emptyList(), w, state.meaningLangMode, retryKey) {
        value = withContext(Dispatchers.IO) {
            com.bhagavatam.app.data.Dictionary.lookup(
                word = w.word,
                lang = w.lang.code,
                preferredGlossLang = state.preferredMeaningLang(w.lang.code)
            )
        }
    }

    // 2. Online meaning fallback (only when offline has no hit and setting is enabled)
    val onlineMeaning by produceState<com.bhagavatam.app.data.Meaning?>(null, w, state.onlineMeanings, offlineEntries, retryKey) {
        value = if (offlineEntries.isEmpty() && state.onlineMeanings && !w.word.contains(' ')) {
            com.bhagavatam.app.data.OnlineMeaning.lookup(w.word, w.lang)
        } else null
    }

    val isSaved = state.isWordSaved(w.word, w.lang.code)
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier
            .fillMaxWidth()
            .then(if (framed) Modifier.shadow(8.dp, shape, ambientColor = Color(0x331C1A17), spotColor = Color(0x331C1A17)).clip(shape).background(c.surface).border(1.dp, c.separator, shape) else Modifier)
            .animateContentSize()
            .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    w.word,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = readingFont(w.lang)
                )
                if (term != null && term.dev.isNotEmpty() && term.dev != term.term) {
                    Text(term.dev, fontSize = 12.sp, color = c.gold, fontWeight = FontWeight.Medium)
                }
            }
            // Save word button
            IconButton(
                onClick = { state.toggleSavedWord(w.word, w.lang.code, w.ref) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    painterResource(Ic.Bookmark),
                    contentDescription = "Save word",
                    tint = if (isSaved) c.gold else c.secondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = { clipboard.setText(AnnotatedString(w.word)) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(painterResource(Ic.Copy), contentDescription = "Copy", tint = c.secondary, modifier = Modifier.size(16.dp))
            }
            IconButton(
                onClick = { state.lookup = null },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(painterResource(Ic.Close), contentDescription = tr(ui, "Close", "बंद करें", "বন্ধ করুন"), tint = c.secondary, modifier = Modifier.size(16.dp))
            }
        }

        if (term != null) {
            Text(term.meaning, fontSize = 14.sp, lineHeight = 20.sp, color = c.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
        } else if (offlineEntries.isNotEmpty()) {
            val first = offlineEntries.first()
            if (!first.headword.equals(w.word, ignoreCase = true)) {
                Text(
                    tr(ui, "Base word: ", "मूल शब्द: ", "মূল শব্দ: ") + first.headword,
                    fontSize = 12.sp,
                    color = c.gold,
                    fontWeight = FontWeight.Medium,
                    fontFamily = readingFont(w.lang)
                )
            }
            val sensesToShow = first.senses.take(3)
            sensesToShow.forEach { sense ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (first.pos.isNotBlank()) Text("[${first.pos}]", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.gold)
                    Text(
                        sense.gloss,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = c.ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = readingFont(w.lang)
                    )
                }
            }
            if (first.isCrossScript) {
                Text(
                    tr(ui, "(Sanskrit origin - also in Hindi)", "(संस्कृत मूल - हिन्दी में भी)", "(সংস্কৃত মূল - হিন্দিতেও)"),
                    fontSize = 11.sp,
                    color = c.secondary
                )
            }
            Text(
                (first.source?.name ?: "Wiktionary") + " · CC BY-SA 4.0",
                fontSize = 11.sp,
                color = c.secondary
            )
        } else {
            val m = onlineMeaning
            when {
                w.word.contains(' ') -> {
                    Text(tr(ui, "Select a single word to see its dictionary meaning.", "शब्दकोश अर्थ के लिए एक शब्द चुनें।", "অভিধান অর্থের জন্য একটি শব্দ নির্বাচন করুন।"), fontSize = 13.sp, color = c.secondary)
                }
                m == null && state.onlineMeanings -> {
                    Text(tr(ui, "Looking up meaning...", "अर्थ खोज रहे हैं...", "অর্থ খোঁজা হচ্ছে..."), fontSize = 13.sp, color = c.secondary)
                }
                m is com.bhagavatam.app.data.Meaning.Found -> {
                    m.entries.take(2).forEach { e ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (e.pos.isNotBlank()) Text("[${e.pos}]", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.gold)
                            Text(e.defs.take(1).joinToString(" "), fontSize = 13.sp, lineHeight = 18.sp, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Text("Wiktionary · CC BY-SA", fontSize = 11.sp, color = c.secondary)
                }
                m is com.bhagavatam.app.data.Meaning.Offline -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(tr(ui, "Offline", "ऑफ़लाइन", "অফলাইন"), fontSize = 13.sp, color = c.secondary)
                        TextAction(tr(ui, "Retry", "पुन: प्रयास", "আবার চেষ্টা")) { retryKey++ }
                    }
                }
                else -> {
                    Text(tr(ui, "No dictionary entry found.", "कोई प्रविष्टि नहीं मिली।", "কোনো অর্থ পাওয়া যায়নি।"), fontSize = 13.sp, color = c.secondary)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onOpenDictionary != null) {
                TextAction(tr(ui, "Open in dictionary", "शब्दकोश में देखें", "অভিধানে দেখুন")) {
                    onOpenDictionary()
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }
            TextAction(tr(ui, "Occurrences in Granth", "ग्रंथ में खोजें", "গ্রন্থে দেখুন")) {
                state.showWordSheet = true
            }
        }
    }
}

/** A tapped word: glossary definition, occurrences in the book, and dictionary options. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordSheet(
    state: AppState,
    onOpenChapter: (Int, Int) -> Unit,
    onOpenDictionary: (() -> Unit)? = null
) {
    if (!state.showWordSheet) return
    val w = state.lookup ?: return
    val c = LocalReaderColors.current
    val ui = state.uiLang
    val ctx = LocalContext.current
    val found by produceState<Pair<Int, List<com.bhagavatam.app.data.Occurrence>>?>(null, w) {
        value = withContext(Dispatchers.Default) { occurrencesOf(w.word, w.lang) }
    }
    val term = remember(w) {
        SampleData.glossary.firstOrNull {
            it.term.equals(w.word, ignoreCase = true) || it.dev == w.word
        }
    }
    val close = { state.showWordSheet = false; state.lookup = null }
    ModalBottomSheet(onDismissRequest = close, containerColor = c.bg) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(w.word, fontSize = 24.sp, fontWeight = FontWeight.Medium, color = c.ink, fontFamily = readingFont(w.lang))
            if (term != null) {
                Text("${term.term} · ${term.dev}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.gold)
                Text(term.meaning, fontSize = 16.sp, lineHeight = 24.sp, color = c.ink)
            }
            if (onOpenDictionary != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextAction(tr(ui, "Open in Dictionary", "शब्दकोश में देखें", "অভিধানে দেখুন")) {
                        close()
                        onOpenDictionary()
                    }
                }
            }
            val f = found
            Text(
                when {
                    f == null -> tr(ui, "Searching the granth...", "ग्रंथ में खोज रहे हैं...", "গ্রন্থে খোঁজা হচ্ছে...")
                    f.first == 0 -> tr(ui, "Not found elsewhere in the granth in this form.", "इस रूप में ग्रंथ में और कहीं नहीं मिला।", "এই রূপে গ্রন্থের আর কোথাও পাওয়া যায়নি।")
                    else -> tr(ui, "In this granth (${f.first})", "इस ग्रंथ में (${localDigits("${f.first}", ui)})", "এই গ্রন্থে (${localDigits("${f.first}", ui)})")
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = c.secondary,
            )
            f?.second?.forEach { o ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { close(); onOpenChapter(o.skandha, o.adhyaya) }
                        .padding(vertical = 8.dp),
                ) {
                    Text(localDigits(o.ref, ui), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.gold)
                    Text(o.snippet, fontSize = 15.sp, lineHeight = 22.sp, color = c.ink)
                }
            }
        }
    }
}

/** One compact panel above the reading controls: the word and its meaning on top, the mark actions underneath. */
@Composable
fun SelectionPanel(
    state: AppState,
    modifier: Modifier = Modifier,
    onOpenDictionary: (() -> Unit)? = null
) {
    if (state.lookup == null && state.selBar == null) return
    val c = LocalReaderColors.current
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.fillMaxWidth()
            .shadow(8.dp, shape, ambientColor = Color(0x331C1A17), spotColor = Color(0x331C1A17))
            .clip(shape).background(c.surface).border(1.dp, c.separator, shape),
    ) {
        if (state.lookup != null) MeaningCard(state, framed = false, onOpenDictionary = onOpenDictionary)
        if (state.lookup != null && state.selBar != null) Box(Modifier.fillMaxWidth().height(1.dp).background(c.separator))
        if (state.selBar != null) SelectionBar(state, framed = false)
    }
}

