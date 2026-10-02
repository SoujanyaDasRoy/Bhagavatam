package com.bhagavatam.app.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
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
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.state.AudioIssue
import com.bhagavatam.app.state.AudioStatus
import com.bhagavatam.app.ui.components.AppSlider
import com.bhagavatam.app.ui.components.Ic
import com.bhagavatam.app.ui.components.keepMarkerTogether
import com.bhagavatam.app.ui.components.opticallyCentred
import com.bhagavatam.app.ui.components.tappable
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

/** The language's name in the app language, for sentences like "No Hindi voice is installed". */
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

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding().navigationBarsPadding()) {
        PlayerHeader(state, t, onClose) { showVoices = true }
        if (landscape) {
            // Wide and short: the text on the left, everything you press on the right.
            Row(Modifier.weight(1f).fillMaxWidth()) {
                ReadingArea(state, t, Modifier.weight(1.15f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) { Controls(state, t) }
            }
        } else {
            ReadingArea(state, t, Modifier.weight(1f).fillMaxWidth())
            Controls(state, t)
        }
    }
    if (showVoices) VoiceSheet(state) { showVoices = false }
}

// ------------------------------------------------------------------ header

@Composable
private fun PlayerHeader(state: AppState, t: PlayerText, onClose: () -> Unit, onVoices: () -> Unit) {
    val c = LocalReaderColors.current
    val s = state.strings
    val ui = state.uiLang
    val v = state.current
    val reference = localDigits(if (v.skandha == 0) "${s.mahatmya} · ${s.adhyaya} ${v.adhyaya}" else "${s.skandha} ${v.skandha} · ${s.adhyaya} ${v.adhyaya}", ui)
    val title = SampleData.adhyayaTitle(v.skandha, v.adhyaya, state.titleLang, s)
    // Swipe down anywhere on the header to close, as on a music player.
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
            IconButton(onClick = onClose, Modifier.size(48.dp)) { Icon(painterResource(Ic.KeyboardArrowDown), t.closePlayer, tint = c.ink) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(reference, fontSize = 12.sp, color = c.secondary, maxLines = 1)
                Text(title, fontFamily = readingFont(state.titleLang), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onVoices, Modifier.size(48.dp)) { Icon(painterResource(Ic.Settings), t.voiceSettings, tint = c.ink, modifier = Modifier.size(22.dp)) }
        }
    }
}

// ------------------------------------------------------------ reading area

/**
 * The text being read. While the voice speaks, the sentence (or line of the shloka) being said is lit and the rest
 * is slightly quieter, so the eye can follow without hunting. Tapping a sentence plays from there. The text scrolls
 * itself to keep the lit sentence in view, but never while the reader is scrolling by hand.
 */
