package com.bhagavatam.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * Where the text lives on the phone.
 *
 * - bundled.db: copied once from the APK's assets/content.db (again after each app update).
 * - current.db: a newer text downloaded later from GitHub Releases (not wired up yet).
 * On start the app opens whichever of the two has the higher content_version.
 */
object ContentDb {
    private const val ASSET = "content.db"

    private fun dir(ctx: Context) = File(ctx.filesDir, "content").apply { mkdirs() }
    fun bundledFile(ctx: Context) = File(dir(ctx), "bundled.db")
    fun currentFile(ctx: Context) = File(dir(ctx), "current.db")

    @Volatile
    var openVersion: Int = 0
        private set

    @Suppress("DEPRECATION")
    private fun appVersionCode(ctx: Context): Int =
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).let {
            if (android.os.Build.VERSION.SDK_INT >= 28) it.longVersionCode.toInt() else it.versionCode
        }

    /** Prepare the files and attach the newest database to [SampleData]. Thread-safe. */
    @Synchronized
    fun open(ctx: Context) {
        val prefs = ctx.getSharedPreferences("content", Context.MODE_PRIVATE)
        val code = appVersionCode(ctx)
        val bundled = bundledFile(ctx)
        if (prefs.getInt("bundledFor", -1) != code || !bundled.exists() || versionOf(bundled) < 7) {
            val tmp = File(bundled.path + ".tmp")
            val copied = runCatching {
                ctx.assets.open(ASSET).use { input -> tmp.outputStream().use { input.copyTo(it) } }
                if (bundled.exists()) bundled.delete()
                tmp.renameTo(bundled)
            }.getOrDefault(false)
            if (copied) prefs.edit().putInt("bundledFor", code).apply()
            else if (tmp.exists()) tmp.delete()
        }
        val best = listOf(currentFile(ctx), bundled).filter { it.exists() }.maxByOrNull { versionOf(it) } ?: return
        runCatching {
            val db = SQLiteDatabase.openDatabase(best.path, null, SQLiteDatabase.OPEN_READONLY)
            SampleData.attach(db)
            openVersion = versionOf(best)
        }
    }

    fun versionOf(file: File): Int {
        if (!file.exists()) return 0
        return runCatching {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery("SELECT value FROM meta WHERE key = 'content_version'", null).use { c ->
                    if (c.moveToFirst()) c.getString(0).toInt() else 0
                }
            }
        }.getOrDefault(0)
    }
}
