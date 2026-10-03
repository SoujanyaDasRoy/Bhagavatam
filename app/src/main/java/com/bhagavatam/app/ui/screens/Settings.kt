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

private enum class DataAction { PROGRESS, SEARCHES, SAVED, RESET }

/**
 * Settings is a short hub. The theme switch comes first, then groups of rows built from [Entry] lists:
 * each row shows its current value and opens its own page (Appearance, Reading, Languages, Listening, Words & notes).
 * The pages live in SettingsAppearance, SettingsReading, SettingsListening and SettingsWords; shared pieces are in SettingsKit.
 */
@Composable
fun SettingsScreen(
    state: AppState, onSaved: () -> Unit, onGlossary: () -> Unit, onLanguages: () -> Unit,
    onAppearance: () -> Unit, onReading: () -> Unit, onListening: () -> Unit, onWords: () -> Unit = {},
) {
    val s = state.strings
    val t = settingsTextFor(state.uiLang)
    val ui = state.uiLang
    var pending by remember { mutableStateOf<DataAction?>(null) }
    val total = remember { SampleData.skandhas.sumOf { it.adhyayaCount } }
    val savedCount = (state.bookmarks + state.highlights.keys).distinct().size + state.annotations.size

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        LargeTitle(s.tabMe)

        Column {
            SectionLabel(t.theme)
            ThemeSwitcher(state, t.modes)
            if (state.themeMode == ThemeMode.SYSTEM) Note(t.matchPhoneNote)
        }

        EntryGroup(
            t.preferences,
            listOf(
                Entry(Ic.Palette, t.appearance, t.modes[state.themeMode.ordinal], onAppearance),
                Entry(Ic.TextSize, t.readingSec, localDigits("${(state.textScale * 100).toInt()}%", ui), onReading),
                Entry(Ic.Languages, s.languages, "${langName(state.uiLang)} \u00b7 ${langName(state.readLang)}", onLanguages),
                Entry(Ic.Headphones, t.listeningSec, "${state.speed}\u00d7", onListening),
                Entry(Ic.Scroll, wordsTitle(ui), tr(ui, if (state.onlineMeanings) "Online" else "Offline", if (state.onlineMeanings) "ऑनलाइन" else "ऑफ़लाइन", if (state.onlineMeanings) "অনলাইন" else "অফলাইন"), onWords),
            ),
        )

        EntryGroup(
            t.librarySec,
            listOf(
                Entry(Ic.Bookmark, s.savedVerses, localDigits(savedCount.toString(), ui), onSaved),
                Entry(Ic.Scroll, s.thematicLilaIndex, "", onGlossary),
            ),
        )

        EntryGroup(
            t.dataSec,
            listOf(
                Entry(Ic.History, t.clearProgress, localDigits("${state.finished.size} / $total", ui)) { pending = DataAction.PROGRESS },
                Entry(Ic.Search, t.clearSearches, localDigits(state.recent.size.toString(), ui)) { pending = DataAction.SEARCHES },
                Entry(Ic.DeleteOutline, t.clearSaved, localDigits(savedCount.toString(), ui)) { pending = DataAction.SAVED },
                Entry(Ic.Settings, t.resetSettings, "") { pending = DataAction.RESET },
            ),
        )

        EntryGroup(
            t.aboutSec,
            listOf(
                Entry(Ic.Scroll, t.textsFrom, s.gitaPress),
                Entry(Ic.Scroll, tr(ui, "Corpus", "सम्पूर्ण ग्रन्थ", "সম্পূর্ণ গ্রন্থ"), localDigits(tr(ui, "341 Chapters \u00b7 14,580 Verses", "३४१ अध्याय \u00b7 १४,५८० श्लोक", "৩৪১টি অধ্যায় \u00b7 ১৪,৫৮০টি শ্লোক"), ui)),
                Entry(Ic.Settings, t.version, "v${BuildConfig.VERSION_NAME}"),
                Entry(Ic.Settings, t.contentVersion, localDigits(ContentDb.openVersion.toString(), ui)),
            ),
        )
        VSpace(200)
    }

    pending?.let { action ->
        val (title, message, confirm) = when (action) {
            DataAction.PROGRESS -> Triple(t.clearProgress, t.clearProgressMsg, t.doClear)
            DataAction.SEARCHES -> Triple(t.clearSearches, t.clearSearchesMsg, t.doClear)
            DataAction.SAVED -> Triple(t.clearSaved, t.clearSavedMsg, t.doRemove)
            DataAction.RESET -> Triple(t.resetSettings, t.resetSettingsMsg, t.doReset)
        }
        ConfirmDialog(title, message, confirm, t.cancel, onDismiss = { pending = null }) {
            when (action) {
                DataAction.PROGRESS -> state.clearProgress()
                DataAction.SEARCHES -> state.clearRecent()
                DataAction.SAVED -> state.clearSaved()
                DataAction.RESET -> state.resetSettings()
            }
        }
    }
}
