package com.bhagavatam.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlin.math.abs

enum class AnnKind { HIGHLIGHT, NOTE, BOOKMARK }

/**
 * A mark on a span of one verse's text in one language layer. The range is only a hint: [quote] is the text that was selected,
 * and [reanchor] finds it again if the text has shifted (a content update, a dash tidy-up). Nothing here leaves the phone.
 */
data class Annotation(
    val id: Long,
    val ref: String,
    val layer: String,
    val start: Int,
    val end: Int,
    val quote: String,
    val kind: AnnKind,
    val colour: Long,
    val note: String,
    val created: Long,
)

/** Where [a] sits in [text] now: the stored range if the quote is still there, else the nearest copy of the quote, else null. */
fun reanchor(text: String, start: Int, end: Int, quote: String): IntRange? {
    if (quote.isEmpty()) return null
    if (start >= 0 && end <= text.length && start < end && text.substring(start, end) == quote) return start until end
    var best = -1
    var i = text.indexOf(quote)
    while (i >= 0) {
        if (best < 0 || abs(i - start) < abs(best - start)) best = i
        i = text.indexOf(quote, i + 1)
    }
    return if (best >= 0) best until best + quote.length else null
}

/** Local SQLite file (annotations.db) for notes and highlights. Separate from content.db so a text update never touches it. */
class AnnotationStore(ctx: Context) : SQLiteOpenHelper(ctx, "annotations.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE annotation(id INTEGER PRIMARY KEY AUTOINCREMENT, ref TEXT NOT NULL, layer TEXT NOT NULL, start INTEGER NOT NULL, " +
                "end INTEGER NOT NULL, quote TEXT NOT NULL, kind TEXT NOT NULL, colour INTEGER NOT NULL, note TEXT NOT NULL, created INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX annotation_ref ON annotation(ref)")
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {}

    private fun values(a: Annotation) = ContentValues().apply {
        put("ref", a.ref); put("layer", a.layer); put("start", a.start); put("end", a.end); put("quote", a.quote)
        put("kind", a.kind.name); put("colour", a.colour); put("note", a.note); put("created", a.created)
    }

    fun all(): List<Annotation> = readableDatabase.rawQuery(
        "SELECT id, ref, layer, start, end, quote, kind, colour, note, created FROM annotation ORDER BY created DESC", null
    ).use { c ->
        buildList {
            while (c.moveToNext()) add(
                Annotation(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3), c.getInt(4), c.getString(5),
                    runCatching { AnnKind.valueOf(c.getString(6)) }.getOrDefault(AnnKind.HIGHLIGHT), c.getLong(7), c.getString(8), c.getLong(9))
            )
        }
    }

    fun insert(a: Annotation): Long = writableDatabase.insert("annotation", null, values(a))
    fun update(a: Annotation) { writableDatabase.update("annotation", values(a), "id = ?", arrayOf(a.id.toString())) }
    fun delete(id: Long) { writableDatabase.delete("annotation", "id = ?", arrayOf(id.toString())) }
    fun clear() { writableDatabase.delete("annotation", null, null) }
}

/** What the note sheet is editing: an existing mark ([existing]) or a new note on [quote]. */
data class AnnDraft(val existing: Annotation?, val ref: String, val layer: String, val start: Int, val end: Int, val quote: String)

/** A word the reader tapped, with the language layer it was tapped in. */
data class WordLookup(val word: String, val lang: Lang, val ref: String)

/** A live text selection in one verse: where it is, and how to clear it. Shown as the action bar above the reader controls. */
class SelBar(val ref: String, val lang: Lang, val start: Int, val end: Int, val quote: String, val clear: () -> Unit)

/** The word around [i] in [text]: letters, digits and the combining marks of Devanagari and Bengali stay together. */
fun wordAt(text: String, i: Int): IntRange? {
    fun inWord(c: Char) = c.isLetterOrDigit() || Character.getType(c).let {
        it == Character.NON_SPACING_MARK.toInt() || it == Character.COMBINING_SPACING_MARK.toInt()
    } || c == '\u200d' || c == '\u200c' || c == '\''
    var a = i.coerceIn(0, text.length)
    if (a >= text.length || !inWord(text[a])) a -= 1
    if (a < 0 || a >= text.length || !inWord(text[a])) return null
    var s = a
    var e = a
    while (s > 0 && inWord(text[s - 1])) s--
    while (e + 1 < text.length && inWord(text[e + 1])) e++
    val w = text.substring(s, e + 1).trim('\'')
    if (w.isEmpty()) return null
    val ts = text.indexOf(w, s)
    return ts until ts + w.length
}
