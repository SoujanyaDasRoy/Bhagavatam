package com.bhagavatam.app.data

import org.json.JSONArray
import org.json.JSONObject

/** Rows of one table as plain maps and as JSON: what the backup file stores for notes, saved words and review cards. */
object BackupRows {
    fun toJson(rows: List<Map<String, Any?>>): JSONArray = JSONArray().also { a ->
        rows.forEach { r -> a.put(JSONObject().also { o -> r.forEach { (k, v) -> o.put(k, v ?: JSONObject.NULL) } }) }
    }

    fun fromJson(a: JSONArray): List<Map<String, Any?>> = (0 until a.length()).map { i ->
        val o = a.getJSONObject(i)
        o.keys().asSequence().associateWith { k -> if (o.isNull(k)) null else o.get(k).let { v -> if (v is Int) v.toLong() else v } }
    }

    /** Throws before anything is deleted when a row has a column the table does not have (a backup from a newer version). */
    fun requireColumns(table: String, rows: List<Map<String, Any?>>, allowed: Set<String>) {
        for (r in rows) {
            val extra = r.keys - allowed
            require(extra.isEmpty()) { "backup has columns $extra that table $table does not have" }
        }
    }

    /** The rows under [key] in the backup, or null when the file does not have that key (a backup made by an older version). */
    fun fromRoot(root: JSONObject, key: String): List<Map<String, Any?>>? = root.optJSONArray(key)?.let { fromJson(it) }
}
