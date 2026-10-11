package com.bhagavatam.app.ui.screens

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bhagavatam.app.audio.AudioMode
import com.bhagavatam.app.audio.Narrator
import com.bhagavatam.app.audio.RecitationStream
import com.bhagavatam.app.audio.SegKind
import com.bhagavatam.app.audio.VersePlan
import com.bhagavatam.app.audio.stream.StreamStatus
import com.bhagavatam.app.data.BENGALI_READY
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.PlayerText
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.SanskritScript
import com.bhagavatam.app.data.Verse
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.data.playerTextFor
import com.bhagavatam.app.data.tr
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.state.AudioIssue
import com.bhagavatam.app.state.AudioStatus
import com.bhagavatam.app.state.PlayThrough
import com.bhagavatam.app.ui.components.AppSlider
import com.bhagavatam.app.ui.components.Ic
import com.bhagavatam.app.ui.components.Mandala
import com.bhagavatam.app.ui.components.artRes
import com.bhagavatam.app.ui.components.chArt
import com.bhagavatam.app.ui.components.keepMarkerTogether
import com.bhagavatam.app.ui.components.opticallyCentred
import com.bhagavatam.app.ui.components.skArt
import com.bhagavatam.app.ui.components.tappable
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.EnglishReading
import com.bhagavatam.app.ui.theme.Jakarta
import com.bhagavatam.app.ui.theme.LocalReaderColors
import com.bhagavatam.app.ui.theme.Motion
import com.bhagavatam.app.ui.theme.NotoDevanagari
import com.bhagavatam.app.ui.theme.NotoSerifBengali
import com.bhagavatam.app.ui.theme.Radius
import com.bhagavatam.app.ui.theme.TiroHindi
import com.bhagavatam.app.util.Transliterate

private fun clock(sec: Int) = "%d:%02d".format(sec / 60, sec % 60)

private fun languageName(ui: Lang, l: Lang) = when (ui) {
    Lang.HI -> when (l) { Lang.EN -> "अंग्रेज़ी"; Lang.BN -> "बंगाली"; else -> "हिन्दी" }
    Lang.BN -> when (l) { Lang.EN -> "ইংরেজি"; Lang.HI, Lang.SA -> "হিন্দি"; Lang.BN -> "বাংলা" }
    else -> when (l) { Lang.EN -> "English"; Lang.BN -> "Bengali"; else -> "Hindi" }
}

private val PeachGradient = Brush.verticalGradient(
    listOf(
        Color(0xFFE8D5CA), // top warm muted sand/rose
        Color(0xFFF6ECE5), // mid soft cream
        Color(0xFFFDF7F3)  // bottom subtle warm peach
    )
)

private val TextPrimary = Color(0xFF1F1A16)
private val TextTerracotta = Color(0xFF9E4424)
private val TextMuted = Color(0xFF7C6C63)
private val AccentTerracotta = Color(0xFFA34828)

