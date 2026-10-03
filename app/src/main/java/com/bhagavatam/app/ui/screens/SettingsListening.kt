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
fun ListeningSettings(state: AppState, onBack: () -> Unit) {
    val s = state.strings
    val t = settingsTextFor(state.uiLang)
    val ui = state.uiLang
    var showVoices by remember { mutableStateOf(false) }
    SubPage(t.listeningSec, s.tabMe, onBack) {
        SettingsGroup(tr(ui, "Voice and speed", "आवाज़ और गति", "কণ্ঠ ও গতি")) {
            ValueRow(playerTextFor(ui).voiceTitle, "", Ic.Headphones) { showVoices = true }; RowDivider()
            ValueRow(s.speed, "${state.speed}×") { state.cycleSpeed() }
        }
        SettingsGroup(tr(ui, "Playback", "प्लेबैक", "প্লেব্যাক"), s.backgroundNote) {
            ValueRow(s.playThrough, when (state.playThrough) { PlayThrough.ADHYAYA -> s.throughAdhyaya; PlayThrough.SKANDHA -> s.throughSkandha; PlayThrough.GRANTH -> s.throughGranth }) { state.cyclePlayThrough() }
            RowDivider()
            SwitchRow(s.keepPlaying, state.keepPlaying, state::updateKeepPlaying); RowDivider()
            SwitchRow(s.keepScreenOn, state.keepScreenOn, state::updateKeepScreenOn)
        }
        SettingsGroup(
            tr(ui, "Following along", "साथ-साथ पढ़ना", "সঙ্গে সঙ্গে পড়া"),
            tr(ui, "Scrolls the chapter to the verse that is being read aloud.", "जो श्लोक सुनाया जा रहा है, अध्याय उसी तक सरकता है।", "যে শ্লোক পড়ে শোনানো হচ্ছে, অধ্যায় সেখানে সরে যায়।"),
        ) {
            SwitchRow(tr(ui, "Follow the narration", "पाठ के साथ चलें", "পাঠের সঙ্গে চলুন"), state.followAudio, state::updateFollowAudio)
        }
    }
    if (showVoices) VoiceSheet(state) { showVoices = false }
}
