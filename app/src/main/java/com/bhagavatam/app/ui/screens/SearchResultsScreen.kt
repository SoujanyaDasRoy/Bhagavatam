package com.bhagavatam.app.ui.screens

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.Episode
import com.bhagavatam.app.data.EpisodeCategory
import com.bhagavatam.app.data.Episodes
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.LooseKey
import com.bhagavatam.app.data.Normalise
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.SearchIndex
import com.bhagavatam.app.data.SearchQuery
import com.bhagavatam.app.data.Verse
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.data.tr
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.components.Ic
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

enum class SearchVerseLayer(val label: String, val font: FontFamily, val size: Int, val lang: Lang) {
    EN("English", EnglishReading, 15, Lang.EN),
    HI("हिन्दी", TiroHindi, 16, Lang.HI),
    BN("বাংলা", NotoSerifBengali, 16, Lang.BN),
    SA("संस्कृत", NotoDevanagari, 18, Lang.SA);

    fun text(v: Verse): String = when (this) {
        EN -> v.en
        HI -> v.hi
        BN -> v.bn
        SA -> v.sa.joinToString(" ")
    }
}

data class VerseCardData(
    val verse: Verse,
    val layer: SearchVerseLayer,
    val hit: Hit
)

data class ParsedReference(
    val skandha: Int,
    val chapter: Int,
    val verse: Int
)

data class SearchUiData(
    val parsedRef: ParsedReference?,
    val matchingStories: List<Episode>,
    val sameChapterMatches: List<Pair<Int, Int>>,
    val verseCards: List<VerseCardData>,
    val suggestedWord: String?,
    val searchTimeMs: Long
)

/**
 * Enhanced snippet extractor supporting exact, folded and loose phonetic keys across scripts.
 */
object SearchSnippetHelper {
    private val wordSplit = Regex("[^\\s\\p{P}]+")

    fun extractSnippet(target: String, queryWords: List<String>, candidateKeys: Set<String>): Hit? {
        if (target.isBlank() || queryWords.isEmpty()) return null

        // 1. Direct case-insensitive match for the full query or individual words
        for (w in queryWords) {
            val simplePos = target.indexOf(w, ignoreCase = true)
            if (simplePos >= 0) {
                return makeHit(target, simplePos, (simplePos + w.length).coerceAtMost(target.length))
            }
        }

        // 2. SearchFinder diacritic folded match
        for (w in queryWords) {
            val finderHit = SearchFinder.find(target, w)
            if (finderHit != null) return finderHit
        }

        // 3. Loose phonetic sound match across words
        val matches = wordSplit.findAll(target)
        for (m in matches) {
            val word = m.value
            val k = LooseKey.keyOf(word)
            if (k.length >= 3 && candidateKeys.contains(k)) {
                return makeHit(target, m.range.first, m.range.last + 1)
            }
        }

        return null
    }

    private fun makeHit(target: String, start: Int, end: Int): Hit {
        val s = start.coerceIn(0, target.length)
        val e = end.coerceIn(s, target.length)
        val match = target.substring(s, e)

        var preStart = (s - 55).coerceIn(0, s)
        if (preStart > 0 && preStart < target.length) {
            val spacePos = target.indexOf(' ', preStart)
            if (spacePos in preStart..s) {
                preStart = spacePos + 1
            }
        }
        preStart = preStart.coerceIn(0, s)

        var postEnd = (e + 55).coerceIn(e, target.length)
        if (postEnd < target.length && postEnd >= e) {
            val spacePos = target.lastIndexOf(' ', postEnd)
            if (spacePos in e..postEnd) {
                postEnd = spacePos
            }
        }
        postEnd = postEnd.coerceIn(e, target.length)

        val pre = (if (preStart > 0) "… " else "") + target.substring(preStart, s).replace('\n', ' ')
        val post = target.substring(e, postEnd).replace('\n', ' ') + (if (postEnd < target.length) " …" else "")
        return Hit(pre, match, post)
    }
}