@Composable
fun PlayerScreen(state: AppState, onClose: () -> Unit) {
    val c = LocalReaderColors.current
    val t = playerTextFor(state.uiLang)
    var showVoices by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showSleepModal by remember { mutableStateOf(false) }
    var showQueueModal by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val v = state.current
    val status = state.effectiveAudioStatus
    val active = if (status == AudioStatus.IDLE || status == AudioStatus.ENDED) -1 else state.activeSegment
    val swipePx = with(LocalDensity.current) { 80.dp.toPx() }

    Box(
        Modifier
            .fillMaxSize()
            .background(if (c.isDark) c.bg else Color.Transparent)
            .background(if (!c.isDark) PeachGradient else Brush.verticalGradient(listOf(c.surface, c.bg)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                var dx = 0f
                detectHorizontalDragGestures(onDragStart = { dx = 0f }, onDragCancel = { dx = 0f }, onDragEnd = {
                    if (dx < -swipePx) state.next() else if (dx > swipePx) state.previous()
                }) { _, d -> dx += d }
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Header
            SpotifyHeader(
                state = state,
                t = t,
                onClose = onClose,
                onOptions = { showOptionsMenu = true }
            )

            // 2. Center Album Hero Art
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SpotifyHeroArt(
                    state = state,
                    v = v,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .aspectRatio(1f)
                )
            }

            // 3. Track Title & Artist Row
            TrackMetadataRow(state, v)

            Spacer(Modifier.height(8.dp))

            // 4. Scrubber / Seekbar
            SpotifyScrubber(state, t)

            // 5. Main 5-Button Transport Bar
            SpotifyTransportBar(state, t)

            // 6. Bottom 5-Item Utility Bar (This phone, Lyrics, Equalizer, Sleep, Queue)
            SpotifyBottomUtilityBar(
                state = state,
                onLyrics = { showLyrics = !showLyrics },
                onEqualizer = { showEqualizer = true },
                onSleep = { showSleepModal = true },
                onQueue = { showQueueModal = true },
            )
        }
    }

    // Modal Bottom Sheets
    if (showLyrics) {
        LyricsBottomSheet(state = state, idx = state.index, active = active) {
            showLyrics = false
        }
    }

    if (showEqualizer) {
        SpeedEqualizerSheet(state = state) {
            showEqualizer = false
        }
    }

    if (showSleepModal) {
        SleepTimerSheet(state = state) {
            showSleepModal = false
        }
    }

    if (showQueueModal) {
        QueueSheet(state = state) {
            showQueueModal = false
        }
    }

    if (showOptionsMenu) {
        PlayerOptionsSheet(
            state = state,
            onDismiss = {
                state.dismissPlayer()
                onClose()
            },
            onVoices = {
                showOptionsMenu = false
                showVoices = true
            },
            onClose = { showOptionsMenu = false }
        )
    }

    if (showVoices) VoiceSheet(state) { showVoices = false }
}

// ------------------------------------------------------------------ Top Header

