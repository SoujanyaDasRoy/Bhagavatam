package com.bhagavatam.app.data

private fun wordOf(card: ReviewCard) = card.key.substringAfter('|')
private fun langOf(card: ReviewCard) = Lang.entries.firstOrNull { it.code == card.key.substringBefore('|') } ?: Lang.SA

/** What the card asks: the first line of the shloka for a verse, the word itself for a word. Null when the verse is not in the book. */
fun reviewPrompt(card: ReviewCard, verseOf: (String) -> Verse?): String? = when (card.kind) {
    "word" -> wordOf(card).ifBlank { null }
    else -> verseOf(card.key)?.let { v -> (v.sa.firstOrNull { it.isNotBlank() } ?: v.iast.firstOrNull { it.isNotBlank() }) }
}

/**
 * What the card answers: the translation of the verse in [lang] (the carrier's text for a verse printed jointly, another language when
 * [lang] has none), or the first meaning of the word. Null when there is nothing to show, so the screen can drop the card.
 */
fun reviewAnswer(card: ReviewCard, verseOf: (String) -> Verse?, meaningOf: (String, Lang) -> String?, lang: Lang): String? {
    if (card.kind == "word") return meaningOf(wordOf(card), langOf(card))?.ifBlank { null }
    val v = verseOf(card.key) ?: return null
    fun text(x: Verse, l: Lang): String? = if (x.hasText(l)) x.translation(l) else null
    fun carrierOf(l: Lang): Verse? = when (l) { Lang.EN -> v.enFrom; Lang.HI -> v.hiFrom; Lang.BN -> v.bnFrom; Lang.OR -> v.orFrom; else -> null }
        ?.let { verseOf("${v.skandha}.${v.adhyaya}.$it") }
    for (l in listOf(lang, Lang.HI, Lang.EN, Lang.BN, Lang.OR).distinct()) {
        text(v, l)?.let { return it }
        carrierOf(l)?.let { c -> text(c, l)?.let { return it } }
    }
    return null
}
