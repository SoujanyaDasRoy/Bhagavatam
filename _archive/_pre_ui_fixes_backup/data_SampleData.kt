package com.bhagavatam.app.data

import android.database.sqlite.SQLiteDatabase
import java.util.concurrent.ConcurrentHashMap

/**
 * The reading text. Everything here is read from content.db (built from the Gita Press PDFs by
 * content/_build/build_db.py). Only the Skandha list, the glossary and the digit helper are static.
 *
 * Sanskrit (mool) and Hindi are the Gita Press Sanskrit-Hindi edition; English is the Gita Press English
 * edition. The Bengali translation has not been extracted yet, so `bn` is empty for now.
 */

enum class Lang(val code: String) { SA("sa"), HI("hi"), BN("bn"), EN("en") }

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
) {
    val ref get() = "$skandha.$adhyaya.$num"
    val numLabel get() = if (numEnd > num) "$num\u2013$numEnd" else "$num"

    fun translation(lang: Lang): String {
        val t = when (lang) { Lang.HI -> hi; Lang.BN -> bn; else -> en }
        if (t.isNotEmpty()) return t
        return when (lang) {
            Lang.BN -> "\u09ac\u09be\u0982\u09b2\u09be \u0985\u09a8\u09c1\u09ac\u09be\u09a6 \u098f\u0996\u09a8\u0993 \u09af\u09cb\u0997 \u0995\u09b0\u09be \u09b9\u09af\u09bc\u09a8\u09bf\u0964"
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

data class GlossaryTerm(val term: String, val dev: String, val meaning: String)

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

    // ---------- content.db ----------
    @Volatile private var db: SQLiteDatabase? = null
    private val titlesEn = HashMap<String, String>()
    private val titlesHi = HashMap<String, String>()
    private val cache = ConcurrentHashMap<String, List<Verse>>()

    val isLoaded get() = db != null

    /** Called once from ContentDb.open(). */
    @Synchronized
    fun attach(d: SQLiteDatabase) {
        db = d
        cache.clear(); titlesEn.clear(); titlesHi.clear()
        d.rawQuery("SELECT skandha, chapter, title_en, title_hi FROM chapter", null).use { c ->
            while (c.moveToNext()) {
                val k = "${c.getInt(0)}.${c.getInt(1)}"
                titlesEn[k] = c.getString(2) ?: ""
                titlesHi[k] = c.getString(3) ?: ""
            }
        }
        // Warm the full list in the background so the first search is instant.
        Thread { allVerses.size }.apply { isDaemon = true }.start()
    }

    /** Title of an adhyaya in [lang] (pass AppState.titleLang). Bengali titles are not extracted yet, so English is shown. */
    fun adhyayaTitle(s: Int, a: Int, lang: Lang, s10n: Strings): String {
        val k = "$s.$a"
        val t = if (lang == Lang.HI || lang == Lang.SA) titlesHi[k].orEmpty().ifEmpty { titlesEn[k].orEmpty() } else titlesEn[k].orEmpty()
        return t.ifEmpty { "${s10n.adhyaya} ${localDigits(a.toString(), lang)}" }
    }

    fun hasTitle(s: Int, a: Int) = titlesEn.containsKey("$s.$a")

    private fun readVerses(where: String, args: Array<String>): List<Verse> {
        val d = db ?: return emptyList()
        val out = ArrayList<Verse>()
        d.rawQuery(
            "SELECT skandha, chapter, num, num_end, speaker, sa, iast, hi, en, en_from, hi_from FROM verse $where ORDER BY skandha, chapter, num",
            args,
        ).use { c ->
            while (c.moveToNext()) {
                val sa = c.getString(5).orEmpty()
                out.add(
                    Verse(
                        skandha = c.getInt(0), adhyaya = c.getInt(1), num = c.getInt(2), numEnd = c.getInt(3),
                        speaker = c.getString(4)?.takeIf { it.isNotBlank() },
                        sa = if (sa.isEmpty()) emptyList() else sa.split('\n'),
                        iast = c.getString(6).orEmpty().let { if (it.isEmpty()) emptyList() else it.split('\n') },
                        hi = c.getString(7).orEmpty(), en = c.getString(8).orEmpty(),
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

    /** Every verse, for search. About 14,500 rows. */
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
