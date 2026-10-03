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

/**
 * Building blocks shared by every settings page, so each page is just a list of titled groups.
 *   SubPage       - back link, title, scrolling body
 *   SettingsGroup - an optional heading, a card of rows, an optional note under it
 *   Entry         - one row of the hub: icon, title and the current value
 */
internal data class Entry(val icon: Int, val title: String, val value: String = "", val onClick: (() -> Unit)? = null)

/** A settings page with a back link, a title, and its own scroll. */
@Composable
internal fun SubPage(title: String, backLabel: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column {
            NavBar(backLabel, onBack)
            Text(title, Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.displaySmall, color = Brand.Ink)
        }
        content()
        VSpace(200)
    }
}

/** A heading, a card and a note: the one shape every group of settings takes. */
@Composable
internal fun SettingsGroup(title: String? = null, note: String? = null, content: @Composable () -> Unit) {
    Column {
        if (title != null) SectionLabel(title)
        GroupCard { content() }
        if (note != null) Note(note)
    }
}

/** A list of hub rows with dividers between them. */
@Composable
internal fun EntryGroup(title: String, entries: List<Entry>) {
    SettingsGroup(title) {
        entries.forEachIndexed { i, e ->
            if (i > 0) RowDivider()
            ValueRow(e.title, e.value, e.icon, e.onClick)
        }
    }
}

/** Name of a language in its own script. */
internal fun langName(l: Lang) = when (l) { Lang.SA -> "संस्कृत"; Lang.HI -> "हिन्दी"; Lang.BN -> "বাংলা"; Lang.EN -> "English" }
