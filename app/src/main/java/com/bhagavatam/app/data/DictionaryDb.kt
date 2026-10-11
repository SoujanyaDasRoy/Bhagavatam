package com.bhagavatam.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * Manages access to the offline dictionary SQLite database (dictionary.db).
 * Copies the bundled asset database to app storage once per version and opens read-only.
 */
object DictionaryDb {
    private const val ASSET = "dictionary.db"

    private var db: SQLiteDatabase? = null
    private val sources = HashMap<Int, DictSource>()
    private var version: String = "1"

    val isOpen: Boolean
        get() = db != null && db?.isOpen == true

    val rawDb: SQLiteDatabase?
        get() = db

    val dictionaryVersion: String
        get() = version

    fun source(id: Int): DictSource? = sources[id]

    fun allSources(): List<DictSource> = sources.values.toList().sortedBy { it.id }

    /** Attach an already-opened database (e.g. for unit testing). */
    @Synchronized
    fun attach(database: SQLiteDatabase) {
        db = database
        loadMetadata()
    }

    @Suppress("DEPRECATION")
    private fun appVersionCode(ctx: Context): Int =
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).let {
            if (android.os.Build.VERSION.SDK_INT >= 28) it.longVersionCode.toInt() else it.versionCode
        }

    @Synchronized
    fun open(ctx: Context) {
        if (isOpen) return

        val dir = File(ctx.filesDir, "dictionary").apply { mkdirs() }
        val targetFile = File(dir, "bundled.db")
        val prefs = ctx.getSharedPreferences("dictionary", Context.MODE_PRIVATE)
        val appCode = appVersionCode(ctx)

        if (!targetFile.exists() || prefs.getInt("dictAppCode", -1) != appCode) {
            val tmp = File(targetFile.path + ".tmp")
            val copied = runCatching {
                ctx.assets.open(ASSET).use { input ->
                    tmp.outputStream().use { output -> input.copyTo(output) }
                }
                if (targetFile.exists()) targetFile.delete()
                tmp.renameTo(targetFile)
            }.getOrDefault(false)

            if (copied) {
                prefs.edit().putInt("dictAppCode", appCode).apply()
            } else if (tmp.exists()) {
                tmp.delete()
            }
        }

        if (!targetFile.exists()) return

        runCatching {
            val database = SQLiteDatabase.openDatabase(targetFile.path, null, SQLiteDatabase.OPEN_READONLY)
            attach(database)
        }
    }

    private fun loadMetadata() {
        sources.clear()
        val d = db ?: return

        runCatching {
            d.rawQuery("SELECT key, value FROM meta", null).use { c ->
                while (c.moveToNext()) {
                    if (c.getString(0) == "dictionary_version") {
                        version = c.getString(1)
                    }
                }
            }
        }

        runCatching {
            d.rawQuery("SELECT id, code, name, licence, url, attribution, retrieved FROM source", null).use { c ->
                while (c.moveToNext()) {
                    val s = DictSource(
                        id = c.getInt(0),
                        code = c.getString(1) ?: "",
                        name = c.getString(2) ?: "",
                        licence = c.getString(3) ?: "",
                        url = c.getString(4) ?: "",
                        attribution = c.getString(5) ?: "",
                        retrieved = c.getString(6) ?: ""
                    )
                    sources[s.id] = s
                }
            }
        }
    }
}