@Composable
private fun SpotifyHeader(
    state: AppState,
    t: PlayerText,
    onClose: () -> Unit,
    onOptions: () -> Unit,
) {
    val ui = state.uiLang
    val v = state.current
    val skandha = SampleData.skandhas.find { it.num == v.skandha }
    val skandhaTitle = skandha?.name(ui) ?: if (v.skandha == 0) state.strings.mahatmya else "${state.strings.skandha} ${localDigits("${v.skandha}", ui)}"

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Down chevron to minimize
        IconButton(onClick = onClose, Modifier.size(40.dp)) {
            Icon(
                painterResource(Ic.KeyboardArrowDown),
                contentDescription = t.closePlayer,
                tint = TextPrimary,
                modifier = Modifier.size(28.dp),
            )
        }

        // Playing From Title / Subtitle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                tr(ui, "PLAYING FROM", "प्रसारित हो रहा है", "সম্প্রচারিত হচ্ছে"),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.5.sp,
                maxLines = 1,
            )
            Text(
                tr(ui, "Where you left off", "जहाँ आपने छोड़ा था", "যেখান থেকে শেষ করেছিলেন"),
                fontFamily = if (state.titleLang == Lang.BN || ui == Lang.BN) NotoSerifBengali else Jakarta,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // 3-dots overflow options menu
        IconButton(onClick = onOptions, Modifier.size(40.dp)) {
            Icon(
                painterResource(Ic.MoreVertical),
                contentDescription = "Options",
                tint = TextPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// ------------------------------------------------------------------ Audio Mode Switcher

// ------------------------------------------------------------------ Hero Album Art

@Composable
private fun SpotifyHeroArt(state: AppState, v: Verse, modifier: Modifier = Modifier) {
    val artId = artRes(chArt(v.skandha, v.adhyaya)).takeIf { it != 0 } ?: artRes(skArt(v.skandha))
    Box(
        modifier
            .shadow(16.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x22000000), spotColor = Color(0x33000000))
            .clip(RoundedCornerShape(22.dp))
            .background(Brand.Card),
        contentAlignment = Alignment.Center,
    ) {
        if (artId != 0) {
            Image(
                painterResource(artId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(Brand.KesariTint, Brand.Gold.copy(alpha = 0.35f))))) {
                Mandala(Brand.Gold.copy(alpha = 0.25f), Modifier.matchParentSize())
            }
        }
    }
}

// ------------------------------------------------------------------ Track Title & Bookmark Row

@Composable
private fun TrackMetadataRow(state: AppState, v: Verse) {
    val ui = state.uiLang
    val isBookmarked = v.ref in state.bookmarks
    val langLabel = when (state.audioLang) {
        Lang.SA -> "संस्कृत"
        Lang.HI -> "हिन्दी"
        Lang.BN -> "বাংলা"
        Lang.EN -> "English"
    }

    val title = if (state.audioMode == AudioMode.YOUTUBE_STREAM && state.ytPlayer.currentTitle.isNotEmpty()) {
        state.ytPlayer.currentTitle
    } else {
        SampleData.adhyayaTitle(v.skandha, v.adhyaya, state.titleLang, state.strings)
    }

    val subtitle = if (state.audioMode == AudioMode.YOUTUBE_STREAM && state.ytPlayer.currentSubtitle.isNotEmpty()) {
        state.ytPlayer.currentSubtitle
    } else if (v.speaker != null) {
        "${v.speaker} · $langLabel"
    } else {
        "${state.strings.shloka} ${localDigits(v.ref, ui)} · $langLabel"
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            Modifier.weight(1f).padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                title,
                fontFamily = if (state.titleLang == Lang.BN || ui == Lang.BN) NotoSerifBengali else Jakarta,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                fontFamily = if (ui == Lang.BN) NotoSerifBengali else Jakarta,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextTerracotta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(
            onClick = { state.toggleBookmark(v.ref) },
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                painterResource(Ic.Heart),
                contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark",
                tint = if (isBookmarked) AccentTerracotta else TextTerracotta,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

// ------------------------------------------------------------------ Spotify Scrubber / Seekbar

@Composable
private fun SpotifyScrubber(state: AppState, t: PlayerText) {
    if (state.audioMode == AudioMode.YOUTUBE_STREAM) {
        val yt = state.ytPlayer
        var drag by remember { mutableStateOf<Float?>(null) }
        val currentProgress = drag ?: yt.progress

        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            AppSlider(
                value = currentProgress,
                onValueChange = { drag = it },
                onValueChangeFinished = {
                    drag?.let { yt.seekToFraction(it) }
                    drag = null
                },
                valueRange = 0f..1f,
                enabled = yt.durationMs > 0,
                active = AccentTerracotta,
                inactive = Color(0xFFE2D2C8),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    formatStreamTime(if (drag != null) (yt.durationMs * drag!!).toLong() else yt.positionMs),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted,
                )
                Text(
                    formatStreamTime(yt.durationMs),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted,
                )
            }
        }
    } else {
        val total = state.queue.size
        var drag by remember { mutableStateOf<Float?>(null) }
        val shown = (drag ?: state.index.toFloat()).toInt().coerceIn(0, (total - 1).coerceAtLeast(0))
        val sec = state.secondsLeft
        val minutes = (sec + 30) / 60

        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            AppSlider(
                value = drag ?: state.index.toFloat(),
                onValueChange = { drag = it },
                onValueChangeFinished = {
                    drag?.let { state.seekTo(it.toInt()) }
                    drag = null
                },
                valueRange = 0f..(total - 1).coerceAtLeast(1).toFloat(),
                enabled = total > 1,
                active = AccentTerracotta,
                inactive = Color(0xFFE2D2C8),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = t.position(shown + 1, total) },
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "0:00",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted,
                )
                Text(
                    if (minutes > 0) "%d:%02d".format(minutes, sec % 60) else "3:25",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted,
                )
            }
        }
    }
}

// ------------------------------------------------------------------ Spotify 5-Button Transport Bar

