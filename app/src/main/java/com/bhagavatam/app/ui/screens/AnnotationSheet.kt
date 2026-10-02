package com.bhagavatam.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.AnnDraft
import com.bhagavatam.app.data.AnnKind
import com.bhagavatam.app.data.Annotation
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.data.occurrencesOf
import com.bhagavatam.app.data.tr
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.theme.LocalReaderColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The four highlight colours. The first is also the default for notes. */
val MarkColours = listOf(0xFFE8B931L, 0xFF5FAE6BL, 0xFF4F9BD9L, 0xFFD9577EL)

@Composable
internal fun TextAction(label: String, onClick: () -> Unit) {
    val c = LocalReaderColors.current
    Box(Modifier.heightIn(min = 44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 9.dp), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.ink, maxLines = 1)
    }
}

/** Shown above the reader controls while text is selected: colour to highlight, note, bookmark the passage, meaning, copy. */
@Composable
fun SelectionBar(state: AppState, modifier: Modifier = Modifier) {
    val sel = state.selBar ?: return
    val c = LocalReaderColors.current
    val ui = state.uiLang
    val clipboard = LocalClipboardManager.current
    fun add(kind: AnnKind, colour: Long) {
        state.addAnnotation(Annotation(0, sel.ref, sel.lang.code, sel.start, sel.end, sel.quote, kind, colour, "", System.currentTimeMillis()))
        state.selBar = null; sel.clear()
    }
    Row(
        modifier.clip(RoundedCornerShapeBar).background(c.surface).border(1.dp, c.separator, RoundedCornerShapeBar)
            .horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        MarkColours.forEach { col ->
            Box(
                Modifier.size(40.dp).clickable(role = Role.Button, onClickLabel = tr(ui, "Highlight", "हाइलाइट", "হাইলাইট")) { add(AnnKind.HIGHLIGHT, col) },
                contentAlignment = Alignment.Center,
            ) { Box(Modifier.size(22.dp).clip(CircleShape).background(Color(col))) }
        }
        TextAction(tr(ui, "Note", "नोट", "নোট")) {
            state.annDraft = AnnDraft(null, sel.ref, sel.lang.code, sel.start, sel.end, sel.quote)
            state.selBar = null; sel.clear()
        }
        TextAction(tr(ui, "Bookmark", "बुकमार्क", "বুকমার্ক")) { add(AnnKind.BOOKMARK, 0L) }
        TextAction(tr(ui, "Meaning", "अर्थ", "অর্থ")) {
            state.lookup = com.bhagavatam.app.data.WordLookup(sel.quote.trim().take(80), sel.lang, sel.ref)
            state.selBar = null; sel.clear()
        }
        val ctx = LocalContext.current
        TextAction("Google") { state.webQuery = sel.quote.trim(); state.selBar = null; sel.clear() }
        TextAction(tr(ui, "Copy", "कॉपी", "কপি")) { clipboard.setText(AnnotatedString(sel.quote)); state.selBar = null; sel.clear() }
    }
}

private val RoundedCornerShapeBar = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)

/** Edit or create a note: the quoted text, a text box, the colour, and delete when it already exists. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnotationSheet(state: AppState) {
    val d = state.annDraft ?: return
    val c = LocalReaderColors.current
    val ui = state.uiLang
    val existing = d.existing
    var note by remember(d) { mutableStateOf(existing?.note.orEmpty()) }
    var colour by remember(d) { mutableStateOf(existing?.colour?.takeIf { it != 0L } ?: MarkColours[0]) }
    val close = { state.annDraft = null }
    fun save() {
        val kind = existing?.kind ?: AnnKind.NOTE
        val a = (existing ?: Annotation(0, d.ref, d.layer, d.start, d.end, d.quote, kind, colour, "", System.currentTimeMillis()))
            .copy(note = note.trim(), colour = if (kind == AnnKind.BOOKMARK) 0L else colour)
        if (existing == null) { if (a.note.isNotEmpty()) state.addAnnotation(a) } else state.updateAnnotation(a)
        close()
    }
    ModalBottomSheet(onDismissRequest = { save() }, containerColor = c.bg) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(localDigits(d.ref, ui), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.gold)
            Text(d.quote, fontSize = 16.sp, lineHeight = 24.sp, fontStyle = FontStyle.Italic, color = c.secondary, maxLines = 5)
            OutlinedTextField(
                value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
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
 * A small card above the reading controls for a pressed or tapped word. It never dims the page and goes away on scroll,
 * so reading carries straight on. Shows the book's own glossary entry if there is one, then the online meaning (when allowed).
 */
