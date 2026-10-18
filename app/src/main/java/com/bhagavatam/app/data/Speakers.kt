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

private val NAMES = mapOf(
    "श्रीशुक उवाच" to Triple("Shri Shukadeva said", "শ্রীশুকদেব বললেন", "श्रीशुक उवाच"),
    "सूत उवाच" to Triple("Suta said", "সূত বললেন", "सूत उवाच"),
    "मैत्रेय उवाच" to Triple("Maitreya said", "মৈত্রেয় বললেন", "मैत्रेয় उवाच"),
    "नारद उवाच" to Triple("Narada said", "নারদ বললেন", "नारद उवाच"),
    "उद्धव उवाच" to Triple("Uddhava said", "উদ্ধব বললেন", "उद्धव उवाच"),
    "श्रीभगवानुवाच" to Triple("The Lord said", "শ্রীভগবান বললেন", "श्रीभगवानुवाच"),
    "विदुर उवाच" to Triple("Vidura said", "বিদুর বললেন", "विदुर उवाच"),
    "शौनक उवाच" to Triple("Shaunaka said", "শৌনক বললেন", "शौनक उवाच"),
)

/** The speaker line in the app language for the eight most frequent speakers; any other line stays as printed. */
fun speakerName(sanskrit: String, ui: Lang): String =
    NAMES[sanskrit.trim()]?.let { (en, bn, hi) -> when (ui) { Lang.HI -> hi; Lang.BN -> bn; else -> en } } ?: sanskrit
