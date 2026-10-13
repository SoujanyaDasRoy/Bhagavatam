package com.bhagavatam.app.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.bhagavatam.app.BuildConfig

/** "Report an error": opens the maker's Telegram chat, with the verse and the word already copied so the reader only pastes. */
object Corrections {
    val enabled: Boolean get() = BuildConfig.TELEGRAM_USER.isNotBlank()

    fun open(ctx: Context, ref: String, word: String) {
        val clip = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clip.setPrimaryClip(ClipData.newPlainText("correction", "$ref: $word -> "))
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/" + BuildConfig.TELEGRAM_USER)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(intent) }
    }
}
