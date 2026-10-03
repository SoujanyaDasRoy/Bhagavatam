package com.bhagavatam.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.LocalReaderColors

enum class Tab(val route: String, val icon: ImageVector) {
    Home("home", Icons.Rounded.Home),
    Granth("granth", Icons.AutoMirrored.Rounded.MenuBook),
    Search("search", Icons.Rounded.Search),
    Downloads("downloads", Icons.Rounded.Download),
    Me("me", Icons.Rounded.Person),
}

/** Floating frosted capsule tab bar; the active tab sits in a saffron pill. */
@Composable
fun FloatingTabBar(state: AppState, active: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val s = state.strings
    val labels = mapOf(Tab.Home to s.tabHome, Tab.Granth to s.tabGranth, Tab.Search to s.tabSearch, Tab.Downloads to s.tabDownloads, Tab.Me to s.tabMe)
    Row(
        modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp).fillMaxWidth().height(64.dp)
            .shadow(18.dp, RoundedCornerShape(32.dp), ambientColor = Color(0x331C1A17), spotColor = Color(0x331C1A17))
            .clip(RoundedCornerShape(32.dp)).background(Color(0xF2FFFFFF))
            .border(0.5.dp, Color(0x121C1A17), RoundedCornerShape(32.dp)).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Tab.entries.forEach { tab ->
            val on = tab == active
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(26.dp))
                    .background(if (on) Brand.Kesari.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val tint = if (on) Brand.Kesari else Brand.Secondary
                Icon(tab.icon, contentDescription = labels[tab], tint = tint, modifier = Modifier.size(23.dp))
                Text(labels.getValue(tab), fontSize = 10.5.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, color = tint, maxLines = 1)
            }
        }
    }
}

/** Mini player: shloka + chapter, seek bar, and speed / previous / play / next / loop. */
@Composable
fun MiniPlayer(state: AppState, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalReaderColors.current
    val v = state.current
    val langLabel = when (state.audioLang) { Lang.SA -> "संस्कृत"; Lang.HI -> "हिन्दी"; Lang.BN -> "বাংলা"; Lang.EN -> "English" }
    val title = SampleData.adhyayaTitle(v.skandha, v.adhyaya, state.titleLang, state.strings)
    Column(
        modifier.padding(horizontal = 10.dp).fillMaxWidth()
            .shadow(20.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x331C1A17), spotColor = Color(0x331C1A17))
            .clip(RoundedCornerShape(24.dp)).background(c.surface)
            .border(0.5.dp, c.separator, RoundedCornerShape(24.dp))
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTile()
            Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
                Text("${state.strings.shloka} ${localDigits(v.ref, state.uiLang)} · $langLabel", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.ink, maxLines = 1)
                Text(title, fontSize = 12.sp, color = c.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(c.track).clickable(onClick = onOpen),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Open full player", tint = c.ink) }
        }
        LinearProgressIndicator(
            progress = { state.progress }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = c.teal, trackColor = c.track, strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {},
        )
        state.audioNote?.let { Text(it, fontSize = 11.sp, color = c.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.clip(CircleShape).border(1.dp, c.separator, CircleShape).clickable { state.cycleSpeed() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) { Text("${state.speed}×", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.ink) }
            IconButton(onClick = state::previous) { Icon(Icons.Rounded.SkipPrevious, "Previous shloka", tint = c.ink) }
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(Brand.Kesari).clickable { state.togglePlay() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (state.isPlaying) "Pause" else "Play", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            IconButton(onClick = state::next) { Icon(Icons.Rounded.SkipNext, "Next shloka", tint = c.ink) }
            IconButton(onClick = state::toggleLoop) {
                Icon(Icons.Rounded.Repeat, "Loop shloka", tint = if (state.loop) c.teal else c.ink)
            }
        }
    }
}
