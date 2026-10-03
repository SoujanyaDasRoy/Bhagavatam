package com.bhagavatam.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.bhagavatam.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String,
    val apkUrl: String,
    val apkSize: Long,
)

object Updater {
    private const val RELEASES_API = "https://api.github.com/repos/SoujanyaDasRoy/Bhagavatam/releases/latest"

    /** Compares semver strings (e.g. "v0.5.1" vs "0.5.0"). Returns true if remote is newer. */
    fun isNewer(remoteTag: String, currentVersion: String): Boolean {
        val r = remoteTag.trim().trimStart('v', 'V').split(".").mapNotNull { it.toIntOrNull() }
        val c = currentVersion.trim().trimStart('v', 'V').split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(r.size, c.size)) {
            val rVal = r.getOrElse(i) { 0 }
            val cVal = c.getOrElse(i) { 0 }
            if (rVal > cVal) return true
            if (rVal < cVal) return false
        }
        return false
    }

    suspend fun checkLatestRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(RELEASES_API)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Bhagavatam-Android-App")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                val tagName = json.optString("tag_name", "")
                val name = json.optString("name", tagName)
                val body = json.optString("body", "")
                val assets = json.optJSONArray("assets")

                var apkUrl = ""
                var apkSize = 0L
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        val aName = a.optString("name", "")
                        if (aName.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = a.optString("browser_download_url", "")
                            apkSize = a.optLong("size", 0L)
                            break
                        }
                    }
                }

                if (tagName.isNotEmpty() && apkUrl.isNotEmpty() && isNewer(tagName, BuildConfig.VERSION_NAME)) {
                    return@withContext ReleaseInfo(tagName, name, body, apkUrl, apkSize)
                }
            }
            null
        }.getOrNull()
    }

    fun canInstallApks(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun installDownloadedApk(context: Context, info: ReleaseInfo): Result<Unit> {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                openInstallPermissionSettings(context)
                throw SecurityException("UNKNOWN_SOURCES_PERMISSION_REQUIRED")
            }
            val cacheDir = context.externalCacheDir ?: context.cacheDir
            val outFile = File(cacheDir, "Bhagavatam-${info.tagName}.apk")
            if (!outFile.exists()) throw IllegalStateException("APK file not found: ${outFile.path}")

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        }
    }

    suspend fun downloadAndInstall(
        context: Context,
        info: ReleaseInfo,
        onProgress: (Float) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Check permission prior to starting heavy download
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                withContext(Dispatchers.Main) { openInstallPermissionSettings(context) }
                throw SecurityException("UNKNOWN_SOURCES_PERMISSION_REQUIRED")
            }

            val cacheDir = context.externalCacheDir ?: context.cacheDir
            val outFile = File(cacheDir, "Bhagavatam-${info.tagName}.apk")
            if (outFile.exists()) outFile.delete()

            val url = URL(info.apkUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 30000
            }

            val totalBytes = if (conn.contentLengthLong > 0) conn.contentLengthLong else info.apkSize
            var downloaded = 0L

            conn.inputStream.use { input ->
                FileOutputStream(outFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        if (totalBytes > 0) {
                            val prog = (downloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
                            withContext(Dispatchers.Main) { onProgress(prog) }
                        }
                    }
                }
            }

            // Launch Package Installer on Main thread
            withContext(Dispatchers.Main) {
                installDownloadedApk(context, info).getOrThrow()
            }
        }
    }
}
