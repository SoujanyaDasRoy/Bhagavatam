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
import com.bhagavatam.app.data.Corrections
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.data.occurrencesOf
import com.bhagavatam.app.data.sentenceAround
import com.bhagavatam.app.data.trimRange
import com.bhagavatam.app.data.SelBar
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
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1)
    }
}

/** Shown above reader controls when text is selected: highlight colours, widen the selection, note, bookmark, meaning, copy. */
@Composable
fun SelectionBar(state: AppState, modifier: Modifier = Modifier, framed: Boolean = true) {
    val sel = state.selBar ?: return
    val c = LocalReaderColors.current
    val ui = state.uiLang
    val clipboard = LocalClipboardManager.current
    fun done() { state.selBar = null; sel.clear() }
    fun add(kind: AnnKind, colour: Long) {
        val id = state.addAnnotation(Annotation(0, sel.ref, sel.lang.code, sel.start, sel.end, sel.quote, kind, colour, "", System.currentTimeMillis()))
        done()
        state.toast(
            if (kind == AnnKind.BOOKMARK) tr(ui, "Passage bookmarked", "अंश बुकमार्क हुआ", "অংশ বুকমার্ক হয়েছে", "ଅଂଶ ବୁକମାର୍କ ହେଲା") else tr(ui, "Highlighted", "हाइलाइट हुआ", "হাইলাইট হয়েছে", "ହାଇଲାଇଟ୍ ହେଲା"),
            tr(ui, "Undo", "पूर्ववत", "ফিরিয়ে নিন", "ପୂର୍ବବତ୍ କରନ୍ତୁ"),
        ) { state.removeAnnotation(id) }
    }
    /** Widen the chosen span to [r] of the verse text; the bar stays open so a colour can be picked. */
    fun widen(r: IntRange) {
        if (r.isEmpty() || sel.text.isEmpty()) return
        state.selBar = SelBar(sel.ref, sel.lang, r.first, r.last + 1, sel.text.substring(r.first, r.last + 1), sel.text, sel.clear)
        state.lookup = null
    }
    val shape = RoundedCornerShape(20.dp)
    val colourNames = List(4) { state.markName(it) }
    Row(
        modifier
            .then(if (framed) Modifier.shadow(6.dp, shape, ambientColor = Color(0x331C1A17), spotColor = Color(0x331C1A17)).clip(shape).background(c.surface).border(1.dp, c.separator, shape) else Modifier)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MarkColours.forEachIndexed { i, col ->
            Box(
                Modifier
                    .size(48.dp)
                    .clickable(role = Role.Button, onClickLabel = tr(ui, "Highlight ", "हाइलाइट ", "হাইলাইট ", "ହାଇଲାଇଟ୍ ") + colourNames[i]) { add(AnnKind.HIGHLIGHT, col) },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(Color(col)))
            }
        }
        Box(Modifier.width(1.dp).height(20.dp).background(c.separator))
        if (sel.text.isNotEmpty()) {
            TextAction(tr(ui, "Sentence", "वाक्य", "বাক্য", "ବାକ୍ଯ")) { widen(sentenceAround(sel.text, sel.start, sel.end)) }
            TextAction(tr(ui, "All", "पूरा", "সব", "ସବୁ")) { widen(trimRange(sel.text, 0, sel.text.length)) }
            Box(Modifier.width(1.dp).height(20.dp).background(c.separator))
        }
        TextAction(tr(ui, "Note", "नोट", "নোট", "ନୋଟ୍")) {
            state.annDraft = AnnDraft(null, sel.ref, sel.lang.code, sel.start, sel.end, sel.quote)
            done()
        }
        TextAction(tr(ui, "Bookmark", "बुकमार्क", "বুকমার্ক", "ବୁକମାର୍କ")) { add(AnnKind.BOOKMARK, 0L) }
        if (sel.quote.none { it.isWhitespace() }) TextAction(tr(ui, "Meaning", "अर्थ", "অর্থ", "ଅର୍ଥ")) {
            state.lookup = com.bhagavatam.app.data.WordLookup(sel.quote.trim().take(80), sel.lang, sel.ref)
            state.selBar = null
            sel.clear()
        }
        TextAction(tr(ui, "Copy", "कॉपी", "কপি", "କପି")) {
            clipboard.setText(AnnotatedString(sel.quote))
            done()
            state.toast(tr(ui, "Copied", "कॉपी हुआ", "কপি হয়েছে", "କପି ହେଲା"))
        }
        TextAction(tr(ui, "Shloka", "श्लोक", "শ্লোক", "ଶ୍ଳୋକ")) { state.verseSheet = sel.ref; done() }
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
            Text(localDigits(d.ref, ui), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.gold)
            Text(d.quote, fontSize = 16.sp, lineHeight = 24.sp, fontStyle = FontStyle.Italic, color = c.secondary, maxLines = 5)
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                placeholder = { Text(tr(ui, "Your note (stays on this phone)", "आपका नोट (केवल इसी फ़ोन में रहता है)", "আপনার নোট (শুধু এই ফোনে থাকে)", "ଆପଣଙ୍କ ନୋଟ୍ (ଏହି ଫୋନରେ ରହେ)")) },
            )
            if (existing?.kind != AnnKind.BOOKMARK) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MarkColours.forEach { col ->
                    Box(Modifier.size(48.dp).clickable(role = Role.RadioButton) { colour = col }, contentAlignment = Alignment.Center) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(Color(col)).then(if (col == colour) Modifier.border(2.dp, c.ink, CircleShape) else Modifier))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextAction(tr(ui, "Save", "सहेजें", "সংরক্ষণ", "ସେଭ୍ କରନ୍ତୁ")) { save() }
                if (existing != null) TextAction(tr(ui, "Delete", "हटाएँ", "মুছুন", "ବିଲୋପ କରନ୍ତୁ")) { state.removeAnnotation(existing.id); close() }
            }
        }
    }
}

