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

@Composable
fun ReadingSettings(state: AppState, onBack: () -> Unit) {
    val s = state.strings
    val t = settingsTextFor(state.uiLang)
    val ui = state.uiLang
    SubPage(t.readingSec, s.tabMe, onBack) {
        SettingsGroup(t.textSize, t.pinchHint) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t.textSize, Modifier.weight(1f), fontSize = 16.sp, color = Brand.Ink)
                    Text(localDigits("${(state.textScale * 100).toInt()}%", ui), fontSize = 14.sp, color = Brand.Secondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("A", fontSize = 14.sp, color = Brand.Secondary)
                    AppSlider(value = state.textScale, onValueChange = state::updateTextScale, valueRange = 0.8f..1.6f, modifier = Modifier.weight(1f))
                    Text("A", fontSize = 24.sp, color = Brand.Secondary)
                }
                Text(
                    t.sample, fontFamily = EnglishReading, fontSize = (17 * state.textScale).sp, lineHeight = (26 * state.textScale * state.lineScale).sp,
                    textAlign = if (state.justifyText) TextAlign.Justify else TextAlign.Start,
                    color = Brand.Ink, modifier = Modifier.fillMaxWidth().animateContentSize().clip(Radius.field).background(Brand.Fill).padding(14.dp),
                )
            }
        }
        SettingsGroup(t.lineSpacing) {
            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                val values = listOf(0.9f, 1.0f, 1.2f)
                Segmented(t.spacings, values.indexOfFirst { it == state.lineScale }.coerceAtLeast(1), { state.updateLineScale(values[it]) })
            }
        }
        SettingsGroup(
            tr(ui, "Page layout", "पृष्ठ का रूप", "পাতার বিন্যাস"),
            tr(ui, "Justified text has straight edges on both sides. Hiding the picture or the reading time gives a plainer page.",
                "सीधे किनारों वाला पाठ दोनों ओर बराबर होता है। चित्र या पाठ-समय छिपाने से पृष्ठ सादा दिखता है।",
                "জাস্টিফাইড লেখার দুই পাশই সোজা থাকে। ছবি বা পড়ার সময় লুকালে পাতা আরও সাদামাটা দেখায়।"),
        ) {
            SwitchRow(tr(ui, "Justify text", "पाठ बराबर करें", "লেখা সমান করুন"), state.justifyText, state::updateJustifyText); RowDivider()
            SwitchRow(tr(ui, "Verse numbers", "श्लोक संख्या", "শ্লোক সংখ্যা"), state.showVerseNumbers, state::updateShowVerseNumbers); RowDivider()
            SwitchRow(tr(ui, "Chapter picture", "अध्याय का चित्र", "অধ্যায়ের ছবি"), state.showChapterArt, state::updateShowChapterArt); RowDivider()
            SwitchRow(tr(ui, "Reading time", "पाठ-समय", "পড়ার সময়"), state.showReadTime, state::updateShowReadTime)
        }
        SettingsGroup(s.reading, s.showSanskritNote) {
            SwitchRow(s.showSanskrit, state.showSanskrit, state::updateShowSanskrit)
        }
        SettingsGroup(
            tr(ui, "Screen", "स्क्रीन", "স্ক্রিন"),
            tr(ui, "Stops the screen from turning off while a chapter is open.", "अध्याय खुला होने पर स्क्रीन बन्द नहीं होती।", "অধ্যায় খোলা থাকলে স্ক্রিন বন্ধ হয় না।"),
        ) {
            SwitchRow(tr(ui, "Keep screen on while reading", "पढ़ते समय स्क्रीन चालू रखें", "পড়ার সময় স্ক্রিন চালু রাখুন"), state.keepScreenOnReading, state::updateKeepScreenOnReading)
        }
    }
}