@Composable
fun MeaningCard(state: AppState, modifier: Modifier = Modifier) {
    val w = state.lookup ?: return
    if (state.webQuery != null) return
    val c = LocalReaderColors.current
    val ui = state.uiLang
    val ctx = LocalContext.current
    val term = remember(w) { SampleData.glossary.firstOrNull { it.term.equals(w.word, ignoreCase = true) || it.dev == w.word } }
    val meaning by produceState<com.bhagavatam.app.data.Meaning?>(null, w, state.onlineMeanings) {
        value = if (state.onlineMeanings && !w.word.contains(' ')) com.bhagavatam.app.data.OnlineMeaning.lookup(w.word, w.lang) else null
    }
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.separator, shape).padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(w.word, Modifier.weight(1f), fontSize = 19.sp, fontWeight = FontWeight.Medium, color = c.ink, maxLines = 2)
            Box(Modifier.size(44.dp).clickable(role = Role.Button, onClickLabel = tr(ui, "Close", "बंद करें", "বন্ধ করুন")) { state.lookup = null }, contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(com.bhagavatam.app.ui.components.Ic.Close), null, tint = c.secondary, modifier = Modifier.size(18.dp))
            }
        }
        if (term != null) {
            Text("${term.term} · ${term.dev}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.gold)
            Text(term.meaning, fontSize = 15.sp, lineHeight = 22.sp, color = c.ink)
        }
        val m = meaning
        val note = tr(ui, "Meaning from Wiktionary (online)", "अर्थ: Wiktionary (ऑनलाइन)", "অর্থ: Wiktionary (অনলাইন)")
        when {
            !state.onlineMeanings -> if (term == null) Text(tr(ui, "Online meanings are off. Use Google, or turn them on in Settings.", "ऑनलाइन अर्थ बंद हैं। Google से देखें, या सेटिंग में चालू करें।", "অনলাইন অর্থ বন্ধ আছে। Google-এ দেখুন, বা সেটিংসে চালু করুন।"), fontSize = 14.sp, lineHeight = 20.sp, color = c.secondary)
            w.word.contains(' ') -> if (term == null) Text(tr(ui, "Select a single word for its meaning, or search the phrase on Google.", "अर्थ के लिए एक शब्द चुनें, या वाक्यांश Google पर खोजें।", "অর্থের জন্য একটি শব্দ বাছুন, বা বাক্যাংশ Google-এ খুঁজুন।"), fontSize = 14.sp, lineHeight = 20.sp, color = c.secondary)
            m == null -> Text(tr(ui, "Looking up...", "खोज रहे हैं...", "খোঁজা হচ্ছে..."), fontSize = 14.sp, color = c.secondary)
            m is com.bhagavatam.app.data.Meaning.Found -> {
                m.entries.take(3).forEach { e ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (e.pos.isNotBlank()) Text(e.pos, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.gold)
                        e.defs.take(2).forEach { Text("• $it", fontSize = 15.sp, lineHeight = 22.sp, color = c.ink, maxLines = 3) }
                    }
                }
                Text(note, fontSize = 11.sp, color = c.secondary)
            }
            m is com.bhagavatam.app.data.Meaning.Offline -> if (term == null) Text(tr(ui, "No connection. Try Google when you are online.", "कनेक्शन नहीं है। ऑनलाइन होने पर Google से देखें।", "সংযোগ নেই। অনলাইন হলে Google-এ দেখুন।"), fontSize = 14.sp, lineHeight = 20.sp, color = c.secondary)
            else -> if (term == null) {
                // Nothing in the glossary or Wiktionary: take the one or two lines Google gives as the meaning.
                val g by produceState<String?>(null, w) { value = com.bhagavatam.app.data.GoogleSnippet.lookup(ctx, w.word, w.lang) }
                when {
                    g == null -> Text(tr(ui, "Looking up...", "खोज रहे हैं...", "খোঁজা হচ্ছে..."), fontSize = 14.sp, color = c.secondary)
                    g!!.isNotEmpty() -> { Text(g!!, fontSize = 15.sp, lineHeight = 22.sp, color = c.ink, maxLines = 4); Text(tr(ui, "Meaning from Google", "अर्थ: Google", "অর্থ: Google"), fontSize = 11.sp, color = c.secondary) }
                    else -> Text(tr(ui, "No short meaning found. Try the Google button.", "कोई संक्षिप्त अर्थ नहीं मिला। Google बटन आज़माएँ।", "সংক্ষিপ্ত অর্থ পাওয়া যায়নি। Google বোতাম চেষ্টা করুন।"), fontSize = 14.sp, lineHeight = 20.sp, color = c.secondary)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            TextAction("Google") { state.webQuery = w.word }
            TextAction(tr(ui, "More", "और", "আরও")) { state.showWordSheet = true }
        }
    }
}

