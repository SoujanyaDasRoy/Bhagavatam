package com.bhagavatam.app.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
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
import com.bhagavatam.app.audio.Narrator
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

    Column(
        Modifier.fillMaxSize().background(c.bg)
            .statusBarsPadding().navigationBarsPadding()
            .pointerInput(Unit) {
                var dx = 0f
                detectHorizontalDragGestures(onDragStart = { dx = 0f }, onDragCancel = { dx = 0f }, onDragEnd = {
                    if (dx < -swipePx) state.next() else if (dx > swipePx) state.previous()
                }) { _, d -> dx += d }
            },
    ) {
        // 1. Spotify-style Top Bar
        PlayerHeader(state, t, onClose) { showVoices = true }

        if (landscape) {
            Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    HeroCoverArt(v, Modifier.size(200.dp))
                }
                Column(Modifier.weight(1.2f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    TrackInfoRow(state, v)
                    LyricsCard(state, state.index, active)
                    StatusArea(state, t)
                    ProgressBar(state, t)
                    SpotifyTransport(state, t)
                    SecondaryControls(state)
                    LanguageBar(state)
                }
            }
        } else {
            // Portrait: artwork and the shloka scroll; the controls stay pinned at the bottom, like a music player.
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                HeroCoverArt(v, Modifier.fillMaxWidth(0.46f).aspectRatio(1f))
                TrackInfoRow(state, v)
                LyricsCard(state, state.index, active)
                if (status == AudioStatus.ENDED) EndedActions(state, t)
                Spacer(Modifier.height(8.dp))
            }
            Column(
                Modifier.fillMaxWidth().background(c.bg).padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatusArea(state, t)
                ProgressBar(state, t)
                SpotifyTransport(state, t)
                SecondaryControls(state)
                LanguageBar(state)
            }
        }
    }
    if (showVoices) VoiceSheet(state) { showVoices = false }
}

// ------------------------------------------------------------------ Top Header

@Composable
private fun PlayerHeader(state: AppState, t: PlayerText, onClose: () -> Unit, onVoices: () -> Unit) {
    val c = LocalReaderColors.current
    val s = state.strings
    val ui = state.uiLang
    val v = state.current
    val reference = localDigits(if (v.skandha == 0) "${s.mahatmya} · ${s.adhyaya} ${v.adhyaya}" else "${s.skandha} ${v.skandha} · ${s.adhyaya} ${v.adhyaya}", ui)
    val title = SampleData.adhyayaTitle(v.skandha, v.adhyaya, state.titleLang, s)

    Column(
        Modifier.fillMaxWidth().pointerInput(Unit) {
            var dy = 0f
            detectVerticalDragGestures(onDragStart = { dy = 0f }, onDragEnd = { if (dy > 80f) onClose() }) { _, d -> dy += d }
        },
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.width(36.dp).height(4.dp).clip(CircleShape).background(c.separator.copy(alpha = 0.8f)))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose, Modifier.size(48.dp)) {
                Icon(painterResource(Ic.KeyboardArrowDown), t.closePlayer, tint = c.ink, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    state.strings.nowPlaying.uppercase(),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.secondary, letterSpacing = 1.2.sp, maxLines = 1,
                )
                Text(
                    "$title · $reference",
                    fontFamily = readingFont(state.titleLang), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onVoices, Modifier.size(48.dp)) {
                Icon(painterResource(Ic.Headphones), t.voiceSettings, tint = c.accent, modifier = Modifier.size(22.dp))
            }
        }
    }
}

// ------------------------------------------------------------------ Hero Album Art

@Composable
private fun HeroCoverArt(v: Verse, modifier: Modifier = Modifier) {
    val artId = artRes(chArt(v.skandha, v.adhyaya)).takeIf { it != 0 } ?: artRes(skArt(v.skandha))
    Box(
        modifier
            .shadow(24.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x331C1A17), spotColor = Color(0x331C1A17))
            .clip(RoundedCornerShape(20.dp)).background(Brand.Card),
        contentAlignment = Alignment.Center,
    ) {
        if (artId != 0) {
            Image(
                painterResource(artId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.40f)))))
        } else {
            Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(Brand.KesariTint, Brand.Gold.copy(alpha = 0.35f))))) {
                Mandala(Brand.Gold.copy(alpha = 0.25f), Modifier.matchParentSize())
            }
        }
        // Verse reference badge on bottom-left of album art
        Box(
            Modifier.align(Alignment.BottomStart).padding(12.dp)
                .clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                v.ref,
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.5.sp,
            )
        }
    }
}

