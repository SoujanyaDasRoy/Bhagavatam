package com.bhagavatam.app.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import com.bhagavatam.app.data.Corrections
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.bhagavatam.app.data.AnnDraft
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.SelBar
import com.bhagavatam.app.data.WordLookup
import com.bhagavatam.app.data.reanchor
import com.bhagavatam.app.data.trimRange
import com.bhagavatam.app.data.wordAt
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.theme.LocalReaderColors

/**
 * Reading text that renders pristine native typography with full ligature support,
 * highlighting, and note markers.
 *
 * Tapping a mark opens that note/highlight. Tapping the text invokes [onTextTap] (e.g. shloka peek).
 * Pressing and holding on any word instantly detects the word and opens the compact meaning card.
 */
@Composable
fun MarkedText(
    state: AppState,
    ref: String,
    lang: Lang,
    rawText: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    prefix: AnnotatedString = AnnotatedString(""),
    onPrefixTap: (() -> Unit)? = null,
    onTextTap: (() -> Unit)? = null,
    doubtWords: Set<String> = emptySet(),
) {
    val c = LocalReaderColors.current
    // Words the readers could not agree on carry "[?]" in the Odia text: shown without it, underlined in red, tappable.
    val (text, marked) = remember(rawText) { splitDoubtful(rawText) }
    val doubtful = remember(text, marked, doubtWords) { (marked + wordRanges(text, doubtWords)).sortedBy { it.first } }
    var asking by remember(text) { mutableStateOf<IntRange?>(null) }
    val anns = state.annotationsFor(ref, lang.code)
    val pl = prefix.length
    val haptic = LocalHapticFeedback.current
    // The span being chosen: while the finger drags, then while the action bar is open for it.
    var dragSel by remember(text) { mutableStateOf<IntRange?>(null) }
    var anchor by remember(text) { mutableStateOf<IntRange?>(null) }
    val barSel = state.selBar?.takeIf { it.ref == ref && it.lang == lang }?.let { it.start until it.end }
    val chosen = dragSel ?: barSel

    val annotated = remember(text, anns, prefix, c, chosen) {
        buildAnnotatedString {
            append(prefix)
            append(text)
            for (a in anns) {
                val r = reanchor(text, a.start, a.end, a.quote) ?: continue
                val col = if (a.colour == 0L) c.gold else Color(a.colour)
                val base = when (a.kind) {
                    com.bhagavatam.app.data.AnnKind.BOOKMARK -> SpanStyle(background = c.gold.copy(alpha = 0.22f), textDecoration = TextDecoration.Underline)
                    else -> SpanStyle(background = col.copy(alpha = 0.38f))
                }
                addStyle(if (a.note.isNotBlank()) base.copy(textDecoration = TextDecoration.Underline) else base, pl + r.first, pl + r.last + 1)
            }
            if (chosen != null && !chosen.isEmpty()) addStyle(SpanStyle(background = c.gold.copy(alpha = 0.45f)), pl + chosen.first, (pl + chosen.last + 1).coerceAtMost(pl + text.length))
        }
    }

    var layoutResult by remember(annotated) { mutableStateOf<TextLayoutResult?>(null) }

    asking?.let { r ->
        val word = text.substring(r.first, r.last + 1).trim(' ', ',', '.', ';', ':', '!', '?', '\u0964', '\u0965')
        DoubtDialog(state, ref, lang, word) { asking = null }
    }

    Text(
        text = annotated,
        style = style,
        onTextLayout = { layoutResult = it },
        modifier = modifier.drawBehind {
            val layout = layoutResult ?: return@drawBehind
            for (r in doubtful) for (i in r) {
                if (i + pl >= layout.layoutInput.text.length) continue
                val b = layout.getBoundingBox(i + pl)
                drawLine(Color(0xFFD32F2F), Offset(b.left, b.bottom - 1f), Offset(b.right, b.bottom - 1f), strokeWidth = 2.5f)
            }
        }.pointerInput(text, anns, pl) {
            detectTapOrHoldDrag(
                onTap = { offset ->
                    val layout = layoutResult ?: return@detectTapOrHoldDrag
                    val charOffset = layout.getOffsetForPosition(offset)
                    val textOffset = charOffset - pl
                    if (textOffset < 0) {
                        onPrefixTap?.invoke()
                        return@detectTapOrHoldDrag
                    }
                    val doubt = doubtful.firstOrNull { textOffset in it }
                    if (doubt != null) {
                        asking = doubt
                        return@detectTapOrHoldDrag
                    }
                    val hit = anns.firstOrNull { a ->
                        reanchor(text, a.start, a.end, a.quote)?.let { textOffset in it } == true
                    }
                    if (hit != null) {
                        state.annDraft = AnnDraft(hit, ref, lang.code, hit.start, hit.end, hit.quote)
                        return@detectTapOrHoldDrag
                    }
                    state.selBar?.let { it.clear(); state.selBar = null }
                    state.lookup = null
                    (onTextTap ?: onPrefixTap)?.invoke()
                },
                onHoldStart = { offset ->
                    val layout = layoutResult ?: return@detectTapOrHoldDrag
                    val charOffset = layout.getOffsetForPosition(offset)
                    if (charOffset - pl < 0) {
                        // Hold on the verse number: the verse's own actions.
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        state.verseSheet = ref
                        anchor = null
                        return@detectTapOrHoldDrag
                    }
                    val range = wordAt(text, (charOffset - pl).coerceIn(0, text.length))
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    anchor = range
                    dragSel = range
                    state.selBar?.let { it.clear(); state.selBar = null }
                    if (range == null && text.isNotEmpty()) {
                        val word = text.split(Regex("\\s+")).firstOrNull { it.isNotBlank() } ?: text.take(20)
                        state.lookup = WordLookup(word, lang, ref)
                    }
                },
                onHoldMove = { offset ->
                    val a0 = anchor ?: return@detectTapOrHoldDrag
                    val layout = layoutResult ?: return@detectTapOrHoldDrag
                    val o = (layout.getOffsetForPosition(offset) - pl).coerceIn(0, text.length)
                    val here = wordAt(text, o) ?: return@detectTapOrHoldDrag
                    val s0 = minOf(a0.first, here.first)
                    val e0 = maxOf(a0.last, here.last)
                    if (dragSel != s0..e0) dragSel = s0..e0
                },
                onHoldEnd = {
                    val sel = dragSel
                    dragSel = null
                    if (sel != null) {
                        val tr = trimRange(text, sel.first, sel.last + 1)
                        val quote = if (tr.isEmpty()) "" else text.substring(tr.first, tr.last + 1)
                        if (quote.isNotEmpty()) {
                            state.selBar = SelBar(ref, lang, tr.first, tr.last + 1, quote, text) { state.selBar = null }
                            state.lookup = if (quote.any { it.isWhitespace() }) null else WordLookup(quote, lang, ref)
                        }
                    }
                    anchor = null
                },
            )
        }
    )
}


