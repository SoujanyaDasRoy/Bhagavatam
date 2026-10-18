package com.bhagavatam.app.data

/**
 * Who is speaking at each verse of one chapter: a verse that carries a speaker line starts a run that lasts until the next speaker line.
 * Verses before the chapter's first speaker line map to null. Keyed by [Verse.ref]; a joint verse has one entry.
 */
fun speakerLabels(verses: List<Verse>): Map<String, String?> {
    var current: String? = null
    val out = LinkedHashMap<String, String?>()
    for (v in verses) {
        v.speaker?.takeIf { it.isNotBlank() }?.let { current = it }
        out[v.ref] = current
    }
    return out
}

/** True when [v]'s speaker differs from the verse before it in [verses]: where a label is shown. */
fun startsSpeakerRun(labels: Map<String, String?>, verses: List<Verse>, v: Verse): Boolean {
    val label = labels[v.ref] ?: return false
    val i = verses.indexOf(v)
    return i <= 0 || labels[verses[i - 1].ref] != label
}

/** Display names (English, Hindi, Bengali) for speakers that need more than a letter-for-letter rendering of the Sanskrit name. */
private val NAMES = mapOf(
    "श्रीशुक" to Triple("Shri Shukadeva", "श्रीशुकदेवजी", "শ্রীশুকদেব"),
    "सूत" to Triple("Suta", "श्रीसूतजी", "সূত"),
    "मैत्रेय" to Triple("Maitreya", "श्रीमैत्रेयजी", "মৈত্রেয়"),
    "नारद" to Triple("Narada", "नारदजी", "নারদ"),
    "उद्धव" to Triple("Uddhava", "उद्धवजी", "উদ্ধব"),
    "विदुर" to Triple("Vidura", "विदुरजी", "বিদুর"),
    "शौनक" to Triple("Shaunaka", "शौनकजी", "শৌনক"),
    "श्रीभगवान्" to Triple("The Lord", "श्रीभगवान्", "শ্রীভগবান"),
    "भगवान्" to Triple("The Lord", "भगवान्", "ভগবান"),
    "राजा" to Triple("The king", "राजा", "রাজা"),
    "ऋषि" to Triple("The sage", "ऋषि", "ঋষি"),
    "ब्रह्मा" to Triple("Brahma", "ब्रह्माजी", "ব্রহ্মা"),
)

/** Groups that speak together ("ऊचुः"), in the three languages. */
private val GROUPS = mapOf(
    "देवा" to Triple("The gods", "देवता", "দেবগণ"),
    "ऋषय" to Triple("The sages", "ऋषि", "ঋষিগণ"),
    "मुनय" to Triple("The sages", "मुनि", "মুনিগণ"),
    "गोप्य" to Triple("The gopis", "गोपियाँ", "গোপীগণ"),
    "गोपा" to Triple("The cowherds", "गोप", "গোপগণ"),
    "ब्राह्मणा" to Triple("The brahmanas", "ब्राह्मण", "ব্রাহ্মণগণ"),
    "प्रजापतय" to Triple("The Prajapatis", "प्रजापति", "প্রজাপতিগণ"),
    "सिद्धा" to Triple("The Siddhas", "सिद्ध", "সিদ্ধগণ"),
    "गन्धर्वा" to Triple("The Gandharvas", "गन्धर्व", "গন্ধর্বগণ"),
    "कुमारा" to Triple("The Kumaras", "कुमार", "কুমারগণ"),
    "पितर" to Triple("The forefathers", "पितर", "পিতৃগণ"),
)

private val SAYS = mapOf(Lang.EN to Pair(" said", " said"), Lang.HI to Pair(" ने कहा", " बोले"), Lang.BN to Pair(" বললেন", " বললেন"), Lang.OR to Pair(" କହିଲେ", " କହିଲେ"))

/** The speaker's name without the verb: "मैत्रेय उवाच" → "मैत्रेय", "राजोवाच" → "राजा", "देवक्युवाच" → "देवकी". Null when [line] is not a speaker line. */
private fun speakerStem(line: String): String? {
    val s = line.trim().replace("ऊचु:", "ऊचुः")
    if (s.endsWith("ऊचुः")) return s.removeSuffix("ऊचुः").trim().takeIf { it.isNotEmpty() }
    if (!s.endsWith("वाच")) return null
    // "मैत्रेय उवाच" has a space and a lone उ; joined forms ("राजोवाच", "बलिरुवाच") carry it inside the last word
    val w = s.removeSuffix("वाच").trim().removeSuffix(" उ").trim().replace(" ", "")  // "श्री भगवानु" → "श्रीभगवानु"
    return when {
        w.isEmpty() -> null
        w.endsWith("ो") -> w.dropLast(1) + "ा"          // राजो, ब्रह्मो
        w.endsWith("ानु") -> w.dropLast(1) + "्"        // अंशुमानु, भगवानु
        w.endsWith("्यु") -> w.dropLast(3) + "ी"        // देवक्यु, गोप्यु, यजमान्यु
        w.endsWith("रु") -> w.dropLast(2)               // बलिरु, मनुरु, अग्निरु
        else -> w
    }
}

/** Devanagari to Odia script: the two blocks line up 0x200 apart; व is written ବ and य ୟ in Odia, as the Odia text of the book does. */
private fun toOdia(deva: String): String = buildString {
    for (c in deva) append(when {
        c == 'व' -> 'ବ'
        c == 'य' -> 'ୟ'
        c in 'ँ'..'ॿ' && c != '।' && c != '॥' -> (c.code + 0x200).toChar()
        else -> c
    })
}

private fun fold(iast: String): String = buildString {
    for (c in iast) append(when (c) {
        'ś', 'ṣ' -> "sh"; 'ṛ', 'ṝ' -> "ri"; 'ā' -> "a"; 'ī' -> "i"; 'ū' -> "u"; 'ṭ' -> "t"; 'ḍ' -> "d"
        'ṇ', 'ṅ', 'ñ' -> "n"; 'ṃ', 'ṁ' -> "m"; 'ḥ' -> "h"; 'ḷ' -> "l"; else -> c.toString()
    })
}

/**
 * The speaker line in language [lang]: "Maitreya said" in English, "ମୈତ୍ରେୟ କହିଲେ" in Odia, "श्रीमैत्रेयजी ने कहा" in Hindi, "মৈত্রেয় বললেন" in Bengali.
 * The line is part of the book, so callers pass the language being read. Anything that is not a speaker line, and the other languages, stay as printed.
 */
fun speakerName(sanskrit: String, lang: Lang): String {
    val says = SAYS[lang] ?: return sanskrit
    val stem = speakerStem(sanskrit) ?: return sanskrit
    val plural = sanskrit.trim().let { it.endsWith("ऊचुः") || it.endsWith("ऊचु:") }
    val verb = if (plural) says.second else says.first
    val known = (if (plural) GROUPS else NAMES)[stem]
    val name = when (lang) {
        Lang.HI -> known?.second ?: stem
        Lang.BN -> known?.third ?: com.bhagavatam.app.util.Transliterate.toBengali(stem)
        Lang.OR -> toOdia(if (stem == "श्रीशुक") "श्रीशुकदेव" else stem)
        else -> known?.first ?: fold(com.bhagavatam.app.util.Transliterate.toIast(stem)).replaceFirstChar { it.uppercase() }
    }
    return name + verb
}
