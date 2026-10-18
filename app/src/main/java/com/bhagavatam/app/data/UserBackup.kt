package com.bhagavatam.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/** Everything the reader has made (settings, bookmarks, notes, saved words, position) as one file, and back again. */
object UserBackup {
    private const val PREFS = "bhagavatam"
    private const val FORMAT = "bhagavatam-backup-1"

    fun export(ctx: Context, to: Uri): Result<Int> = runCatching {
        val all = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all
        val items = JSONArray()
        for ((k, v) in all) {
            val o = JSONObject().put("k", k)
            when (v) {
                is Boolean -> o.put("t", "b").put("v", v)
                is Int -> o.put("t", "i").put("v", v)
                is Long -> o.put("t", "l").put("v", v)
                is Float -> o.put("t", "f").put("v", v.toDouble())
                is String -> o.put("t", "s").put("v", v)
                is Set<*> -> o.put("t", "set").put("v", JSONArray(v.map { it.toString() }))
                else -> continue
            }
            items.put(o)
        }
        val root = JSONObject().put("format", FORMAT).put("items", items)
        // notes, saved words and review cards live in annotations.db, not in the preferences
        val db = AnnotationStore(ctx).readableDatabase
        for (table in TABLES) root.put(table, BackupRows.toJson(dump(db, table)))
        ctx.contentResolver.openOutputStream(to)!!.use { it.write(root.toString().toByteArray(Charsets.UTF_8)) }
        items.length()
    }

    /** Replaces the saved data with the file's. The app must be restarted afterwards so the screens read it again. */
    fun import(ctx: Context, from: Uri): Result<Int> = runCatching {
        val text = ctx.contentResolver.openInputStream(from)!!.use { it.readBytes().toString(Charsets.UTF_8) }
        val root = JSONObject(text)
        require(root.optString("format") == FORMAT) { "not a Bhagavatam backup" }
        val items = root.getJSONArray("items")
        val ed = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear()
        for (i in 0 until items.length()) {
            val o = items.getJSONObject(i)
            val k = o.getString("k")
            when (o.getString("t")) {
                "b" -> ed.putBoolean(k, o.getBoolean("v"))
                "i" -> ed.putInt(k, o.getInt("v"))
                "l" -> ed.putLong(k, o.getLong("v"))
                "f" -> ed.putFloat(k, o.getDouble("v").toFloat())
                "s" -> ed.putString(k, o.getString("v"))
                "set" -> ed.putStringSet(k, o.getJSONArray("v").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() })
            }
        }
        check(ed.commit()) { "could not save" }
        // a table the file does not have (a backup from an older version) is left as it is
        val db = AnnotationStore(ctx).writableDatabase
        for (table in TABLES) BackupRows.fromRoot(root, table)?.let { restore(db, table, it) }
        items.length()
    }

    private val TABLES = listOf("annotation", "saved_word", "review")

    private fun dump(db: android.database.sqlite.SQLiteDatabase, table: String): List<Map<String, Any?>> = runCatching {
        db.rawQuery("SELECT * FROM $table", null).use { c ->
            buildList {
                while (c.moveToNext()) add((0 until c.columnCount).associate { i ->
                    c.getColumnName(i) to when (c.getType(i)) {
                        android.database.Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                        android.database.Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
                        android.database.Cursor.FIELD_TYPE_NULL -> null
                        else -> c.getString(i)
                    }
                })
            }
        }
    }.getOrDefault(emptyList())

    private fun restore(db: android.database.sqlite.SQLiteDatabase, table: String, rows: List<Map<String, Any?>>) {
        db.beginTransaction()
        try {
            db.delete(table, null, null)
            for (r in rows) {
                val v = android.content.ContentValues()
                for ((k, x) in r) when (x) { null -> v.putNull(k); is Long -> v.put(k, x); is Double -> v.put(k, x); else -> v.put(k, x.toString()) }
                db.insert(table, null, v)
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
}
