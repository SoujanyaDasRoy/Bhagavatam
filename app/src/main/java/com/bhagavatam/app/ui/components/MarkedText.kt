package com.bhagavatam.app.ui.components

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import com.bhagavatam.app.data.AnnDraft
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.SelBar
import com.bhagavatam.app.data.WordLookup
import com.bhagavatam.app.data.reanchor
import com.bhagavatam.app.data.wordAt
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.theme.LocalReaderColors

/** The system's own selection menu is switched off: the reader shows its own bar (highlight, note, bookmark, meaning, copy). */
private object NoToolbar : TextToolbar {
    override val status = TextToolbarStatus.Hidden
    override fun showMenu(rect: Rect, onCopyRequested: (() -> Unit)?, onPasteRequested: (() -> Unit)?, onCutRequested: (() -> Unit)?, onSelectAllRequested: (() -> Unit)?) {}
    override fun hide() {}
}

/**
 * Reading text that can be selected, marked and tapped. It is a read-only text field, so the selection comes back as exact
 * character offsets (no clipboard tricks). Tapping a word opens its meaning; tapping a mark opens that note; selecting text
 * shows the action bar. [prefix] (the verse number in book mode) sits before the text; tapping it calls [onPrefixTap].
 */
@Composable
fun MarkedText(
    state: AppState, ref: String, lang: Lang, text: String, style: TextStyle, modifier: Modifier = Modifier,
    prefix: AnnotatedString = AnnotatedString(""), onPrefixTap: (() -> Unit)? = null,
) {
    val c = LocalReaderColors.current
    val focus = LocalFocusManager.current
    val anns = state.annotationsFor(ref, lang.code)
    val pl = prefix.length
    val shown = remember(text, anns, prefix, c) {
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
                // A mark with a note also gets an underline, so notes can be told from plain highlights.
                addStyle(if (a.note.isNotBlank()) base.copy(textDecoration = TextDecoration.Underline) else base, pl + r.first, pl + r.last + 1)
            }
        }
    }
    var tfv by remember(shown) { mutableStateOf(TextFieldValue(shown)) }
    // A selection that is cleared on purpose (a mark was made) still sends a collapsed-caret event; it must not count as a tap.
    val ctl = remember { object { var selecting = false; var ignoreUntil = 0L } }
    val clear = { ctl.selecting = false; ctl.ignoreUntil = System.currentTimeMillis() + 600; tfv = TextFieldValue(shown, TextRange.Zero); focus.clearFocus() }

    // The bar belongs to the selection that made it; drop it when this text leaves the screen.
    DisposableEffect(ref, lang) { onDispose { if (state.selBar?.ref == ref && state.selBar?.lang == lang) state.selBar = null } }

    fun tapAt(offset: Int) {
        val i = offset - pl
        if (i < 0) { onPrefixTap?.invoke(); return }
        val hit = anns.firstOrNull { a -> reanchor(text, a.start, a.end, a.quote)?.let { i in it } == true }
        if (hit != null) { state.annDraft = AnnDraft(hit, ref, lang.code, hit.start, hit.end, hit.quote); return }
        // A plain tap on a word does nothing: meanings come from press and hold.
    }

    CompositionLocalProvider(LocalTextToolbar provides NoToolbar) {
        BasicTextField(
            value = tfv,
            onValueChange = { nv ->
                val sel = nv.selection
                if (!sel.collapsed) {
                    ctl.selecting = true
                    tfv = TextFieldValue(shown, sel)
                    val s0 = (sel.min - pl).coerceAtLeast(0)
                    val e0 = (sel.max - pl).coerceAtMost(text.length)
                    if (e0 > s0) {
                        val quote = text.substring(s0, e0)
                        state.selBar = SelBar(ref, lang, s0, e0, quote, clear)
                        // Press and hold on one word: show its meaning at once, in the small card. A longer selection waits for "Meaning".
                        val one = quote.trim()
                        val single = one.isNotEmpty() && one.none { it.isWhitespace() } && one.length <= 40
                        state.lookup = if (single) WordLookup(one, lang, ref) else null
                    }
                } else {
                    if (state.selBar?.ref == ref && state.selBar?.lang == lang) state.selBar = null
                    // A collapsed caret with no selection before it is a tap. Reset to the start so the same word can be tapped again.
                    val wasSelecting = ctl.selecting || System.currentTimeMillis() < ctl.ignoreUntil
                    ctl.selecting = false
                    tfv = TextFieldValue(shown, TextRange.Zero)
                    if (!wasSelecting && sel.start > 0) tapAt(sel.start)
                }
            },
            modifier = modifier.onFocusChanged { if (!it.isFocused && !tfv.selection.collapsed) tfv = TextFieldValue(shown, TextRange.Zero) },
            readOnly = true,
            textStyle = style,
            cursorBrush = SolidColor(Color.Transparent),
        )
    }
}
