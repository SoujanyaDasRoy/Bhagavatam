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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

@Composable
fun PlayerScreen(state: AppState, onClose: () -> Unit) {
    val c = LocalReaderColors.current
    val t = playerTextFor(state.uiLang)
    var showVoices by remember { mutableStateOf(false) }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val v = state.current
    val status = state.audioStatus
    val active = if (status == AudioStatus.IDLE || status == AudioStatus.ENDED) -1 else state.activeSegment
    val swipePx = with(LocalDensity.current) { 80.dp.toPx() }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        c.surface.copy(alpha = 0.95f),
                        c.bg,
                        c.bg,
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                var dx = 0f
                detectHorizontalDragGestures(onDragStart = { dx = 0f }, onDragCancel = { dx = 0f }, onDragEnd = {
                    if (dx < -swipePx) state.next() else if (dx > swipePx) state.previous()
                }) { _, d -> dx += d }
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            // 1. Spotify Minimal Top Header
            SpotifyHeader(
                state = state,
                t = t,
                onClose = onClose,
                onDismiss = {
                    state.dismissPlayer()
                    onClose()
                },
                onVoices = { showVoices = true }
            )

            // 2. Audio Mode Pill (Paath vs YouTube Stream)
            AudioModePill(state = state)

            if (landscape) {
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (state.audioMode == AudioMode.YOUTUBE_STREAM) {
                            YouTubeStreamCard(state = state, v = v)
                        } else {
                            SpotifyHeroArt(state, v, Modifier.size(240.dp))
                        }
                    }
                    Column(
                        Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        TrackMetadataRow(state, v)
                        if (state.audioMode == AudioMode.KARAOKE_PAATH) {
                            SpotifyLyricsCard(state, state.index, active)
                        }
                        StatusArea(state, t)
                        SpotifyScrubber(state, t)
                        SpotifyTransportBar(state, t)
                        SpotifyUtilityRow(state, onVoices = { showVoices = true })
                        LanguageBar(state)
                    }
                }
            } else {
                // Portrait: Spotify Vertical Flow
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (state.audioMode == AudioMode.YOUTUBE_STREAM) {
                        YouTubeStreamCard(state = state, v = v)
                    } else {
                        // Square Hero Album Art matching Spotify
                        SpotifyHeroArt(state, v, Modifier.fillMaxWidth(0.85f).aspectRatio(1f))
                    }

                    // Track Title & Favorite / Bookmark Row
                    TrackMetadataRow(state, v)

                    // Spotify Scrubber Seekbar
                    SpotifyScrubber(state, t)

                    // Spotify 5-Control Main Transport
                    SpotifyTransportBar(state, t)

                    // Secondary Utilities (Speed, Sleep Timer, Voices)
                    SpotifyUtilityRow(state, onVoices = { showVoices = true })

                    // Language Quick Switcher
                    LanguageBar(state)

                    // Status Messages (e.g. Preparing / TTS info)
                    StatusArea(state, t)

                    // Spotify Live Synchronized Lyrics Card
                    if (state.audioMode == AudioMode.KARAOKE_PAATH) {
                        SpotifyLyricsCard(state, state.index, active)
                    }

                    if (status == AudioStatus.ENDED && state.audioMode == AudioMode.KARAOKE_PAATH) {
                        EndedActions(state, t)
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    if (showVoices) VoiceSheet(state) { showVoices = false }
}

// ------------------------------------------------------------------ Top Header (Spotify Style)

@Composable
private fun SpotifyHeader(
    state: AppState,
    t: PlayerText,
    onClose: () -> Unit,
    onDismiss: () -> Unit,
    onVoices: () -> Unit,
) {
    val c = LocalReaderColors.current
    val s = state.strings
    val ui = state.uiLang
    val v = state.current
    val skandha = SampleData.skandhas.find { it.num == v.skandha }
    val skandhaTitle = skandha?.name(ui) ?: if (v.skandha == 0) s.mahatmya else "${s.skandha} ${localDigits("${v.skandha}", ui)}"
    val chapterLabel = "${s.adhyaya} ${localDigits("${v.adhyaya}", ui)}"
    val chapterTitle = SampleData.adhyayaTitle(v.skandha, v.adhyaya, state.titleLang, s)

    Column(
        Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                var dy = 0f
                detectVerticalDragGestures(onDragStart = { dy = 0f }, onDragEnd = { if (dy > 80f) onClose() }) { _, d -> dy += d }
            }
    ) {
        // Subtle Grab Handle
        Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(c.secondary.copy(alpha = 0.35f))
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Down chevron to minimize
            IconButton(onClick = onClose, Modifier.size(44.dp)) {
                Icon(
                    painterResource(Ic.KeyboardArrowDown),
                    contentDescription = t.closePlayer,
                    tint = c.ink,
                    modifier = Modifier.size(28.dp),
                )
            }

            // Playing From Title / Subtitle
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    tr(ui, "PLAYING FROM $skandhaTitle", "$skandhaTitle से प्रसारित", "$skandhaTitle থেকে সম্প্রচার").uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.secondary,
                    letterSpacing = 1.2.sp,
                    maxLines = 1,
                )
                Text(
                    "$chapterLabel · $chapterTitle",
                    fontFamily = if (state.titleLang == Lang.BN || ui == Lang.BN) NotoSerifBengali else com.bhagavatam.app.ui.screens.readingFont(state.titleLang),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Voice settings & Dismiss buttons
            IconButton(onClick = onVoices, Modifier.size(40.dp)) {
                Icon(
                    painterResource(Ic.Headphones),
                    contentDescription = t.voiceSettings,
                    tint = c.accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onDismiss, Modifier.size(40.dp)) {
                Icon(
                    painterResource(Ic.Close),
                    contentDescription = tr(ui, "Stop & Dismiss", "बंद करें", "বন্ধ করুন"),
                    tint = c.secondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ------------------------------------------------------------------ Audio Mode Switcher

@Composable
private fun AudioModePill(state: AppState) {
    val c = LocalReaderColors.current
    val ui = state.uiLang

    val paathLabel = tr(ui, "🪔 Line-by-Line Paath", "🪔 श्लोक पाठ (काराओके)", "🪔 শ্লোক পাঠ (কারাওকে)")
    val ytLabel = tr(ui, "📺 YouTube Recitation", "📺 यूट्यूब संपूर्ण पाठ", "📺 ইউটিউব পাঠ")

    Row(
        Modifier
            .padding(horizontal = 24.dp, vertical = 2.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.track.copy(alpha = 0.5f))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val paathSelected = state.audioMode == AudioMode.KARAOKE_PAATH
        Box(
            Modifier
                .weight(1f)
                .height(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (paathSelected) c.surface else Color.Transparent)
                .clickable { state.updateAudioMode(AudioMode.KARAOKE_PAATH) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                paathLabel,
                fontSize = 12.sp,
                fontWeight = if (paathSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (paathSelected) c.accent else c.secondary,
                maxLines = 1,
            )
        }

        val ytSelected = state.audioMode == AudioMode.YOUTUBE_STREAM
        Box(
            Modifier
                .weight(1f)
                .height(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (ytSelected) c.surface else Color.Transparent)
                .clickable { state.updateAudioMode(AudioMode.YOUTUBE_STREAM) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                ytLabel,
                fontSize = 12.sp,
                fontWeight = if (ytSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (ytSelected) Color(0xFFE53935) else c.secondary,
                maxLines = 1,
            )
        }
    }
}

// ------------------------------------------------------------------ Hero Album Art

@Composable
private fun SpotifyHeroArt(state: AppState, v: Verse, modifier: Modifier = Modifier) {
    val artId = artRes(chArt(v.skandha, v.adhyaya)).takeIf { it != 0 } ?: artRes(skArt(v.skandha))
    Box(
        modifier
            .shadow(28.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x33000000), spotColor = Color(0x44000000))
            .clip(RoundedCornerShape(18.dp))
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
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))))
            )
        } else {
            Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(Brand.KesariTint, Brand.Gold.copy(alpha = 0.35f))))) {
                Mandala(Brand.Gold.copy(alpha = 0.25f), Modifier.matchParentSize())
            }
        }

        // Verse reference badge on bottom-left of album art
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                localDigits(v.ref, state.uiLang),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

