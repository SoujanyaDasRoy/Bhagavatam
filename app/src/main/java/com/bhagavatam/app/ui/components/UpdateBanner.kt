package com.bhagavatam.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.ReleaseInfo
import com.bhagavatam.app.data.Updater
import com.bhagavatam.app.data.tr
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.Radius
import kotlinx.coroutines.launch

private var checkedThisRun = false

/** Asks GitHub once per app start for a newer release; shows a card with Install when there is one. Silent when offline. */
@Composable
fun UpdateBanner(state: AppState, modifier: Modifier = Modifier) {
    val ui = state.uiLang
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<ReleaseInfo?>(null) }
    var progress by remember { mutableStateOf<Float?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!checkedThisRun) {
            checkedThisRun = true
            info = Updater.checkLatestRelease()
        }
    }
    val r = info ?: return
    Column(
        modifier.fillMaxWidth().clip(Radius.group).background(Brand.KesariTint).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            tr(ui, "Version ${r.tagName.trimStart('v', 'V')} is available", "संस्करण ${r.tagName.trimStart('v', 'V')} उपलब्ध है", "সংস্করণ ${r.tagName.trimStart('v', 'V')} পাওয়া যাচ্ছে", "ସଂସ୍କରଣ ${r.tagName.trimStart('v', 'V')} ଉପଲବ୍ଧ"),
            fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Brand.Ink,
        )
        if (failed) Text(
            tr(ui, "Could not install. Allow installs from this app, then try again.", "इंस्टॉल नहीं हो सका। इस ऐप को इंस्टॉल की अनुमति दें, फिर दोबारा करें।", "ইনস্টল করা যায়নি। এই অ্যাপকে ইনস্টলের অনুমতি দিয়ে আবার চেষ্টা করুন।", "ଇନ୍‌ଷ୍ଟଲ୍ ହୋଇପାରିଲା ନାହିଁ। ଏହି ଆପ୍‌ରୁ ଇନ୍‌ଷ୍ଟଲ୍ ଅନୁମତି ଦିଅନ୍ତୁ, ତା'ପରେ ପୁଣି ଚେଷ୍ଟା କରନ୍ତୁ।"),
            fontSize = 14.sp, color = Brand.Secondary,
        )
        val p = progress
        if (p != null && !failed) LinearProgressIndicator(progress = { p }, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                tr(ui, "Install", "इंस्टॉल करें", "ইনস্টল করুন", "ଇନ୍‌ଷ୍ଟଲ୍ କରନ୍ତୁ"),
                Modifier.heightIn(min = 48.dp).clip(Radius.group).background(Brand.Kesari).clickable(enabled = p == null || failed) {
                    failed = false
                    progress = 0f
                    scope.launch {
                        val res = Updater.downloadAndInstall(ctx, r) { progress = it }
                        if (res.isFailure) { failed = true; progress = null }
                    }
                }.padding(horizontal = 22.dp, vertical = 12.dp),
                fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Brand.OnKesari,
            )
            Text(
                tr(ui, "Your notes and settings stay.", "आपके नोट और सेटिंग बनी रहेंगी।", "আপনার নোট ও সেটিংস থাকবে।", "ଆପଣଙ୍କ ନୋଟ୍ ଓ ସେଟିଂସ୍ ରହିବ।"),
                fontSize = 14.sp, color = Brand.Secondary,
            )
        }
    }
}
