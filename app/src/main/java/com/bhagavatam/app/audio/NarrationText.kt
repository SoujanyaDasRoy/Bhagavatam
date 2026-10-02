package com.bhagavatam.app.audio

import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.Verse
import java.text.Normalizer

/** What kind of thing a segment is, which decides how the player shows it. */
enum class SegKind { SPEAKER, SHLOKA, SENTENCE }

/**
 * One breath of narration.
 *
 * [speech] is what the engine is given. [start] and [end] point back into the text that is shown: a range of the
 * translation for a sentence, or the line number (start == end) for a line of the shloka. -1 means "not shown".
 * [pauseMs] is the silence after it, before the listener's pause setting is applied.
 */
data class Segment(val speech: String, val start: Int, val end: Int, val pauseMs: Int, val kind: SegKind)

data class VersePlan(val lang: Lang, val segments: List<Segment>) {
    val isEmpty get() = segments.isEmpty()
    val speechChars get() = segments.sumOf { it.speech.length }
}

/** A sentence (or clause) of the shown text, by position. */
data class Span(val start: Int, val end: Int, val pauseMs: Int, val intro: Boolean = false)

/**
 * The narration layer. The canonical verse text is never changed: everything here builds a separate, speakable
 * version of it (sentence-sized pieces, cleaned and, for English, with transliterated names spelt the way they
 * are said), plus the pauses a person would leave.
 */
object NarrationText {
    // Pauses are the gap wanted between two pieces, in milliseconds. The engine already ends every utterance with some
    // silence of its own (see tailMs), so the narrator only adds whatever is missing.
    const val PAUSE_SENTENCE = 900
    const val PAUSE_CLAUSE = 450
    const val PAUSE_INTRO = 700
    const val PAUSE_VERSE_END = 1500
    const val PAUSE_PADA_CONT = 400
    const val PAUSE_PADA_END = 900
    const val PAUSE_SHLOKA_END = 1600
    const val PAUSE_SPEAKER = 700

    private const val TINY = 10
    private const val MIN_PART = 35
    private const val TERMINATORS = ".!?\u0964\u0965\u2026"
    private const val CLOSERS = "\"'\u201d\u2019)]\u00bb"

    // ---------------------------------------------------------------- plans

    fun plan(v: Verse, lang: Lang): VersePlan {
        if (lang == Lang.SA) return sanskritPlan(v)
        if (!v.hasText(lang)) return VersePlan(lang, emptyList())
        val text = when (lang) { Lang.HI -> v.hi; Lang.BN -> v.bn; else -> v.en }
        val segs = sentences(text, lang).mapNotNull { sp ->
            val said = speech(text.substring(sp.start, sp.end), lang)
            if (said.isBlank()) null else Segment(withTerminal(said, lang, sp.intro), sp.start, sp.end, sp.pauseMs, SegKind.SENTENCE)
        }.toMutableList()
        if (segs.isNotEmpty()) segs[segs.lastIndex] = segs.last().copy(pauseMs = PAUSE_VERSE_END)
        return VersePlan(lang, segs)
    }

    private fun sanskritPlan(v: Verse): VersePlan {
        val segs = ArrayList<Segment>()
        v.speaker?.takeIf { it.isNotBlank() }?.let {
            val said = speechSanskritLine(it)
            if (said.isNotBlank()) segs += Segment(said, -1, -1, PAUSE_SPEAKER, SegKind.SPEAKER)
        }
        v.sa.forEachIndexed { i, line ->
            val said = speechSanskritLine(line)
            if (said.isBlank()) return@forEachIndexed
            // A line that ends in a danda ends a half-verse: a longer rest. A line that is only a wrapped continuation gets a short one.
            val after = if (line.trimEnd().endsWith('\u0964')) PAUSE_PADA_END else PAUSE_PADA_CONT
            val pieces = breaths(said, 150)
            pieces.forEachIndexed { k, piece -> segs += Segment(piece, i, i, if (k == pieces.lastIndex) after else PAUSE_CLAUSE, SegKind.SHLOKA) }
        }
        if (segs.isNotEmpty() && segs.last().kind == SegKind.SHLOKA) segs[segs.lastIndex] = segs.last().copy(pauseMs = PAUSE_SHLOKA_END)
        return VersePlan(Lang.SA, segs)
    }

    private fun withTerminal(said: String, lang: Lang, intro: Boolean): String {
        if (said.last() in ".!?,:;\u0964") return said
        return said + when {
            intro -> if (lang == Lang.EN) ":" else ","
            lang == Lang.EN -> "."
            else -> " \u0964"
        }
    }