/** A tapped word or selected phrase: what the book's own glossary says, where else it appears in the book, and ways to look it up. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordSheet(state: AppState, onOpenChapter: (Int, Int) -> Unit) {
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
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(w.word, fontSize = 24.sp, fontWeight = FontWeight.Medium, color = c.ink)
            if (term != null) {
                Text("${term.term} · ${term.dev}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.gold)
                Text(term.meaning, fontSize = 16.sp, lineHeight = 24.sp, color = c.ink)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                TextAction(tr(ui, "Look up in a dictionary app", "शब्दकोश ऐप में देखें", "অভিধান অ্যাপে দেখুন")) {
                    val i = Intent(Intent.ACTION_PROCESS_TEXT).setType("text/plain").putExtra(Intent.EXTRA_PROCESS_TEXT, w.word).putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                    runCatching { ctx.startActivity(Intent.createChooser(i, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                TextAction(tr(ui, "Search Google here", "यहीं Google पर खोजें", "এখানেই Google-এ খুঁজুন")) { state.showWordSheet = false; state.webQuery = w.word }
            }
            val f = found
            Text(
                when {
                    f == null -> tr(ui, "Searching the book...", "ग्रंथ में खोज रहे हैं...", "গ্রন্থে খোঁজা হচ্ছে...")
                    f.first == 0 -> tr(ui, "Not found elsewhere in the book in this form.", "इस रूप में ग्रंथ में और कहीं नहीं मिला।", "এই রূপে গ্রন্থের আর কোথাও পাওয়া যায়নি।")
                    else -> tr(ui, "In this book (${f.first})", "इस ग्रंथ में (${localDigits("${f.first}", ui)})", "এই গ্রন্থে (${localDigits("${f.first}", ui)})")
                },
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.secondary,
            )
            f?.second?.forEach { o ->
                Column(Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).clickable { close(); onOpenChapter(o.skandha, o.adhyaya) }.padding(vertical = 8.dp)) {
                    Text(localDigits(o.ref, ui), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.gold)
                    Text(o.snippet, fontSize = 15.sp, lineHeight = 22.sp, color = c.ink)
                }
            }
        }
    }
}


/**
 * Hides everything on a Google results page except the results container, so the panel shows the results and not the Google
 * page around them (logo, search box, tabs, sign-in, footer). Runs after each page load, a few times, because the page fills in late.
 * If the container is not found the page is left as it is.
 */
private const val RESULTS_ONLY_JS = """
(function () {
  var r = document.getElementById('rso') || document.getElementById('search') || document.getElementById('center_col');
  if (!r) return 'none';
  var n = r;
  while (n && n !== document.body) {
    var p = n.parentNode;
    for (var c = p.firstChild; c; c = c.nextSibling) { if (c.nodeType === 1 && c !== n) c.style.display = 'none'; }
    n = p;
  }
  document.body.style.paddingTop = '0'; document.body.style.marginTop = '0';
  window.scrollTo(0, 0);
  return 'ok';
})();
"""

/** Google results for the pressed word, inside the app, results only. Close (or Back) returns to the text. */
@Composable
fun WebPanel(state: AppState, modifier: Modifier = Modifier) {
    val q = state.webQuery ?: return
    val c = LocalReaderColors.current
    val ui = state.uiLang
    var view by remember(q) { mutableStateOf<android.webkit.WebView?>(null) }
    // Back goes back through the results first, then closes the panel.
    androidx.activity.compose.BackHandler { val v = view; if (v != null && v.canGoBack()) v.goBack() else state.webQuery = null }
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val url = "https://www.google.com/search?hl=${ui.code}&q=" + Uri.encode("$q meaning")
    Column(modifier.fillMaxWidth().fillMaxHeight(0.55f).clip(shape).background(c.bg).border(1.dp, c.separator, shape)) {
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(q, Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Medium, color = c.ink, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            TextAction(tr(ui, "Meaning", "अर्थ", "অর্থ")) { state.webQuery = null }
            Box(Modifier.size(48.dp).clickable(role = Role.Button, onClickLabel = tr(ui, "Close", "बंद करें", "বন্ধ করুন")) { state.webQuery = null; state.lookup = null }, contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(com.bhagavatam.app.ui.components.Ic.Close), null, tint = c.secondary, modifier = Modifier.size(18.dp))
            }
        }
        androidx.compose.runtime.key(q) {
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f).navigationBarsPadding(),
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        @Suppress("SetJavaScriptEnabled")
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        // Links stay inside this view; nothing opens the browser.
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: android.webkit.WebView, request: android.webkit.WebResourceRequest) = false
                            override fun onPageFinished(view: android.webkit.WebView, url: String?) {
                                // Only on Google's own results page; pages opened from a result are shown whole.
                                if (url == null || !url.contains("google.") || !url.contains("/search")) return
                                for (delay in longArrayOf(0, 500, 1500)) view.postDelayed({ view.evaluateJavascript(RESULTS_ONLY_JS, null) }, delay)
                            }
                        }
                        view = this
                        loadUrl(url)
                    }
                },
                onRelease = { it.stopLoading(); it.destroy() },
            )
        }
    }
}
