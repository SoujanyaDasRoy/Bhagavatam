package com.bhagavatam.app.ui.components

import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.CircularProgressIndicator
import com.bhagavatam.app.data.playerTextFor
import com.bhagavatam.app.state.AudioStatus
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.tween
import com.bhagavatam.app.ui.theme.Motion
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.bhagavatam.app.ui.theme.Radius
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

enum class Tab(val route: String, val icon: Int) {
    Home("home", Ic.Home),
    Granth("granth", Ic.MenuBook),
    Search("search", Ic.Search),
    Downloads("downloads", Ic.Download),
    Me("me", Ic.Settings),
}

/** Chrome that must stay one line (tab labels, mini player) grows with the user's text size, but only up to [max]. */
@Composable
private fun CappedFontScale(max: Float = 1.3f, content: @Composable () -> Unit) {
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale.coerceAtMost(max)), content = content)
}

/** Floating frosted capsule tab bar; the active tab sits in a saffron pill. */
@Composable
fun FloatingTabBar(state: AppState, active: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) = CappedFontScale {
    val s = state.strings
    val labels = mapOf(Tab.Home to s.tabHome, Tab.Granth to s.tabGranth, Tab.Search to s.tabSearch, Tab.Downloads to s.tabDownloads, Tab.Me to s.tabMe)
    Row(
        modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp).fillMaxWidth().heightIn(min = 64.dp)
            .shadow(18.dp, RoundedCornerShape(32.dp), ambientColor = Color(0x331C1A17), spotColor = Color(0x331C1A17))
            .clip(RoundedCornerShape(32.dp)).background(Brand.Card)
            .border(1.dp, Brand.Separator, RoundedCornerShape(32.dp)).padding(6.dp).height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Tab.entries.filter { it != Tab.Downloads }.forEach { tab ->
            val on = tab == active
            val pill by animateColorAsState(if (on) Brand.KesariTint else Brand.KesariTint.copy(alpha = 0f), tween(Motion.sheet), label = "tabPill")
            val tint by animateColorAsState(if (on) Brand.Kesari else Brand.Secondary, tween(Motion.sheet), label = "tabTint")
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(26.dp))
                    .background(pill)
                    .selectable(selected = on, role = Role.Tab) { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(painterResource(tab.icon), contentDescription = null, tint = tint, modifier = Modifier.size(23.dp))
                Text(labels.getValue(tab), fontSize = 12.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, color = tint, maxLines = 1)
            }
        }
    }
}

/** Fades scrolling content into the page colour behind floating controls, so text never runs under a bare pill. */
@Composable
fun BottomScrim(modifier: Modifier = Modifier, height: Dp = 140.dp) {
    val bg = Brand.Paper
    Box(modifier.fillMaxWidth().height(height).background(Brush.verticalGradient(listOf(bg.copy(alpha = 0f), bg.copy(alpha = 0.92f), bg))))
}

/** Mini player: one compact row (shloka and chapter, play/pause, open). Speed, previous, next and loop live in the full player. */
@Composable
fun MiniPlayer(state: AppState, onOpen: () -> Unit, modifier: Modifier = Modifier) = CappedFontScale {
    val c = LocalReaderColors.current
    val v = state.current
    val langLabel = when (state.audioLang) { Lang.SA -> "संस्कृत"; Lang.HI -> "हिन्दी"; Lang.BN -> "বাংলা"; Lang.EN -> "English" }
    val title = SampleData.adhyayaTitle(v.skandha, v.adhyaya, state.titleLang, state.strings)
    val t = playerTextFor(state.uiLang)
    // What the second line says: the problem if there is one, otherwise the state, otherwise the chapter.
    val line = when {
        state.audioIssue != null -> t.textOnly
        state.audioStatus == AudioStatus.PREPARING -> t.preparing
        state.audioStatus == AudioStatus.PAUSED -> t.paused
        state.audioStatus == AudioStatus.ENDED -> t.ended
        else -> title
    }
    val chapterProgress = if (state.queue.isEmpty()) 0f else ((state.index + state.progress) / state.queue.size).coerceIn(0f, 1f)
    Column(
        modifier.padding(horizontal = 10.dp).fillMaxWidth()
            .shadow(16.dp, Radius.card, ambientColor = Color(0x331C1A17), spotColor = Color(0x331C1A17))
            .clip(Radius.card).background(c.surface)
            .border(1.dp, c.separator, Radius.card),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onOpen)
                .pointerInput(Unit) {
                    var dy = 0f
                    detectVerticalDragGestures(onDragStart = { dy = 0f }, onDragCancel = { dy = 0f }, onDragEnd = { if (dy < -40f) onOpen() }) { _, d -> dy += d }
                }
                .padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppTile()
            Column(Modifier.weight(1f)) {
                Text("${state.strings.shloka} ${localDigits(v.ref, state.uiLang)} · $langLabel", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.ink, maxLines = 1)
                Text(line, fontSize = 12.sp, color = c.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val label = if (state.isPlaying) t.pause else t.play
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(c.accent).clickable(onClickLabel = label, role = Role.Button) { state.togglePlay() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (state.isPlaying) painterResource(Ic.Pause) else painterResource(Ic.PlayArrow), label, tint = c.chipOnText, modifier = Modifier.size(26.dp))
                if (state.audioStatus == AudioStatus.PREPARING) CircularProgressIndicator(Modifier.matchParentSize().padding(3.dp), color = c.chipOnText.copy(alpha = 0.85f), strokeWidth = 2.dp)
            }
            Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onOpen), contentAlignment = Alignment.Center) {
                Icon(painterResource(Ic.KeyboardArrowUp), contentDescription = t.voiceTitle.let { state.strings.nowPlaying }, tint = c.ink)
            }
        }
        LinearProgressIndicator(
            progress = { chapterProgress }, modifier = Modifier.fillMaxWidth().height(3.dp),
            color = c.accent, trackColor = c.track, strokeCap = StrokeCap.Butt, gapSize = 0.dp, drawStopIndicator = {},
        )
    }
}