    /** Speaking speed in characters per second at the narrator's base rate, measured on the engine's voices. Used to estimate time left. */
    fun charsPerSecond(lang: Lang) = when (lang) { Lang.EN -> 16.5f; Lang.HI -> 13.5f; Lang.BN -> 13f; Lang.SA -> 10.2f }

    /** The silence the engine itself leaves after each utterance, in ms, measured per language (English 790, Hindi 450, Bengali 550, Sanskrit 250). */
    fun tailMs(lang: Lang) = when (lang) { Lang.EN -> 790; Lang.HI -> 450; Lang.BN -> 550; Lang.SA -> 250 }

    fun estimateSeconds(plan: VersePlan, pauseScale: Float = 1f, speed: Float = 1f): Float {
        val gaps = plan.segments.sumOf { maxOf(it.pauseMs * pauseScale, tailMs(plan.lang).toFloat()).toDouble() }.toFloat()
        return plan.speechChars / (charsPerSecond(plan.lang) * speed) + gaps / 1000f
    }

    // ------------------------------------------------------------ sentences

    fun sentences(text: String, lang: Lang): List<Span> {
        val n = text.length
        val raw = ArrayList<Span>()
        var start = 0
        // English keeps "Suta says:" with its sentence: the engine already leaves ~0.8 s after each utterance, so a cut there is a hole.
        val intro = if (lang == Lang.EN) -1 else introEnd(text, lang)
        if (intro > 0) {
            span(text, 0, intro, PAUSE_INTRO, intro = true)?.let { raw += it }
            start = skipSpace(text, intro)
        }
        var i = start
        while (i < n) {
            val c = text[i]
            if (c in TERMINATORS) {
                var j = i + 1
                while (j < n && (text[j] in TERMINATORS || text[j] in CLOSERS)) j++
                val boundary = j >= n || text[j].isWhitespace()
                val notReally = c == '.' && (isAbbreviation(text, i) || (lang == Lang.EN && nextIsLower(text, j)))
                if (boundary && !notReally) {
                    span(text, start, j, PAUSE_SENTENCE)?.let { raw += it }
                    start = skipSpace(text, j)
                    i = start
                    continue
                }
                i = j
                continue
            }
            i++
        }
        if (start < n) span(text, start, n, PAUSE_SENTENCE)?.let { raw += it }

        // A fragment of a few letters is joined to the sentence before it, so the voice does not stutter.
        val merged = ArrayList<Span>()
        for (sp in raw) {
            val prev = merged.lastOrNull()
            if (prev != null && !prev.intro && !sp.intro && sp.end - sp.start < TINY) merged[merged.lastIndex] = Span(prev.start, sp.end, sp.pauseMs)
            else merged += sp
        }
        val max = if (lang == Lang.EN) 380 else 150
        return merged.flatMap { splitLong(text, it, max) }
    }

    private fun span(text: String, from: Int, to: Int, pause: Int, intro: Boolean = false): Span? {
        var s = from
        var e = to
        while (s < e && (text[s].isWhitespace() || text[s] == '\u00a0')) s++
        while (e > s && (text[e - 1].isWhitespace() || text[e - 1] == '\u00a0')) e--
        return if (e > s) Span(s, e, pause, intro) else null
    }

    private fun skipSpace(text: String, from: Int): Int {
        var i = from
        while (i < text.length && (text[i].isWhitespace() || text[i] == '\u00a0')) i++
        return i
    }

    private val abbreviations = setOf("viz", "i.e", "e.g", "etc", "vs", "cf", "no", "st", "mr", "mrs", "dr", "lit", "approx")

    private fun isAbbreviation(text: String, dot: Int): Boolean {
        var k = dot
        while (k > 0 && (text[k - 1].isLetter() || text[k - 1] == '.')) k--
        val word = text.substring(k, dot).lowercase()
        return word in abbreviations || (word.length == 1 && text[k].isUpperCase())
    }

    private fun nextIsLower(text: String, from: Int): Boolean {
        val i = skipSpace(text, from)
        return i < text.length && text[i].isLowerCase()
    }

