package com.bhagavatam.app.data

import android.database.sqlite.SQLiteDatabase

/**
 * Meanings from the book itself, for words the dictionary does not have (table book_gloss in content.db, built by
 * content/_dict/book_gloss.py): the words the same verse uses in the Hindi or English translation, and the word split into
 * parts the dictionary knows. Shown as the last step of [Dictionary.lookup], marked as made by machine. It lives in
 * content.db, beside the translations it comes from, because dictionary.db holds openly licensed text only.
 */
object BookGloss {
    data class Row(
        val hi: String, val en: String, val parts: String,
        /** A form of this word that has a meaning (the word with its endings taken off). */
        val stem: String = "",
        /** The first verse where the word occurs: its translation is the last resort. */
        val ref: String = "",
        /** The word this one is probably a misprint of (the page readings are not perfect). */
        val fix: String = "",
    )

    const val SOURCE_CODE = "book"

    /** Shown as the source of an entry made from book_gloss. */
    val source = DictSource(-1, SOURCE_CODE, "This book's translations (matched by machine)", "", "", "", "")

    fun row(lang: String, word: String): Row? = SampleData.rawDb?.let { row(it, lang, word) }

    fun row(d: SQLiteDatabase, lang: String, word: String): Row? = runCatching {
        d.rawQuery("SELECT hi, en, parts, stem, ref, fix FROM book_gloss WHERE lang = ? AND word = ?", arrayOf(lang, word)).use { c ->
            if (c.moveToFirst()) Row(c.getString(0) ?: "", c.getString(1) ?: "", c.getString(2) ?: "", c.getString(3) ?: "", c.getString(4) ?: "", c.getString(5) ?: "") else null
        }
    }.getOrNull()

    /**
     * The lines to show, best first: matched words ("≈ यदुवंशमें (in the Yadu dynasty)"), the parts with their meanings,
     * the base form ("form of X (meaning)"), the corrected spelling ("probably a misprint of X (meaning)"), and last of all
     * the verse where the word occurs ("In verse 10.29.1: ..."), only when nothing else is known.
     * [meaning] gives the short meaning of a word, or null; [verse] gives the translation of the verse with the given reference.
     */
    fun senses(row: Row, verse: (String) -> String? = { null }, meaning: (String) -> String?): List<String> {
        val out = ArrayList<String>()
        fun withMeaning(w: String) = meaning(w)?.let { "$w ($it)" } ?: w
        val matched = (row.hi.split('|') + row.en.split('|')).filter { it.isNotBlank() }
        if (matched.isNotEmpty()) out.add("≈ " + matched.joinToString(", ") { withMeaning(it) })
        val parts = row.parts.split('+').filter { it.isNotBlank() }
        if (parts.size > 1) out.add(parts.joinToString(" + ") { withMeaning(it) })
        if (row.stem.isNotBlank()) out.add("form of " + withMeaning(row.stem))
        if (row.fix.isNotBlank()) out.add("probably a misprint of " + withMeaning(row.fix))
        if (out.isEmpty() && row.ref.isNotBlank()) verse(row.ref)?.takeIf { it.isNotBlank() }?.let { out.add("In verse ${row.ref}: $it") }
        return out
    }
}
