package com.bhagavatam.app.data

import java.text.Normalizer
import java.util.regex.Pattern

/**
 * Phonetic loose key generator (Rule A) for the cross-script search index.
 * Matches Python implementation in content/_search/prototype.py.
 * Verified against content/_search/tests/loose_pairs.json.
 */
object LooseKey {
    const val MIN_KEY = 3

    private val CONS = HashMap<Char, String>()
    init {
        val mapping = mapOf(
            "k" to "कखक़ख़",
            "g" to "गघग़",
            "n" to "ङञणनऩम",
            "c" to "चछ",
            "j" to "जझज़यय़",
            "t" to "टठतथ",
            "d" to "डढदधड़ढ़",
            "p" to "पफफ़",
            "b" to "बभवव",
            "r" to "रऱ",
            "l" to "लळ",
            "s" to "शषस",
            "h" to "ह"
        )
        for ((roman, letters) in mapping) {
            for (ch in letters) {
                CONS[ch] = roman
            }
        }
    }

    private val VOWEL_SIGN = mapOf(
        'ा' to "a", 'ि' to "i", 'ी' to "i", 'ु' to "u", 'ू' to "u",
        'ृ' to "ri", 'ॄ' to "ri", 'े' to "e", 'ै' to "e", 'ो' to "o",
        'ौ' to "o", 'ॉ' to "o", 'ॅ' to "e"
    )

    private val VOWEL_IND = mapOf(
        'अ' to "a", 'आ' to "a", 'इ' to "i", 'ई' to "i", 'उ' to "u", 'ऊ' to "u",
        'ऋ' to "ri", 'ॠ' to "ri", 'ए' to "e", 'ऐ' to "e", 'ओ' to "o",
        'औ' to "o", 'ऑ' to "o"
    )

    private val RE_ASPIRATES = Pattern.compile("(?<=[kgcjtdpb])h")
    private val RE_NASALS = Pattern.compile("[mn]")
    private val RE_GEMINATES = Pattern.compile("(.)\\1+")

    private fun tidy(s: String): String {
        val collapsed = RE_GEMINATES.matcher(s).replaceAll("$1")
        return if (collapsed.endsWith("a") && collapsed.length > 1) {
            collapsed.dropLast(1)
        } else {
            collapsed
        }
    }

    fun looseFromIndic(word: String): String {
        val w = Normalise.toDevanagari(word).replace("ँ", "ं")
        val out = StringBuilder()
        var i = 0
        val n = w.length
        while (i < n) {
            val ch = w[i]
            if (CONS.containsKey(ch)) {
                out.append(CONS[ch])
                val nxt = if (i + 1 < n) w[i + 1] else ' '
                if (nxt == '्') {
                    i++
                } else if (VOWEL_SIGN.containsKey(nxt)) {
                    out.append(VOWEL_SIGN[nxt])
                    i++
                } else {
                    out.append("a")
                }
            } else if (VOWEL_IND.containsKey(ch)) {
                out.append(VOWEL_IND[ch])
            } else if (ch == 'ं') {
                out.append("n")
            }
            i++
        }
        return tidy(out.toString())
    }

    fun looseFromRoman(word: String): String {
        var w = Normalizer.normalize(word.lowercase(), Normalizer.Form.NFD)
        w = w.replace("r\u0323\u0304", "ri").replace("r\u0323", "ri").replace("l\u0323", "li")

        val sb = StringBuilder(w.length)
        for (i in 0 until w.length) {
            val ch = w[i]
            val type = Character.getType(ch)
            if (type != Character.NON_SPACING_MARK.toInt() && type != Character.COMBINING_SPACING_MARK.toInt()) {
                if (ch in 'a'..'z') {
                    sb.append(ch)
                }
            }
        }
        var s = sb.toString()
        s = RE_ASPIRATES.matcher(s).replaceAll("")
        s = s.replace("sh", "s").replace("ng", "n").replace("ny", "n")
        s = s.replace("w", "b").replace("v", "b").replace("y", "j")
            .replace("z", "j").replace("q", "k").replace("x", "ks").replace("f", "p")
        s = RE_NASALS.matcher(s).replaceAll("n")
        s = s.replace("ai", "e").replace("au", "o").replace("ee", "i").replace("oo", "u")
        return tidy(s)
    }

    /** Compute the loose key for any word (Indic or Roman). */
    fun keyOf(word: String): String =
        if (Normalise.isIndic(word)) looseFromIndic(word) else looseFromRoman(word)
}
