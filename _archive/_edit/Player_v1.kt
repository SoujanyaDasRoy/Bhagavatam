package com.bhagavatam.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.BENGALI_READY
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.components.ShlokaText
import com.bhagavatam.app.ui.theme.Jakarta
import com.bhagavatam.app.ui.theme.LocalReaderColors
import com.bhagavatam.app.ui.theme.TiroBangla
import com.bhagavatam.app.ui.theme.TiroHindi
import com.bhagavatam.app.ui.theme.TiroSanskrit

private fun clock(sec: Int) = "%d:%02d".format(sec / 60, sec % 60)

@Composable
fun PlayerScreen(state: AppState, onClose: () -> Unit) {
    val c = LocalReaderColors.current
    val s = state.strings
    val ui = state.uiLang
    val v = state.current
    val total = state.queue.size
    var showSleep by remember { mutableStateOf(false) }
    var drag by remember { mutableStateOf<Float?>(null) }
    val scale = state.textScale
    val lang = state.audioLang
    val shownIdx = (drag ?: state.index.toFloat()).toInt().coerceIn(0, total - 1)

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding().navigationBarsPadding()) {
        // Header: swipe down or tap the chevron to close.
        Column(
            Modifier.fillMaxWidth().pointerInput(Unit) {
                var dy = 0f
                detectVerticalDragGestures(onDragStart = { dy = 0f }, onDragEnd = { if (dy > 90f) onClose() }) { _, d -> dy += d }
            },
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(40.dp).height(5.dp).clip(CircleShape).background(c.separator))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, Modifier.size(48.dp)) { Icon(Icons.Rounded.KeyboardArrowDown, "Close player", tint = c.ink) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.nowPlaying, fontSize = 12.sp, color = c.secondary)
                    Text(
                        localDigits("${v.skandha}.${v.adhyaya}", ui) + " · " + SampleData.adhyayaTitle(v.skandha, v.adhyaya, state.titleLang, s),
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1,
                    )
                }
                IconButton(onClick = { showSleep = !showSleep }, Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Bedtime, s.sleepTimer, tint = if (state.sleepMinutes > 0) c.accent else c.ink)
                }
            }
        }

        // The verse
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(
                localDigits("${s.shloka} ${v.numLabel}", ui), Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.gold,
            )
            if (lang == Lang.SA) {
                ShlokaText(v, state.script, c.shloka, 22f * scale, center = true)
                val tr = state.alongsideLayers().firstOrNull()
                if (tr != null && v.hasText(tr)) {
                    Text(v.translation(tr), fontFamily = readingFont(tr), fontSize = (16 * scale).sp, lineHeight = (25 * scale).sp, color = c.secondary)
                }
            } else {
                if (state.showSanskrit) ShlokaText(v, state.script, c.shloka, 17f * scale, center = true)
                if (v.hasText(lang)) {
                    Text(v.translation(lang), fontFamily = readingFont(lang), fontSize = (20 * scale).sp, lineHeight = (32 * scale).sp, color = c.ink)
                }
            }
            state.audioNote?.let { Text(it, fontSize = 12.sp, color = c.secondary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
        }

        // Position in the chapter
        Column(Modifier.padding(horizontal = 20.dp)) {
            Slider(
                value = drag ?: state.index.toFloat(),
                onValueChange = { drag = it },
                onValueChangeFinished = { drag?.let { state.seekTo(it.toInt()) }; drag = null },
                valueRange = 0f..(total - 1).coerceAtLeast(1).toFloat(),
                enabled = total > 1,
                colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent, inactiveTrackColor = c.track),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Shloka ${shownIdx + 1} of $total" },
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Text(localDigits("${s.shloka} ${shownIdx + 1} / $total", ui), Modifier.weight(1f), fontSize = 13.sp, color = c.secondary)
                val len = state.verseSeconds
                Text(localDigits("${clock((len * state.progress).toInt())} / ${clock(len)}", ui), fontSize = 13.sp, color = c.secondary)
            }
        }

        // Transport
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = state::previous, Modifier.size(56.dp)) { Icon(Icons.Rounded.SkipPrevious, "Previous shloka", tint = c.ink, modifier = Modifier.size(34.dp)) }
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(c.accent)
                    .clickable(onClickLabel = if (state.isPlaying) "Pause" else "Play", role = Role.Button) { state.togglePlay() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (state.isPlaying) "Pause" else "Play",
                    tint = c.chipOnText, modifier = Modifier.size(38.dp))
            }
            IconButton(onClick = state::next, Modifier.size(56.dp)) { Icon(Icons.Rounded.SkipNext, "Next shloka", tint = c.ink, modifier = Modifier.size(34.dp)) }
        }

        // Speed, loop, sleep
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Chip("${state.speed}×", s.speed, on = false) { state.cycleSpeed() }
            Chip(null, s.loop, on = state.loop, icon = Icons.Rounded.Repeat) { state.toggleLoop() }
            Chip(if (state.sleepMinutes > 0) clock(state.sleepLeftSec) else null, s.sleep, on = state.sleepMinutes > 0, icon = Icons.Rounded.Bedtime) { showSleep = !showSleep }
        }
        if (showSleep) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                listOf(0, 15, 30, 45, 60).forEach { m ->
                    val on = state.sleepMinutes == m
                    Box(
                        Modifier.heightIn(min = 48.dp).clip(CircleShape).background(if (on) c.chipOn else c.track)
                            .clickable(role = Role.RadioButton) { state.setSleep(m); showSleep = false }.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(if (m == 0) s.off else localDigits("$m", ui), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (on) c.chipOnText else c.ink) }
                }
            }
        }

        // Language heard
        val langs = buildList { add(Lang.SA); add(Lang.HI); if (BENGALI_READY) add(Lang.BN); add(Lang.EN) }
        val names = langs.map { when (it) { Lang.SA -> "संस्कृत"; Lang.HI -> "हिन्दी"; Lang.BN -> "বাংলা"; Lang.EN -> "English" } }
        val fonts = langs.map { when (it) { Lang.SA -> TiroSanskrit; Lang.HI -> TiroHindi; Lang.BN -> TiroBangla; Lang.EN -> Jakarta } }
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.track).padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            langs.forEachIndexed { i, l ->
                val on = l == state.readLang
                Box(
                    Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(11.dp)).background(if (on) c.surface else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable(role = Role.Tab) { state.updateReadLang(l); if (state.isPlaying) { state.togglePlay(); state.togglePlay() } },
                    contentAlignment = Alignment.Center,
                ) { Text(names[i], fontFamily = fonts[i], fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (on) c.ink else c.secondary, maxLines = 1) }
            }
        }
    }
}

@Composable
private fun Chip(top: String?, label: String, on: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    val c = LocalReaderColors.current
    Row(
        Modifier.heightIn(min = 48.dp).clip(CircleShape).border(1.dp, if (on) c.accent else c.separator, CircleShape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = if (on) c.accent else c.ink, modifier = Modifier.size(18.dp))
        if (top != null) Text(top, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (on) c.accent else c.ink)
        else Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (on) c.accent else c.ink)
    }
}
