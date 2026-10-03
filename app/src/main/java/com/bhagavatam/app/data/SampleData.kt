package com.bhagavatam.app.data

import android.database.sqlite.SQLiteDatabase
import java.util.concurrent.ConcurrentHashMap

/**
 * The reading text. Everything here is read from content.db (built from the Gita Press PDFs by
 * content/_build/build_db.py). Only the Skandha list, the glossary and the digit helper are static.
 *
 * Sanskrit (mool) and Hindi are the Gita Press Sanskrit-Hindi edition; English is the Gita Press English
 * edition. The Bengali translation (Gita Press Bengali edition) is in for Skandha 1, Skandha 2 and Skandha 3 chapters 1 to 11
 * so far; elsewhere `bn` is empty and the app says so (see [SampleData.hasBengali]).
 */

enum class Lang(val code: String) { SA("sa"), HI("hi"), BN("bn"), EN("en") }

/** Bengali is offered as a reading layer. It is partial (see [SampleData.hasBengali]); chapters without it fall back to Hindi for listening and say so when read. */
const val BENGALI_READY = true

enum class SanskritScript { DEVANAGARI, BENGALI, IAST }

data class Verse(
    val skandha: Int,
    val adhyaya: Int,
    val num: Int,
    val sa: List<String>,
    val iast: List<String>,
    val en: String,
    val hi: String,
    val bn: String = "",
    val speaker: String? = null,
    /** Last shloka number when one block covers a range, e.g. 5-6. */
    val numEnd: Int = num,
    /** When the English text is printed with another verse of the group, that verse's number. */
    val enFrom: Int? = null,
    /** Same for the Hindi bhavartha (Gita Press prints it once for a group of shlokas). */
    val hiFrom: Int? = null,
    /** Same for the Bengali translation: for a joint verse, the verse that carries it. */
    val bnFrom: Int? = null,
) {
    val ref get() = "$skandha.$adhyaya.$num"

    /** True when this block carries its own text in [lang] (grouped verses print the translation only once). */
    fun hasText(lang: Lang) = when (lang) { Lang.HI -> hi; Lang.BN -> bn; Lang.EN -> en; else -> "x" }.isNotEmpty()

    val numLabel get() = if (numEnd > num) "$num-$numEnd" else "$num"

    fun translation(lang: Lang): String {
        val t = when (lang) { Lang.HI -> hi; Lang.BN -> bn; else -> en }
        if (t.isNotEmpty()) return t
        return when (lang) {
            Lang.BN -> if (bnFrom != null) "→ শ্লোক $bnFrom-এর অনুবাদের সঙ্গে" else "বাংলা অনুবাদ এখনও যোগ করা হয়নি।"
            Lang.HI -> if (hiFrom != null) "\u2192 \u0936\u094d\u0932\u094b\u0915 $hiFrom \u0915\u0947 \u092d\u093e\u0935\u093e\u0930\u094d\u0925 \u0915\u0947 \u0938\u093e\u0925" else "\u092d\u093e\u0935\u093e\u0930\u094d\u0925 \u0909\u092a\u0932\u092c\u094d\u0927 \u0928\u0939\u0940\u0902\u0964"
            else -> if (enFrom != null) "\u2192 Translated together with verse $enFrom" else "No English translation is printed for this verse."
        }
    }
}

data class Skandha(
    val num: Int, // 0 = Mahatmya
    val adhyayaCount: Int,
    val nameSa: String,
    val titleEn: String,
    val titleHi: String,
    val titleBn: String,
) {
    fun title(ui: Lang) = when (ui) { Lang.HI -> titleHi; Lang.BN -> titleBn; else -> titleEn }
}

/** Colophon ("Thus ends ...") and Gita Press notes printed after the last shloka of a chapter. */
data class ChapterEnd(val colophon: String, val notes: String)

data class GlossaryTerm(val term: String, val dev: String, val meaning: String)

private val digitRange = Regex("(?<=\\d)[ \\t\u00a0]*[\u2013\u2014][ \\t\u00a0]*(?=\\d)")
private val longDash = Regex("[ \\t\u00a0]*(?:[\u2013\u2014]+|--+)[ \\t\u00a0]*")

/** The printed text is full of em dashes, often with no spaces around them. Shown as a plain spaced hyphen (a bare hyphen between digits). */
internal fun String.tidyDashes(): String =
    if (none { it == '\u2013' || it == '\u2014' || it == '-' }) this
    else replace(digitRange, "-").replace(longDash, " - ").trim(' ', '\t')