@Composable
private fun SpotifyTransportBar(state: AppState, t: PlayerText) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. Shuffle / PlayThrough
        IconButton(
            onClick = state::cyclePlayThrough,
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                painterResource(Ic.Shuffle),
                contentDescription = "Shuffle",
                tint = if (state.playThrough != PlayThrough.ADHYAYA) AccentTerracotta else TextPrimary,
                modifier = Modifier.size(22.dp),
            )
        }

        // 2. Skip Previous
        IconButton(
            onClick = state::previous,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painterResource(Ic.SkipPrevious),
                contentDescription = t.prevShloka,
                tint = TextPrimary,
                modifier = Modifier.size(28.dp),
            )
        }

        // 3. Hero Play / Pause Circle (Terracotta)
        val playLabel = if (state.isPlaying) t.pause else t.play
        Box(
            Modifier
                .size(68.dp)
                .shadow(12.dp, CircleShape, ambientColor = Color(0x33A34828), spotColor = Color(0x44A34828))
                .clip(CircleShape)
                .background(AccentTerracotta)
                .clickable(role = Role.Button, onClickLabel = playLabel) { state.togglePlay() },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(state.isPlaying, animationSpec = tween(Motion.press), label = "playIcon") { playing ->
                Icon(
                    painterResource(if (playing) Ic.Pause else Ic.PlayArrow),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }
            if (state.audioStatus == AudioStatus.PREPARING) {
                CircularProgressIndicator(
                    Modifier.matchParentSize().padding(4.dp),
                    color = Color.White.copy(alpha = 0.85f),
                    strokeWidth = 2.5.dp,
                )
            }
        }

        // 4. Skip Next
        IconButton(
            onClick = state::next,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painterResource(Ic.SkipNext),
                contentDescription = t.nextShloka,
                tint = TextPrimary,
                modifier = Modifier.size(28.dp),
            )
        }

        // 5. Repeat / Loop
        IconButton(
            onClick = state::toggleLoop,
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                painterResource(Ic.Repeat),
                contentDescription = state.strings.loop,
                tint = if (state.loop) AccentTerracotta else TextPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// ------------------------------------------------------------------ Spotify Bottom Utility Row

@Composable
private fun SpotifyBottomUtilityBar(
    state: AppState,
    onLyrics: () -> Unit,
    onEqualizer: () -> Unit,
    onSleep: () -> Unit,
    onQueue: () -> Unit,
) {
    val ui = state.uiLang

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. This phone
        UtilityItem(
            icon = Ic.Smartphone,
            label = tr(ui, "This phone", "यह फोन", "এই ফোন"),
            onClick = { /* Device audio output */ }
        )

        // 2. Lyrics
        UtilityItem(
            icon = Ic.MessageSquare,
            label = tr(ui, "Lyrics", "श्लोक", "লিরিক্স"),
            onClick = onLyrics
        )

        // 3. Equalizer
        UtilityItem(
            icon = Ic.Sliders,
            label = tr(ui, "Equalizer", "इक्वलाइज़र", "ইকুয়ালাইজার"),
            onClick = onEqualizer
        )

        // 4. Sleep
        UtilityItem(
            icon = Ic.Bedtime,
            label = tr(ui, "Sleep", "स्लीप", "ঘুম"),
            active = state.sleepMinutes > 0,
            onClick = onSleep
        )

        // 5. Queue
        UtilityItem(
            icon = Ic.ListMusic,
            label = tr(ui, "Queue", "कतार", "সারি"),
            onClick = onQueue
        )
    }
}

@Composable
private fun UtilityItem(
    icon: Int,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painterResource(icon),
            contentDescription = label,
            tint = if (active) AccentTerracotta else TextMuted,
            modifier = Modifier.size(20.dp),
        )
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (active) AccentTerracotta else TextMuted,
            maxLines = 1,
        )
    }
}

