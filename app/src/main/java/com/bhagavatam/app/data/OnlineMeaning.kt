package com.bhagavatam.app.data

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** One part of speech of a word, with its first meanings. */
data class MeaningEntry(val lang: String, val pos: String, val defs: List<String>)

sealed class Meaning {
    data class Found(val entries: List<MeaningEntry>) : Meaning()
    object NotFound : Meaning()
    object Offline : Meaning()
}

/**
 * Meanings from Wiktionary's public definition service (English, Hindi, Bengali and Sanskrit words all have entries there),
 * fetched on demand and kept in memory. Only the single tapped word is sent. Text is community-written (CC BY-SA); the card says so.
 * An offline dictionary will sit in front of this once it is built.
 */
object OnlineMeaning {
    private val cache = HashMap<String, Meaning>()
    private val tag = Regex("<[^>]+>")

    /** Sections to prefer, in order, for a word tapped in [lang]. */
    private fun preferred(lang: Lang) = when (lang) {
        Lang.EN -> listOf("en")
        Lang.HI -> listOf("hi", "sa", "mr", "ne")
        Lang.BN -> listOf("bn", "sa", "hi")
        Lang.SA -> listOf("sa", "hi", "mr", "ne", "bn")
    }

    private val suffixes = mapOf(
        Lang.BN to listOf("গুলোর", "গুলো", "দের", "েরা", "ের", "কে", "তে", "েই", "টি", "রা", "র", "ে", "ই", "ও"),
        Lang.HI to listOf("ाओं", "ओं", "ें", "ों", "ने", "को", "का", "की", "के", "से", "में", "पर", "ा", "ी", "े"),
        Lang.SA to listOf("स्य", "ाय", "ेन", "ेषु", "ानि", "ाः", "ः", "म्", "ं"),
        Lang.EN to listOf("ing", "ed", "es", "ly", "s"),
    )

    /** Simpler forms of [w]: each known ending removed (longest first), and for Bengali also the "ৎ" spelling of a final "ত". */
    internal fun stems(w: String, lang: Lang): List<String> {
        val out = ArrayList<String>()
        for (suf in (suffixes[lang] ?: emptyList()).sortedByDescending { it.length }) {
            if (w.length > suf.length + 1 && w.endsWith(suf)) {
                val stem = w.dropLast(suf.length)
                out.add(stem)
                if (lang == Lang.BN && stem.endsWith("ত")) out.add(stem.dropLast(1) + "ৎ")
            }
        }
        return out
    }

    /** Bengali script to Devanagari (the two scripts are laid out alike). Bengali "ব" is both ब and व, so both readings are returned. */
    internal fun bengaliToDevanagari(w: String): List<String> {
        fun conv(ch: Char, ba: Char): String = when (ch) {
            'ৎ' -> "त्"
            'ব' -> ba.toString()
            in 'ঁ'..'৿' -> (ch.code - 0x80).toChar().toString()
            else -> ch.toString()
        }
        val ba = w.count { it == 'ব' }
        if (ba > 3) return emptyList()
        return listOf('ब', 'व').map { b -> w.map { conv(it, b) }.joinToString("") }.distinct()
    }

    suspend fun lookup(word: String, lang: Lang): Meaning {
        val w = word.trim().trim('.', ',', ';', ':', '!', '?', '"', '।', '॥', '(', ')', '‘', '’', '“', '”')
        if (w.isEmpty()) return Meaning.NotFound
        val key = "${lang.code}:$w"
        synchronized(cache) { cache[key] }?.let { return it }
        val result = withContext(Dispatchers.IO) {
            // Every spelling worth trying is fetched at once (the exact word, simpler forms, and for Bengali the same word in Devanagari,
            // because many of these words are Sanskrit and have entries there). The best hit in priority order wins.
            val base = (listOf(w, w.lowercase()) + stems(w, lang)).distinct()
            val candidates = (base + if (lang == Lang.BN) base.flatMap { bengaliToDevanagari(it) } else emptyList()).distinct().take(10)
            val results = coroutineScope { candidates.map { c -> async { fetch(c, lang) } }.awaitAll() }
            results.firstOrNull { it is Meaning.Found }
                ?: if (results.any { it is Meaning.Offline }) Meaning.Offline else Meaning.NotFound
        }
        if (result !is Meaning.Offline) synchronized(cache) { cache[key] = result }
        return result
    }

    private fun fetch(word: String, lang: Lang): Meaning = try {
        val url = URL("https://en.wiktionary.org/api/rest_v1/page/definition/" + URLEncoder.encode(word, "UTF-8").replace("+", "%20"))
        val c = url.openConnection() as HttpURLConnection
        c.connectTimeout = 6000; c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "BhagavatamReader/0.4 (personal offline reader; single-word lookups)")
        c.setRequestProperty("Accept", "application/json")
        when (c.responseCode) {
            200 -> parse(c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }, lang)
            404 -> Meaning.NotFound
            else -> Meaning.Offline
        }
    } catch (_: IOException) {
        Meaning.Offline
    } catch (_: org.json.JSONException) {
        Meaning.NotFound
    }

    private fun clean(html: String): String =
        Html.fromHtml(tag.replace(html, ""), Html.FROM_HTML_MODE_COMPACT).toString().replace(Regex("\\s+"), " ").trim()

    internal fun parse(body: String, lang: Lang): Meaning {
        val root = JSONObject(body)
        val sections = root.keys().asSequence().toList()
        val order = preferred(lang).filter { it in sections } + sections.filter { it !in preferred(lang) }.take(1)
        val entries = ArrayList<MeaningEntry>()
        for (code in order.take(2)) {
            val arr = root.optJSONArray(code) ?: continue
            for (i in 0 until minOf(arr.length(), 3)) {
                val e = arr.getJSONObject(i)
                val defsArr = e.optJSONArray("definitions") ?: continue
                val defs = (0 until minOf(defsArr.length(), 3)).map { clean(defsArr.getJSONObject(it).optString("definition")) }.filter { it.isNotEmpty() }
                if (defs.isNotEmpty()) entries.add(MeaningEntry(code, e.optString("partOfSpeech"), defs))
            }
            if (entries.isNotEmpty()) break
        }
        return if (entries.isEmpty()) Meaning.NotFound else Meaning.Found(entries)
    }
}