/** Characters ignored when comparing a heading: whitespace, joiners, dashes and punctuation. */
private fun isNoise(c: Char) = c.isWhitespace() || c in "‌‍ -‐‑‒–—,.;:।॥'’\"()"

/**
 * Removes [title] from the start of [text] when the chapter heading leaked into it. Matching ignores spacing and
 * punctuation and allows a couple of spelling differences, so a heading printed as "मोहिनी-रूपसे" still matches
 * the title stored as "मोहिनीरूपसे". Text that does not start with the title is returned unchanged.
 */
internal fun stripLeadingTitle(text: String, title: String): String {
    val t = title.filterNot(::isNoise)
    if (t.length < 10 || text.isEmpty()) return text
    val head = text.take(title.length * 2 + 40)
    val h = StringBuilder(); val at = ArrayList<Int>()
    head.forEachIndexed { i, ch -> if (!isNoise(ch)) { h.append(ch); at.add(i) } }
    val n = t.length; val m = minOf(h.length, n + 4)
    if (m < n - 4) return text
    var prev = IntArray(m + 1) { it }
    for (i in 1..n) {
        val cur = IntArray(m + 1); cur[0] = i
        for (j in 1..m) cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (t[i - 1] == h[j - 1]) 0 else 1)
        prev = cur
    }
    var bestJ = -1; var bestD = Int.MAX_VALUE
    for (j in maxOf(1, n - 4)..m) if (prev[j] < bestD) { bestD = prev[j]; bestJ = j }
    if (bestJ < 0 || bestD > maxOf(1, n / 12)) return text
    return text.substring(at[bestJ - 1] + 1).trimStart { it.isWhitespace() || it == '‌' || it == '‍' || it == '-' || it == '–' || it == '—' }
}

object SampleData {

    val skandhas = listOf(
        Skandha(0, 6, "श्रीमद्भागवत माहात्म्य", "Glory of the Bhagavatam", "श्रीमद्भागवत माहात्म्य", "শ্রীমদ্ভাগবত মাহাত্ম্য"),
        Skandha(1, 19, "प्रथम स्कन्ध", "Sages at Naimisharanya", "नैमिषारण्य के ऋषि", "নৈমিষারণ্যের ঋষিগণ"),
        Skandha(2, 10, "द्वितीय स्कन्ध", "Shuka begins", "शुकदेवजी का आरम्भ", "শুকদেবের সূচনা"),
        Skandha(3, 33, "तृतीय स्कन्ध", "Vidura and Maitreya", "विदुर और मैत्रेय", "বিদুর ও মৈত্রেয়"),
        Skandha(4, 31, "चतुर्थ स्कन्ध", "Dhruva and Prithu", "ध्रुव और पृथु", "ধ্রুব ও পৃথু"),
        Skandha(5, 26, "पञ्चम स्कन्ध", "Rishabha and Bharata", "ऋषभ और भरत", "ঋষভ ও ভরত"),
        Skandha(6, 19, "षष्ठ स्कन्ध", "Ajamila and Vritra", "अजामिल और वृत्र", "অজামিল ও বৃত্র"),
        Skandha(7, 15, "सप्तम स्कन्ध", "Prahlada and Narasimha", "प्रह्लाद और नृसिंह", "প্রহ্লাদ ও নৃসিংহ"),
        Skandha(8, 24, "अष्टम स्कन्ध", "Gajendra and the churning", "गजेन्द्र और समुद्र-मन्थन", "গজেন্দ্র ও সমুদ্রমন্থন"),
        Skandha(9, 24, "नवम स्कन्ध", "Rama and the royal lines", "श्रीराम और राजवंश", "শ্রীরাম ও রাজবংশ"),
        Skandha(10, 90, "दशम स्कन्ध", "Krishna’s life", "श्रीकृष्ण-लीला", "শ্রীকৃষ্ণলীলা"),
        Skandha(11, 31, "एकादश स्कन्ध", "Uddhava Gita", "उद्धव गीता", "উদ্ধব গীতা"),
        Skandha(12, 13, "द्वादश स्कन्ध", "Kali yuga and the close", "कलियुग और उपसंहार", "কলিযুগ ও উপসংহার"),
    )


    fun skandha(n: Int) = skandhas.first { it.num == n }

    /** Every chapter in reading order: the Mahatmya, then Skandha 1 to 12. */
    private val order by lazy { skandhas.flatMap { sk -> (1..sk.adhyayaCount).map { sk.num to it } } }