/**
 * Fast, compact, offline-first card above the reading controls for a pressed or tapped word.
 * Shows the book's own glossary entry if there is one, then the offline dictionary, then online Wiktionary.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
    val context = LocalContext.current

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
    // Online lookup is off unless the reader turned it on in Settings; with it off, this card can ask once for this one word.
    var onceOnline by remember(w) { mutableStateOf(false) }
    val allowOnline = state.onlineMeanings || onceOnline
    val onlineMeaning by produceState<com.bhagavatam.app.data.Meaning?>(null, w, allowOnline, offlineEntries, retryKey) {
        value = if (offlineEntries.isEmpty() && allowOnline && !w.word.contains(' ')) {
            com.bhagavatam.app.data.OnlineMeaning.lookup(w.word, w.lang)
        } else null
    }

    var more by remember(w) { mutableStateOf(false) }
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
                if (w.lang == com.bhagavatam.app.data.Lang.SA && w.word.any { it in 'ऀ'..'ॿ' }) {
                    Text(com.bhagavatam.app.util.Transliterate.toIast(w.word), fontSize = 15.sp, color = c.gold, fontStyle = FontStyle.Italic, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (term != null && term.dev.isNotEmpty() && term.dev != term.term) {
                    Text(term.dev, fontSize = 15.sp, color = c.gold, fontWeight = FontWeight.Medium)
                }
            }
            // Save word button
            IconButton(
                onClick = { state.toggleSavedWord(w.word, w.lang.code, w.ref) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painterResource(Ic.Bookmark),
                    contentDescription = if (isSaved) tr(ui, "Remove saved word", "सहेजा शब्द हटाएँ", "সংরক্ষিত শব্দ সরান", "ସେଭ୍ ହୋଇଥିବା ଶବ୍ଦ ହଟାନ୍ତୁ") else tr(ui, "Save word", "शब्द सहेजें", "শব্দ সংরক্ষণ করুন", "ଶବ୍ଦ ସେଭ୍ କରନ୍ତୁ"),
                    tint = if (isSaved) c.gold else c.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (Corrections.enabled) IconButton(onClick = {
                Corrections.open(context, w.ref, w.word)
                android.widget.Toast.makeText(context, tr(ui, "Details copied. Paste them in the chat.", "विवरण कॉपी हो गया। चैट में पेस्ट करें।", "বিবরণ কপি হয়েছে। চ্যাটে পেস্ট করুন।", "ବିବରଣୀ କପି ହେଲା। ଚାଟ୍‌ରେ ପେଷ୍ଟ୍ କରନ୍ତୁ।"), android.widget.Toast.LENGTH_LONG).show()
            }, modifier = Modifier.size(48.dp)) {
                Icon(painterResource(Ic.MessageSquare), contentDescription = tr(ui, "Report an error", "त्रुटि बताएँ", "ভুল জানান", "ତ୍ରୁଟି ଜଣାନ୍ତୁ"), tint = c.secondary, modifier = Modifier.size(20.dp))
            }
            IconButton(
                onClick = { clipboard.setText(AnnotatedString(w.word)) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(painterResource(Ic.Copy), contentDescription = tr(ui, "Copy", "कॉपी", "কপি", "କପି"), tint = c.secondary, modifier = Modifier.size(20.dp))
            }
            IconButton(
                onClick = { state.lookup = null },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(painterResource(Ic.Close), contentDescription = tr(ui, "Close", "बंद करें", "বন্ধ করুন", "ବନ୍ଦ କରନ୍ତୁ"), tint = c.secondary, modifier = Modifier.size(20.dp))
            }
        }

        if (term != null) {
            Text(term.meaning, fontSize = 15.sp, lineHeight = 20.sp, color = c.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
        } else if (offlineEntries.isNotEmpty()) {
            val first = offlineEntries.first()
            if (!first.headword.equals(w.word, ignoreCase = true)) {
                Text(
                    tr(ui, "Base word: ", "मूल शब्द: ", "মূল শব্দ: ", "ମୂଳ ଶବ୍ଦ: ") + first.headword,
                    fontSize = 15.sp,
                    color = c.gold,
                    fontWeight = FontWeight.Medium,
                    fontFamily = readingFont(w.lang)
                )
            }
            val sensesToShow = first.senses.take(if (more) 8 else 3)
            sensesToShow.forEach { sense ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (first.pos.isNotBlank()) Text("[${first.pos}]", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.gold)
                    Text(
                        sense.gloss,
                        fontSize = 15.sp, lineHeight = 24.sp,
                        color = c.ink,
                        maxLines = if (more) 6 else 2,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = readingFont(w.lang)
                    )
                }
            }
            if (!more && first.senses.size > 3) TextAction(tr(ui, "More meanings", "और अर्थ", "আরও অর্থ", "ଅଧିକ ଅର୍ଥ")) { more = true }
            if (first.isCrossScript) {
                Text(
                    tr(ui, "(Sanskrit origin - also in Hindi)", "(संस्कृत मूल - हिन्दी में भी)", "(সংস্কৃত মূল - হিন্দিতেও)", "(ସଂସ୍କୃତ ମୂଳ - ହିନ୍ଦୀରେ ମଧ୍ଯ)"),
                    fontSize = 15.sp,
                    color = c.secondary
                )
            }
            Text(
                (first.source?.name ?: "Wiktionary") + " · CC BY-SA 4.0",
                fontSize = 13.sp,
                color = c.secondary
            )
        } else {
            val m = onlineMeaning
            when {
                w.word.contains(' ') -> {
                    Text(tr(ui, "Select a single word to see its dictionary meaning.", "शब्दकोश अर्थ के लिए एक शब्द चुनें।", "অভিধান অর্থের জন্য একটি শব্দ নির্বাচন করুন।", "ଅଭିଧାନ ଅର୍ଥ ଦେଖିବାକୁ ଗୋଟିଏ ଶବ୍ଦ ବାଛନ୍ତୁ।"), fontSize = 15.sp, color = c.secondary)
                }
                m == null && allowOnline -> {
                    Text(tr(ui, "Looking up meaning...", "अर्थ खोज रहे हैं...", "অর্থ খোঁজা হচ্ছে...", "ଅର୍ଥ ଖୋଜୁଛି..."), fontSize = 15.sp, color = c.secondary)
                }
                m is com.bhagavatam.app.data.Meaning.Found -> {
                    m.entries.take(2).forEach { e ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (e.pos.isNotBlank()) Text("[${e.pos}]", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.gold)
                            Text(e.defs.take(1).joinToString(" "), fontSize = 15.sp, lineHeight = 21.sp, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Text("Wiktionary · CC BY-SA", fontSize = 15.sp, color = c.secondary)
                }
                m is com.bhagavatam.app.data.Meaning.Offline -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(tr(ui, "Offline", "ऑफ़लाइन", "অফলাইন", "ଅଫଲାଇନ୍"), fontSize = 15.sp, color = c.secondary)
                        TextAction(tr(ui, "Retry", "पुन: प्रयास", "আবার চেষ্টা", "ପୁଣି ଚେଷ୍ଟା କରନ୍ତୁ")) { retryKey++ }
                    }
                }
                else -> {
                    Text(tr(ui, "No dictionary entry found.", "कोई प्रविष्टि नहीं मिली।", "কোনো অর্থ পাওয়া যায়নি।", "ଅଭିଧାନରେ କିଛି ମିଳିଲା ନାହିଁ।"), fontSize = 15.sp, color = c.secondary)
                    if (!allowOnline) TextAction(tr(ui, "Look it up online (sends this word to Wiktionary)", "ऑनलाइन खोजें (यह शब्द Wiktionary को भेजा जाएगा)", "অনলাইনে খুঁজুন (শব্দটি Wiktionary-তে যাবে)", "ଅନଲାଇନ୍‌ରେ ଖୋଜନ୍ତୁ (ଏହି ଶବ୍ଦ Wiktionary କୁ ଯିବ)")) { onceOnline = true }
                    if (!allowOnline) Text(
                        tr(ui, "Turn on online meanings in Settings > Words & notes to look it up.", "सेटिंग > शब्द और नोट में ऑनलाइन अर्थ चालू करके खोजें।", "সেটিংস > শব্দ ও নোট-এ অনলাইন অর্থ চালু করে খুঁজুন।", "ଖୋଜିବାକୁ ସେଟିଂସ୍ > ଶବ୍ଦ ଓ ନୋଟ୍‌ରେ ଅନଲାଇନ୍ ଅର୍ଥ ଚାଲୁ କରନ୍ତୁ।"),
                        fontSize = 15.sp, color = c.secondary,
                    )
                }
            }
        }

        androidx.compose.foundation.layout.FlowRow(
            Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            if (onOpenDictionary != null) {
                TextAction(tr(ui, "Open in dictionary", "शब्दकोश में देखें", "অভিধানে দেখুন", "ଅଭିଧାନରେ ଖୋଲନ୍ତୁ")) {
                    onOpenDictionary()
                }
            }
            val reviewKey = w.lang.code + "|" + w.word
            TextAction(if (state.isInReview("word", reviewKey)) tr(ui, "In review", "समीक्षा में", "পর্যালোচনায়", "ସମୀକ୍ଷାରେ") else tr(ui, "Add to review", "समीक्षा में जोड़ें", "পর্যালোচনায় যোগ", "ସମୀକ୍ଷାରେ ଯୋଡ଼ନ୍ତୁ")) {
                if (!state.isInReview("word", reviewKey)) { state.addReview("word", reviewKey); state.toast(tr(ui, "Word added to review", "शब्द समीक्षा में जुड़ा", "শব্দ পর্যালোচনায় যোগ হল", "ଶବ୍ଦ ସମୀକ୍ଷାରେ ଯୋଡ଼ାଗଲା")) }
            }
            TextAction(tr(ui, "Occurrences in the book", "ग्रंथ में खोजें", "গ্রন্থে দেখুন", "ଗ୍ରନ୍ଥରେ ଏହାର ପ୍ରୟୋଗ")) {
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
                Text("${term.term} · ${term.dev}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.gold)
                Text(term.meaning, fontSize = 16.sp, lineHeight = 24.sp, color = c.ink)
            }
            if (onOpenDictionary != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextAction(tr(ui, "Open in Dictionary", "शब्दकोश में देखें", "অভিধানে দেখুন", "ଅଭିଧାନରେ ଖୋଲନ୍ତୁ")) {
                        close()
                        onOpenDictionary()
                    }
                }
            }
            val f = found
            Text(
                when {
                    f == null -> tr(ui, "Searching the book...", "ग्रंथ में खोज रहे हैं...", "গ্রন্থে খোঁজা হচ্ছে...", "ଗ୍ରନ୍ଥରେ ଖୋଜୁଛି...")
                    f.first == 0 -> tr(ui, "Not found elsewhere in the book in this form.", "इस रूप में ग्रंथ में और कहीं नहीं मिला।", "এই রূপে গ্রন্থের আর কোথাও পাওয়া যায়নি।", "ଏହି ରୂପରେ ଗ୍ରନ୍ଥର ଅନ୍ଯତ୍ର ମିଳିଲା ନାହିଁ।")
                    else -> tr(ui, "In this book (${f.first})", "इस ग्रंथ में (${localDigits("${f.first}", ui)})", "এই গ্রন্থে (${localDigits("${f.first}", ui)})", "ଏହି ଗ୍ରନ୍ଥରେ (${f.first})")
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = c.secondary,
            )
            f?.second?.forEach { o ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { close(); state.scrollToVerse = o.ref; onOpenChapter(o.skandha, o.adhyaya) }
                        .padding(vertical = 8.dp),
                ) {
                    Text(localDigits(o.ref, ui), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.gold)
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
