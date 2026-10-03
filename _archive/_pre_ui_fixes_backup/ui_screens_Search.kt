package com.bhagavatam.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.Verse
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.components.LargeTitle
import com.bhagavatam.app.ui.components.Pill
import com.bhagavatam.app.ui.components.SectionLabel
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.Literata
import com.bhagavatam.app.ui.theme.TiroBangla
import com.bhagavatam.app.ui.theme.TiroHindi
import com.bhagavatam.app.ui.theme.TiroSanskrit
import java.text.Normalizer

/** A match inside some text: the excerpt split around the matched part. */
data class Hit(val pre: String, val hit: String, val post: String)

private enum class Layer(val label: String, val font: FontFamily, val size: Int) {
    SA("Mool", TiroSanskrit, 18), IAST("IAST", Literata, 15), EN("English", Literata, 15), HI("हिन्दी", TiroHindi, 16), BN("বাংলা", TiroBangla, 16);
    fun text(v: Verse) = when (this) { SA -> v.sa.joinToString(" "); IAST -> v.iast.joinToString(" "); EN -> v.en; HI -> v.hi; BN -> v.bn }
}

private const val MaxVerseResults = 150

private enum class Scope { ALL, MOOL, EN, HI, BN, CHAPTERS, GLOSSARY }

/**
 * Cheap pre-check so a keystroke does not fold 14,000 verses: English and IAST are folded once
 * (lower-case, no diacritics) and kept; Devanagari and Bengali are matched as they are.
 * Only verses that pass go through [Finder.find], which builds the highlighted excerpt.
 */
private object SearchIndex {
    private val marks = Regex("[\\u0300-\\u036f]")
    private fun fold(s: String) = marks.replace(Normalizer.normalize(s, Normalizer.Form.NFD), "").lowercase()

    private val en: Array<String> by lazy { SampleData.allVerses.map { fold(it.en) }.toTypedArray() }
    private val iast: Array<String> by lazy { SampleData.allVerses.map { fold(it.iast.joinToString(" ")) }.toTypedArray() }

    fun warm() { en.size; iast.size }

    fun mayMatch(i: Int, v: Verse, l: Layer, variants: List<String>): Boolean {
        if (variants.isEmpty()) return false
        return when (l) {
            Layer.EN -> variants.any { en[i].contains(it) }
            Layer.IAST -> variants.any { iast[i].contains(it) }
            Layer.SA -> v.sa.isNotEmpty() && variants.any { q -> v.sa.any { it.contains(q) } }
            Layer.HI -> v.hi.isNotEmpty() && variants.any { v.hi.contains(it) }
            Layer.BN -> v.bn.isNotEmpty() && variants.any { v.bn.contains(it) }
        }
    }
}

private data class VerseResult(val verse: Verse, val layer: Layer, val hit: Hit, val alsoIn: List<Layer>)

/** Case- and diacritic-insensitive search that remembers where each folded character came from. */
object Finder {
    private val marks = Regex("[\\u0300-\\u036f]")

    private fun fold(s: String): Pair<String, IntArray> {
        val out = StringBuilder(); val map = ArrayList<Int>()
        s.forEachIndexed { i, ch ->
            val d = marks.replace(Normalizer.normalize(ch.toString(), Normalizer.Form.NFD), "").lowercase()
            d.forEach { out.append(it); map.add(i) }
        }
        return out.toString() to map.toIntArray()
    }

    fun variants(q: String): List<String> {
        val base = marks.replace(Normalizer.normalize(q, Normalizer.Form.NFD), "").lowercase().trim()
        if (base.isEmpty()) return emptyList()
        return listOf(base, base.replace("sh", "s").replace("ri", "r"), base.replace("sh", "s")).distinct()
    }

