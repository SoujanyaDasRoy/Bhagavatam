package com.bhagavatam.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.AnnDraft
import com.bhagavatam.app.data.AnnKind
import com.bhagavatam.app.data.Annotation
import com.bhagavatam.app.data.Corrections
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.Verse
import com.bhagavatam.app.data.digitsTo
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.data.tr
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.data.SanskritScript
import com.bhagavatam.app.ui.components.splitDoubtful
import com.bhagavatam.app.ui.theme.LocalReaderColors
import com.bhagavatam.app.util.Transliterate

/** The translation a whole-verse action (highlight, note, share) works on: the reading language, else the first translation shown. */
internal fun primaryLayer(state: AppState, v: Verse): Lang? =
    (listOf(state.readLang).filter { it != Lang.SA } + state.alongsideLayers() + listOf(Lang.HI, Lang.EN, Lang.BN, Lang.OR)).firstOrNull { v.hasText(it) }

private fun verseFor(ref: String): Verse? {
    val p = ref.split(".").mapNotNull { it.toIntOrNull() }
    if (p.size < 3) return null
    val all = SampleData.versesFor(p[0], p[1])
    return all.firstOrNull { it.ref == ref } ?: all.firstOrNull { p[2] in it.num..it.numEnd }
}

/** Plain text for sharing or copying: reference, Sanskrit in the chosen script, translation, and where it came from. */
fun verseShareText(state: AppState, v: Verse, lang: Lang?): String {
    val ui = state.uiLang
    val ref = (if (v.skandha == 0) "Mahatmya ${v.adhyaya}.${v.numLabel}" else "Bhagavatam ${v.skandha}.${v.adhyaya}.${v.numLabel}")
    val sa = when (state.script) {
        SanskritScript.BENGALI -> v.sa.map { Transliterate.toBengali(it) }
        SanskritScript.IAST -> v.iast
        else -> v.sa
    }.joinToString("\n")
    val body = lang?.let { digitsTo(v.translation(it), it) }.orEmpty().trim()
    return buildString {
        append(ref).append("\n\n")
        if (sa.isNotBlank()) append(sa).append("\n\n")
        if (body.isNotEmpty()) append(body).append("\n\n")
        if (lang == Lang.OR) append(tr(ui, "(Odia text is machine-read; check the printed book.)", "(ओड़िया पाठ मशीन से पढ़ा गया है; छपी पुस्तक से मिलाएँ।)", "(ওড়িয়া পাঠ যন্ত্রে পড়া; ছাপা বইয়ের সঙ্গে মিলিয়ে নিন।)", "(ଓଡ଼ିଆ ଟେକ୍ସଟ୍ ମେସିନ୍ ଦ୍ୱାରା ପଢ଼ାଯାଇଛି; ଛପା ବହି ଦେଖନ୍ତୁ।)")).append("\n\n")
        append("Shrimad Bhagavat Mahapuran: https://github.com/SoujanyaDasRoy/Bhagavatam")
    }
}

private fun languageName(l: Lang) = when (l) { Lang.HI -> "हिन्दी"; Lang.BN -> "বাংলা"; Lang.OR -> "ଓଡ଼ିଆ"; Lang.SA -> "संस्कृत"; Lang.EN -> "English" }