@Composable
private fun ReadingArea(state: AppState, t: PlayerText, modifier: Modifier) {
    val scroll = rememberScrollState()
    var viewport by remember { mutableIntStateOf(0) }
    var boxTop by remember { mutableFloatStateOf(0f) }
    var textTop by remember { mutableFloatStateOf(0f) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val swipePx = with(LocalDensity.current) { 80.dp.toPx() }
    val status = state.audioStatus
    val active = if (status == AudioStatus.IDLE || status == AudioStatus.ENDED) -1 else state.activeSegment
    val plan = state.planAt(state.index)

    LaunchedEffect(state.index) { scroll.scrollTo(0) }
    LaunchedEffect(active, status, layout) {
        val l = layout ?: return@LaunchedEffect
        val seg = plan.segments.getOrNull(active) ?: return@LaunchedEffect
        if (!state.isPlaying || seg.kind != SegKind.SENTENCE || scroll.isScrollInProgress) return@LaunchedEffect
        val at = seg.start.coerceIn(0, (l.layoutInput.text.length - 1).coerceAtLeast(0))
        val y = textTop + l.getLineTop(l.getLineForOffset(at))
        scroll.animateScrollTo((y - viewport * 0.3f).toInt().coerceIn(0, scroll.maxValue), tween(Motion.screen))
    }

    Column(
        modifier.onSizeChanged { viewport = it.height }.onGloballyPositioned { boxTop = it.positionInRoot().y }
            .pointerInput(Unit) {
                // Swipe sideways for the previous or next shloka.
                var dx = 0f
                detectHorizontalDragGestures(onDragStart = { dx = 0f }, onDragCancel = { dx = 0f }, onDragEnd = {
                    if (dx < -swipePx) state.next() else if (dx > swipePx) state.previous()
                }) { _, d -> dx += d }
            }
            .verticalScroll(scroll).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        AnimatedContent(state.index, transitionSpec = { fadeIn(tween(Motion.sheet)) togetherWith fadeOut(tween(Motion.press)) }, label = "verse") { idx ->
            VerseContent(state, idx, active, onLayout = { layout = it }, onTextTop = { rootY -> textTop = rootY - boxTop + scroll.value })
        }
        if (status == AudioStatus.ENDED) EndedActions(state, t)
    }
}

@Composable
private fun VerseContent(state: AppState, idx: Int, active: Int, onLayout: (TextLayoutResult) -> Unit, onTextTop: (Float) -> Unit) {
    val c = LocalReaderColors.current
    val ui = state.uiLang
    val verse = state.queue.getOrElse(idx) { state.current }
    val lang = state.audioLang
    val scale = state.textScale
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(localDigits("${verse.skandha}.${verse.adhyaya}.${verse.numLabel}", ui), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.gold)
            if (verse.speaker != null) Text(verse.speaker, fontFamily = NotoDevanagari, fontSize = 13.sp, color = c.secondary)
        }
        if (lang == Lang.SA) {
            ShlokaLines(state, verse, state.planAt(idx), active, 22f * scale)
            val tr = state.alongsideLayers().firstOrNull()
            if (tr != null && verse.hasText(tr)) {
                Text(verse.translation(tr), Modifier.fillMaxWidth(), fontFamily = readingFont(tr), fontSize = (16 * scale).sp, lineHeight = (26 * scale * state.lineScale).sp, color = c.secondary)
            }
        } else {
            if (state.showSanskrit) ShlokaLines(state, verse, state.planAt(idx), -1, 17f * scale)
            TranslationText(state, verse, lang, state.planAt(idx), active, onLayout, onTextTop)
        }
    }
}

