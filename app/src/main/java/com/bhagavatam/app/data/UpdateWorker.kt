package com.bhagavatam.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bhagavatam.app.MainActivity
import java.util.concurrent.TimeUnit

/** Twice a day, while online, asks GitHub for a newer release and posts one notification per version. Off when the switch in Settings is off. */
class UpdateWorker(private val ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val prefs = ctx.getSharedPreferences("bhagavatam", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("updateAlerts", true)) return Result.success()
        val info = Updater.checkLatestRelease() ?: return Result.success()
        if (prefs.getString("notifiedTag", "") == info.tagName) return Result.success()
        if (android.os.Build.VERSION.SDK_INT >= 33 && ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Updates", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val ui = prefs.getString("uiLang", "EN")
        val title = when (ui) { "HI" -> "भागवतम का नया संस्करण"; "BN" -> "ভাগবতমের নতুন সংস্করণ"; else -> "A new version of Bhagavatam" }
        val text = when (ui) { "HI" -> "संस्करण ${info.tagName.trimStart('v', 'V')} उपलब्ध है। खोलकर इंस्टॉल करें।"; "BN" -> "সংস্করণ ${info.tagName.trimStart('v', 'V')} পাওয়া যাচ্ছে। খুলে ইনস্টল করুন।"; else -> "Version ${info.tagName.trimStart('v', 'V')} is available. Open the app to install it." }
        nm.notify(7001, NotificationCompat.Builder(ctx, CHANNEL).setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title).setContentText(text).setContentIntent(open).setAutoCancel(true).build())
        prefs.edit().putString("notifiedTag", info.tagName).apply()
        return Result.success()
    }

    companion object {
        private const val CHANNEL = "updates"

        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<UpdateWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("update-check", ExistingPeriodicWorkPolicy.KEEP, req)
            // and once now, so an update is noticed the day it is published and not only at the next 12-hour tick
            val now = OneTimeWorkRequestBuilder<UpdateWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(ctx).enqueueUniqueWork("update-check-now", androidx.work.ExistingWorkPolicy.REPLACE, now)
        }
    }
}
