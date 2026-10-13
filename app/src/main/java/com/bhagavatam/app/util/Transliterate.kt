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

    private val IAST_VOWEL = mapOf(
        'अ' to "a", 'आ' to "ā", 'इ' to "i", 'ई' to "ī", 'उ' to "u", 'ऊ' to "ū", 'ऋ' to "ṛ", 'ॠ' to "ṝ", 'ऌ' to "ḷ",
        'ए' to "e", 'ऐ' to "ai", 'ओ' to "o", 'औ' to "au",
    )
    private val IAST_MATRA = mapOf(
        'ा' to "ā", 'ि' to "i", 'ी' to "ī", 'ु' to "u", 'ू' to "ū", 'ृ' to "ṛ", 'ॄ' to "ṝ", 'े' to "e", 'ै' to "ai", 'ो' to "o", 'ौ' to "au",
    )
    private val IAST_CONS = mapOf(
        'क' to "k", 'ख' to "kh", 'ग' to "g", 'घ' to "gh", 'ङ' to "ṅ", 'च' to "c", 'छ' to "ch", 'ज' to "j", 'झ' to "jh", 'ञ' to "ñ",
        'ट' to "ṭ", 'ठ' to "ṭh", 'ड' to "ḍ", 'ढ' to "ḍh", 'ण' to "ṇ", 'त' to "t", 'थ' to "th", 'द' to "d", 'ध' to "dh", 'न' to "n",
        'प' to "p", 'फ' to "ph", 'ब' to "b", 'भ' to "bh", 'म' to "m", 'य' to "y", 'र' to "r", 'ल' to "l", 'व' to "v",
        'श' to "ś", 'ष' to "ṣ", 'स' to "s", 'ह' to "h", 'ळ' to "ḷ",
    )

    /** Devanagari Sanskrit to Roman (IAST), for readers who read in English. Keeps every inherent "a", as Sanskrit does. */
    fun toIast(deva: String): String {
        val sb = StringBuilder(deva.length * 2)
        var i = 0
        while (i < deva.length) {
            val ch = deva[i]
            val cons = IAST_CONS[ch]
            when {
                cons != null -> {
                    sb.append(cons)
                    val nx = deva.getOrNull(i + 1)
                    when {
                        nx != null && IAST_MATRA.containsKey(nx) -> { sb.append(IAST_MATRA.getValue(nx)); i++ }
                        nx == '्' -> i++
                        else -> sb.append('a')
                    }
                }
                IAST_VOWEL.containsKey(ch) -> sb.append(IAST_VOWEL.getValue(ch))
                ch == 'ं' || ch == 'ँ' -> sb.append('ṃ')
                ch == 'ः' -> sb.append('ḥ')
                ch == 'ऽ' -> sb.append('\'')
                ch == 'ॐ' -> sb.append("oṃ")
                ch == '।' -> sb.append('|')
                ch == '॥' -> sb.append("||")
                ch in '०'..'९' -> sb.append(('0' + (ch - '०')))
                ch == '़' || ch == '्' -> {}
                else -> sb.append(ch)
            }
            i++
        }
        return sb.toString()
    }

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
