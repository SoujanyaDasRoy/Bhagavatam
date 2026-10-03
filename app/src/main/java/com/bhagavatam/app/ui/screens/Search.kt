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
        // Fast path: direct case-insensitive match (zero Unicode normalizations)
        val simplePos = target.indexOf(query, ignoreCase = true)
        val (start, end, match) = if (simplePos >= 0) {
            Triple(simplePos, (simplePos + query.length).coerceAtMost(target.length), target.substring(simplePos, (simplePos + query.length).coerceAtMost(target.length)))
        } else {
            // Fallback: diacritic folding for Sanskrit IAST / accented text
            val (folded, map) = fold(target)
            val (qFold, _) = fold(query)
            val pos = folded.indexOf(qFold)
            if (pos < 0) return null
            val s = map.getOrElse(pos) { 0 }
            val e = if (pos + qFold.length < map.size) map[pos + qFold.length] else target.length
            Triple(s, e, target.substring(s, e))
        }

        var preStart = (start - 60).coerceAtLeast(0)
        if (preStart > 0) preStart = target.indexOf(' ', preStart).let { if (it in 0..start) it + 1 else start }
        var postEnd = (end + 60).coerceAtMost(target.length)
        if (postEnd < target.length) postEnd = target.lastIndexOf(' ', postEnd).let { if (it > end) it else end }

        val pre = (if (preStart > 0) "… " else "") + target.substring(preStart, start).replace('\n', ' ')
        val post = target.substring(end, postEnd).replace('\n', ' ') + (if (postEnd < target.length) " …" else "")
        return Hit(pre, match, post)
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
    val s = state.strings
    val ui = state.uiLang
    var query by rememberSaveable { mutableStateOf("") }
    var activeScope by rememberSaveable { mutableStateOf(SearchScope.ALL) }
    var showVoiceSheet by remember { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf(EpisodeCategory.ALL) }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }

    // Direct Verse Jump check (e.g. "10.29.1" or "10.29")
    val parsedJump = remember(query) {
        val match = Regex("^(\\d+)[.:/\\-\\s]+(\\d+)(?:[.:/\\-\\s]+(\\d+))?$").find(query.trim())
        if (match != null) {
            val sk = match.groupValues[1].toIntOrNull() ?: 1
            val ad = match.groupValues[2].toIntOrNull() ?: 1
            val vs = match.groupValues.getOrNull(3)?.toIntOrNull() ?: 1
            if (sk in 0..12) Triple(sk, ad, vs) else null
        } else null
    }

    // Incident Matches
    val matchingIncidents = remember(query) {
        if (query.trim().length >= 2) Episodes.all.filter { it.matchesQuery(query) } else emptyList()
    }

    // Full-Text Verse Matches executed off-thread with 180ms debounce
    LaunchedEffect(query, activeScope) {
        val q = query.trim()
        if (q.length < 2) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(180)
        val searchResults = withContext(Dispatchers.Default) {
            val candidateVerses = SampleData.searchVerses(q, activeScope.name, limit = 100)
            val out = ArrayList<SearchResult>()
            val layersToSearch = when (activeScope) {
                SearchScope.ALL -> listOf(SearchLayer.EN, SearchLayer.HI, SearchLayer.BN, SearchLayer.SA)
                SearchScope.EN -> listOf(SearchLayer.EN)
                SearchScope.HI -> listOf(SearchLayer.HI)
                SearchScope.BN -> listOf(SearchLayer.BN)
            }

            for (v in candidateVerses) {
                for (layer in layersToSearch) {
                    val text = layer.text(v)
                    val hit = SearchFinder.find(text, q)
                    if (hit != null) {
                        out.add(SearchResult(v, layer, hit))
                        break
                    }
                }
                if (out.size >= 100) break
            }
            out
        }
        results = searchResults
    }

    Column(Modifier.fillMaxSize()) {
        // Search Header
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)) {
            LargeTitle(s.tabSearch, null, horizontalPadding = 4.dp)
            
            // Search Input Box with Mic button
            var focused by remember { mutableStateOf(false) }
            val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp)
                    .shadow(if (focused) 8.dp else 3.dp, Radius.bar, ambientColor = Color(0x331C1A17), spotColor = Color(0x331C1A17))
                    .clip(Radius.bar).background(Brand.Card)
                    .border(if (focused) 2.dp else 1.5.dp, if (focused) Brand.Kesari else Brand.Kesari.copy(alpha = 0.45f), Radius.bar)
                    .clickable { focusRequester.requestFocus() }
                    .padding(start = 8.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Brand.KesariTint), contentAlignment = Alignment.Center) {
                    Icon(painterResource(Ic.Search), null, tint = Brand.Kesari, modifier = Modifier.size(22.dp))
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 17.sp, color = Brand.Ink),
                    cursorBrush = SolidColor(Brand.Kesari),
                    modifier = Modifier.weight(1f).focusRequester(focusRequester).onFocusChanged { focused = it.isFocused },
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text(s.searchHint, fontSize = 15.sp, color = Brand.Secondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        inner()
                    }
                )
                if (query.isNotEmpty()) {
                    Box(Modifier.size(36.dp).clip(CircleShape).clickable { query = "" }, contentAlignment = Alignment.Center) {
                        Icon(painterResource(Ic.Close), null, tint = Brand.Secondary, modifier = Modifier.size(18.dp))
                    }
                }
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(Brand.KesariTint).clickable { showVoiceSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(Ic.Mic), null, tint = Brand.Kesari, modifier = Modifier.size(20.dp))
                }
            }

            // Cross-Language Filter Tabs (when searching)
            if (query.isNotEmpty()) {
                Row(
                    Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        SearchScope.ALL to s.tabAll,
                        SearchScope.EN to s.tabEnglish,
                        SearchScope.HI to s.tabHindi,
                        SearchScope.BN to s.tabBangla
                    )
                    tabs.forEach { (sc, label) ->
                        val on = activeScope == sc
                        Pill(text = label, on = on, onClick = { activeScope = sc })
                    }
                }
            }
        }

        // Search Content / Results
        if (query.isEmpty()) {
            // Landing State: 2-Column Curated Stories & Thematic Lila Guide
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = BottomRoom),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(2) }) {
                    SectionHeading(tr(ui, "Stories", "कथाएँ", "কাহিনি"), Modifier.padding(top = 4.dp))
                }

                // Category Filter Pills
                item(span = { GridItemSpan(2) }) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EpisodeCategory.entries.forEach { cat ->
                            val on = selectedCategory == cat
                            Pill(text = cat.label(ui), on = on, onClick = { selectedCategory = cat })
                        }
                    }
                }

                // Filtered Episodes in 2-Column Grid
                val displayedEpisodes = if (selectedCategory == EpisodeCategory.ALL) {
                    Episodes.all
                } else {
                    Episodes.all.filter { it.category == selectedCategory }
                }

                items(displayedEpisodes, key = { "${it.s}.${it.a}" }) { ep ->
                    StoryCard(ep, state, onOpenChapter)
                }
            }
        } else {
            // Active Search Results
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = BottomRoom),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Direct Verse Jump Card
                if (parsedJump != null) {
                    item {
                        val (sk, ad, vs) = parsedJump
                        val skTitle = if (sk == 0) s.mahatmya else SampleData.skandha(sk).title(ui)
                        Box(
                            Modifier.fillMaxWidth().tappable(Radius.card) { onOpenChapter(sk, ad) }
                                .background(Brand.KesariTint).border(1.dp, Brand.Kesari, Radius.card)
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(painterResource(Ic.MenuBook), null, tint = Brand.Kesari, modifier = Modifier.size(24.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(localDigits("${s.goTo} $skTitle · ${s.adhyaya} $ad · ${s.shloka} $vs", ui), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Brand.Ink)
                                    Text(SampleData.adhyayaTitle(sk, ad, state.titleLang, s), fontSize = 13.sp, color = Brand.Secondary, maxLines = 1)
                                }
                                Icon(painterResource(Ic.KeyboardArrowRight), null, tint = Brand.Kesari)
                            }
                        }
                    }
                }

                // 2. Incident Matches
                if (matchingIncidents.isNotEmpty()) {
                    item {
                        SectionHeading(tr(ui, "Matching Divine Incidents", "प्रासंगिक लीला प्रसंग", "অনুরূপ লীলা প্রসঙ্গ"))
                    }
                    items(matchingIncidents, key = { "inc_${it.s}_${it.a}" }) { ep ->
                        Box(
                            Modifier.fillMaxWidth().tappable(Radius.group) { onOpenChapter(ep.s, ep.a) }
                                .background(Brand.Card).border(1.dp, Brand.Separator, Radius.group)
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(ep.title(ui), Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Brand.Ink)
                                    Text(localDigits("${if (ep.s == 0) s.mahatmya else "${s.skandha} ${ep.s}"} · ${s.adhyaya} ${ep.a}", ui), fontSize = 12.sp, color = Brand.Gold)
                                }
                                if (ep.description(ui).isNotEmpty()) {
                                    Text(ep.description(ui), fontSize = 13.sp, lineHeight = 18.sp, color = Brand.Secondary)
                                }
                            }
                        }
                    }
                }

                // 3. Verse Matches Count Header
                item {
                    val countStr = localDigits("${results.size} ${if (results.size == 1) s.result else s.results}", ui)
                    SectionHeading(countStr)
                }

                // 4. Verse Result Cards
                if (results.isEmpty() && matchingIncidents.isEmpty() && parsedJump == null) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(s.noMatches, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Brand.Ink)
                            Text(s.noMatchesHint, fontSize = 14.sp, color = Brand.Secondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                } else {
                    items(results, key = { "${it.verse.ref}_${it.layer.name}" }) { res ->
                        val v = res.verse
                        val skName = if (v.skandha == 0) s.mahatmya else "${s.skandha} ${v.skandha}"
                        val refText = localDigits("$skName · ${s.adhyaya} ${v.adhyaya} · ${s.shloka} ${v.num}", ui)
                        Box(
                            Modifier.fillMaxWidth().tappable(Radius.group) { onOpenChapter(v.skandha, v.adhyaya) }
                                .background(Brand.Card).border(1.dp, Brand.Separator, Radius.group)
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(refText, Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Brand.Gold)
                                    Text(res.layer.label, fontSize = 11.sp, color = Brand.Secondary)
                                }
                                val excerpt = buildAnnotatedString {
                                    append(res.hit.pre)
                                    withStyle(SpanStyle(color = Brand.Kesari, fontWeight = FontWeight.Bold, background = Brand.KesariTint)) {
                                        append(res.hit.hit)
                                    }
                                    append(res.hit.post)
                                }
                                Text(excerpt, fontFamily = res.layer.font, fontSize = res.layer.size.sp, lineHeight = (res.layer.size * 1.5).sp, color = Brand.Ink)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showVoiceSheet) {
        VoiceNavigatorSheet(state, onOpenChapter = onOpenChapter) { showVoiceSheet = false }
    }
}