/** The shloka line by line, so each line can be lit as it is recited and tapped to start from there. */
@Composable
private fun ShlokaLines(state: AppState, verse: Verse, plan: VersePlan, active: Int, size: Float) {
    val c = LocalReaderColors.current
    val (lines, font) = when (state.script) {
        SanskritScript.DEVANAGARI -> verse.sa to NotoDevanagari
        SanskritScript.BENGALI -> verse.sa.map { Transliterate.toBengali(it) } to NotoSerifBengali
        SanskritScript.IAST -> verse.iast to EnglishReading
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        lines.forEachIndexed { i, line ->
            val seg = plan.segments.indexOfFirst { it.kind == SegKind.SHLOKA && it.start == i }
            val lit = seg >= 0 && seg == active
            val quiet = active >= 0 && !lit
            Text(
                keepMarkerTogether(line),
                Modifier.clip(RoundedCornerShape(8.dp)).then(if (lit) Modifier.background(c.accent.copy(alpha = 0.12f)) else Modifier)
                    .then(if (seg >= 0) Modifier.clickable(role = Role.Button) { state.playFromSegment(seg) } else Modifier)
                    .padding(horizontal = 8.dp, vertical = 1.dp),
                fontFamily = font, fontSize = size.sp, lineHeight = (size * 1.75f).sp,
                color = if (quiet) c.shloka.copy(alpha = 0.72f) else c.shloka, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TranslationText(
    state: AppState, verse: Verse, lang: Lang, plan: VersePlan, active: Int,
    onLayout: (TextLayoutResult) -> Unit, onTextTop: (Float) -> Unit,
) {
    val c = LocalReaderColors.current
    val scale = state.textScale
    val big = lang == Lang.HI || lang == Lang.BN
    val text = verse.translation(lang)
    // The lit sentence gets a tint; the others step back a little (never below 4.5:1).
    val shown = remember(text, plan, active, c.ink, c.accent) {
        buildAnnotatedString {
            append(text)
            if (active >= 0) plan.segments.forEachIndexed { i, seg ->
                if (seg.kind == SegKind.SENTENCE && seg.start >= 0 && seg.end <= text.length) {
                    addStyle(if (i == active) SpanStyle(background = c.accent.copy(alpha = 0.14f), color = c.ink) else SpanStyle(color = c.ink.copy(alpha = 0.72f)), seg.start, seg.end)
                }
            }
        }
    }
    var result by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        shown,
        Modifier.fillMaxWidth().onGloballyPositioned { onTextTop(it.positionInRoot().y) }.pointerInput(plan) {
            detectTapGestures { pos ->
                val off = result?.getOffsetForPosition(pos) ?: return@detectTapGestures
                val hit = plan.segments.indexOfFirst { it.kind == SegKind.SENTENCE && off >= it.start && off < it.end }
                if (hit >= 0) state.playFromSegment(hit)
            }
        },
        onTextLayout = { result = it; onLayout(it) },
        fontFamily = readingFont(lang), fontSize = ((if (big) 21 else 20) * scale).sp,
        lineHeight = ((if (big) 36 else 32) * scale * state.lineScale).sp, color = c.ink,
    )
}

@Composable
private fun EndedActions(state: AppState, t: PlayerText) {
    val c = LocalReaderColors.current
    val v = state.queue.firstOrNull()
    val hasNext = v != null && SampleData.neighbour(v.skandha, v.adhyaya, 1) != null
    Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.weight(1f).heightIn(min = 52.dp).tappable(Radius.group, t.replay) { state.togglePlay() }.border(1.dp, c.separator, Radius.group).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) { Text(t.replay, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.ink, textAlign = TextAlign.Center) }
        if (hasNext) Box(
            Modifier.weight(1f).heightIn(min = 52.dp).tappable(Radius.group, t.nextChapter) { state.playNextChapter() }.background(c.accent).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) { Text(t.nextChapter, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.chipOnText, textAlign = TextAlign.Center) }
    }
}

// ---------------------------------------------------------------- controls

@Composable
private fun Controls(state: AppState, t: PlayerText) {
    Column(Modifier.fillMaxWidth()) {
        StatusArea(state, t)
        ProgressBar(state, t)
        Transport(state, t)
        SecondaryControls(state)
        LanguageBar(state)
    }
}

/** One quiet line saying what the voice is doing, or a plain explanation (and a way out) when it cannot speak. */
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
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().animateContentSize().clip(Radius.group).background(c.surface)
                .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp).semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text, Modifier.weight(1f), fontSize = 13.sp, lineHeight = 18.sp, color = c.ink)
            if (issue != AudioIssue.NO_ENGINE) Box(
                Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button) {
                    runCatching { context.startActivity(Narrator.settingsIntent()) }
                }.padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) { Text(t.installVoice, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.accent) }
        }
    } else {
        Crossfade(text, animationSpec = tween(Motion.sheet), label = "status") { line ->
            Text(
                line, Modifier.fillMaxWidth().heightIn(min = 28.dp).padding(horizontal = 24.dp).semantics { liveRegion = LiveRegionMode.Polite },
                fontSize = 13.sp, lineHeight = 18.sp, color = c.secondary, textAlign = TextAlign.Center,
            )
        }
    }
}

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
    Column(Modifier.padding(horizontal = 20.dp)) {
        AppSlider(
            value = drag ?: state.index.toFloat(),
            onValueChange = { drag = it },
            onValueChangeFinished = { drag?.let { state.seekTo(it.toInt()) }; drag = null },
            valueRange = 0f..(total - 1).coerceAtLeast(1).toFloat(),
            enabled = total > 1, active = c.accent, inactive = c.track,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = t.position(shown + 1, total) },
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text(localDigits("${shown + 1} / $total", ui), Modifier.weight(1f), fontSize = 13.sp, color = c.secondary)
            Text(localDigits(left, ui), fontSize = 13.sp, color = c.secondary)
        }
    }
}

