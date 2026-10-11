package com.bhagavatam.app.ui.screens

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.BuildConfig
import com.bhagavatam.app.data.AnnKind
import com.bhagavatam.app.data.ContentDb
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.data.playerTextFor
import com.bhagavatam.app.data.settingsTextFor
import com.bhagavatam.app.data.tr
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.state.PlayThrough
import com.bhagavatam.app.state.ThemeMode
import com.bhagavatam.app.ui.components.AppSlider
import com.bhagavatam.app.ui.components.ConfirmDialog
import com.bhagavatam.app.ui.components.GroupCard
import com.bhagavatam.app.ui.components.Ic
import com.bhagavatam.app.ui.components.LargeTitle
import com.bhagavatam.app.ui.components.NavBar
import com.bhagavatam.app.ui.components.Note
import com.bhagavatam.app.ui.components.RowDivider
import com.bhagavatam.app.ui.components.SectionLabel
import com.bhagavatam.app.ui.components.Segmented
import com.bhagavatam.app.ui.components.SwitchRow
import com.bhagavatam.app.ui.components.VSpace
import com.bhagavatam.app.ui.components.ValueRow
import com.bhagavatam.app.ui.theme.AppColors
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.EnglishReading
import com.bhagavatam.app.ui.theme.LocalAppColors
import com.bhagavatam.app.ui.theme.Motion
import com.bhagavatam.app.ui.theme.Radius
import com.bhagavatam.app.ui.theme.ReaderTheme
import com.bhagavatam.app.ui.theme.appColorsFor

/** Plain text of every note, highlight and passage bookmark, in book order, for sharing out of the app. */
private fun notesAsText(state: AppState): String = buildString {
    fun part(ref: String, i: Int) = ref.split('.').getOrNull(i)?.toIntOrNull() ?: 0
    state.annotations.sortedWith(compareBy({ part(it.ref, 0) }, { part(it.ref, 1) }, { part(it.ref, 2) })).forEach { a ->
        append(a.ref).append("  [").append(a.kind.name.lowercase()).append("]\n")
        append(a.quote).append('\n')
        if (a.note.isNotBlank()) append("Note: ").append(a.note).append('\n')
        append('\n')
    }
}

@Composable
fun WordsSettings(state: AppState, onBack: () -> Unit) {
    val s = state.strings
    val ui = state.uiLang
    val ctx = LocalContext.current
    SubPage(wordsTitle(ui), s.tabMe, onBack) {
        SettingsGroup(
            tr(ui, "Word meanings", "शब्दों के अर्थ", "শব্দের অর্থ"),
            tr(ui, "When on, pressing and holding a word asks Wiktionary for its meaning. Only that one word is sent. Off keeps the app fully offline.",
                "चालू होने पर किसी शब्द को दबाए रखने पर उसका अर्थ Wiktionary से मँगाया जाता है। केवल वही एक शब्द भेजा जाता है। बंद रखने पर ऐप पूरी तरह ऑफ़लाइन रहता है।",
                "চালু থাকলে কোনো শব্দ চেপে ধরলে Wiktionary থেকে তার অর্থ আনা হয়। শুধু সেই একটি শব্দ পাঠানো হয়। বন্ধ রাখলে অ্যাপ পুরোপুরি অফলাইন থাকে।"),
        ) {
            val meaningLangOptions = listOf(
                tr(ui, "Word's language", "शब्द की भाषा", "শব্দের ভাষা"),
                tr(ui, "English", "अंग्रेज़ी", "ইংরেজি"),
                tr(ui, "App language", "ऐप की भाषा", "অ্যাপের ভাষা")
            )
            Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(
                    tr(ui, "Meaning language", "अर्थ की भाषा", "অর্থের ভাষা"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Brand.Secondary
                )
                Spacer(Modifier.height(6.dp))
                Segmented(
                    meaningLangOptions,
                    state.meaningLangMode.ordinal,
                    { state.updateMeaningLangMode(com.bhagavatam.app.state.MeaningLangMode.entries[it]) }
                )
            }
            RowDivider()
            SwitchRow(tr(ui, "Look up meanings online", "अर्थ ऑनलाइन खोजें", "অর্থ অনলাইনে খুঁজুন"), state.onlineMeanings, state::updateOnlineMeanings)
        }
        SettingsGroup(
            tr(ui, "Highlight colour", "हाइलाइट का रंग", "হাইলাইটের রং"),
            tr(ui, "The colour a new note starts with. You can still pick any of the four when you highlight.", "नया नोट इसी रंग से शुरू होता है। हाइलाइट करते समय चारों रंग चुन सकते हैं।", "নতুন নোট এই রং দিয়ে শুরু হয়। হাইলাইট করার সময় চারটির যেকোনোটি বেছে নিতে পারেন।"),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MarkColours.forEachIndexed { i, col ->
                    Box(Modifier.size(48.dp).selectable(selected = state.defaultMark == i, role = Role.RadioButton) { state.updateDefaultMark(i) }, contentAlignment = Alignment.Center) {
                        Box(Modifier.size(30.dp).clip(CircleShape).background(Color(col)).then(if (state.defaultMark == i) Modifier.border(3.dp, Brand.Ink, CircleShape) else Modifier))
                    }
                }
            }
        }
        val count = state.annotations.size
        SettingsGroup(
            tr(ui, "Your notes", "आपके नोट", "আপনার নোট"),
            tr(ui, "Notes and highlights live only on this phone. Sharing sends them as plain text to the app you choose.", "नोट और हाइलाइट केवल इसी फ़ोन में रहते हैं। साझा करने पर वे सादे पाठ के रूप में आपकी चुनी ऐप को जाते हैं।", "নোট ও হাইলাইট শুধু এই ফোনেই থাকে। শেয়ার করলে সেগুলো সাদা লেখা হিসেবে আপনার বাছা অ্যাপে যায়।"),
        ) {
            ValueRow(tr(ui, "Share notes as text", "नोट पाठ के रूप में साझा करें", "নোট লেখা হিসেবে শেয়ার করুন"), localDigits(count.toString(), ui), Ic.Scroll) {
                if (count > 0) {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, notesAsText(state))
                    runCatching { ctx.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
            }
        }
    }
}

internal fun wordsTitle(ui: Lang) = tr(ui, "Words & notes", "शब्द और नोट", "শব্দ ও নোট")