    // The words that open a spoken line ("Suta says:", "श्रीसूतजी कहते हैं -"), so the speaker is announced on its own.
    private val sayingEn = listOf("says", "said", "replied", "asked", "continued", "went on", "began", "spoke", "addressed", "exclaimed", "narrated", "answered", "inquired", "enquired", "resumed", "remarked", "observed", "declared")
    private val sayingHi = listOf("कहते हैं", "कहती हैं", "ने कहा", "कहा", "बोले", "बोलीं", "बोली", "पूछा", "पूछते हैं", "उवाच")
    private val sayingBn = listOf("বললেন", "বলেন", "বলছেন", "জিজ্ঞাসা", "প্রশ্ন", "উত্তর", "বলিলেন")

    private fun introEnd(text: String, lang: Lang): Int {
        val limit = minOf(text.length, 80)
        var d = -1
        for (k in 0 until limit) {
            val c = text[k]
            val spacedHyphen = (c == '-' || c == '\u2013' || c == '\u2014') && k > 0 && (text[k - 1] == ' ' || text[k - 1] == '\u00a0') && k + 1 < text.length && text[k + 1] == ' '
            if (c == ':' || spacedHyphen) { d = k; break }
        }
        if (d < 0 || d + 1 >= text.length) return -1
        val prefix = text.substring(0, d).lowercase()
        if (prefix.any { it in ".!?\u0964\u0965" }) return -1
        val words = when (lang) { Lang.EN -> sayingEn; Lang.BN -> sayingBn; else -> sayingHi }
        return if (words.any { prefix.contains(it) }) d + 1 else -1
    }

    private fun splitLong(text: String, sp: Span, max: Int): List<Span> {
        if (sp.end - sp.start <= max) return listOf(sp)
        val mid = (sp.start + sp.end) / 2
        var best = -1
        var bestDist = Int.MAX_VALUE
        for (k in sp.start + MIN_PART until sp.end - MIN_PART) {
            if (isClauseBreak(text, k)) {
                val d = kotlin.math.abs(k + 1 - mid)
                if (d < bestDist) { best = k + 1; bestDist = d }
            }
        }
        if (best < 0) {
            for (k in sp.start + MIN_PART until sp.end - MIN_PART) {
                if (text[k] == ' ') {
                    val d = kotlin.math.abs(k + 1 - mid)
                    if (d < bestDist) { best = k + 1; bestDist = d }
                }
            }
        }
        if (best < 0) return listOf(sp)
        val left = span(text, sp.start, best, PAUSE_CLAUSE) ?: return listOf(sp)
        val right = span(text, best, sp.end, sp.pauseMs) ?: return listOf(sp)
        return splitLong(text, left, max) + splitLong(text, right, max)
    }

    private fun isClauseBreak(text: String, k: Int): Boolean {
        val c = text[k]
        if (c in ",;:" && k + 1 < text.length && text[k + 1].isWhitespace()) return true
        return c == '-' && k > 0 && (text[k - 1] == ' ' || text[k - 1] == '\u00a0') && k + 1 < text.length && text[k + 1] == ' '
    }

    // ---------------------------------------------------------- speech text

    private val footnotes = Regex("[*\u2020\u2021]|\\[\\d+]")
    private val joiners = Regex("[\u200c\u200d\u00ad]")
    private val spacedHyphen = Regex("[\\s\u00a0]+[-\u2013\u2014][\\s\u00a0]+")
    private val trailingHyphen = Regex("[\\s\u00a0]*[-\u2013\u2014][\\s\u00a0]*$")
    private val devanagari = Regex("[\u0900-\u097f]+")
    private val devanagariWrap = Regex("(?<=[\u0900-\u097f])-\\s*(?=[\u0900-\u097f])")
    private val lineWrapHyphen = Regex("(?<=\\p{L})-\\s+(?=\\p{Ll})")
    private val brackets = Regex("[()\\[\\]]")
    private val insideWord =Regex("(?<=\\p{L})[\u2019](?=\\p{L})")
    private val quotes = Regex("[\u201c\u201d\u2018\u2019\"]")
    private val spaceBeforePunct = Regex("\\s+([,.;:!?])")
    private val doubleComma = Regex(",\\s*,+")
    private val commaBeforeStop = Regex(",\\s*([.!?;:])")
    private val leadingJunk = Regex("^[,\\s]+")
    private val manySpaces = Regex("\\s+")