// ------------------------------------------------------------------ Modals & Sheets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsBottomSheet(state: AppState, idx: Int, active: Int, onDismiss: () -> Unit) {
    val c = LocalReaderColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (c.isDark) c.surface else Color(0xFFFDF7F3),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SpotifyLyricsCard(state, idx, active)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedEqualizerSheet(state: AppState, onDismiss: () -> Unit) {
    val c = LocalReaderColors.current
    val ui = state.uiLang
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (c.isDark) c.surface else Color(0xFFFDF7F3),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                tr(ui, "Sound & Playback Settings", "ध्वनि और प्लेबैक सेटिंग्स", "শব্দ ও প্লেব্যাক সেটিংস"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )

            // Playback Speed Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    tr(ui, "Playback Speed", "प्लेबैक गति", "প্লেব্যাক গতি"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                        val selected = state.speed == spd
                        Box(
                            Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) AccentTerracotta else c.track.copy(alpha = 0.5f))
                                .clickable { state.updateSpeed(spd) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${spd}×",
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else TextPrimary,
                            )
                        }
                    }
                }
            }

            // Audio Mode Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    tr(ui, "Audio Source", "ऑडियो स्रोत", "অডিও উৎস"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val isPaath = state.audioMode == AudioMode.KARAOKE_PAATH
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isPaath) AccentTerracotta else c.track.copy(alpha = 0.5f))
                            .clickable { state.updateAudioMode(AudioMode.KARAOKE_PAATH) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            tr(ui, "🪔 Shloka Paath", "🪔 श्लोक पाठ", "🪔 শ্লোক পাঠ"),
                            fontSize = 13.sp,
                            fontWeight = if (isPaath) FontWeight.Bold else FontWeight.Medium,
                            color = if (isPaath) Color.White else TextPrimary,
                        )
                    }
                    val isYt = state.audioMode == AudioMode.YOUTUBE_STREAM
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isYt) AccentTerracotta else c.track.copy(alpha = 0.5f))
                            .clickable { state.updateAudioMode(AudioMode.YOUTUBE_STREAM) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            tr(ui, "📺 YouTube Recitation", "📺 यूट्यूब पाठ", "📺 ইউটিউব পাঠ"),
                            fontSize = 13.sp,
                            fontWeight = if (isYt) FontWeight.Bold else FontWeight.Medium,
                            color = if (isYt) Color.White else TextPrimary,
                        )
                    }
                }
            }

            // Audio Language Selector
            LanguageBar(state)

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(state: AppState, onDismiss: () -> Unit) {
    val c = LocalReaderColors.current
    val ui = state.uiLang
    val s = state.strings
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (c.isDark) c.surface else Color(0xFFFDF7F3),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                tr(ui, "Sleep Timer", "स्लीप टाइमर", "স্লিপ টাইমার"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            listOf(
                0 to s.off,
                15 to localDigits("15 minutes", ui),
                30 to localDigits("30 minutes", ui),
                45 to localDigits("45 minutes", ui),
                60 to localDigits("60 minutes", ui),
            ).forEach { (m, label) ->
                val selected = state.sleepMinutes == m
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) AccentTerracotta.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable {
                            state.setSleep(m)
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        label,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) AccentTerracotta else TextPrimary,
                    )
                    if (selected) {
                        Icon(
                            painterResource(Ic.Check),
                            contentDescription = null,
                            tint = AccentTerracotta,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(state: AppState, onDismiss: () -> Unit) {
    val c = LocalReaderColors.current
    val ui = state.uiLang
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (c.isDark) c.surface else Color(0xFFFDF7F3),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                tr(ui, "Playback Queue", "प्लेबैक कतार", "প্লেব্যাক সারি"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                state.queue.forEachIndexed { idx, verse ->
                    val isCurrent = idx == state.index
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCurrent) AccentTerracotta.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable {
                                state.seekTo(idx)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${state.strings.shloka} ${localDigits(verse.ref, ui)}",
                                fontSize = 14.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) AccentTerracotta else TextPrimary,
                            )
                            if (verse.speaker != null) {
                                Text(
                                    verse.speaker,
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                )
                            }
                        }
                        if (isCurrent) {
                            Icon(
                                painterResource(Ic.PlayArrow),
                                contentDescription = "Playing",
                                tint = AccentTerracotta,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerOptionsSheet(
    state: AppState,
    onDismiss: () -> Unit,
    onVoices: () -> Unit,
    onClose: () -> Unit,
) {
    val c = LocalReaderColors.current
    val ui = state.uiLang
    val v = state.current
    val isBookmarked = v.ref in state.bookmarks

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = if (c.isDark) c.surface else Color(0xFFFDF7F3),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "${state.strings.shloka} ${localDigits(v.ref, ui)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )

            // Bookmark Option
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { state.toggleBookmark(v.ref) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    painterResource(Ic.Heart),
                    contentDescription = null,
                    tint = if (isBookmarked) AccentTerracotta else TextPrimary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    if (isBookmarked) tr(ui, "Remove from Favorites", "पसंदीदा से हटाएं", "পছন্দ থেকে সরান")
                    else tr(ui, "Add to Favorites", "पसंदीदा में जोड़ें", "পছন্দে যোগ করুন"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                )
            }

            // Voices option
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onVoices() }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    painterResource(Ic.Headphones),
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    tr(ui, "Voice & Narration Settings", "आवाज़ सेटिंग्स", "কণ্ঠ সেটিংস"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                )
            }

            // Stop & Dismiss player
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDismiss() }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    painterResource(Ic.Close),
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    tr(ui, "Close & Stop Audio", "ऑडियो बंद करें", "অডিও বন্ধ করুন"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE53935),
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}


// ------------------------------------------------------------------ Spotify-Style Lyrics Card

@Composable
private fun SpotifyLyricsCard(state: AppState, idx: Int, active: Int) {
    val c = LocalReaderColors.current
    val verse = state.queue.getOrElse(idx) { state.current }
    val lang = state.audioLang
    val scale = state.textScale
    val plan = state.planAt(idx)
    val ui = state.uiLang

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.surface)
            .border(1.dp, c.separator.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(c.accent)
                )
                Text(
                    tr(ui, "LYRICS & SYNCHRONIZED PAATH", "गीत और समकालिक पाठ", "শ্লোক ও সমकालिक পাঠ"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.gold,
                    letterSpacing = 1.sp,
                )
            }
            if (verse.speaker != null) {
                Text(
                    verse.speaker,
                    fontFamily = if (ui == Lang.BN) NotoSerifBengali else NotoDevanagari,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.secondary,
                )
            }
        }

        // 1. Sanskrit Shloka with live line karaoke
        if (lang == Lang.SA || state.showSanskrit) {
            ShlokaLines(state, verse, plan, if (lang == Lang.SA) active else -1, 18f * scale)
        }

        // 2. Translation layer
        if (lang != Lang.SA) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(c.separator.copy(alpha = 0.5f))
            )
            TranslationText(state, verse, lang, plan, active)
        } else {
            val tr = state.alongsideLayers().firstOrNull()
            if (tr != null && verse.hasText(tr)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(c.separator.copy(alpha = 0.5f))
                )
                Text(
                    verse.translation(tr),
                    Modifier.fillMaxWidth(),
                    fontFamily = readingFont(tr),
                    fontSize = (15 * scale).sp,
                    lineHeight = (24 * scale * state.lineScale).sp,
                    color = c.secondary,
                )
            }
        }
    }
}

