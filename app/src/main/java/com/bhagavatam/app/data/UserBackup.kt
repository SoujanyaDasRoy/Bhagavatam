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
        items.length()
    }
}