/**
 * Everything one can do with a whole verse, in one place: bookmark, highlight, note, share, copy, listen from here, other translations,
 * report a mistake. Opened by pressing and holding the verse number or the shloka, or from the selection bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseSheet(state: AppState) {
    val ref = state.verseSheet ?: return
    val v = remember(ref) { verseFor(ref) } ?: run { state.verseSheet = null; return }
    val c = LocalReaderColors.current
    val ui = state.uiLang
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val lang = primaryLayer(state, v)
    val text = remember(v, lang) { lang?.let { splitDoubtful(digitsTo(v.translation(it), it)).first }.orEmpty() }
    val close = { state.verseSheet = null }
    val saved = ref in state.bookmarks || v.ref in state.bookmarks
    var others by remember(ref) { mutableStateOf(false) }
    val label = localDigits(if (v.skandha == 0) "Mahatmya ${v.adhyaya}.${v.numLabel}" else "${v.skandha}.${v.adhyaya}.${v.numLabel}", ui)

    @Composable
    fun Action(text: String, enabled: Boolean = true, onClick: () -> Unit) {
        Box(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(CircleShape).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = if (enabled) c.ink else c.secondary) }
    }

    ModalBottomSheet(onDismissRequest = { close() }, containerColor = c.bg, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 620.dp).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp).navigationBarsPadding().padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, Modifier.padding(horizontal = 12.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.gold)
            if (text.isNotEmpty()) Text(
                text, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 15.sp, lineHeight = 22.sp, color = c.secondary,
                maxLines = 3, overflow = TextOverflow.Ellipsis, fontFamily = lang?.let { readingFont(it) },
            )
            Action(if (saved) tr(ui, "Remove bookmark", "बुकमार्क हटाएँ", "বুকমার্ক সরান", "ବୁକମାର୍କ ହଟାନ୍ତୁ") else tr(ui, "Bookmark this verse", "इस श्लोक को बुकमार्क करें", "এই শ্লোক বুকমার্ক করুন", "ଏହି ଶ୍ଳୋକ ବୁକମାର୍କ କରନ୍ତୁ")) {
                state.toggleBookmark(v.ref)
                val now = v.ref in state.bookmarks
                state.toast(
                    if (now) tr(ui, "Verse bookmarked", "श्लोक बुकमार्क हुआ", "শ্লোক বুকমার্ক হয়েছে", "ଶ୍ଳୋକ ବୁକମାର୍କ ହେଲା") else tr(ui, "Bookmark removed", "बुकमार्क हटा", "বুকমার্ক সরানো হয়েছে", "ବୁକମାର୍କ ହଟିଗଲା"),
                    tr(ui, "Undo", "पूर्ववत", "ফিরিয়ে নিন", "ପୂର୍ବବତ୍ କରନ୍ତୁ"),
                ) { state.toggleBookmark(v.ref) }
                close()
            }
            if (text.isNotEmpty() && lang != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr(ui, "Highlight", "हाइलाइट", "হাইলাইট", "ହାଇଲାଇଟ୍"), Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = c.ink)
                    val names = List(4) { state.markName(it) }
                    MarkColours.forEachIndexed { i, col ->
                        Box(
                            Modifier.size(48.dp).clickable(role = Role.Button, onClickLabel = tr(ui, "Highlight ", "हाइलाइट ", "হাইলাইট ", "ହାଇଲାଇଟ୍ ") + names[i]) {
                                val id = state.addAnnotation(Annotation(0, v.ref, lang.code, 0, text.length, text, AnnKind.HIGHLIGHT, col, "", System.currentTimeMillis()))
                                state.toast(tr(ui, "Verse highlighted", "श्लोक हाइलाइट हुआ", "শ্লোক হাইলাইট হয়েছে", "ଶ୍ଳୋକ ହାଇଲାଇଟ୍ ହେଲା"), tr(ui, "Undo", "पूर्ववत", "ফিরিয়ে নিন", "ପୂର୍ବବତ୍ କରନ୍ତୁ")) { state.removeAnnotation(id) }
                                close()
                            },
                            contentAlignment = Alignment.Center,
                        ) { Box(Modifier.size(24.dp).clip(CircleShape).background(Color(col))) }
                    }
                }
                Action(tr(ui, "Add a note", "नोट जोड़ें", "নোট যোগ করুন", "ନୋଟ୍ ଯୋଡ଼ନ୍ତୁ")) {
                    state.annDraft = AnnDraft(null, v.ref, lang.code, 0, text.length, text)
                    close()
                }
            }
            val inReview = state.isInReview("verse", v.ref)
            Action(if (inReview) tr(ui, "In review (comes back soon)", "समीक्षा में है (जल्द लौटेगा)", "পর্যালোচনায় আছে (শীঘ্র ফিরবে)", "ସମୀକ୍ଷାରେ (ଶୀଘ୍ର ଫେରିବ)") else tr(ui, "Memorise this verse", "इस श्लोक को याद करें", "এই শ্লোক মনে রাখুন", "ଏହି ଶ୍ଳୋକ ମନେରଖନ୍ତୁ"), enabled = !inReview) {
                state.addReview("verse", v.ref)
                state.toast(tr(ui, "Added to review. It comes back tomorrow.", "समीक्षा में जुड़ा। यह कल लौटेगा।", "পর্যালোচনায় যোগ হল। কাল ফিরবে।", "ସମୀକ୍ଷାରେ ଯୋଡ଼ାଗଲା। କାଲି ଫେରିବ।"))
                close()
            }
            Action(tr(ui, "Share", "साझा करें", "শেয়ার করুন", "ଶେୟାର୍")) {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, verseShareText(state, v, lang))
                runCatching { ctx.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                close()
            }
            Action(tr(ui, "Copy with reference", "संदर्भ सहित कॉपी करें", "রেফারেন্স সহ কপি করুন", "ସନ୍ଦର୍ଭ ସହିତ କପି କରନ୍ତୁ")) {
                clipboard.setText(AnnotatedString(verseShareText(state, v, lang)))
                state.toast(tr(ui, "Copied", "कॉपी हुआ", "কপি হয়েছে", "କପି ହେଲା"))
                close()
            }
            Action(tr(ui, "Listen from here", "यहाँ से सुनें", "এখান থেকে শুনুন", "ଏଠାରୁ ଶୁଣନ୍ତୁ")) {
                val all = SampleData.versesFor(v.skandha, v.adhyaya)
                val i = all.indexOfFirst { it.ref == v.ref }.coerceAtLeast(0)
                state.playVerses(all, i)
                close()
            }
            val other = listOf(Lang.SA, Lang.HI, Lang.EN, Lang.BN, Lang.OR).filter { it != lang && it != Lang.SA && v.hasText(it) }
            if (other.isNotEmpty()) {
                Action(if (others) tr(ui, "Hide other translations", "अन्य अनुवाद छिपाएँ", "অন্য অনুবাদ লুকান", "ଅନ୍ଯ ଅନୁବାଦ ଲୁଚାନ୍ତୁ") else tr(ui, "Other translations", "अन्य अनुवाद", "অন্য অনুবাদ", "ଅନ୍ଯ ଅନୁବାଦ")) { others = !others }
                if (others) other.forEach { l ->
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(languageName(l), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.gold)
                        Text(digitsTo(v.translation(l), l).replace("[?]", ""), fontSize = 16.sp, lineHeight = 26.sp, color = c.ink, fontFamily = readingFont(l))
                    }
                }
            }
            if (Corrections.enabled) Action(tr(ui, "Report an error", "त्रुटि बताएँ", "ভুল জানান", "ତ୍ରୁଟି ଜଣାନ୍ତୁ")) {
                Corrections.open(ctx, v.ref, "")
                state.toast(tr(ui, "Details copied. Paste them in the chat.", "विवरण कॉपी हो गया। चैट में पेस्ट करें।", "বিবরণ কপি হয়েছে। চ্যাটে পেস্ট করুন।", "ବିବରଣୀ କପି ହେଲା। ଚାଟ୍‌ରେ ପେଷ୍ଟ୍ କରନ୍ତୁ।"))
                close()
            }
        }
    }
}
