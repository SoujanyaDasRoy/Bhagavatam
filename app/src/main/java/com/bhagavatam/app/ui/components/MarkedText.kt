package com.bhagavatam.app.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
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
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    prefix: AnnotatedString = AnnotatedString(""),
    onPrefixTap: (() -> Unit)? = null,
    onTextTap: (() -> Unit)? = null,
) {
    val c = LocalReaderColors.current
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

    Text(
        text = annotated,
        style = style,
        onTextLayout = { layoutResult = it },
        modifier = modifier.pointerInput(text, anns, pl) {
            detectTapGestures(
                onTap = { offset ->
                    val layout = layoutResult ?: return@detectTapGestures
                    val charOffset = layout.getOffsetForPosition(offset)
                    val textOffset = charOffset - pl
                    if (textOffset < 0) {
                        onPrefixTap?.invoke()
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
