package com.bhagavatam.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bhagavatam.app.data.ContentDb
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.AppNav
import com.bhagavatam.app.ui.screens.SplashScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    // The state lives in the Application, so the voice carries on when this screen goes away.
    private val state: AppState get() = (application as BhagavatamApp).state

    private val askNotifications = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }

    override fun onStop() {
        super.onStop()
        state.onAppBackgrounded()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.bhagavatam.app.data.UpdateWorker.schedule(applicationContext)
        setContent {
            // The text database is opened off the main thread while the splash shows (at least 700 ms so it never flashes).
            var ready by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                val t0 = System.currentTimeMillis()
                withContext(Dispatchers.IO) {
                    ContentDb.open(applicationContext)
                    com.bhagavatam.app.data.SearchIndex.open(applicationContext)
                    com.bhagavatam.app.data.DictionaryDb.open(applicationContext)
                }
                val left = 700 - (System.currentTimeMillis() - t0)
                if (left > 0) delay(left)
                ready = true
                // Debug builds only: check the narration layer against the real text and engine (see NarrationProbe).
                if (BuildConfig.DEBUG && intent?.getBooleanExtra("narration_probe", false) == true) com.bhagavatam.app.audio.NarrationProbe.run(application)
            }
            Crossfade(ready, label = "splash") { isReady ->
                if (!isReady) SplashScreen()
                else {
                    // Lock-screen controls need the notification permission on Android 13 and later; asked once, when playback first starts.
                    LaunchedEffect(state.hasSession) {
                        if (state.hasSession && android.os.Build.VERSION.SDK_INT >= 33 &&
                            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            askNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    // Once, on the first Home: say why, then let Android ask (Android 13 and later).
                    if (android.os.Build.VERSION.SDK_INT >= 33 && state.onboarded && !state.askedNotify &&
                        checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        val u = state.uiLang
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { state.markAskedNotify() },
                            title = { androidx.compose.material3.Text(com.bhagavatam.app.data.tr(u, "Notifications", "सूचनाएँ", "বিজ্ঞপ্তি")) },
                            text = { androidx.compose.material3.Text(com.bhagavatam.app.data.tr(u,
                                "Allow notifications to control playback from the lock screen and to hear about new versions of the app.",
                                "लॉक स्क्रीन से पाठ नियंत्रित करने और ऐप के नए संस्करण की जानकारी पाने के लिए सूचनाओं की अनुमति दें।",
                                "লক স্ক্রিন থেকে পাঠ নিয়ন্ত্রণ ও অ্যাপের নতুন সংস্করণের খবর পেতে বিজ্ঞপ্তির অনুমতি দিন।")) },
                            confirmButton = { androidx.compose.material3.TextButton(onClick = { state.markAskedNotify(); askNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS) }) {
                                androidx.compose.material3.Text(com.bhagavatam.app.data.tr(u, "Continue", "आगे बढ़ें", "এগিয়ে যান")) } },
                            dismissButton = { androidx.compose.material3.TextButton(onClick = { state.markAskedNotify() }) {
                                androidx.compose.material3.Text(com.bhagavatam.app.data.tr(u, "Not now", "अभी नहीं", "এখন নয়")) } },
                        )
                    }
                    LaunchedEffect(state.keepScreenOn) {
                        if (state.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    AppNav(state)
                }
            }
        }
    }
}
