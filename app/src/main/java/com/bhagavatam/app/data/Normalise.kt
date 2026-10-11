package com.bhagavatam.app.data

import java.text.Normalizer
import java.util.regex.Pattern

/**
 * Pure functions for text normalisation, script alignment and matching keys.
 * Shared across the search engine and dictionary.
 */
object Normalise {
    private val JOINERS = Pattern.compile("[\u200C\u200D]")
    private val INDIC = Pattern.compile("[\u0900-\u097F\u0980-\u09FF\u0B00-\u0B7F]")
    private val DANDAS = Pattern.compile("[।॥]")

    /** NFC normalisation with zero-width non-joiner and joiner stripped. */
    fun nfc(text: String): String {
        val norm = Normalizer.normalize(text, Normalizer.Form.NFC)
        return JOINERS.matcher(norm).replaceAll("")
    }

    /**
     * Map Bengali (0x0980..0x09FF) and Odia (0x0B00..0x0B7F) letters to the Devanagari block (0x0900..0x097F).
     * The scripts are laid out in parallel Unicode offsets: Bengali is -0x80, Odia is -0x200.
     */
    fun toDevanagari(text: String): String {
        val s = nfc(text)
        val sb = java.lang.StringBuilder(s.length)
        for (i in 0 until s.length) {
            val c = s[i].code
            when (c) {
                in 0x0980..0x09FF -> sb.append((c - 0x80).toChar())
                in 0x0B00..0x0B7F -> sb.append((c - 0x200).toChar())
                else -> sb.append(s[i])
            }
        }
        return sb.toString()
    }

    /**
     * Exact form used for related words: NFC without joiners; lowercase for Roman text.
     * Matches Python exact_of(w) in build_index.py.
     */
    fun exactOf(word: String): String {
        val w = nfc(word)
        return if (INDIC.matcher(w).find()) w else w.lowercase()
    }

    /**
     * Script-neutral key (skey) for tatsama words in the dictionary:
     * maps Bengali and Odia words into Devanagari, lowercases Roman words.
     */
    fun skey(word: String): String {
        val w = nfc(word)
        return if (INDIC.matcher(w).find()) toDevanagari(w).lowercase() else w.lowercase()
    }

    /**
     * Clean word: strip whitespace, quotes and book punctuation.
     */
    fun word(text: String, lang: String? = null): String {
        val trimmed = text.trim().trim(
            '.', ',', ';', ':', '!', '?', '"', '\'', '‘', '’', '“', '”', '(', ')', '[', ']', '{', '}',
            '।', '॥', '-', '\u2014', '\u2013'
        )
        val clean = nfc(trimmed)
        return if (lang == "en" || !INDIC.matcher(clean).find()) clean.lowercase() else clean
    }

    /** Returns true if text contains Indic letters (Devanagari, Bengali, or Odia). */
    fun isIndic(text: String): Boolean = INDIC.matcher(text).find()

    /**
     * Map chandrabindu (ँ / ঁ / ଁ) to anusvara (ं / ং / ଂ) for script-variant matching.
     */
    fun foldChandrabindu(text: String): String {
        val s = nfc(text)
        val sb = java.lang.StringBuilder(s.length)
        for (i in 0 until s.length) {
            when (s[i]) {
                '\u0901' -> sb.append('\u0902') // Devanagari
                '\u0981' -> sb.append('\u0982') // Bengali
                '\u0B01' -> sb.append('\u0B02') // Odia
                else -> sb.append(s[i])
            }
        }
        return sb.toString()
    }

    /**
     * Strip nukta marks (U+093C, U+09BC, U+0B3C) across Indic scripts.
     * Decomposes via NFD first so composite characters (e.g. क़, ড়) are also stripped,
     * then recomposes back to NFC.
     */
    fun stripNukta(text: String): String {
        val decomp = Normalizer.normalize(text, Normalizer.Form.NFD)
        val noNukta = decomp.replace("[\u093C\u09BC\u0B3C]".toRegex(), "")
        return nfc(noNukta)
    }
}