    /**
     * The text as it should be said, for a translation in [lang]. The shown text is not touched.
     * English gets transliterated names spelt the way they are pronounced; every language loses footnote marks,
     * quote marks and joiners, and brackets become short spoken asides.
     */
    fun speech(src: String, lang: Lang): String {
        var t = Normalizer.normalize(src, Normalizer.Form.NFC)
        t = t.replace(footnotes, "").replace(joiners, "")
        t = t.replace(spacedHyphen, ", ").replace(trailingHyphen, "")
        t = t.replace(brackets, ", ")
        t = t.replace(insideWord, "'").replace(quotes, "")
        t = t.replace('\u00a0', ' ')
        if (lang == Lang.EN) {
            t = anglicise(expandAbbreviations(t))
            // Text the old print font garbled cannot be said; Devanagari letters inside English cannot be said by an English voice.
            if (t.any { isLegacyGlyph(it) }) return ""
            t = t.replace(devanagari, " ")
        }
        t = t.replace(lineWrapHyphen, "-")
        t = t.replace(spaceBeforePunct, "$1").replace(manySpaces, " ")
        t = t.replace(doubleComma, ",").replace(commaBeforeStop, "$1").replace(leadingJunk, "")
        t = t.replace(manySpaces, " ").trim()
        return if (lang == Lang.EN && t.count { it.isLetter() } < 3) "" else t
    }

    /** One line of a shloka, without the closing verse marker or danda, for the voice. */
    fun speechSanskritLine(line: String): String {
        var t = Normalizer.normalize(line, Normalizer.Form.NFC)
        t = t.replace(Regex("\u0965\\s*[\u0966-\u096f0-9]*\\s*\u0965"), " ")
        t = t.replace('\u0964', ' ').replace('\u0965', ' ')
        t = t.replace(footnotes, "").replace(joiners, "").replace('\u00a0', ' ')
        // A hyphen at the end of a printed line only marks the wrap ("सकलजीव- निकाय"): the word is one word.
        t = t.replace(devanagariWrap, "")
        return t.replace(manySpaces, " ").trim()
    }

    private fun expandAbbreviations(t: String) = t
        .replace(Regex("\\bviz\\.", RegexOption.IGNORE_CASE), "namely")
        .replace(Regex("\\bi\\.e\\.", RegexOption.IGNORE_CASE), "that is")
        .replace(Regex("\\be\\.g\\.", RegexOption.IGNORE_CASE), "for example")
        .replace(Regex("\\betc\\."), "and so on")

    // The usual English spelling of transliterated Sanskrit, so the engine says "Krishna", not "K-r-s-na".
    private val sounds = mapOf(
        '\u0101' to "a", '\u0100' to "A", '\u012b' to "ee", '\u012a' to "Ee", '\u016b' to "oo", '\u016a' to "Oo",
        '\u1e5b' to "ri", '\u1e5a' to "Ri", '\u1e5d' to "ri", '\u1e37' to "li",
        '\u1e45' to "n", '\u1e44' to "N", '\u00f1' to "n", '\u00d1' to "N",
        '\u1e6d' to "t", '\u1e6c' to "T", '\u1e0d' to "d", '\u1e0c' to "D", '\u1e47' to "n", '\u1e46' to "N",
        '\u015b' to "sh", '\u015a' to "Sh", '\u1e63' to "sh", '\u1e62' to "Sh",
        '\u1e43' to "m", '\u1e41' to "m", '\u1e42' to "M", '\u1e25' to "h", '\u1e24' to "H",
    )

    /** Characters that only appear when the old print font was read as plain text ("∞ﬂt YI¬EaEEt"). */
    private fun isLegacyGlyph(c: Char) = c.code in 0x80..0xFF || c.code in 0x2C6..0x2DD || c.code in 0x2200..0x22FF ||
        c == '\u2044' || c == '\u25ca' || c == '\uf8ff' || c == '\ufb01' || c == '\ufb02' || c == '\u201a' || c == '\u201e' || c == '\u0153' || c == '\u0131'

    /** Cuts text longer than [max] at the space nearest the middle, again and again, so each piece is one breath. */
    private fun breaths(text: String, max: Int): List<String> {
        if (text.length <= max) return listOf(text)
        val mid = text.length / 2
        var best = -1
        var bestDist = Int.MAX_VALUE
        for (k in MIN_PART until text.length - MIN_PART) {
            if (text[k] == ' ') {
                val d = kotlin.math.abs(k - mid)
                if (d < bestDist) { best = k; bestDist = d }
            }
        }
        if (best < 0) return listOf(text)
        return breaths(text.substring(0, best).trim(), max) + breaths(text.substring(best + 1).trim(), max)
    }

    private fun anglicise(t: String): String {
        val out = StringBuilder(t.length + 8)
        for (ch in t) out.append(sounds[ch] ?: ch.toString())
        // Anything left that is a combining mark would only confuse the engine.
        return Normalizer.normalize(out, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
    }
}