    fun find(text: String, variants: List<String>): Hit? {
        if (variants.isEmpty()) return null
        val (n, map) = fold(text)
        for (v in variants) {
            val i = n.indexOf(v)
            if (i >= 0) {
                val start = map[i]; val end = map[i + v.length - 1] + 1
                var from = maxOf(0, start - 36); var to = minOf(text.length, end + 70)
                if (from > 0) text.indexOf(' ', from).let { if (it in 0 until start) from = it + 1 }
                if (to < text.length) text.lastIndexOf(' ', to).let { if (it > end) to = it }
                return Hit((if (from > 0) "…" else "") + text.substring(from, start), text.substring(start, end),
                    text.substring(end, to) + if (to < text.length) "…" else "")
            }
        }
        return null
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(state: AppState, onOpenVerse: (Int, Int) -> Unit, onOpenGlossary: () -> Unit) {
    val s = state.strings
    val ui = state.uiLang
    var query by rememberSaveable { mutableStateOfString("") }
    var scope by rememberSaveable { androidx.compose.runtime.mutableStateOf(Scope.ALL) }

    val variants = remember(query) { Finder.variants(query) }
    // Excerpts are built only for the first results shown; the counts use the cheap check alone.
    fun verseHits(layers: List<Layer>) = SampleData.allVerses.asSequence().mapIndexedNotNull { i, v ->
        val hits = layers.mapNotNull { l -> if (SearchIndex.mayMatch(i, v, l, variants)) Finder.find(l.text(v), variants)?.let { l to it } else null }
        if (hits.isEmpty()) null else VerseResult(v, hits.first().first, hits.first().second, hits.drop(1).map { it.first })
    }.take(MaxVerseResults).toList()
    fun countHits(layers: List<Layer>): Int {
        val all = SampleData.allVerses
        return all.indices.count { i -> layers.any { l -> SearchIndex.mayMatch(i, all[i], l, variants) } }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { SearchIndex.warm() }
    }
    val layersFor = mapOf(
        Scope.ALL to Layer.entries.toList(), Scope.MOOL to listOf(Layer.SA, Layer.IAST),
        Scope.EN to listOf(Layer.EN), Scope.HI to listOf(Layer.HI), Scope.BN to listOf(Layer.BN),
    )
    val chapterList = remember(state.titleLang) {
        SampleData.skandhas.flatMap { sk -> (1..sk.adhyayaCount).map { a -> Triple(sk.num, a, SampleData.adhyayaTitle(sk.num, a, state.titleLang, s)) } }
            .filter { (sk, a, _) -> SampleData.hasTitle(sk, a) }
    }
    fun chapterHits() = chapterList.mapNotNull { (sk, a, t) -> Finder.find(t, variants)?.let { Triple(sk, a, it) } }
    fun glossHits() = SampleData.glossary.mapNotNull { g ->
        val inTerm = Finder.find(g.term, variants)
        val m = inTerm ?: Finder.find(g.dev, variants) ?: Finder.find(g.meaning, variants)
        m?.let { Triple(g, it, inTerm != null) }
    }

    val counts = remember(variants, ui, state.titleLang) {
        mapOf(
            Scope.ALL to countHits(Layer.entries.toList()) + chapterHits().size + glossHits().size,
            Scope.MOOL to countHits(layersFor.getValue(Scope.MOOL)), Scope.EN to countHits(listOf(Layer.EN)),
            Scope.HI to countHits(listOf(Layer.HI)), Scope.BN to countHits(listOf(Layer.BN)),
            Scope.CHAPTERS to chapterHits().size, Scope.GLOSSARY to glossHits().size,
        )
    }
    val verses = remember(variants, scope) { layersFor[scope]?.let { verseHits(it) } ?: emptyList() }
    val chapters = remember(variants, scope, ui, state.titleLang) { if (scope == Scope.ALL || scope == Scope.CHAPTERS) chapterHits() else emptyList() }
    val gloss = remember(variants, scope) { if (scope == Scope.ALL || scope == Scope.GLOSSARY) glossHits() else emptyList() }

    // "10", "10.29" or "10.29.1" → a Go to card
    val ref = Regex("^(\\d{1,2})(?:[.\\s:](\\d{1,3}))?(?:[.\\s:](\\d{1,3}))?$").find(query.trim())
    val goto = ref?.let { m ->
        val sk = m.groupValues[1].toInt(); val a = m.groupValues[2].toIntOrNull() ?: 0; val vn = m.groupValues[3].toIntOrNull() ?: 0
        if (sk in 1..12 && a <= SampleData.skandha(sk).adhyayaCount) Triple(sk, maxOf(a, 1), vn) else null
    }
    val verseTotal = remember(variants, scope) { layersFor[scope]?.let { countHits(it) } ?: 0 }
    val total = verseTotal + chapters.size + gloss.size
    val hasQuery = variants.isNotEmpty()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = BottomRoom), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { LargeTitle(s.tabSearch) }
        item {
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(Brand.Card)
                    .border(1.dp, Brand.Separator, RoundedCornerShape(14.dp)).padding(start = 12.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Search, null, tint = Brand.Kesari)
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(s.searchHint, color = Brand.Tertiary, fontSize = 16.sp, maxLines = 1)
                    BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = TextStyle(fontSize = 16.sp, color = Brand.Ink), cursorBrush = SolidColor(Brand.Kesari),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (hasQuery) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFF0EBE3)).clickable { query = "" }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Close, "Clear search", tint = Brand.Secondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        item {
            val labels = listOf(s.all, s.mool, "English", "हिन्दी", "বাংলা", s.chapters.lowercase().replaceFirstChar { it.uppercase() }, s.glossary)
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Scope.entries.forEachIndexed { i, sc ->
                    Pill(labels[i], scope == sc, { scope = sc }, count = if (hasQuery) localDigits(counts.getValue(sc).toString(), ui) else null,
                        fontFamily = when (sc) { Scope.HI -> TiroHindi; Scope.BN -> TiroBangla; else -> null })
                }
            }
        }
        if (!hasQuery) {
            item {
                Column(Modifier.padding(top = 8.dp)) {
                    SectionLabel(s.tryThese)
                    FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Krishna", "भक्ति", "কুন্তী", "dharma", "Narada", "10.29.1").forEach { t ->
                            Box(Modifier.clip(CircleShape).background(Brand.Card).border(1.dp, Brand.Separator, CircleShape).clickable { query = t }
                                .padding(horizontal = 14.dp, vertical = 8.dp)) { Text(t, fontSize = 15.sp, color = Brand.Ink) }
                        }
                    }
                    Text(s.searchAbout, Modifier.padding(horizontal = 20.dp, vertical = 16.dp), fontSize = 13.sp, lineHeight = 19.sp, color = Brand.Secondary)
                }
            }
        }
        if (goto != null) {
            item {
                val (sk, a, vn) = goto
                Row(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brand.Card)
                        .border(1.5.dp, Brand.Kesari.copy(alpha = 0.35f), RoundedCornerShape(16.dp)).clickable { onOpenVerse(sk, a) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Brand.KesariTint), contentAlignment = Alignment.Center) {
                        Text(localDigits("$sk", ui), fontFamily = Literata, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Brand.Kesari)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(s.goTo, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Brand.Kesari, letterSpacing = 0.6.sp)
                        Text(SampleData.adhyayaTitle(sk, a, state.titleLang, s), fontFamily = readingFont(state.titleLang), fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        Text(localDigits("${s.skandha} $sk · ${s.adhyaya} $a" + if (vn > 0) " · ${s.shloka} $vn" else "", ui), fontSize = 13.sp, color = Brand.Secondary)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color(0xFFB5AEA2))
                }
            }
        }
        if (hasQuery) {
            item { Text(localDigits("$total ${if (total == 1) s.result else s.results}", ui), Modifier.padding(horizontal = 20.dp), fontSize = 13.sp, color = Brand.Secondary) }
        }
        if (verses.isNotEmpty()) {
            item { SectionHeader(s.shlokas, verseTotal, ui) }
            items(verses, key = { "v" + it.verse.ref }) { r ->
                Column(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brand.Card)
                        .clickable { onOpenVerse(r.verse.skandha, r.verse.adhyaya) }.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(localDigits(r.verse.ref, ui), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Brand.Gold)
                        Text(SampleData.adhyayaTitle(r.verse.skandha, r.verse.adhyaya, state.titleLang, s), Modifier.weight(1f), fontSize = 12.5.sp, color = Brand.Secondary, maxLines = 1)
                        Text(if (r.layer == Layer.SA) s.mool else r.layer.label, Modifier.clip(RoundedCornerShape(8.dp)).background(Brand.TealTint).padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Brand.Teal)
                    }
                    Text(highlighted(r.hit), fontFamily = r.layer.font, fontSize = r.layer.size.sp, lineHeight = (r.layer.size * 1.6f).sp,
                        color = if (r.layer == Layer.SA) Brand.Sindoor else Brand.Ink)
                    if (r.layer == Layer.SA || r.layer == Layer.IAST) {
                        Text(r.verse.en, fontFamily = Literata, fontSize = 14.sp, lineHeight = 20.sp, color = Brand.Secondary)
                    }
                    if (r.alsoIn.isNotEmpty()) Text("${s.alsoIn} " + r.alsoIn.joinToString { if (it == Layer.SA) s.mool else it.label }, fontSize = 12.sp, color = Brand.Tertiary)
                }
            }
        }
        if (chapters.isNotEmpty()) {
            item { SectionHeader(s.chapters, chapters.size, ui) }
            item {
                Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brand.Card)) {
                    chapters.forEach { (sk, a, hit) ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpenVerse(sk, a) }.heightIn(min = 52.dp).padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(localDigits("$sk.$a", ui), Modifier.width(44.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Brand.Gold)
                            Text(highlighted(hit), Modifier.weight(1f), fontFamily = Literata, fontSize = 16.sp)
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color(0xFFB5AEA2))
                        }
                    }
                }
            }
        }
        if (gloss.isNotEmpty()) {
            item { SectionHeader(s.glossary.uppercase(), gloss.size, ui) }
            items(gloss, key = { "g" + it.first.term }) { (g, hit, inTerm) ->
                Column(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brand.Card)
                        .clickable(onClick = onOpenGlossary).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(g.term, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(g.dev, fontFamily = TiroSanskrit, fontSize = 15.sp, color = Brand.Sindoor)
                    }
                    Text(if (inTerm) AnnotatedString(g.meaning) else highlighted(hit), fontSize = 14.sp, lineHeight = 19.sp, color = Brand.Secondary)
                }
            }
        }
        if (hasQuery && total == 0 && goto == null) {
            item {
                Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Search, null, tint = Brand.Tertiary, modifier = Modifier.size(32.dp))
                    Text(s.noMatches, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(s.noMatchesHint, fontSize = 14.sp, lineHeight = 20.sp, color = Brand.Secondary)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String, count: Int, ui: Lang) {
    Row(Modifier.padding(start = 20.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Brand.Secondary, letterSpacing = 0.6.sp)
        Text(localDigits(count.toString(), ui), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Brand.Tertiary)
    }
}

private fun highlighted(h: Hit) = buildAnnotatedString {
    append(h.pre)
    withStyle(SpanStyle(background = Brand.Kesari.copy(alpha = 0.18f))) { append(h.hit) }
    append(h.post)
}

private fun mutableStateOfString(v: String) = androidx.compose.runtime.mutableStateOf(v)
