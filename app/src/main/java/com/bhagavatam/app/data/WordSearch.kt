package com.bhagavatam.app.data

import com.bhagavatam.app.util.Transliterate

/** One place in the book where a word appears. */
data class Occurrence(val ref: String, val skandha: Int, val adhyaya: Int, val snippet: String)

private fun haystacks(v: Verse, lang: Lang): List<String> = when (lang) {
    Lang.SA -> v.sa + v.sa.map { Transliterate.toBengali(it) } + v.iast
    Lang.HI -> listOf(v.hi)
    Lang.BN -> listOf(v.bn)
    Lang.EN -> listOf(v.en)
}

/**
 * Where [word] appears in the book in the given language: the total, and the first [limit] places with a short snippet.
 * Whole-word and exact form only (a Sanskrit word in another case is a different form). Reads every verse, so call it off the main thread.
 */
fun occurrencesOf(word: String, lang: Lang, limit: Int = 30): Pair<Int, List<Occurrence>> {
    val w = word.trim()
    if (w.isEmpty()) return 0 to emptyList()
    val re = Regex("(?<![\\p{L}\\p{M}])" + Regex.escape(w) + "(?![\\p{L}\\p{M}])", RegexOption.IGNORE_CASE)
    var total = 0
    val out = ArrayList<Occurrence>()
    val candidateVerses = SampleData.searchVerses(w, lang.name, limit = 300)
    for (v in candidateVerses) {
        for (h in haystacks(v, lang)) {
            val m = re.find(h) ?: continue
            total++
            if (out.size < limit) {
                // Cut at spaces, never inside a word (a cut inside a Bengali or Devanagari word breaks its letters apart).
                var a = (m.range.first - 36).coerceAtLeast(0)
                if (a > 0) a = h.indexOf(' ', a).let { if (it in 0..m.range.first) it + 1 else m.range.first }
                var b = (m.range.last + 1 + 36).coerceAtMost(h.length)
                if (b < h.length) b = h.lastIndexOf(' ', b).let { if (it > m.range.last) it else m.range.last + 1 }
                out.add(Occurrence(v.ref, v.skandha, v.adhyaya, (if (a > 0) "…" else "") + h.substring(a, b).replace('\n', ' ') + (if (b < h.length) "…" else "")))
            }
            break
        }
    }
    return total to out
}