// ------------------------------------------------------------------ Track Title & Bookmark Row

@Composable
private fun TrackMetadataRow(state: AppState, v: Verse) {
    val c = LocalReaderColors.current
    val ui = state.uiLang
    val isBookmarked = v.ref in state.bookmarks
    val langLabel = when (state.audioLang) {
        Lang.SA -> "সংस्कृत"
        Lang.HI -> "हिन्दी"
        Lang.BN -> "বাংলা"
        Lang.EN -> "English"
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "${state.strings.shloka} ${localDigits(v.ref, ui)}",
                fontFamily = if (ui == Lang.BN) NotoSerifBengali else Jakarta,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = c.ink,
                maxLines = 1,
            )
            val sub = if (v.speaker != null) "${v.speaker} · $langLabel" else "$langLabel Narration"
            Text(
                sub,
                fontFamily = if (ui == Lang.BN) NotoSerifBengali else Jakarta,
                fontSize = 14.sp,
                color = c.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(
            onClick = { state.toggleBookmark(v.ref) },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painterResource(if (isBookmarked) Ic.Heart else Ic.Heart),
                contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark",
                tint = if (isBookmarked) c.accent else c.secondary.copy(alpha = 0.45f),
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

// ------------------------------------------------------------------ Spotify Scrubber / Seekbar

@Composable
private fun SpotifyScrubber(state: AppState, t: PlayerText) {
    val c = LocalReaderColors.current
    val ui = state.uiLang
    val total = state.queue.size
    var drag by remember { mutableStateOf<Float?>(null) }
    val shown = (drag ?: state.index.toFloat()).toInt().coerceIn(0, (total - 1).coerceAtLeast(0))
    val sec = state.secondsLeft
    val minutes = (sec + 30) / 60
    val left = when {
        state.audioStatus == AudioStatus.ENDED -> ""
        sec < 60 -> t.lessThanMinute
        minutes >= 60 -> t.hoursLeft(minutes / 60, minutes % 60)
        else -> t.minutesLeft(minutes)
    }

    Column(Modifier.fillMaxWidth()) {
        AppSlider(
            value = drag ?: state.index.toFloat(),
            onValueChange = { drag = it },
            onValueChangeFinished = {
                drag?.let { state.seekTo(it.toInt()) }
                drag = null
            },
            valueRange = 0f..(total - 1).coerceAtLeast(1).toFloat(),
            enabled = total > 1,
            active = c.accent,
            inactive = c.track,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = t.position(shown + 1, total) },
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                localDigits("${shown + 1} / $total", ui),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = c.secondary,
            )
            Text(
                localDigits(left, ui),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = c.secondary,
            )
        }
    }
}

// ------------------------------------------------------------------ Spotify 5-Button Transport Bar

@Composable
private fun SpotifyTransportBar(state: AppState, t: PlayerText) {
    val c = LocalReaderColors.current
    val ui = state.uiLang

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. PlayThrough / Shuffle Mode Toggle
        IconButton(
            onClick = state::cyclePlayThrough,
            modifier = Modifier.size(46.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painterResource(Ic.Sparkles),
                    contentDescription = "Playthrough mode",
                    tint = c.accent,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    when (state.playThrough) {
                        PlayThrough.ADHYAYA -> tr(ui, "CH", "अ.", "অ.")
                        PlayThrough.SKANDHA -> tr(ui, "CANTO", "स्कं.", "স্ক.")
                        PlayThrough.GRANTH -> tr(ui, "ALL", "सर्व", "সব")
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.accent,
                )
            }
        }

        // 2. Skip Previous Shloka
        IconButton(
            onClick = state::previous,
            modifier = Modifier.size(52.dp),
        ) {
            Icon(
                painterResource(Ic.SkipPrevious),
                contentDescription = t.prevShloka,
                tint = c.ink,
                modifier = Modifier.size(32.dp),
            )
        }

        // 3. Hero Spotify Center Play / Pause
        val playLabel = if (state.isPlaying) t.pause else t.play
        Box(
            Modifier
                .size(68.dp)
                .shadow(16.dp, CircleShape, ambientColor = Color(0x331C1A17), spotColor = Color(0x331C1A17))
                .clip(CircleShape)
                .background(c.accent)
                .clickable(role = Role.Button, onClickLabel = playLabel) { state.togglePlay() },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(state.isPlaying, animationSpec = tween(Motion.press), label = "playIcon") { playing ->
                Icon(
                    painterResource(if (playing) Ic.Pause else Ic.PlayArrow),
                    contentDescription = null,
                    tint = c.chipOnText,
                    modifier = Modifier.size(36.dp),
                )
            }
            if (state.audioStatus == AudioStatus.PREPARING) {
                CircularProgressIndicator(
                    Modifier.matchParentSize().padding(4.dp),
                    color = c.chipOnText.copy(alpha = 0.85f),
                    strokeWidth = 2.5.dp,
                )
            }
        }

        // 4. Skip Next Shloka
        IconButton(
            onClick = state::next,
            modifier = Modifier.size(52.dp),
        ) {
            Icon(
                painterResource(Ic.SkipNext),
                contentDescription = t.nextShloka,
                tint = c.ink,
                modifier = Modifier.size(32.dp),
            )
        }

        // 5. Loop / Repeat Toggle
        IconButton(
            onClick = state::toggleLoop,
            modifier = Modifier.size(46.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painterResource(Ic.Repeat),
                    contentDescription = state.strings.loop,
                    tint = if (state.loop) c.accent else c.secondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp),
                )
                if (state.loop) {
                    Box(
                        Modifier
                            .padding(top = 2.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(c.accent)
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Spotify Bottom Utility Row

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpotifyUtilityRow(state: AppState, onVoices: () -> Unit) {
    val c = LocalReaderColors.current
    val s = state.strings
    val ui = state.uiLang
    var showSleep by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Speed Pill
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(c.track.copy(alpha = 0.45f))
                .clickable { state.cycleSpeed() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${state.speed}×",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = c.ink,
            )
        }

        // Sleep Timer Pill
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (state.sleepMinutes > 0) c.accent.copy(alpha = 0.18f) else c.track.copy(alpha = 0.45f))
                .clickable { showSleep = !showSleep }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    painterResource(Ic.Bedtime),
                    contentDescription = null,
                    tint = if (state.sleepMinutes > 0) c.accent else c.secondary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    if (state.sleepMinutes > 0) clock(state.sleepLeftSec) else s.sleep,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (state.sleepMinutes > 0) c.accent else c.ink,
                )
            }
        }

        // Voices Picker Pill
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(c.track.copy(alpha = 0.45f))
                .clickable { onVoices() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    painterResource(Ic.Headphones),
                    contentDescription = null,
                    tint = c.accent,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    tr(ui, "Voices", "आवाज़ें", "কণ্ঠ"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.ink,
                )
            }
        }
    }

    if (showSleep) {
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(0, 15, 30, 45, 60).forEach { m ->
                val on = state.sleepMinutes == m
                Box(
                    Modifier
                        .height(34.dp)
                        .clip(CircleShape)
                        .background(if (on) c.chipOn else c.track)
                        .selectable(selected = on, role = Role.RadioButton) {
                            state.setSleep(m)
                            showSleep = false
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (m == 0) s.off else localDigits("$m min", ui),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (on) c.chipOnText else c.ink,
                    )
                }
            }
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

// ------------------------------------------------------------------ YouTube Stream Card

@Composable
private fun YouTubeStreamCard(state: AppState, v: Verse) {
    val c = LocalReaderColors.current
    val context = LocalContext.current
    val ui = state.uiLang
    val title = SampleData.adhyayaTitle(v.skandha, v.adhyaya, state.titleLang, state.strings)
    val query = RecitationStream.getQuery(v.skandha, v.adhyaya, state.audioLang)
    val embedUrl = RecitationStream.getEmbedUrl(v.skandha, v.adhyaya, state.audioLang)
    val searchUrl = RecitationStream.getSearchUrl(v.skandha, v.adhyaya, state.audioLang)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.surface)
            .border(1.dp, c.accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                )
                Text(
                    tr(ui, "AUTHENTIC YOUTUBE RECITATION STREAM", "यूट्यूब सम्पूर्ण पाठ प्रवाह", "ইউটিউব সম্পূর্ণ পাঠ প্রবাহ"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.gold,
                    letterSpacing = 0.8.sp,
                )
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brand.KesariTint)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    tr(ui, "Zero Download", "बिना डाउनलोड", "ডাউনলোড ছাড়া"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Brand.Kesari,
                )
            }
        }

        // Chapter title info
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                fontFamily = if (state.titleLang == Lang.BN || ui == Lang.BN) NotoSerifBengali else readingFont(state.titleLang),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = c.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                query,
                fontSize = 12.sp,
                color = c.secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Embedded YouTube In-App Web Player
        Box(
            Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black)
                .border(1.dp, c.separator, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.domStorageEnabled = true
                        webViewClient = WebViewClient()
                        loadUrl(embedUrl)
                    }
                },
                update = { webView ->
                    webView.loadUrl(embedUrl)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Direct YouTube App / Background Stream button
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE53935))
                .clickable {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
                        context.startActivity(intent)
                    }
                }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                painterResource(Ic.PlayArrow),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                tr(ui, "Open in YouTube / Background", "यूट्यूब में सुनें / बैकग्राउंड", "ইউটিউবে চালান / ব্যাকগ্রাউন্ড"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
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