/** "word[?]" markers removed from [t]; returns the text without them and the character ranges of the doubtful words in it. */
internal fun splitDoubtful(t: String): Pair<String, List<IntRange>> {
    if (!t.contains("[?]")) return t to emptyList()
    val out = StringBuilder()
    val ranges = ArrayList<IntRange>()
    var wordStart = 0
    var i = 0
    while (i < t.length) {
        if (t.startsWith("[?]", i)) {
            if (out.length > wordStart) ranges.add(wordStart until out.length)
            i += 3
            continue
        }
        val ch = t[i]
        out.append(ch)
        if (ch.isWhitespace()) wordStart = out.length
        i++
    }
    return out.toString() to ranges
}

/** Asked when a reader taps a red-underlined word: is it spelled correctly? If not, the correction goes to the maker. */
@Composable
internal fun DoubtDialog(state: AppState, ref: String, lang: Lang, word: String, onDone: () -> Unit) {
    val ui = state.uiLang
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(word) },
        text = {
            Text(
                com.bhagavatam.app.data.tr(
                    ui,
                    "This word may have been misread when the book was typed from the scan. Is it spelled correctly?",
                    "स्कैन से पुस्तक टाइप करते समय यह शब्द गलत पढ़ा गया हो सकता है। क्या वर्तनी सही है?",
                    "স্ক্যান থেকে বইটি টাইপ করার সময় এই শব্দটি ভুল পড়া হয়ে থাকতে পারে। বানান কি ঠিক?", "ସ୍କାନ୍‌ରୁ ବହି ଟାଇପ୍ କରିବା ବେଳେ ଏହି ଶବ୍ଦ ଭୁଲ ପଢ଼ାଯାଇଥାଇପାରେ। ଏହାର ବନାନ ଠିକ୍ ଅଛି କି?",
                )
            )
        },
        confirmButton = {
            if (Corrections.enabled) TextButton(onClick = {
                Corrections.open(context, ref, word)
                android.widget.Toast.makeText(context, com.bhagavatam.app.data.tr(ui, "Details copied. Paste them in the chat.", "विवरण कॉपी हो गया। चैट में पेस्ट करें।", "বিবরণ কপি হয়েছে। চ্যাটে পেস্ট করুন।", "ବିବରଣୀ କପି ହେଲା। ଚାଟ୍‌ରେ ପେଷ୍ଟ୍ କରନ୍ତୁ।"), android.widget.Toast.LENGTH_LONG).show()
                onDone()
            }) { Text(com.bhagavatam.app.data.tr(ui, "No, send the correct word", "नहीं, सही शब्द भेजें", "না, সঠিক শব্দ পাঠান", "ନା, ଠିକ୍ ଶବ୍ଦ ପଠାନ୍ତୁ")) }
        },
        dismissButton = { TextButton(onClick = onDone) { Text(com.bhagavatam.app.data.tr(ui, "Yes, it is correct", "हाँ, सही है", "হ্যাঁ, ঠিক আছে", "ହଁ, ଠିକ୍ ଅଛି")) } },
    )
}

/** Character ranges of whole-word occurrences of [words] in [text]. */
internal fun wordRanges(text: String, words: Set<String>): List<IntRange> {
    if (words.isEmpty()) return emptyList()
    val out = ArrayList<IntRange>()
    for (w in words) {
        var i = text.indexOf(w)
        while (i >= 0) {
            val before = if (i == 0) ' ' else text[i - 1]
            val after = if (i + w.length >= text.length) ' ' else text[i + w.length]
            if (!before.isLetterOrDigit() && !after.isLetterOrDigit() && before != '\u094d' && after !in '\u093e'..'\u094d' && after !in '\u09be'..'\u09cd') out.add(i until i + w.length)
            i = text.indexOf(w, i + w.length)
        }
    }
    return out
}