    /** The chapter [step] places after (or, when negative, before) this one, across Skandha boundaries; null at either end. */
    fun neighbour(s: Int, a: Int, step: Int): Pair<Int, Int>? {
        val i = order.indexOf(s to a)
        return if (i < 0) null else order.getOrNull(i + step)
    }

    // ---------- content.db ----------
    @Volatile private var db: SQLiteDatabase? = null
    private val titlesEn = HashMap<String, String>()
    private val titlesHi = HashMap<String, String>()
    private val titlesBn = HashMap<String, String>()
    private val bengaliChapters = HashSet<String>()
    @Volatile private var hasBn = false
    @Volatile private var hasIastPlain = false
    private val cache = ConcurrentHashMap<String, List<Verse>>()
    private val verseCounts = HashMap<String, Int>()
    private val endCache = ConcurrentHashMap<String, ChapterEnd>()

    val isLoaded get() = db != null

    /** Called once from ContentDb.open(). */
    @Synchronized
    fun attach(d: SQLiteDatabase) {
        db = d
        cache.clear(); titlesEn.clear(); titlesHi.clear(); titlesBn.clear(); bengaliChapters.clear(); verseCounts.clear(); endCache.clear()
        // An older text database has no Bengali columns; the app must still open it.
        hasBn = d.rawQuery("PRAGMA table_info(verse)", null).use { c -> var found = false; while (c.moveToNext()) if (c.getString(1) == "bn") found = true; found }
        hasIastPlain = d.rawQuery("PRAGMA table_info(verse)", null).use { c -> var found = false; while (c.moveToNext()) if (c.getString(1) == "iast_plain") found = true; found }
        val titleBn = if (hasBn) "title_bn" else "NULL"
        d.rawQuery("SELECT skandha, chapter, title_en, title_hi, verse_count, $titleBn FROM chapter", null).use { c ->
            while (c.moveToNext()) {
                val k = "${c.getInt(0)}.${c.getInt(1)}"
                titlesEn[k] = (c.getString(2) ?: "").tidyDashes()
                titlesHi[k] = (c.getString(3) ?: "").tidyDashes()
                titlesBn[k] = (c.getString(5) ?: "").tidyDashes()
                if (!c.isNull(4)) verseCounts[k] = c.getInt(4)
            }
        }
        if (hasBn) d.rawQuery("SELECT DISTINCT skandha, chapter FROM verse WHERE bn IS NOT NULL AND bn <> ''", null).use { c ->
            while (c.moveToNext()) bengaliChapters.add("${c.getInt(0)}.${c.getInt(1)}")
        }
    }

    /** True when this chapter has the Bengali translation. Skandha 1, 2 and 3 (chapters 1 to 11) do so far. */
    fun hasBengali(s: Int, a: Int) = "$s.$a" in bengaliChapters

    /** Title of an adhyaya in [lang] (pass AppState.titleLang). Bengali titles exist for the chapters that have Bengali; elsewhere English is shown. */
    fun adhyayaTitle(s: Int, a: Int, lang: Lang, s10n: Strings): String {
        val k = "$s.$a"
        val t = when (lang) {
            Lang.HI, Lang.SA -> titlesHi[k].orEmpty().ifEmpty { titlesEn[k].orEmpty() }
            // A chapter without a Bengali title falls back to Hindi (the same script family), then English.
            Lang.BN -> titlesBn[k].orEmpty().ifEmpty { titlesHi[k].orEmpty() }.ifEmpty { titlesEn[k].orEmpty() }
            else -> titlesEn[k].orEmpty()
        }
        return t.ifEmpty { "${s10n.adhyaya} ${localDigits(a.toString(), lang)}" }
    }

    /** Number of shloka blocks in a chapter, or 0 when unknown. */
    fun verseCount(s: Int, a: Int) = verseCounts["$s.$a"] ?: 0

    private val legacyGlyphs = Regex("[\\u0080-\\u00ff\\u2021\\u2044\\u2211\\uf8ff\\u02dc\\ufb01\\ufb02]")

    /**
     * Colophon and notes for the end of a chapter, in the reading language. Some English notes were printed in the
     * legacy Nalanda font and cannot be read; those lines are left out rather than shown as garbage.
     */
    fun chapterEnd(s: Int, a: Int, lang: Lang): ChapterEnd {
        val key = "$s.$a.${if (lang == Lang.HI || lang == Lang.SA) "hi" else "en"}"
        return endCache.getOrPut(key) { loadChapterEnd(s, a, lang) }
    }

