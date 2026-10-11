package com.bhagavatam.app.data

import android.database.sqlite.SQLiteDatabase

/**
 * A source of dictionary definitions (Wiktionary editions, CC BY-SA 4.0).
 */
data class DictSource(
    val id: Int,
    val code: String,
    val name: String,
    val licence: String,
    val url: String,
    val attribution: String,
    val retrieved: String
)

/**
 * A single definition/gloss for an entry.
 */
data class DictSense(
    val id: Int,
    val entryId: Int,
    val idx: Int,
    val glossLang: String,
    val gloss: String,
    val example: String?
)

/**
 * A full dictionary entry with its part of speech, pronunciation, etymology and meanings.
 */
data class DictEntry(
    val id: Int,
    val lang: String,
    val headword: String,
    val norm: String,
    val skey: String,
    val pos: String,
    val ipa: String?,
    val etymology: String?,
    val freq: Int,
    val sourceId: Int,
    val source: DictSource? = null,
    val senses: List<DictSense> = emptyList(),
    val isCrossScript: Boolean = false,
    val originLang: String? = null
)

/**
 * Fast offline dictionary lookup engine for English, Hindi, Bengali, and Odia.
 *
 * Implements the lookup pipeline defined in docs/superpowers/plans/2026-10-09-dictionary-db.md:
 *  1. Normalise word (NFC, strip punctuation/quotes, lowercase Roman).
 *  2. Exact match in entry table by (lang, norm) or (lang, headword).
 *  3. Inflected form match via form table.
 *  4. Stem match via suffix stripping (Lemmatizer).
 *  5. Chandrabindu/anusvara and nukta variant match.
 *  6. Cross-script tatsama lookup by script-neutral skey.
 */
object Dictionary {

    /**
     * Look up word in the offline dictionary.
     *
     * @param word The word as tapped or selected.
     * @param lang The language code of the text ("en", "hi", "bn", "or", "sa").
     * @param preferredGlossLang Language code to prefer for definitions (e.g. "hi", "en", "bn").
     * @return List of matching dictionary entries, ranked by exactness and book frequency.
     */
    fun lookup(
        word: String,
        lang: String,
        preferredGlossLang: String? = null
    ): List<DictEntry> {
        val d = DictionaryDb.rawDb ?: return emptyList()
        if (!DictionaryDb.isOpen) return emptyList()

        val clean = Normalise.word(word, lang)
        if (clean.isBlank()) return emptyList()

        val lCode = if (lang == "sa") "hi" else lang.lowercase()

        // 1. Exact match in entry
        var entries = findEntriesByNorm(d, lCode, clean)
        if (entries.isNotEmpty()) {
            return hydrateAndRank(d, entries, preferredGlossLang)
        }

        // 2. Inflected form match in form table
        entries = findEntriesByForm(d, lCode, clean)
        if (entries.isNotEmpty()) {
            return hydrateAndRank(d, entries, preferredGlossLang)
        }

        // 3. Stem matching with Lemmatizer
        val stems = Lemmatizer.stems(clean, lCode)
        for (st in stems) {
            val stemEntries = findEntriesByNorm(d, lCode, st) + findEntriesByForm(d, lCode, st)
            if (stemEntries.isNotEmpty()) {
                return hydrateAndRank(d, stemEntries.distinctBy { it.id }, preferredGlossLang)
            }
        }

        // 4. Variant matching: chandrabindu <-> anusvara, nukta stripping
        if (Normalise.isIndic(clean)) {
            val cbFolded = Normalise.foldChandrabindu(clean)
            if (cbFolded != clean) {
                val varEntries = findEntriesByNorm(d, lCode, cbFolded) + findEntriesByForm(d, lCode, cbFolded)
                if (varEntries.isNotEmpty()) {
                    return hydrateAndRank(d, varEntries.distinctBy { it.id }, preferredGlossLang)
                }
            }
            val noNukta = Normalise.stripNukta(clean)
            if (noNukta != clean) {
                val nuktaEntries = findEntriesByNorm(d, lCode, noNukta) + findEntriesByForm(d, lCode, noNukta)
                if (nuktaEntries.isNotEmpty()) {
                    return hydrateAndRank(d, nuktaEntries.distinctBy { it.id }, preferredGlossLang)
                }
            }
        }

        // 5. Cross-script tatsama lookup by skey (Bengali <-> Devanagari / Hindi <-> Odia)
        if (Normalise.isIndic(clean)) {
            val sk = Normalise.skey(clean)
            val crossEntries = findEntriesBySkey(d, lCode, sk)
            if (crossEntries.isNotEmpty()) {
                return hydrateAndRank(d, crossEntries, preferredGlossLang)
            }
        }

        return emptyList()
    }

