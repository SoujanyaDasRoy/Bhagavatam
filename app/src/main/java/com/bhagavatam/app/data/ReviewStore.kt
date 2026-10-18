package com.bhagavatam.app.data

import android.content.ContentValues

/** A verse or a word the reader asked to be reminded of. [kind] is "verse" or "word"; [key] is the verse ref or "lang|word". */
data class ReviewCard(val id: Long, val kind: String, val key: String, val stage: Int, val dueAt: Long, val created: Long)

/** When a card comes back: after 1, 3, 7 and 21 days; a card answered wrongly starts again; the last stage means learned. */
object ReviewScheduler {
    val steps: List<Int> = listOf(1, 3, 7, 21)
    private const val DAY = 86_400_000L
    /** A due time further ahead than the longest step (plus a day) can only come from a wrong clock, so it counts as due. */
    private val maxAhead = (steps.last() + 1) * DAY

    fun after(stage: Int, remembered: Boolean): Int = if (!remembered) 0 else (stage + 1).coerceAtMost(steps.size)

    fun dueAt(now: Long, stage: Int): Long = now + steps[stage.coerceIn(0, steps.lastIndex)] * DAY

    fun isDue(now: Long, dueAt: Long, stage: Int): Boolean = stage < steps.size && (dueAt - now <= 0 || dueAt - now > maxAhead)
}

/** Table `review` inside annotations.db (created on first use, like saved words). */
class ReviewStore(private val helper: AnnotationStore) {
    init {
        runCatching {
            helper.writableDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS review(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL, key TEXT NOT NULL, " +
                    "stage INTEGER NOT NULL, dueAt INTEGER NOT NULL, created INTEGER NOT NULL, UNIQUE(kind, key))"
            )
        }
    }

    fun all(): List<ReviewCard> = runCatching {
        helper.readableDatabase.rawQuery("SELECT id, kind, key, stage, dueAt, created FROM review ORDER BY dueAt", null).use { c ->
            buildList { while (c.moveToNext()) add(ReviewCard(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3), c.getLong(4), c.getLong(5))) }
        }
    }.getOrDefault(emptyList())

    /** Adds a card due in one day; an existing card for the same verse or word is kept (its id is returned). */
    fun add(kind: String, key: String, now: Long): Long = runCatching {
        val v = ContentValues().apply {
            put("kind", kind); put("key", key); put("stage", 0); put("dueAt", ReviewScheduler.dueAt(now, 0)); put("created", now)
        }
        val id = helper.writableDatabase.insertWithOnConflict("review", null, v, android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE)
        if (id != -1L) id else helper.readableDatabase.rawQuery("SELECT id FROM review WHERE kind = ? AND key = ?", arrayOf(kind, key)).use { if (it.moveToFirst()) it.getLong(0) else -1L }
    }.getOrDefault(-1L)

    fun update(card: ReviewCard) {
        runCatching {
            helper.writableDatabase.update("review", ContentValues().apply { put("stage", card.stage); put("dueAt", card.dueAt) }, "id = ?", arrayOf(card.id.toString()))
        }
    }

    fun remove(id: Long) { runCatching { helper.writableDatabase.delete("review", "id = ?", arrayOf(id.toString())) } }
}