    private fun loadChapterEnd(s: Int, a: Int, lang: Lang): ChapterEnd {
        val d = db ?: return ChapterEnd("", "")
        d.rawQuery("SELECT notes_en, notes_hi, colophon_en FROM chapter WHERE skandha = ? AND chapter = ?", arrayOf(s.toString(), a.toString())).use { c ->
            if (!c.moveToFirst()) return ChapterEnd("", "")
            val notesEn = c.getString(0).orEmpty(); val notesHi = c.getString(1).orEmpty(); val colEn = c.getString(2).orEmpty()
            fun clean(t: String) = t.lines().map { it.removePrefix("- ").trim() }.filter { it.isNotEmpty() && it != "*" && !legacyGlyphs.containsMatchIn(it) }.joinToString("\n").tidyDashes()
            if (lang == Lang.HI || lang == Lang.SA) {
                val flat = notesHi.lines().joinToString(" ") { it.removePrefix("- ").trim() }.replace(Regex("\\s+"), " ")
                val m = Regex("इति श्रीमद्.*?॥\\s*[०-९0-9]+\\s*॥").find(flat)
                val colophon = m?.value.orEmpty()
                val rest = if (m != null) notesHi.lines().joinToString("\n").replace(Regex("(?s)- \\*\\s*\\n- इति श्रीमद्.*?॥\\s*[०-९0-9]+\\s*॥\\s*\\n?"), "") else notesHi
                return ChapterEnd(colophon.tidyDashes(), clean(rest))
            }
            val colophon = colEn.substringBefore("\n\n### Notes").removeSuffix("*").trim()
            return ChapterEnd(colophon.tidyDashes(), clean(notesEn))
        }
    }

    fun hasTitle(s: Int, a: Int) = titlesEn.containsKey("$s.$a")

    private fun readVerses(where: String, args: Array<String>): List<Verse> {
        val d = db ?: return emptyList()
        val out = ArrayList<Verse>()
        var prevChapter = ""
        d.rawQuery(
            "SELECT skandha, chapter, num, num_end, speaker, sa, iast, hi, en, en_from, hi_from, ${if (hasBn) "bn, bn_from" else "NULL, NULL"} FROM verse $where ORDER BY skandha, chapter, num",
            args,
        ).use { c ->
            while (c.moveToNext()) {
                val key = "${c.getInt(0)}.${c.getInt(1)}"
                val firstOfChapter = key != prevChapter
                prevChapter = key
                var sa = c.getString(5).orEmpty()
                var hi = c.getString(7).orEmpty()
                var speaker = c.getString(4)?.takeIf { it.isNotBlank() }
                if (firstOfChapter) {
                    // In the source PDFs the chapter heading ran into the first shloka. The reader shows the title once, above the text.
                    val title = titlesHi[key].orEmpty()
                    val cleanSa = stripLeadingTitle(sa, title)
                    if (cleanSa != sa) {
                        sa = cleanSa
                        // What is left on top is the speaker line, which every other shloka keeps in its own field.
                        val head = sa.substringBefore('\n')
                        if (head.contains("उवाच")) { if (speaker == null) speaker = head.trim(); sa = sa.substringAfter('\n', "") }
                    }
                    hi = stripLeadingTitle(hi, title)
                }
                out.add(
                    Verse(
                        skandha = c.getInt(0), adhyaya = c.getInt(1), num = c.getInt(2), numEnd = c.getInt(3),
                        speaker = speaker,
                        sa = if (sa.isEmpty()) emptyList() else sa.split('\n'),
                        iast = c.getString(6).orEmpty().tidyDashes().let { if (it.isEmpty()) emptyList() else it.split('\n') },
                        hi = hi.tidyDashes(), en = c.getString(8).orEmpty().tidyDashes(), bn = c.getString(11).orEmpty().tidyDashes(),
                        bnFrom = if (c.isNull(12)) null else c.getInt(12),
                        enFrom = if (c.isNull(9)) null else c.getInt(9),
                        hiFrom = if (c.isNull(10)) null else c.getInt(10),
                    )
                )
            }
        }
        return out
    }

    fun versesFor(s: Int, a: Int): List<Verse> =
        cache.getOrPut("$s.$a") { readVerses("WHERE skandha = ? AND chapter = ?", arrayOf(s.toString(), a.toString())) }