    /**
     * Prefix search for the dictionary search screen as the user types.
     */
    fun search(
        prefix: String,
        lang: String? = null,
        limit: Int = 40
    ): List<DictEntry> {
        val d = DictionaryDb.rawDb ?: return emptyList()
        if (!DictionaryDb.isOpen) return emptyList()

        val clean = Normalise.word(prefix, lang)
        if (clean.isBlank()) return emptyList()

        val queryLang = if (lang == null || lang == "all") null else lang.lowercase()
        val paramPattern = "$clean%"

        val query = if (queryLang != null) {
            """SELECT id, lang, headword, norm, skey, pos, ipa, etymology, freq, source_id
               FROM entry
               WHERE lang = ? AND (norm LIKE ? OR headword LIKE ?)
               ORDER BY (norm = ?) DESC, (headword = ?) DESC, freq DESC
               LIMIT ?"""
        } else {
            """SELECT id, lang, headword, norm, skey, pos, ipa, etymology, freq, source_id
               FROM entry
               WHERE norm LIKE ? OR headword LIKE ?
               ORDER BY (norm = ?) DESC, (headword = ?) DESC, freq DESC
               LIMIT ?"""
        }

        val args = if (queryLang != null) {
            arrayOf(queryLang, paramPattern, paramPattern, clean, clean, limit.toString())
        } else {
            arrayOf(paramPattern, paramPattern, clean, clean, limit.toString())
        }

        val entries = ArrayList<DictEntry>()
        runCatching {
            d.rawQuery(query, args).use { c ->
                while (c.moveToNext()) {
                    entries.add(readEntryRow(c))
                }
            }
        }

        return hydrateAndRank(d, entries, null)
    }

    private fun findEntriesByNorm(d: SQLiteDatabase, lang: String, norm: String): List<DictEntry> {
        val out = ArrayList<DictEntry>()
        val sql = """SELECT id, lang, headword, norm, skey, pos, ipa, etymology, freq, source_id
                     FROM entry
                     WHERE lang = ? AND (norm = ? OR headword = ?)
                     ORDER BY (norm = ?) DESC, freq DESC"""
        runCatching {
            d.rawQuery(sql, arrayOf(lang, norm, norm, norm)).use { c ->
                while (c.moveToNext()) out.add(readEntryRow(c))
            }
        }
        return out
    }

    private fun findEntriesByForm(d: SQLiteDatabase, lang: String, formNorm: String): List<DictEntry> {
        val out = ArrayList<DictEntry>()
        val sql = """SELECT e.id, e.lang, e.headword, e.norm, e.skey, e.pos, e.ipa, e.etymology, e.freq, e.source_id
                     FROM form f
                     JOIN entry e ON e.id = f.entry_id
                     WHERE e.lang = ? AND f.form_norm = ?
                     ORDER BY e.freq DESC"""
        runCatching {
            d.rawQuery(sql, arrayOf(lang, formNorm)).use { c ->
                while (c.moveToNext()) out.add(readEntryRow(c))
            }
        }
        return out
    }

    private fun findEntriesBySkey(d: SQLiteDatabase, excludeLang: String, skey: String): List<DictEntry> {
        val out = ArrayList<DictEntry>()
        val sql = """SELECT id, lang, headword, norm, skey, pos, ipa, etymology, freq, source_id
                     FROM entry
                     WHERE lang != ? AND skey = ?
                     ORDER BY freq DESC
                     LIMIT 3"""
        runCatching {
            d.rawQuery(sql, arrayOf(excludeLang, skey)).use { c ->
                while (c.moveToNext()) {
                    val e = readEntryRow(c)
                    out.add(e.copy(isCrossScript = true, originLang = e.lang))
                }
            }
        }
        return out
    }

    private fun readEntryRow(c: android.database.Cursor): DictEntry {
        return DictEntry(
            id = c.getInt(0),
            lang = c.getString(1) ?: "",
            headword = c.getString(2) ?: "",
            norm = c.getString(3) ?: "",
            skey = c.getString(4) ?: "",
            pos = c.getString(5) ?: "",
            ipa = c.getString(6),
            etymology = c.getString(7),
            freq = c.getInt(8),
            sourceId = c.getInt(9),
            source = DictionaryDb.source(c.getInt(9))
        )
    }

    private fun hydrateAndRank(
        d: SQLiteDatabase,
        entries: List<DictEntry>,
        preferredGlossLang: String?
    ): List<DictEntry> {
        if (entries.isEmpty()) return emptyList()

        val entryIds = entries.map { it.id }
        val sensesByEntry = HashMap<Int, ArrayList<DictSense>>()

        // Chunk entry IDs in groups of 400 for SQLite IN clause limit
        for (chunk in entryIds.chunked(400)) {
            val placeholders = chunk.joinToString(",") { "?" }
            val sql = "SELECT id, entry_id, idx, gloss_lang, gloss, example FROM sense WHERE entry_id IN ($placeholders) ORDER BY idx ASC"
            val args = chunk.map { it.toString() }.toTypedArray()
            runCatching {
                d.rawQuery(sql, args).use { c ->
                    while (c.moveToNext()) {
                        val sense = DictSense(
                            id = c.getInt(0),
                            entryId = c.getInt(1),
                            idx = c.getInt(2),
                            glossLang = c.getString(3) ?: "",
                            gloss = c.getString(4) ?: "",
                            example = c.getString(5)
                        )
                        sensesByEntry.getOrPut(sense.entryId) { ArrayList() }.add(sense)
                    }
                }
            }
        }

        return entries.map { e ->
            val rawSenses = sensesByEntry[e.id] ?: emptyList()
            val sortedSenses = if (preferredGlossLang != null) {
                rawSenses.sortedWith(compareByDescending<DictSense> { it.glossLang == preferredGlossLang }
                    .thenByDescending { it.glossLang == "en" }
                    .thenBy { it.idx })
            } else {
                rawSenses
            }
            e.copy(
                source = DictionaryDb.source(e.sourceId),
                senses = sortedSenses
            )
        }
    }
}