// ------------------------------------------------------------------ Track Title & Bookmark Row

@Composable
private fun TrackInfoRow(state: AppState, v: Verse) {
    val c = LocalReaderColors.current
    val ui = state.uiLang
    val isBookmarked = v.ref in state.bookmarks
    val langLabel = when (state.audioLang) { Lang.SA -> "संस्कृत"; Lang.HI -> "हिन्दी"; Lang.BN -> "বাংলা"; Lang.EN -> "English" }

    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "${state.strings.shloka} ${localDigits(v.ref, ui)}",
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = c.ink, maxLines = 1,
            )
            val sub = if (v.speaker != null) "${v.speaker} · $langLabel" else langLabel
            Text(
                sub,
                fontSize = 14.sp, color = c.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(
            onClick = { state.toggleBookmark(v.ref) },
            modifier = Modifier.size(46.dp),
        ) {
            Icon(
                painterResource(Ic.Bookmark),
                contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark",
                tint = if (isBookmarked) c.gold else c.secondary.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

// ------------------------------------------------------------------ Spotify-Style Lyrics / Shloka Card

@Composable
private fun LyricsCard(state: AppState, idx: Int, active: Int) {
    val c = LocalReaderColors.current
    val verse = state.queue.getOrElse(idx) { state.current }
    val lang = state.audioLang
    val scale = state.textScale
    val plan = state.planAt(idx)

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(c.surface).border(1.dp, c.separator.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                tr(state.uiLang, "SHLOKA & RECITATION", "श्लोक और पाठ", "শ্লোক ও পাঠ"),
                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.gold, letterSpacing = 1.sp,
            )
            if (verse.speaker != null) {
                Text(verse.speaker, fontFamily = NotoDevanagari, fontSize = 12.sp, color = c.secondary)
            }
        }

        // 1. Sanskrit Shloka with live line karaoke
        if (lang == Lang.SA || state.showSanskrit) {
            ShlokaLines(state, verse, plan, if (lang == Lang.SA) active else -1, 18f * scale)
        }

        // 2. Translation layer
        if (lang != Lang.SA) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.separator.copy(alpha = 0.5f)))
            TranslationText(state, verse, lang, plan, active)
        } else {
            val tr = state.alongsideLayers().firstOrNull()
            if (tr != null && verse.hasText(tr)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.separator.copy(alpha = 0.5f)))
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
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEachIndexed { i, line ->
            val seg = plan.segments.indexOfFirst { it.kind == SegKind.SHLOKA && it.start == i }
            val lit = seg >= 0 && seg == active
            val fill by animateColorAsState(if (lit) c.accent.copy(alpha = 0.18f) else Color.Transparent, tween(Motion.press), label = "litFill")
            Text(
                keepMarkerTogether(line),
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(fill)
                    .then(if (seg >= 0) Modifier.clickable(role = Role.Button) { state.playFromSegment(seg) } else Modifier)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                fontFamily = font, fontSize = size.sp, lineHeight = (size * 1.6f).sp,
                color = if (lit) c.ink else c.shloka, fontWeight = if (lit) FontWeight.SemiBold else FontWeight.Normal,
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
                        if (i == active) SpanStyle(background = c.accent.copy(alpha = 0.18f), color = c.ink, fontWeight = FontWeight.SemiBold)
                        else SpanStyle(color = c.ink.copy(alpha = 0.75f)),
                        seg.start, seg.end,
                    )
                }
            }
        }
    }
    var result by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        shown,
        Modifier.fillMaxWidth().pointerInput(plan) {
            detectTapGestures { pos ->
                val off = result?.getOffsetForPosition(pos) ?: return@detectTapGestures
                val hit = plan.segments.indexOfFirst { it.kind == SegKind.SENTENCE && off >= it.start && off < it.end }
                if (hit >= 0) state.playFromSegment(hit)
            }
        },
        onTextLayout = { result = it },
        fontFamily = readingFont(lang), fontSize = ((if (big) 18 else 17) * scale).sp,
        lineHeight = ((if (big) 30 else 26) * scale * state.lineScale).sp, color = c.ink,
    )
}

