package com.bhagavatam.app.data

import android.database.sqlite.SQLiteDatabase

/**
 * Meanings from the book itself, for words the dictionary does not have (table book_gloss in content.db, built by
 * content/_dict/book_gloss.py): the words the same verse uses in the Hindi or English translation, and the word split into
 * parts the dictionary knows. Shown as the last step of [Dictionary.lookup], marked as made by machine. It lives in
 * content.db, beside the translations it comes from, because dictionary.db holds openly licensed text only.
 */
object BookGloss {
    data class Row(val hi: String, val en: String, val parts: String)

    const val SOURCE_CODE = "book"

    /** Shown as the source of an entry made from book_gloss. */
    val source = DictSource(-1, SOURCE_CODE, "This book's translations (matched by machine)", "", "", "", "")

    fun row(lang: String, word: String): Row? = SampleData.rawDb?.let { row(it, lang, word) }

    fun row(d: SQLiteDatabase, lang: String, word: String): Row? = runCatching {
        d.rawQuery("SELECT hi, en, parts FROM book_gloss WHERE lang = ? AND word = ?", arrayOf(lang, word)).use { c ->
            if (c.moveToFirst()) Row(c.getString(0) ?: "", c.getString(1) ?: "", c.getString(2) ?: "") else null
        }
    }.getOrNull()

    /**
     * The lines to show: matched words first ("≈ यदुवंशमें (in the Yadu dynasty)"), then the parts with their meanings.
     * [meaning] gives the short dictionary meaning of a word, or null.
     */
    fun senses(row: Row, meaning: (String) -> String?): List<String> {
        val out = ArrayList<String>()
        fun withMeaning(w: String) = meaning(w)?.let { "$w ($it)" } ?: w
        val matched = (row.hi.split('|') + row.en.split('|')).filter { it.isNotBlank() }
        if (matched.isNotEmpty()) out.add("≈ " + matched.joinToString(", ") { withMeaning(it) })
        val parts = row.parts.split('+').filter { it.isNotBlank() }
        if (parts.size > 1) out.add(parts.joinToString(" + ") { withMeaning(it) })
        return out
    }
}