@Composable
private fun ShlokaLines(state: AppState, verse: Verse, plan: VersePlan, active: Int, size: Float) {
    val c = LocalReaderColors.current
    val (lines, font) = when (state.script) {
        SanskritScript.DEVANAGARI -> verse.sa to NotoDevanagari
        SanskritScript.BENGALI -> verse.sa.map { Transliterate.toBengali(it) } to NotoSerifBengali
        SanskritScript.IAST -> verse.iast to EnglishReading
    }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        lines.forEachIndexed { i, line ->
            val seg = plan.segments.indexOfFirst { it.kind == SegKind.SHLOKA && it.start == i }
            val lit = seg >= 0 && seg == active
            val fill by animateColorAsState(
                if (lit) c.accent.copy(alpha = 0.20f) else Color.Transparent,
                tween(Motion.press),
                label = "litFill"
            )
            Text(
                keepMarkerTogether(line),
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(fill)
                    .then(if (seg >= 0) Modifier.clickable(role = Role.Button) { state.playFromSegment(seg) } else Modifier)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                fontFamily = font,
                fontSize = size.sp,
                lineHeight = (size * 1.6f).sp,
                color = if (lit) c.ink else c.shloka,
                fontWeight = if (lit) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TranslationText(state: AppState, verse: Verse, lang: Lang, plan: VersePlan, active: Int) {
    val c = LocalReaderColors.current
    val scale = state.textScale
    val big = lang == Lang.HI || lang == Lang.BN
    val text = verse.translation(lang)

    val shown = remember(text, plan, active, c.ink, c.accent) {
        buildAnnotatedString {
            append(text)
            if (active >= 0) plan.segments.forEachIndexed { i, seg ->
                if (seg.kind == SegKind.SENTENCE && seg.start >= 0 && seg.end <= text.length) {
                    addStyle(
                        if (i == active) SpanStyle(background = c.accent.copy(alpha = 0.20f), color = c.ink, fontWeight = FontWeight.SemiBold)
                        else SpanStyle(color = c.ink.copy(alpha = 0.75f)),
                        seg.start,
                        seg.end,
                    )
                }
            }
        }
    }
    var result by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        shown,
        Modifier
            .fillMaxWidth()
            .pointerInput(plan) {
                detectTapGestures { pos ->
                    val off = result?.getOffsetForPosition(pos) ?: return@detectTapGestures
                    val hit = plan.segments.indexOfFirst { it.kind == SegKind.SENTENCE && off >= it.start && off < it.end }
                    if (hit >= 0) state.playFromSegment(hit)
                }
            },
        onTextLayout = { result = it },
        fontFamily = readingFont(lang),
        fontSize = ((if (big) 18 else 17) * scale).sp,
        lineHeight = ((if (big) 30 else 26) * scale * state.lineScale).sp,
        color = c.ink,
    )
}

// ------------------------------------------------------------------ YouTube Stream Card (Ad-Free Native Stream)

private fun formatStreamTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
private fun YouTubeStreamCard(state: AppState, v: Verse) {
    val c = LocalReaderColors.current
    val context = LocalContext.current
    val ui = state.uiLang
    val yt = state.ytPlayer
    val title = SampleData.adhyayaTitle(v.skandha, v.adhyaya, state.titleLang, state.strings)
    val query = RecitationStream.getQuery(v.skandha, v.adhyaya, state.audioLang)
    val searchUrl = RecitationStream.getSearchUrl(v.skandha, v.adhyaya, state.audioLang)

    val isPreparing = yt.status == StreamStatus.PREPARING
    val isPlaying = yt.status == StreamStatus.PLAYING
    val isError = yt.status == StreamStatus.ERROR

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.surface)
            .border(1.dp, c.accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Chapter title info & Channel info
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                title,
                fontFamily = if (state.titleLang == Lang.BN || ui == Lang.BN) NotoSerifBengali else readingFont(state.titleLang),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = c.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val subText = if (yt.currentSubtitle.isNotEmpty()) yt.currentSubtitle else query
            Text(
                subText,
                fontSize = 12.sp,
                color = c.secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Native Wave / Hero Card
        Box(
            Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Brand.KesariTint.copy(alpha = 0.5f),
                            c.track.copy(alpha = 0.35f)
                        )
                    )
                )
                .border(1.dp, c.separator, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isPreparing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Brand.Kesari,
                        strokeWidth = 2.5.dp
                    )
                } else if (isError) {
                    Text(
                        yt.errorMessage ?: tr(ui, "Stream unavailable", "प्रवाह अनुपलब्ध", "স্ট্রিম পাওয়া যায়নি"),
                        fontSize = 12.sp,
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    IconButton(
                        onClick = { state.playYouTubeStreamForCurrentChapter() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(painterResource(Ic.Repeat), contentDescription = "Retry", tint = Brand.Kesari)
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { i ->
                            Box(
                                Modifier
                                    .width(4.dp)
                                    .height((16 + (i * 7) % 24).dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isPlaying) Brand.Kesari else c.secondary.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }
        }

        // Native Scrub Bar
        var scrubDrag by remember { mutableStateOf<Float?>(null) }
        val currentProgress = scrubDrag ?: yt.progress

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AppSlider(
                value = currentProgress,
                onValueChange = { scrubDrag = it },
                onValueChangeFinished = {
                    scrubDrag?.let { yt.seekToFraction(it) }
                    scrubDrag = null
                },
                valueRange = 0f..1f,
                enabled = yt.durationMs > 0,
                active = Brand.Kesari,
                inactive = c.track,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatStreamTime(if (scrubDrag != null) (yt.durationMs * scrubDrag!!).toLong() else yt.positionMs),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = c.secondary
                )
                Text(
                    formatStreamTime(yt.durationMs),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = c.secondary
                )
            }
        }

        // Stream Control Transport Row
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed button
            IconButton(
                onClick = {
                    val nextSpeed = when (yt.speed) {
                        1.0f -> 1.25f
                        1.25f -> 1.5f
                        1.5f -> 0.75f
                        else -> 1.0f
                    }
                    yt.setPlaybackSpeed(nextSpeed)
                },
                modifier = Modifier.size(42.dp)
            ) {
                Text(
                    "${yt.speed}x",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.secondary
                )
            }

            // Rewind 10s
            IconButton(
                onClick = { yt.seekTo(yt.positionMs - 10_000L) },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(painterResource(Ic.SkipPrevious), contentDescription = "-10s", tint = c.ink, modifier = Modifier.size(24.dp))
            }

            // Primary Play / Pause
            Box(
                Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Brand.Kesari)
                    .clickable { state.togglePlay() },
                contentAlignment = Alignment.Center
            ) {
                if (isPreparing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.5.dp)
                } else {
                    Icon(
                        painterResource(if (isPlaying) Ic.Pause else Ic.PlayArrow),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // Forward 10s
            IconButton(
                onClick = { yt.seekTo(yt.positionMs + 10_000L) },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(painterResource(Ic.SkipNext), contentDescription = "+10s", tint = c.ink, modifier = Modifier.size(24.dp))
            }

            // Open in External App (optional fallback)
            IconButton(
                onClick = {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.size(42.dp)
            ) {
                Icon(painterResource(Ic.ArrowRight), contentDescription = "External", tint = c.secondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ------------------------------------------------------------------ Recitation Language Bar

@Composable
private fun LanguageBar(state: AppState) {
    val c = LocalReaderColors.current
    val langs = buildList {
        add(Lang.SA)
        add(Lang.HI)
        if (BENGALI_READY) add(Lang.BN)
        add(Lang.EN)
    }
    val names = langs.map {
        when (it) {
            Lang.SA -> "संस्कृत"
            Lang.HI -> "हिन्दी"
            Lang.BN -> "বাংলা"
            Lang.EN -> "English"
        }
    }
    val fonts = langs.map {
        when (it) {
            Lang.SA -> NotoDevanagari
            Lang.HI -> TiroHindi
            Lang.BN -> NotoSerifBengali
            Lang.EN -> Jakarta
        }
    }

    Row(
        Modifier
            .padding(horizontal = 2.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.track.copy(alpha = 0.5f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        langs.forEachIndexed { i, l ->
            val on = l == state.readLang
            Box(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) c.surface else Color.Transparent)
                    .selectable(selected = on, role = Role.Tab) {
                        if (!on) {
                            state.updateReadLang(l)
                            state.restartNarration()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    names[i],
                    Modifier.opticallyCentred(fonts[i]),
                    fontFamily = fonts[i],
                    fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    color = if (on) c.ink else c.secondary,
                    maxLines = 1,
                )
            }
        }
    }
}

// ------------------------------------------------------------------ Status Area

@Composable
private fun StatusArea(state: AppState, t: PlayerText) {
    val c = LocalReaderColors.current
    val context = LocalContext.current
    val ui = state.uiLang
    val issue = state.audioIssue
    val voiceLang = if (state.audioLang == Lang.SA) Lang.HI else state.audioLang
    val text = when {
        issue == AudioIssue.NO_VOICE -> t.noVoice(languageName(ui, voiceLang))
        issue == AudioIssue.NO_ENGINE -> t.noEngine
        issue == AudioIssue.ERROR -> t.voiceStopped
        state.audioStatus == AudioStatus.PREPARING -> t.preparing
        state.audioStatus == AudioStatus.PAUSED -> t.pausedHint
        state.audioStatus == AudioStatus.ENDED -> t.ended
        state.audioStatus == AudioStatus.PLAYING -> if (state.audioLang == Lang.SA) t.sanskritViaHindi else languageName(ui, state.audioLang)
        else -> ""
    }
    if (issue != null) {
        Row(
            Modifier
                .padding(horizontal = 4.dp)
                .fillMaxWidth()
                .clip(Radius.group)
                .background(c.surface)
                .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text, Modifier.weight(1f), fontSize = 12.sp, lineHeight = 16.sp, color = c.ink)
            if (issue != AudioIssue.NO_ENGINE) Box(
                Modifier
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button) {
                        runCatching { context.startActivity(Narrator.settingsIntent()) }
                    }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(t.installVoice, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.accent)
            }
        }
    } else if (text.isNotEmpty()) {
        Text(
            text,
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            fontSize = 12.sp,
            color = c.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

// ------------------------------------------------------------------ Ended Actions

@Composable
private fun EndedActions(state: AppState, t: PlayerText) {
    val c = LocalReaderColors.current
    val v = state.queue.firstOrNull()
    val hasNext = v != null && SampleData.neighbour(v.skandha, v.adhyaya, 1) != null
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .weight(1f)
                .heightIn(min = 46.dp)
                .tappable(Radius.group, t.replay) { state.togglePlay() }
                .border(1.dp, c.separator, Radius.group)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                t.replay,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.ink,
                textAlign = TextAlign.Center,
            )
        }
        if (hasNext) {
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp)
                    .tappable(Radius.group, t.nextChapter) { state.playNextChapter() }
                    .background(c.accent)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    t.nextChapter,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.chipOnText,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