// ------------------------------------------------------------------ Scrubber / Seekbar

@Composable
private fun ProgressBar(state: AppState, t: PlayerText) {
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

    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        AppSlider(
            value = drag ?: state.index.toFloat(),
            onValueChange = { drag = it },
            onValueChangeFinished = { drag?.let { state.seekTo(it.toInt()) }; drag = null },
            valueRange = 0f..(total - 1).coerceAtLeast(1).toFloat(),
            enabled = total > 1, active = c.accent, inactive = c.track,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = t.position(shown + 1, total) },
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(localDigits("${shown + 1} / $total", ui), fontSize = 12.sp, color = c.secondary)
            Text(localDigits(left, ui), fontSize = 12.sp, color = c.secondary)
        }
    }
}

// ------------------------------------------------------------------ Spotify Transport Bar

@Composable
private fun SpotifyTransport(state: AppState, t: PlayerText) {
    val c = LocalReaderColors.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Repeat / Loop toggle
        IconButton(onClick = state::toggleLoop, Modifier.size(46.dp)) {
            Icon(
                painterResource(Ic.Repeat),
                contentDescription = state.strings.loop,
                tint = if (state.loop) c.accent else c.secondary.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp),
            )
        }

        // Skip Previous
        IconButton(onClick = state::previous, Modifier.size(52.dp)) {
            Icon(
                painterResource(Ic.SkipPrevious),
                contentDescription = t.prevShloka,
                tint = c.ink,
                modifier = Modifier.size(32.dp),
            )
        }

        // Hero Play / Pause Button
        val playLabel = if (state.isPlaying) t.pause else t.play
        Box(
            Modifier.size(68.dp).shadow(12.dp, CircleShape, ambientColor = Color(0x331C1A17), spotColor = Color(0x331C1A17))
                .clip(CircleShape).background(c.accent)
                .clickable(role = Role.Button, onClickLabel = playLabel) { state.togglePlay() },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(state.isPlaying, animationSpec = tween(Motion.press), label = "playIcon") { playing ->
                Icon(
                    painterResource(if (playing) Ic.Pause else Ic.PlayArrow),
                    null, tint = c.chipOnText, modifier = Modifier.size(36.dp),
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

        // Skip Next
        IconButton(onClick = state::next, Modifier.size(52.dp)) {
            Icon(
                painterResource(Ic.SkipNext),
                contentDescription = t.nextShloka,
                tint = c.ink,
                modifier = Modifier.size(32.dp),
            )
        }

        // Speed Toggle
        Box(
            Modifier.size(46.dp).clip(CircleShape).clickable(role = Role.Button) { state.cycleSpeed() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${state.speed}×",
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.ink,
            )
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
            Modifier.padding(horizontal = 8.dp, vertical = 2.dp).fillMaxWidth().clip(Radius.group).background(c.surface)
                .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp).semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text, Modifier.weight(1f), fontSize = 12.sp, lineHeight = 16.sp, color = c.ink)
            if (issue != AudioIssue.NO_ENGINE) Box(
                Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) {
                    runCatching { context.startActivity(Narrator.settingsIntent()) }
                }.padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) { Text(t.installVoice, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.accent) }
        }
    } else if (text.isNotEmpty()) {
        Text(
            text, Modifier.fillMaxWidth().padding(horizontal = 16.dp).semantics { liveRegion = LiveRegionMode.Polite },
            fontSize = 12.sp, color = c.secondary, textAlign = TextAlign.Center,
        )
    }
}

