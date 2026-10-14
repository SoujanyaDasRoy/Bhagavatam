package com.bhagavatam.app.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * A word bookmarked by the reader with its language and verse reference.
 * Stored locally in annotations.db; never leaves the device.
 */
data class SavedWord(
    val id: Long,
    val word: String,
    val lang: String,
    val ref: String,
    val created: Long
)

/**
 * Local store for saved words inside annotations.db.
 */
class SavedWordStore(private val helper: AnnotationStore) {

    init {
        ensureTable()
    }

    private fun ensureTable() {
        runCatching {
            helper.writableDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS saved_word (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "word TEXT NOT NULL, " +
                    "lang TEXT NOT NULL, " +
                    "ref TEXT NOT NULL, " +
                    "created INTEGER NOT NULL, " +
                    "UNIQUE(word, lang))"
            )
            helper.writableDatabase.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_saved_word_lang ON saved_word(word, lang)"
            )
        }
    }

    fun all(): List<SavedWord> = runCatching {
        helper.readableDatabase.rawQuery(
            "SELECT id, word, lang, ref, created FROM saved_word ORDER BY created DESC",
            null
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        SavedWord(
                            id = c.getLong(0),
                            word = c.getString(1) ?: "",
                            lang = c.getString(2) ?: "",
                            ref = c.getString(3) ?: "",
                            created = c.getLong(4)
                        )
                    )
                }
            }
        }
    }.getOrDefault(emptyList())

    fun isSaved(word: String, lang: String): Boolean = runCatching {
        helper.readableDatabase.rawQuery(
            "SELECT 1 FROM saved_word WHERE word = ? AND lang = ? LIMIT 1",
            arrayOf(word.trim(), lang.lowercase())
        ).use { it.moveToFirst() }
    }.getOrDefault(false)

    fun save(word: String, lang: String, ref: String): Long = runCatching {
        val cv = ContentValues().apply {
            put("word", word.trim())
            put("lang", lang.lowercase())
            put("ref", ref)
            put("created", System.currentTimeMillis())
        }
        helper.writableDatabase.insertWithOnConflict(
            "saved_word",
            null,
            cv,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }.getOrDefault(-1L)

    fun remove(word: String, lang: String) {
        runCatching {
            helper.writableDatabase.delete(
                "saved_word",
                "word = ? AND lang = ?",
                arrayOf(word.trim(), lang.lowercase())
            )
        }
    }

    fun removeById(id: Long) {
        runCatching {
            helper.writableDatabase.delete("saved_word", "id = ?", arrayOf(id.toString()))
        }
    }

    fun clear() {
        runCatching {
            helper.writableDatabase.delete("saved_word", null, null)
        }
    }
}