@Composable
fun SearchResultsScreen(
    state: AppState,
    onOpenChapter: (Int, Int) -> Unit = { _, _ -> },
    onOpenVerse: (Int, Int) -> Unit = onOpenChapter,
    onOpenGlossary: () -> Unit = {}
) {
    val s = state.strings
    val ui = state.uiLang

    var query by rememberSaveable { mutableStateOf("") }
    var activeScope by rememberSaveable { mutableStateOf(SearchScope.ALL) }
    var selectedSkandha by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedCategory by rememberSaveable { mutableStateOf(EpisodeCategory.ALL) }
    var showVoiceSheet by remember { mutableStateOf(false) }

    var recentSearches by rememberSaveable {
        mutableStateOf(listOf("Krishna", "Gajendra", "Rasa Lila", "Dhruva", "Prahlada"))
    }

    var searchData by remember { mutableStateOf<SearchUiData?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    // Debounced search query execution (150ms off-thread)
    LaunchedEffect(query, activeScope, selectedSkandha) {
        val q = query.trim()
        if (q.length < 2) {
            searchData = null
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(150)

        val data = withContext(Dispatchers.IO) {
            val t0 = System.currentTimeMillis()

            // 1. Check scripture reference jump
            val parsedRef = runCatching {
                val match = Regex("^(\\d+)[.:/\\-\\s]+(\\d+)(?:[.:/\\-\\s]+(\\d+))?$").find(q)
                if (match != null) {
                    val sk = match.groupValues[1].toIntOrNull() ?: 1
                    val ad = match.groupValues[2].toIntOrNull() ?: 1
                    val vs = match.groupValues.getOrNull(3)?.toIntOrNull() ?: 1
                    if (sk in 0..12) ParsedReference(sk, ad, vs) else null
                } else null
            }.getOrNull()

            // 2. Matching Stories from Episodes.all
            val queryWords = SearchQuery.parse(q).terms.ifEmpty { listOf(q) }
            val queryKeys = queryWords.map { LooseKey.keyOf(it) }.filter { it.length >= 3 }.toSet()

            val matchingStories = runCatching {
                Episodes.all.filter { ep ->
                    if (selectedSkandha != null && ep.s != selectedSkandha) return@filter false
                    ep.matchesQuery(q) || queryKeys.any { k ->
                        val epTexts = listOf(ep.en, ep.hi, ep.bn) + ep.keywords
                        epTexts.any { t ->
                            t.split(Regex("[\\W\\d_]+")).any { w -> LooseKey.keyOf(w) == k }
                        }
                    }
                }
            }.getOrDefault(emptyList())

            // 3. Search verses and chapters using SearchIndex (or fallback)
            var candidateVerses: List<Verse> = emptyList()
            var sameChapters: List<Pair<Int, Int>> = emptyList()
            var suggestedWord: String? = null

            var correctedKey: String? = null
            if (SearchIndex.isOpen) {
                val idxResult = SearchIndex.search(q)
                suggestedWord = idxResult.suggestedWord
                correctedKey = idxResult.correctedKey

                var vIds = idxResult.verseIds
                if (selectedSkandha != null) {
                    vIds = vIds.filter { SearchIndex.chapterOf(it).first == selectedSkandha }
                }

                candidateVerses = SampleData.versesByIds(vIds.take(100))

                if (vIds.isEmpty() && idxResult.sameChapterOnly.isNotEmpty()) {
                    sameChapters = if (selectedSkandha != null) {
                        idxResult.sameChapterOnly.filter { it.first == selectedSkandha }
                    } else {
                        idxResult.sameChapterOnly
                    }
                }
            } else {
                // Fallback to SQLite column like search if search.db is unavailable
                val verses = SampleData.searchVerses(q, activeScope.name, limit = 100)
                candidateVerses = if (selectedSkandha != null) verses.filter { it.skandha == selectedSkandha } else verses
            }

            // 4. Highlighted snippets for matching verses
            val layersToSearch = when (activeScope) {
                SearchScope.ALL -> {
                    val pref = when (state.readLang) {
                        Lang.EN -> SearchVerseLayer.EN
                        Lang.HI -> SearchVerseLayer.HI
                        Lang.BN -> SearchVerseLayer.BN
                        Lang.SA -> SearchVerseLayer.SA
                        else -> SearchVerseLayer.EN
                    }
                    listOf(pref) + SearchVerseLayer.entries.filter { it != pref }
                }
                SearchScope.EN -> listOf(SearchVerseLayer.EN)
                SearchScope.HI -> listOf(SearchVerseLayer.HI)
                SearchScope.BN -> listOf(SearchVerseLayer.BN)
            }

            val cardList = ArrayList<VerseCardData>()
            val candidateKeys = HashSet(queryKeys)
            if (correctedKey != null && correctedKey.length >= 3) {
                candidateKeys.add(correctedKey)
            }
            if (suggestedWord != null) {
                val sk = LooseKey.keyOf(suggestedWord)
                if (sk.length >= 3) candidateKeys.add(sk)
            }

            for (v in candidateVerses) {
                var foundHit = false
                for (layer in layersToSearch) {
                    val text = layer.text(v)
                    val hit = SearchSnippetHelper.extractSnippet(text, queryWords, candidateKeys)
                    if (hit != null) {
                        cardList.add(VerseCardData(v, layer, hit))
                        foundHit = true
                        break
                    }
                }
                if (!foundHit && layersToSearch.isNotEmpty()) {
                    val fallbackLayer = layersToSearch.first()
                    val fallbackText = fallbackLayer.text(v)
                    if (fallbackText.isNotBlank()) {
                        val snippet = fallbackText.take(120).replace('\n', ' ') + (if (fallbackText.length > 120) " …" else "")
                        cardList.add(VerseCardData(v, fallbackLayer, Hit("", "", snippet)))
                    }
                }
                if (cardList.size >= 100) break
            }

            SearchUiData(
                parsedRef = parsedRef,
                matchingStories = matchingStories,
                sameChapterMatches = sameChapters,
                verseCards = cardList,
                suggestedWord = suggestedWord,
                searchTimeMs = System.currentTimeMillis() - t0
            )
        }

        searchData = data
        isSearching = false

        // Record recent search on non-empty query
        if (q.length >= 3 && !recentSearches.contains(q)) {
            recentSearches = (listOf(q) + recentSearches.take(4)).distinct()
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Search Header
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp)) {
            LargeTitle(s.tabSearch, null, horizontalPadding = 4.dp)

            // Search Input Field
            var focused by remember { mutableStateOf(false) }
            val focusRequester = remember { FocusRequester() }

            Row(
                Modifier.fillMaxWidth().heightIn(min = 58.dp)
                    .shadow(if (focused) 6.dp else 2.dp, Radius.bar, ambientColor = Color(0x331C1A17), spotColor = Color(0x331C1A17))
                    .clip(Radius.bar).background(Brand.Card)
                    .border(if (focused) 2.dp else 1.5.dp, if (focused) Brand.Kesari else Brand.Kesari.copy(alpha = 0.45f), Radius.bar)
                    .padding(start = 8.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
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
                        if (query.isEmpty()) {
                            Text(s.searchHint, fontSize = 15.sp, color = Brand.Secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        inner()
                    }
                )
                if (query.isNotEmpty()) {
                    Box(Modifier.size(48.dp).clip(CircleShape).clickable { query = "" }, contentAlignment = Alignment.Center) {
                        Icon(painterResource(Ic.Close), null, tint = Brand.Secondary, modifier = Modifier.size(20.dp))
                    }
                }
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Brand.KesariTint).clickable { showVoiceSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(Ic.Mic), null, tint = Brand.Kesari, modifier = Modifier.size(20.dp))
                }
            }

            // Language Scope Filter Pills
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

                // Skandha Filter Pills
                Row(
                    Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val allLabel = tr(ui, "All Skandhas", "सभी स्कन्ध", "সব স্কন্ধ")
                    Pill(text = allLabel, on = selectedSkandha == null, onClick = { selectedSkandha = null })

                    val mahatmyaLabel = if (ui == Lang.HI) "माहात्म्य" else if (ui == Lang.BN) "মাহাত্ম্য" else "Mahatmya"
                    Pill(text = mahatmyaLabel, on = selectedSkandha == 0, onClick = { selectedSkandha = 0 })

                    for (sk in 1..12) {
                        val skNumStr = localDigits("$sk", ui)
                        Pill(text = skNumStr, on = selectedSkandha == sk, onClick = { selectedSkandha = sk })
                    }
                }
            }
        }

        // Search Content / Results
        if (query.isEmpty()) {
            // Landing State: Recent Searches + 2-Column Curated Stories
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = BottomRoom),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Recent Searches Row
                if (recentSearches.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SectionHeading(s.recent, Modifier.padding(top = 2.dp))
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recentSearches.forEach { term ->
                                    Box(
                                        Modifier.clip(Radius.field).background(Brand.KesariTint)
                                            .border(1.dp, Brand.Kesari.copy(alpha = 0.35f), Radius.field)
                                            .clickable { query = term }
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Text(term, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Brand.Ink)
                                    }
                                }
                            }
                        }
                    }
                }

                item(span = { GridItemSpan(2) }) {
                    SectionHeading(tr(ui, "Stories", "कथाएँ", "কাহিনি"), Modifier.padding(top = 6.dp))
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
            val data = searchData

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = BottomRoom),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (data != null) {
                    // 1. Direct Scripture Reference Jump Card
                    val ref = data.parsedRef
                    if (ref != null) {
                        item {
                            val skTitle = if (ref.skandha == 0) s.mahatmya else SampleData.skandha(ref.skandha).title(ui)
                            val jumpLabel = localDigits("${s.goTo} $skTitle · ${s.adhyaya} ${ref.chapter} · ${s.shloka} ${ref.verse}", ui)
                            val adTitle = SampleData.adhyayaTitle(ref.skandha, ref.chapter, state.titleLang, s)

                            Box(
                                Modifier.fillMaxWidth().tappable(Radius.card) { onOpenChapter(ref.skandha, ref.chapter) }
                                    .background(Brand.KesariTint).border(1.dp, Brand.Kesari, Radius.card)
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(painterResource(Ic.MenuBook), null, tint = Brand.Kesari, modifier = Modifier.size(24.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(jumpLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Brand.Ink)
                                        Text(adTitle, fontFamily = readingFont(state.titleLang), fontSize = 13.sp, color = Brand.Secondary, maxLines = 1)
                                    }
                                    Icon(painterResource(Ic.KeyboardArrowRight), null, tint = Brand.Kesari)
                                }
                            }
                        }
                    }

                    // 2. Did you mean suggestion card
                    if (data.suggestedWord != null && data.suggestedWord.lowercase() != query.trim().lowercase()) {
                        item {
                            val suggestionPrompt = tr(ui, "Did you mean", "क्या आपका तात्पर्य है", "আপনি কি বোঝাতে চেয়েছেন")
                            Box(
                                Modifier.fillMaxWidth()
                                    .clip(Radius.card)
                                    .background(Brand.Card)
                                    .border(1.dp, Brand.Kesari.copy(alpha = 0.5f), Radius.card)
                                    .clickable { query = data.suggestedWord }
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(painterResource(Ic.Search), null, tint = Brand.Kesari, modifier = Modifier.size(20.dp))
                                    Text(
                                        buildAnnotatedString {
                                            append("$suggestionPrompt: ")
                                            withStyle(SpanStyle(color = Brand.Kesari, fontWeight = FontWeight.Bold)) {
                                                append(data.suggestedWord)
                                            }
                                            append("?")
                                        },
                                        fontSize = 15.sp,
                                        color = Brand.Ink
                                    )
                                }
                            }
                        }
                    }

                    // 3. Matching Stories (Divine Incidents)
                    if (data.matchingStories.isNotEmpty()) {
                        item {
                            SectionHeading(tr(ui, "Matching Divine Incidents", "प्रासंगिक लीला प्रसंग", "অনুরূপ লীলা প্রসঙ্গ"))
                        }
                        items(data.matchingStories, key = { "inc_${it.s}_${it.a}" }) { ep ->
                            Box(
                                Modifier.fillMaxWidth().tappable(Radius.group) { onOpenChapter(ep.s, ep.a) }
                                    .background(Brand.Card).border(1.dp, Brand.Separator, Radius.group)
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(ep.title(ui), Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Brand.Ink)
                                        val skLabel = if (ep.s == 0) s.mahatmya else "${s.skandha} ${ep.s}"
                                        Text(localDigits("$skLabel · ${s.adhyaya} ${ep.a}", ui), fontSize = 12.sp, color = Brand.Gold)
                                    }
                                    if (ep.description(ui).isNotEmpty()) {
                                        Text(ep.description(ui), fontSize = 13.sp, lineHeight = 18.sp, color = Brand.Secondary)
                                    }
                                }
                            }
                        }
                    }

                    // 4. Words appear together in the same chapter
                    if (data.sameChapterMatches.isNotEmpty() && data.verseCards.isEmpty()) {
                        item {
                            SectionHeading(tr(ui, "Words appear in the same chapter", "ये शब्द एक ही अध्याय में मिलते हैं", "এই শব্দগুলি একই অধ্যায়ে পাওয়া যায়"))
                        }
                        items(data.sameChapterMatches, key = { "ch_${it.first}_${it.second}" }) { (sk, ad) ->
                            val skLabel = if (sk == 0) s.mahatmya else "${s.skandha} $sk"
                            val chTitle = SampleData.adhyayaTitle(sk, ad, state.titleLang, s)
                            Box(
                                Modifier.fillMaxWidth().tappable(Radius.group) { onOpenChapter(sk, ad) }
                                    .background(Brand.Card).border(1.dp, Brand.Separator, Radius.group)
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(localDigits("$skLabel · ${s.adhyaya} $ad", ui), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Brand.Gold)
                                        Text(chTitle, fontFamily = readingFont(state.titleLang), fontSize = 15.sp, color = Brand.Ink)
                                    }
                                    Icon(painterResource(Ic.KeyboardArrowRight), null, tint = Brand.Secondary)
                                }
                            }
                        }
                    }

                    // 5. Verse Matches Count Header
                    if (data.verseCards.isNotEmpty()) {
                        item {
                            val countStr = localDigits("${data.verseCards.size} ${if (data.verseCards.size == 1) s.result else s.results}", ui)
                            SectionHeading(countStr)
                        }
                    }

                    // 6. Verse Result Cards
                    if (data.verseCards.isEmpty() && data.matchingStories.isEmpty() && ref == null && data.sameChapterMatches.isEmpty()) {
                        item {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(s.noMatches, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Brand.Ink)
                                Text(s.noMatchesHint, fontSize = 14.sp, color = Brand.Secondary, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        items(data.verseCards, key = { "${it.verse.ref}_${it.layer.name}" }) { res ->
                            val v = res.verse
                            val skName = if (v.skandha == 0) s.mahatmya else "${s.skandha} ${v.skandha}"
                            val refText = localDigits("$skName · ${s.adhyaya} ${v.adhyaya} · ${s.shloka} ${v.num}", ui)

                            Box(
                                Modifier.fillMaxWidth().tappable(Radius.group) { onOpenVerse(v.skandha, v.adhyaya) }
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
                                    Text(
                                        excerpt,
                                        fontFamily = res.layer.font,
                                        fontSize = res.layer.size.sp,
                                        lineHeight = (res.layer.size * 1.5).sp,
                                        color = Brand.Ink
                                    )
                                }
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