// ------------------------------------------------------------------ Secondary Controls

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SecondaryControls(state: AppState) {
    val s = state.strings
    val ui = state.uiLang
    var showSleep by remember { mutableStateOf(false) }
    val c = LocalReaderColors.current

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Sleep timer chip
        Chip(
            if (state.sleepMinutes > 0) clock(state.sleepLeftSec) else null,
            s.sleep, on = state.sleepMinutes > 0, icon = painterResource(Ic.Bedtime),
        ) { showSleep = !showSleep }

        // Playthrough mode badge (Adhyaya / Skandha / Granth)
        Chip(
            null,
            when (state.playThrough) {
                com.bhagavatam.app.state.PlayThrough.ADHYAYA -> s.throughAdhyaya
                com.bhagavatam.app.state.PlayThrough.SKANDHA -> s.throughSkandha
                com.bhagavatam.app.state.PlayThrough.GRANTH -> s.throughGranth
            },
            on = false,
        ) { state.cyclePlayThrough() }
    }

    if (showSleep) {
        FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp).animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(0, 15, 30, 45, 60).forEach { m ->
                val on = state.sleepMinutes == m
                Box(
                    Modifier.heightIn(min = 40.dp).clip(CircleShape).background(if (on) c.chipOn else c.track)
                        .selectable(selected = on, role = Role.RadioButton) { state.setSleep(m); showSleep = false }.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (m == 0) s.off else localDigits("$m min", ui),
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (on) c.chipOnText else c.ink,
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Recitation Language Bar

@Composable
private fun LanguageBar(state: AppState) {
    val c = LocalReaderColors.current
    val langs = buildList { add(Lang.SA); add(Lang.HI); if (BENGALI_READY) add(Lang.BN); add(Lang.EN) }
    val names = langs.map { when (it) { Lang.SA -> "संस्कृत"; Lang.HI -> "हिन्दी"; Lang.BN -> "বাংলা"; Lang.EN -> "English" } }
    val fonts = langs.map { when (it) { Lang.SA -> NotoDevanagari; Lang.HI -> TiroHindi; Lang.BN -> NotoSerifBengali; Lang.EN -> Jakarta } }

    Row(
        Modifier.padding(horizontal = 4.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.track.copy(alpha = 0.6f)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        langs.forEachIndexed { i, l ->
            val on = l == state.readLang
            Box(
                Modifier.weight(1f).heightIn(min = 42.dp).clip(RoundedCornerShape(9.dp)).background(if (on) c.surface else Color.Transparent)
                    .selectable(selected = on, role = Role.Tab) { if (!on) { state.updateReadLang(l); state.restartNarration() } },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    names[i], Modifier.opticallyCentred(fonts[i]), fontFamily = fonts[i],
                    fontSize = 14.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, color = if (on) c.ink else c.secondary, maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun Chip(top: String?, label: String, on: Boolean, icon: Painter? = null, toggle: Boolean = false, onClick: () -> Unit) {
    val c = LocalReaderColors.current
    Row(
        Modifier.heightIn(min = 40.dp).clip(CircleShape).border(1.dp, if (on) c.accent else c.separator, CircleShape)
            .then(if (toggle) Modifier.toggleable(value = on, role = Role.Switch) { onClick() } else Modifier.clickable(role = Role.Button, onClick = onClick))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = if (on) c.accent else c.ink, modifier = Modifier.size(16.dp))
        if (top != null) Text(top, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (on) c.accent else c.ink)
        else Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (on) c.accent else c.ink)
    }
}

@Composable
private fun EndedActions(state: AppState, t: PlayerText) {
    val c = LocalReaderColors.current
    val v = state.queue.firstOrNull()
    val hasNext = v != null && SampleData.neighbour(v.skandha, v.adhyaya, 1) != null
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.weight(1f).heightIn(min = 48.dp).tappable(Radius.group, t.replay) { state.togglePlay() }.border(1.dp, c.separator, Radius.group).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) { Text(t.replay, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink, textAlign = TextAlign.Center) }
        if (hasNext) Box(
            Modifier.weight(1f).heightIn(min = 48.dp).tappable(Radius.group, t.nextChapter) { state.playNextChapter() }.background(c.accent).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) { Text(t.nextChapter, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.chipOnText, textAlign = TextAlign.Center) }
    }
}