@Composable
private fun Transport(state: AppState, t: PlayerText) {
    val c = LocalReaderColors.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = state::previous, Modifier.size(56.dp)) { Icon(painterResource(Ic.SkipPrevious), t.prevShloka, tint = c.ink, modifier = Modifier.size(30.dp)) }
        val label = if (state.isPlaying) t.pause else t.play
        Box(
            Modifier.size(72.dp).tappable(CircleShape, label) { state.togglePlay() }.background(c.accent).semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(state.isPlaying, animationSpec = tween(Motion.press), label = "playIcon") { playing ->
                Icon(painterResource(if (playing) Ic.Pause else Ic.PlayArrow), null, tint = c.chipOnText, modifier = Modifier.size(38.dp))
            }
            // While the voice is getting ready (first start, a new language, a new voice) a ring runs round the button.
            if (state.audioStatus == AudioStatus.PREPARING) CircularProgressIndicator(Modifier.matchParentSize().padding(5.dp), color = c.chipOnText.copy(alpha = 0.85f), strokeWidth = 2.5.dp)
        }
        IconButton(onClick = state::next, Modifier.size(56.dp)) { Icon(painterResource(Ic.SkipNext), t.nextShloka, tint = c.ink, modifier = Modifier.size(30.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SecondaryControls(state: AppState) {
    val s = state.strings
    val ui = state.uiLang
    var showSleep by remember { mutableStateOf(false) }
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Chip("${state.speed}×", s.speed, on = false) { state.cycleSpeed() }
        Chip(null, s.loop, on = state.loop, icon = painterResource(Ic.Repeat), toggle = true) { state.toggleLoop() }
        Chip(if (state.sleepMinutes > 0) clock(state.sleepLeftSec) else null, s.sleep, on = state.sleepMinutes > 0, icon = painterResource(Ic.Bedtime)) { showSleep = !showSleep }
    }
    if (showSleep) {
        val c = LocalReaderColors.current
        FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(0, 15, 30, 45, 60).forEach { m ->
                val on = state.sleepMinutes == m
                Box(
                    Modifier.heightIn(min = 48.dp).clip(CircleShape).background(if (on) c.chipOn else c.track)
                        .selectable(selected = on, role = Role.RadioButton) { state.setSleep(m); showSleep = false }.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(if (m == 0) s.off else localDigits("$m", ui), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (on) c.chipOnText else c.ink) }
            }
        }
    }
}

/** The language that is heard. Choosing one rebuilds the narration and carries on from the start of this shloka. */
@Composable
private fun LanguageBar(state: AppState) {
    val c = LocalReaderColors.current
    val langs = buildList { add(Lang.SA); add(Lang.HI); if (BENGALI_READY) add(Lang.BN); add(Lang.EN) }
    val names = langs.map { when (it) { Lang.SA -> "संस्कृत"; Lang.HI -> "हिन्दी"; Lang.BN -> "বাংলা"; Lang.EN -> "English" } }
    val fonts = langs.map { when (it) { Lang.SA -> NotoDevanagari; Lang.HI -> TiroHindi; Lang.BN -> NotoSerifBengali; Lang.EN -> Jakarta } }
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth().clip(Radius.field).background(c.track).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        langs.forEachIndexed { i, l ->
            val on = l == state.readLang
            Box(
                Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(9.dp)).background(if (on) c.surface else Color.Transparent)
                    .selectable(selected = on, role = Role.Tab) { if (!on) { state.updateReadLang(l); state.restartNarration() } },
                contentAlignment = Alignment.Center,
            ) { Text(names[i], Modifier.opticallyCentred(fonts[i]), fontFamily = fonts[i], fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (on) c.ink else c.secondary, maxLines = 1) }
        }
    }
}

@Composable
private fun Chip(top: String?, label: String, on: Boolean, icon: Painter? = null, toggle: Boolean = false, onClick: () -> Unit) {
    val c = LocalReaderColors.current
    Row(
        Modifier.heightIn(min = 48.dp).clip(CircleShape).border(1.dp, if (on) c.accent else c.separator, CircleShape)
            .then(if (toggle) Modifier.toggleable(value = on, role = Role.Switch) { onClick() } else Modifier.clickable(role = Role.Button, onClick = onClick))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = if (on) c.accent else c.ink, modifier = Modifier.size(18.dp))
        if (top != null) Text(top, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (on) c.accent else c.ink)
        else Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (on) c.accent else c.ink)
    }
}
