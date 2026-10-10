package com.bhagavatam.app.ui.screens

import androidx.compose.ui.res.painterResource
import com.bhagavatam.app.ui.components.Ic
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.Episode
import com.bhagavatam.app.data.EpisodeCategory
import com.bhagavatam.app.data.Episodes
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.Verse
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.data.tr
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.components.LargeTitle
import com.bhagavatam.app.ui.components.Pill
import com.bhagavatam.app.ui.components.SectionHeading
import com.bhagavatam.app.ui.components.tappable
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.EnglishReading
import com.bhagavatam.app.ui.theme.NotoDevanagari
import com.bhagavatam.app.ui.theme.NotoSerifBengali
import com.bhagavatam.app.ui.theme.Radius
import com.bhagavatam.app.ui.theme.TiroHindi
import java.text.Normalizer

data class Hit(val pre: String, val hit: String, val post: String)

enum class SearchScope { ALL, EN, HI, BN }

private enum class SearchLayer(val label: String, val font: FontFamily, val size: Int, val lang: Lang) {
    EN("English", EnglishReading, 15, Lang.EN),
    HI("हिन्दी", TiroHindi, 16, Lang.HI),
    BN("বাংলা", NotoSerifBengali, 16, Lang.BN),
    SA("संस्कृत", NotoDevanagari, 18, Lang.SA);

    fun text(v: Verse) = when (this) {
        EN -> v.en
        HI -> v.hi
        BN -> v.bn
        SA -> v.sa.joinToString(" ")
    }
}

private data class SearchResult(val verse: Verse, val layer: SearchLayer, val hit: Hit)

object SearchFinder {
    private val marks = Regex("[\\u0300-\\u036f]")

    fun variants(q: String): List<String> = if (q.trim().isEmpty()) emptyList() else listOf(q.trim())

    fun foldQuery(s: String): String {
        val t = s.trim().lowercase()
        return marks.replace(Normalizer.normalize(t, Normalizer.Form.NFD), "")
    }

    private fun fold(s: String): Pair<String, IntArray> {
        val out = StringBuilder()
        val map = ArrayList<Int>()
        s.forEachIndexed { i, ch ->
            val d = marks.replace(Normalizer.normalize(ch.toString(), Normalizer.Form.NFD), "").lowercase()
            for (k in d.indices) {
                out.append(d[k])
                map.add(i)
            }
        }
        return out.toString() to map.toIntArray()
    }

    fun find(target: String, query: String): Hit? {
        if (target.isBlank() || query.isBlank()) return null
        return try {
            // Fast path: direct case-insensitive match (zero Unicode normalizations)
            val simplePos = target.indexOf(query, ignoreCase = true)
            val (s, e, match) = if (simplePos >= 0) {
                val start = simplePos.coerceIn(0, target.length)
                val end = (simplePos + query.length).coerceIn(start, target.length)
                Triple(start, end, target.substring(start, end))
            } else {
                // Fallback: diacritic folding for Sanskrit IAST / accented text
                val (folded, map) = fold(target)
                val (qFold, _) = fold(query)
                if (qFold.isEmpty()) return null
                val pos = folded.indexOf(qFold)
                if (pos < 0 || map.isEmpty()) return null
                val startChar = map.getOrElse(pos) { 0 }
                val lastCharIndexInFold = (pos + qFold.length - 1).coerceIn(pos, map.lastIndex)
                val endChar = (map.getOrElse(lastCharIndexInFold) { target.lastIndex } + 1).coerceIn(startChar, target.length)
                val start = startChar.coerceIn(0, target.length)
                val end = endChar.coerceIn(start, target.length)
                Triple(start, end, target.substring(start, end))
            }

            var preStart = (s - 60).coerceIn(0, s)
            if (preStart > 0 && preStart < target.length) {
                val spacePos = target.indexOf(' ', preStart)
                if (spacePos in preStart..s) {
                    preStart = spacePos + 1
                }
            }
            preStart = preStart.coerceIn(0, s)

            var postEnd = (e + 60).coerceIn(e, target.length)
            if (postEnd < target.length && postEnd >= e) {
                val spacePos = target.lastIndexOf(' ', postEnd)
                if (spacePos in e..postEnd) {
                    postEnd = spacePos
                }
            }
            postEnd = postEnd.coerceIn(e, target.length)

            val pre = (if (preStart > 0) "… " else "") + target.substring(preStart, s).replace('\n', ' ')
            val post = target.substring(e, postEnd).replace('\n', ' ') + (if (postEnd < target.length) " …" else "")
            Hit(pre, match, post)
        } catch (_: Throwable) {
            null
        }
    }

    fun find(target: String, variants: List<String>): Hit? {
        for (v in variants) {
            val h = find(target, v)
            if (h != null) return h
        }
        return null
    }
}

/** Legacy alias for compatibility with Me.kt. */
val Finder = SearchFinder

@Composable
fun SearchScreen(
    state: AppState,
    onOpenChapter: (Int, Int) -> Unit = { _, _ -> },
    onOpenVerse: (Int, Int) -> Unit = onOpenChapter,
    onOpenGlossary: () -> Unit = {},
) {
    val openTarget: (Int, Int) -> Unit = { sk, ch ->
        onOpenVerse(sk, ch)
        onOpenChapter(sk, ch)
    }
    SearchResultsScreen(
        state = state,
        onOpenChapter = openTarget,
        onOpenVerse = openTarget,
        onOpenGlossary = onOpenGlossary,
    )
}
