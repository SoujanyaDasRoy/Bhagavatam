package com.bhagavatam.app.util

/**
 * Devanagari → Bengali script for Sanskrit.
 *
 * The two Unicode blocks line up code point by code point (U+0900 → U+0980), so most letters
 * map by offset. A few letters are mapped by hand, then two Bengali print conventions apply:
 *  - "त्" before most consonants is written ৎ (e.g. নির্মৎসরাণাং)
 *  - य inside a word, not in a conjunct, is written য় (e.g. তাপত্রয়োন্মূলনম্)
 */
object Transliterate {
    private const val DEVA_START = 0x0900
    private const val DEVA_END = 0x097F
    private const val OFFSET = 0x80

    /** Letters whose Bengali slot is empty or differs from the offset rule. */
    private val SPECIAL = mapOf(
        'व' to "ব", // va is written with ba in Bengali script
        'ळ' to "ল",
        'ऽ' to "ঽ",
        'ॐ' to "ওঁ",
    )

    fun toBengali(deva: String): String {
        val sb = StringBuilder(deva.length)
        for (ch in deva) {
            val c = ch.code
            sb.append(
                when {
                    ch == '।' || ch == '॥' -> ch.toString()
                    SPECIAL.containsKey(ch) -> SPECIAL.getValue(ch)
                    c in DEVA_START..DEVA_END -> (c + OFFSET).toChar().toString()
                    else -> ch.toString()
                }
            )
        }
        var s = sb.toString()
        // khanda ta: ত + hasanta before most consonants
        s = s.replace(Regex("ত্(?=[কখগঘচছজঝটঠডঢপফবভলশষসহ])"), "ৎ")
        // antastha ya after a letter or vowel sign (not after hasanta) becomes য়
        s = s.replace(Regex("(?<=[\\u0985-\\u09CC])য"), "য়")
        return s
    }
}