    /** Targeted SQLite search matching any relevant language column. */
    fun searchVerses(query: String, scope: String = "ALL", limit: Int = 100): List<Verse> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        val pattern = "%$q%"
        val qPlain = com.bhagavatam.app.ui.screens.SearchFinder.foldQuery(q)
        val plainPattern = "%$qPlain%"
        val iastCondition = if (hasIastPlain) "(iast LIKE ? OR iast_plain LIKE ?)" else "iast LIKE ?"
        val iastArgs = if (hasIastPlain) listOf(pattern, plainPattern) else listOf(pattern)

        val (whereClause, args) = when (scope.uppercase()) {
            "EN" -> "WHERE en LIKE ?" to listOf(pattern)
            "HI" -> "WHERE hi LIKE ?" to listOf(pattern)
            "BN" -> "WHERE bn LIKE ?" to listOf(pattern)
            "SA" -> "WHERE (sa LIKE ? OR $iastCondition)" to (listOf(pattern) + iastArgs)
            else -> {
                val conditions = mutableListOf("en LIKE ?", "hi LIKE ?", "sa LIKE ?", iastCondition)
                val allArgs = mutableListOf(pattern, pattern, pattern)
                allArgs.addAll(iastArgs)
                if (hasBn) {
                    conditions.add("bn LIKE ?")
                    allArgs.add(pattern)
                }
                "WHERE (" + conditions.joinToString(" OR ") + ")" to allArgs
            }
        }
        return readVerses("$whereClause LIMIT $limit", args.toTypedArray())
    }

    /** Every verse, evaluated on-demand if needed. */
    val allVerses: List<Verse> by lazy { readVerses("", emptyArray()) }

    fun verse(ref: String): Verse? {
        val p = ref.split('.')
        if (p.size != 3) return null
        val s = p[0].toIntOrNull() ?: return null; val a = p[1].toIntOrNull() ?: return null; val n = p[2].toIntOrNull() ?: return null
        return versesFor(s, a).firstOrNull { n >= it.num && n <= it.numEnd }
    }

    /** The first chapter, used as the starting queue of the player and the onboarding preview. */
    val adhyaya1: List<Verse> get() = versesFor(1, 1)

    private val famous = listOf("1.1.1", "1.2.6", "1.2.11", "1.3.28", "1.8.18", "10.29.1", "11.2.37", "12.13.23", "2.3.10", "6.3.22", "7.5.23", "8.3.1")

    /** A well-known shloka that changes every day. */
    val shlokaOfTheDay: Verse
        get() {
            val day = (System.currentTimeMillis() / 86_400_000L).toInt()
            for (i in famous.indices) {
                verse(famous[(day + i) % famous.size])?.takeIf { it.sa.isNotEmpty() }?.let { return it }
            }
            return adhyaya1.firstOrNull { it.sa.isNotEmpty() } ?: adhyaya1.first()
        }

    val glossary = listOf(
        GlossaryTerm("Adhyaya", "अध्याय", "A chapter within a Skandha."),
        GlossaryTerm("Kunti", "कुन्ती", "Mother of the Pandavas, whose prayers fill Adhyaya 1.8."),
        GlossaryTerm("Naimisharanya", "नैमिषारण्य", "The forest where the sages gathered to hear the Bhagavatam."),
        GlossaryTerm("Narada", "नारद", "The divine sage who urges Vyasa to write the Bhagavatam."),
        GlossaryTerm("Parikshit", "परीक्षित्", "The king to whom Shuka told the Bhagavatam."),
        GlossaryTerm("Rasa", "रास", "Krishna’s circle dance with the gopis, in Skandha 10."),
        GlossaryTerm("Shaunaka", "शौनक", "Leader of the sages at Naimisharanya."),
        GlossaryTerm("Shuka", "शुक", "Son of Vyasa, who recites the Bhagavatam."),
        GlossaryTerm("Skandha", "स्कन्ध", "One of the twelve books of the Bhagavatam."),
        GlossaryTerm("Suta", "सूत", "The narrator who retells it to the sages."),
    )
}

private val devaDigits = "०१२३४५६७८९"
private val bengDigits = "০১২৩৪৫৬৭৮৯"

/** Swap ASCII digits for Devanagari or Bengali digits to match the interface language. */
fun localDigits(s: String, ui: Lang): String = when (ui) {
    Lang.HI -> s.map { if (it in '0'..'9') devaDigits[it - '0'] else it }.joinToString("")
    Lang.BN -> s.map { if (it in '0'..'9') bengDigits[it - '0'] else it }.joinToString("")
    else -> s
}
