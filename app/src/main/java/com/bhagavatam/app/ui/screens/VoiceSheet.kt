package com.bhagavatam.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.audio.Narrator
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.data.playerTextFor
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.components.GroupCard
import com.bhagavatam.app.ui.components.Ic
import com.bhagavatam.app.ui.components.Note
import com.bhagavatam.app.ui.components.RowDivider
import com.bhagavatam.app.ui.components.SectionLabel
import com.bhagavatam.app.ui.components.Segmented
import com.bhagavatam.app.ui.components.ValueRow
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.Jakarta
import com.bhagavatam.app.ui.theme.NotoSerifBengali
import com.bhagavatam.app.ui.theme.TiroHindi

/**
 * Which voice reads each language, how long the pauses are, and a way to install more voices.
 * Voices come from the phone's own speech engine; nothing is downloaded by this app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSheet(state: AppState, onDismiss: () -> Unit) {
    val t = playerTextFor(state.uiLang)
    val context = LocalContext.current
    val langs = listOf(Lang.HI, Lang.BN, Lang.EN)
    // Sanskrit is read with the Hindi voice, so it starts on Hindi.
    var lang by remember { mutableStateOf(if (state.audioLang == Lang.SA) Lang.HI else state.audioLang) }
    LaunchedEffect(Unit) { state.prepareVoices() }
    val options = state.voiceOptions(lang)
    val chosen = state.voiceFor(lang)
    val scales = listOf(0.6f, 1f, 1.5f)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Brand.Paper) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(t.voiceTitle, Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleLarge, color = Brand.Ink)
            Segmented(
                listOf("हिन्दी", "বাংলা", "English"), langs.indexOf(lang), { lang = langs[it] },
                fonts = listOf(TiroHindi, NotoSerifBengali, Jakarta),
            )
            Column {
                SectionLabel(t.voiceWord)
                GroupCard {
                    VoiceRow(t.automatic, "", chosen == null) { state.previewVoice(lang, null); state.chooseVoice(lang, null) }
                    options.forEach { o ->
                        RowDivider()
                        val quality = t.qualities[o.level.coerceIn(0, 3)] + ", " + if (o.needsInternet) t.online else t.offline
                        VoiceRow("${t.voiceWord} ${localDigits(o.number.toString(), state.uiLang)}", quality, chosen == o.name) {
                            // Hear it first; the choice is remembered at once.
                            state.previewVoice(lang, o.name); state.chooseVoice(lang, o.name)
                        }
                    }
                }
                if (state.voicesReady && options.isEmpty()) Note(t.noVoicesFor) else Note(t.tapToHear)
                if (lang == Lang.HI) Note(t.sanskritViaHindi)
            }
            Column {
                SectionLabel(t.pausesTitle)
                Segmented(t.pauseOptions, scales.indexOfFirst { it == state.pauseScale }.coerceAtLeast(1), { state.updatePauseScale(scales[it]) })
            }
            GroupCard {
                ValueRow(t.installMore, "", Ic.Headphones) { runCatching { context.startActivity(Narrator.settingsIntent()) } }
            }
        }
    }
}

@Composable
private fun VoiceRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onClick).heightIn(min = 60.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = Brand.Ink, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 13.sp, color = Brand.Secondary)
        }
        if (selected) Icon(painterResource(Ic.Check), null, tint = Brand.Kesari)
    }
}
