package com.bhagavatam.app.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
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
) {
    val c = LocalReaderColors.current
    // Words the readers could not agree on carry "[?]" in the Odia text: shown without it, underlined in red, tappable.
    val (text, doubtful) = remember(rawText) { splitDoubtful(rawText) }
    var asking by remember(text) { mutableStateOf<IntRange?>(null) }
    val anns = state.annotationsFor(ref, lang.code)
    val pl = prefix.length

    val annotated = remember(text, anns, prefix, c) {
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
            detectTapGestures(
                onTap = { offset ->
                    val layout = layoutResult ?: return@detectTapGestures
                    val charOffset = layout.getOffsetForPosition(offset)
                    val textOffset = charOffset - pl
                    if (textOffset < 0) {
                        onPrefixTap?.invoke()
                        return@detectTapGestures
                    }
                    val doubt = doubtful.firstOrNull { textOffset in it }
                    if (doubt != null) {
                        asking = doubt
                        return@detectTapGestures
                    }
                    val hit = anns.firstOrNull { a ->
                        reanchor(text, a.start, a.end, a.quote)?.let { textOffset in it } == true
                    }
                    if (hit != null) {
                        state.annDraft = AnnDraft(hit, ref, lang.code, hit.start, hit.end, hit.quote)
                        return@detectTapGestures
                    }
                    (onTextTap ?: onPrefixTap)?.invoke()
                },
                onLongPress = { offset ->
                    val layout = layoutResult ?: return@detectTapGestures
                    val charOffset = layout.getOffsetForPosition(offset)
                    val textOffset = (charOffset - pl).coerceIn(0, text.length)
                    val range = wordAt(text, textOffset)
                    if (range != null) {
                        val word = text.substring(range).trim()
                        if (word.isNotEmpty()) {
                            val s0 = range.first
                            val e0 = range.last + 1
                            state.selBar = SelBar(ref, lang, s0, e0, word) {
                                state.selBar = null
                            }
                            state.lookup = WordLookup(word, lang, ref)
                        }
                    } else if (text.isNotEmpty()) {
                        val word = text.split("\\s+".toRegex()).firstOrNull { it.isNotBlank() } ?: text.take(20)
                        state.lookup = WordLookup(word, lang, ref)
                    }
                }
            )
        }
    )
}


/** "word[?]" markers removed from [t]; returns the text without them and the character ranges of the doubtful words in it. */
private fun splitDoubtful(t: String): Pair<String, List<IntRange>> {
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
private fun DoubtDialog(state: AppState, ref: String, lang: Lang, word: String, onDone: () -> Unit) {
    val ui = state.uiLang
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(word) },
        text = {
            Text(
                com.bhagavatam.app.data.tr(
                    ui,
                    "This book was read from the scan by a computer and this word is not certain. Is it spelled correctly?",
                    "यह पुस्तक स्कैन से कंप्यूटर द्वारा पढ़ी गई है और यह शब्द निश्चित नहीं है। क्या वर्तनी सही है?",
                    "এই বইটি স্ক্যান থেকে কম্পিউটার পড়েছে এবং এই শব্দটি নিশ্চিত নয়। বানান কি ঠিক?",
                )
            )
        },
        confirmButton = {
            if (Corrections.enabled) TextButton(onClick = {
                Corrections.open(context, ref, word)
                android.widget.Toast.makeText(context, com.bhagavatam.app.data.tr(ui, "Details copied. Paste them in the chat.", "विवरण कॉपी हो गया। चैट में पेस्ट करें।", "বিবরণ কপি হয়েছে। চ্যাটে পেস্ট করুন।"), android.widget.Toast.LENGTH_LONG).show()
                onDone()
            }) { Text(com.bhagavatam.app.data.tr(ui, "No, send the correct word", "नहीं, सही शब्द भेजें", "না, সঠিক শব্দ পাঠান")) }
        },
        dismissButton = { TextButton(onClick = onDone) { Text(com.bhagavatam.app.data.tr(ui, "Yes, it is correct", "हाँ, सही है", "হ্যাঁ, ঠিক আছে")) } },
    )
}
