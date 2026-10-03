package com.bhagavatam.app.state

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bhagavatam.app.audio.ConfigResult
import com.bhagavatam.app.audio.FocusController
import com.bhagavatam.app.audio.NarrationText
import com.bhagavatam.app.audio.PlaybackService
import com.bhagavatam.app.audio.Narrator
import com.bhagavatam.app.audio.VersePlan
import com.bhagavatam.app.audio.VoiceOption
import com.bhagavatam.app.data.BENGALI_READY
import com.bhagavatam.app.data.Annotation
import com.bhagavatam.app.data.AnnDraft
import com.bhagavatam.app.data.AnnotationStore
import com.bhagavatam.app.data.SelBar
import com.bhagavatam.app.data.WordLookup
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.SanskritScript
import com.bhagavatam.app.data.Verse
import com.bhagavatam.app.data.stringsFor
import com.bhagavatam.app.ui.theme.ReaderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PlayThrough { ADHYAYA, SKANDHA, GRANTH }
enum class PackState { DONE, DOWNLOADING, NONE }

/** Where the voice is: not started, getting ready, speaking, stopped by the listener, or at the end of the chapter. */
enum class AudioStatus { IDLE, PREPARING, PLAYING, PAUSED, ENDED }

/** Why the voice cannot speak. NO_VOICE: none installed for the language. NO_ENGINE: no speech engine. ERROR: it failed while speaking. */
enum class AudioIssue { NO_VOICE, NO_ENGINE, ERROR }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * All app state for the prototype: settings, reading position, saved verses and the player.
 * Settings are kept in SharedPreferences; move to DataStore + Room when the real content lands.
 */
class AppState(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("bhagavatam", Context.MODE_PRIVATE)

    // ---------- settings ----------
    var onboarded by mutableStateOf(prefs.getBoolean("onboarded", false))
        private set
    var uiLang by mutableStateOf(Lang.valueOf(prefs.getString("uiLang", "EN")!!))
        private set
    var readLang by mutableStateOf(Lang.valueOf(prefs.getString("readLang", "SA")!!).let { if (!BENGALI_READY && it == Lang.BN) Lang.EN else it })
        private set
    var script by mutableStateOf(SanskritScript.valueOf(prefs.getString("script", "DEVANAGARI")!!))
        private set
    var showSanskrit by mutableStateOf(prefs.getBoolean("showSanskrit", true))
        private set
    var showIast by mutableStateOf(prefs.getBoolean("showIast", false))
        private set
    /** Meanings of a tapped word are fetched from Wiktionary when this is on (only the word is sent). */
    var onlineMeanings by mutableStateOf(prefs.getBoolean("onlineMeanings", true))
        private set
    /** The full word sheet (all places the word appears) on top of the compact meaning card. */
    var showWordSheet by mutableStateOf(false)
    /** When set, Google results for this text are shown inside the app (the web panel). */
    var webQuery by mutableStateOf<String?>(null)
    var showHi by mutableStateOf(prefs.getBoolean("showHi", false))
        private set
    var showBn by mutableStateOf(prefs.getBoolean("showBn", false) && BENGALI_READY)
        private set
    var showEn by mutableStateOf(prefs.getBoolean("showEn", true))
        private set
    var readerTheme by mutableStateOf(ReaderTheme.valueOf(prefs.getString("theme", "Prabhat")!!))
        private set
    var textScale by mutableStateOf(prefs.getFloat("textScale", 1f))
        private set
    var keepPlaying by mutableStateOf(prefs.getBoolean("keepPlaying", true))
        private set
    var playThrough by mutableStateOf(PlayThrough.valueOf(prefs.getString("playThrough", "SKANDHA")!!))
        private set
    var keepScreenOn by mutableStateOf(prefs.getBoolean("keepScreenOn", true))
        private set

    var followSystem by mutableStateOf(prefs.getBoolean("followSystem", true))
        private set
    var lightTheme by mutableStateOf(ReaderTheme.valueOf(prefs.getString("lightTheme", "Prabhat")!!))
        private set
    var darkTheme by mutableStateOf(ReaderTheme.valueOf(prefs.getString("darkTheme", "Sandhya")!!))
        private set
    var lineScale by mutableStateOf(prefs.getFloat("lineScale", 1f))
        private set
    var showDaily by mutableStateOf(prefs.getBoolean("showDaily", true))
        private set

    val strings get() = stringsFor(uiLang)

    private fun save(block: android.content.SharedPreferences.Editor.() -> Unit) = with(prefs.edit()) { block(); apply() }

    fun finishOnboarding() { onboarded = true; save { putBoolean("onboarded", true) } }
    fun updateUiLang(l: Lang) { uiLang = l; save { putString("uiLang", l.name) } }
    fun updateReadLang(l: Lang) {
        readLang = l; save { putString("readLang", l.name) }
        if (l != Lang.SA) { showSanskrit = false; save { putBoolean("showSanskrit", false) } }
        else { showSanskrit = true; save { putBoolean("showSanskrit", true) } }
    }
    fun updateScript(s: SanskritScript) { script = s; save { putString("script", s.name) } }
    fun updateShowSanskrit(on: Boolean) { showSanskrit = on; save { putBoolean("showSanskrit", on) } }
    fun updateShowIast(on: Boolean) { showIast = on; save { putBoolean("showIast", on) } }
    fun updateOnlineMeanings(on: Boolean) { onlineMeanings = on; save { putBoolean("onlineMeanings", on) } }
    fun updateShowHi(on: Boolean) { showHi = on; save { putBoolean("showHi", on) } }
    fun updateShowBn(on: Boolean) { showBn = on; save { putBoolean("showBn", on) } }
    fun updateShowEn(on: Boolean) { showEn = on; save { putBoolean("showEn", on) } }
    /** Picking one theme by hand turns off "match phone". */
    fun updateTheme(t: ReaderTheme) {
        readerTheme = t; followSystem = false
        if (t.colors.isDark) darkTheme = t else lightTheme = t
        save { putString("theme", t.name); putBoolean("followSystem", false); putString("lightTheme", lightTheme.name); putString("darkTheme", darkTheme.name) }
    }
    fun updateFollowSystem(on: Boolean) { followSystem = on; save { putBoolean("followSystem", on) } }

    /** System, Light or Dark in one control; Light and Dark use the look chosen for that side. */
    val themeMode: ThemeMode get() = if (followSystem) ThemeMode.SYSTEM else if (readerTheme.colors.isDark) ThemeMode.DARK else ThemeMode.LIGHT
    fun setThemeMode(m: ThemeMode) = when (m) {
        ThemeMode.SYSTEM -> updateFollowSystem(true)
        ThemeMode.LIGHT -> updateTheme(lightTheme)
        ThemeMode.DARK -> updateTheme(darkTheme)
    }
    private var saveScaleJob: Job? = null
    private var pendingScaleSave: (android.content.SharedPreferences.Editor.() -> Unit)? = null

    private fun saveScaleDebounced(block: android.content.SharedPreferences.Editor.() -> Unit) {
        pendingScaleSave = block
        saveScaleJob?.cancel()
        saveScaleJob = viewModelScope.launch {
            delay(350)
            pendingScaleSave?.let { save(it) }
            pendingScaleSave = null
            saveScaleJob = null
        }
    }

    /** Immediately writes any unwritten scale/zoom settings to disk on Activity stop or process backgrounding. */
    fun flushPendingSaves() {
        saveScaleJob?.cancel()
        saveScaleJob = null
        pendingScaleSave?.let { block ->
            save(block)
            pendingScaleSave = null
        }
    }

    fun updateLightTheme(t: ReaderTheme) { lightTheme = t; save { putString("lightTheme", t.name) } }
    fun updateDarkTheme(t: ReaderTheme) { darkTheme = t; save { putString("darkTheme", t.name) } }
    fun updateLineScale(v: Float) { lineScale = v; saveScaleDebounced { putFloat("lineScale", v) } }
    fun updateShowDaily(on: Boolean) { showDaily = on; save { putBoolean("showDaily", on) } }
    fun updateTextScale(v: Float) { textScale = v; saveScaleDebounced { putFloat("textScale", v) } }
    fun updateKeepPlaying(on: Boolean) { keepPlaying = on; save { putBoolean("keepPlaying", on) } }
    fun cyclePlayThrough() {
        playThrough = PlayThrough.entries[(playThrough.ordinal + 1) % PlayThrough.entries.size]
        save { putString("playThrough", playThrough.name) }
    }
    fun updateKeepScreenOn(on: Boolean) { keepScreenOn = on; save { putBoolean("keepScreenOn", on) } }

    /** Translation layers to show under a shloka, in reading order. */
    fun alongsideLayers(): List<Lang> = buildList {
        if (showHi) add(Lang.HI)
        if (showBn && BENGALI_READY) add(Lang.BN)
        if (showEn) add(Lang.EN)
    }.ifEmpty { listOf(Lang.EN) }

    // ---------- reading position ----------
    var lastSkandha by mutableStateOf(prefs.getInt("lastS", 1))
        private set
    var lastAdhyaya by mutableStateOf(prefs.getInt("lastA", 1))
        private set
    var lastVerse by mutableStateOf(prefs.getInt("lastV", 1))
        private set

    fun markRead(s: Int, a: Int, v: Int) {
        lastSkandha = s; lastAdhyaya = a; lastVerse = v
        save { putInt("lastS", s); putInt("lastA", a); putInt("lastV", v) }
    }

    /** Chapters whose last shloka has been reached, as "skandha.chapter" keys. */
    val finished = mutableStateListOf<String>().apply { addAll(prefs.getStringSet("finished", emptySet()).orEmpty()) }

    fun isFinished(s: Int, a: Int) = "$s.$a" in finished

    fun markFinished(s: Int, a: Int) {
        val k = "$s.$a"
        if (k in finished) return
        finished.add(k)
        save { putStringSet("finished", finished.toSet()) }
    }

    // ---------- recent searches ----------
    val recent = mutableStateListOf<String>().apply { addAll(prefs.getString("recent", "")!!.split("\n").filter { it.isNotBlank() }) }
    fun addRecent(q: String) {
        val t = q.trim(); if (t.length < 2) return
        recent.remove(t); recent.add(0, t); while (recent.size > 8) recent.removeAt(recent.size - 1)
        save { putString("recent", recent.joinToString("\n")) }
    }
    fun clearRecent() { recent.clear(); save { putString("recent", "") } }

    // ---------- saved verses ----------
    // The first run starts with a few sample verses; after that whatever is saved (or removed) is remembered.
    val bookmarks = mutableStateListOf<String>().apply {
        addAll(prefs.getString("bookmarks", null)?.split("\n")?.filter { it.isNotBlank() } ?: listOf("1.3.28", "10.29.1", "1.1.3"))
    }
    val highlights = mutableStateMapOf<String, Long>().apply {
        val saved = prefs.getString("highlights", null)
        if (saved == null) { put("1.3.28", 0xFFD9577E); put("1.1.3", 0xFFE07A1F) }
        else saved.split("\n").forEach { line -> line.split("=").takeIf { it.size == 2 }?.let { p -> p[1].toLongOrNull()?.let { put(p[0], it) } } }
    }

    private fun saveSaved() = save {
        putString("bookmarks", bookmarks.joinToString("\n"))
        putString("highlights", highlights.entries.joinToString("\n") { "${it.key}=${it.value}" })
    }

    fun toggleBookmark(ref: String) { if (ref in bookmarks) bookmarks.remove(ref) else bookmarks.add(0, ref); saveSaved() }

    // ---------- notes, passage highlights and passage bookmarks (local only) ----------
    private val annotationStore = AnnotationStore(app)
    val annotations = mutableStateListOf<Annotation>().apply { addAll(runCatching { annotationStore.all() }.getOrDefault(emptyList())) }

    /** The note sheet, the word sheet and the selection bar of the reader. */
    var annDraft by mutableStateOf<AnnDraft?>(null)
    var lookup by mutableStateOf<WordLookup?>(null)
    var selBar by mutableStateOf<SelBar?>(null)

    fun annotationsFor(ref: String, layer: String) = annotations.filter { it.ref == ref && it.layer == layer }

    fun addAnnotation(a: Annotation) {
        val id = annotationStore.insert(a)
        annotations.add(0, a.copy(id = id))
    }

    fun updateAnnotation(a: Annotation) {
        annotationStore.update(a)
        val i = annotations.indexOfFirst { it.id == a.id }
        if (i >= 0) annotations[i] = a
    }

    fun removeAnnotation(id: Long) {
        annotationStore.delete(id)
        annotations.removeAll { it.id == id }
    }

    /** Removes every bookmark, highlight and note. */
    fun clearSaved() { bookmarks.clear(); highlights.clear(); saveSaved(); annotationStore.clear(); annotations.clear() }

    /** Forgets finished chapters and the reading position. Saved verses are not touched. */
    fun clearProgress() {
        finished.clear(); lastSkandha = 1; lastAdhyaya = 1; lastVerse = 1
        save { putStringSet("finished", emptySet()); putInt("lastS", 1); putInt("lastA", 1); putInt("lastV", 1) }
    }

    /** Theme, text and listening options back to their defaults. Languages, progress and saved verses are kept. */
    fun resetSettings() {
        followSystem = true; lightTheme = ReaderTheme.Prabhat; darkTheme = ReaderTheme.Sandhya; readerTheme = ReaderTheme.Prabhat
        textScale = 1f; lineScale = 1f; showDaily = true
        keepPlaying = true; playThrough = PlayThrough.SKANDHA; keepScreenOn = true; speed = 1f; pauseScale = 1f
        save {
            putBoolean("followSystem", true); putString("lightTheme", "Prabhat"); putString("darkTheme", "Sandhya"); putString("theme", "Prabhat")
            putFloat("textScale", 1f); putFloat("lineScale", 1f); putBoolean("showDaily", true)
            putBoolean("keepPlaying", true); putString("playThrough", PlayThrough.SKANDHA.name); putBoolean("keepScreenOn", true); putFloat("speed", 1f); putFloat("pauseScale", 1f)
        }
        restartNarration()
    }

    // ---------- downloads (demo states) ----------
    val packs = mutableStateMapOf<Int, PackState>().apply {
        for (s in 0..12) put(s, if (s <= 1) PackState.DONE else PackState.NONE)
    }

    fun download(s: Int) {
        if (packs[s] != PackState.NONE) return
        packs[s] = PackState.DOWNLOADING
        viewModelScope.launch { delay(2500); packs[s] = PackState.DONE }
    }

    fun removePack(s: Int) { packs[s] = PackState.NONE }

    // ---------- player ----------
    var queue by mutableStateOf(SampleData.adhyaya1)
        private set
    var index by mutableStateOf(0)
        private set
    /** How far through the current shloka the voice is, 0 to 1. */
    var progress by mutableStateOf(0f)
        private set
    var speed by mutableStateOf(prefs.getFloat("speed", 1.0f))
        private set
    var loop by mutableStateOf(false)
        private set
    var hasSession by mutableStateOf(false)
        private set
    var audioStatus by mutableStateOf(AudioStatus.IDLE)
        private set
    /** Why the voice cannot speak, when it cannot. The text still follows along silently. */
    var audioIssue by mutableStateOf<AudioIssue?>(null)
        private set
    /** The piece of the current shloka being said now (an index into its plan), or -1. */
    var activeSegment by mutableStateOf(-1)
        private set
    val isPlaying: Boolean get() = audioStatus == AudioStatus.PREPARING || audioStatus == AudioStatus.PLAYING

    // ---- narration settings ----
    /** 0.6 shorter, 1.0 normal, 1.5 longer: scales every pause between sentences, lines and verses. */
    var pauseScale by mutableStateOf(prefs.getFloat("pauseScale", 1f))
        private set
    var voiceHi by mutableStateOf(prefs.getString("voiceHi", null))
        private set
    var voiceBn by mutableStateOf(prefs.getString("voiceBn", null))
        private set
    var voiceEn by mutableStateOf(prefs.getString("voiceEn", null))
        private set
    var voicesReady by mutableStateOf(false)
        private set

    /** The chosen voice for a language; Sanskrit is read with the Hindi voice. */
    fun voiceFor(lang: Lang): String? = when (lang) { Lang.HI, Lang.SA -> voiceHi; Lang.BN -> voiceBn; Lang.EN -> voiceEn }

    fun chooseVoice(lang: Lang, name: String?) {
        when (lang) {
            Lang.HI, Lang.SA -> { voiceHi = name; save { putString("voiceHi", name) } }
            Lang.BN -> { voiceBn = name; save { putString("voiceBn", name) } }
            Lang.EN -> { voiceEn = name; save { putString("voiceEn", name) } }
        }
        restartNarration()
    }

    fun updatePauseScale(v: Float) { pauseScale = v; save { putFloat("pauseScale", v) }; restartNarration() }

    fun prepareVoices() { narrator.start() }
    fun voiceOptions(lang: Lang): List<VoiceOption> = if (voicesReady) narrator.options(lang) else emptyList()

    private var pendingPreview: Pair<Lang, String?>? = null

    /** Says a short sample in a voice, so it can be judged before it is chosen. */
    fun previewVoice(lang: Lang, name: String?) {
        pausePlayback()
        pendingPreview = lang to name
        narrator.start()
        if (narrator.ready) runPreview(lang, name)
    }

    private fun runPreview(lang: Lang, name: String?) {
        pendingPreview = null
        val sample = when (lang) {
            Lang.HI, Lang.SA -> "श्रीमद्भागवत महापुराण का पाठ सुनिए।"
            Lang.BN -> "শ্রীমদ্ভাগবত মহাপুরাণ শুনুন।"
            Lang.EN -> "Listen to the Shrimad Bhagavat Mahapuran."
        }
        narrator.preview(lang, speed, name, sample)
    }

    // ---- sleep timer: 0 = off, otherwise minutes ----
    var sleepMinutes by mutableStateOf(0)
        private set
    var sleepLeftSec by mutableStateOf(0)
        private set
    private var sleepJob: Job? = null
    fun setSleep(min: Int) {
        sleepJob?.cancel(); sleepMinutes = min; sleepLeftSec = min * 60
        if (min > 0) sleepJob = viewModelScope.launch {
            while (isActive && sleepLeftSec > 0) { delay(1000); sleepLeftSec-- }
            if (isActive) { sleepMinutes = 0; pausePlayback() }
        }
    }

    val current: Verse get() = queue[index.coerceIn(0, queue.lastIndex)]

    private fun langFor(v: Verse): Lang = if (readLang == Lang.BN && !v.hasText(Lang.BN)) Lang.HI else readLang

    /** Language that is heard: follows the Bhagavatam (paath) language. */
    val audioLang: Lang get() = langFor(current)

    /** Chapter and Skandha titles follow the reading (paath) language: Sanskrit or Hindi -> Hindi titles,
     *  English -> English, Bengali -> Bengali (chapter titles fall back to English until Bengali is extracted). */
    val titleLang: Lang get() = when (readLang) { Lang.SA, Lang.HI -> Lang.HI; Lang.BN -> Lang.BN; Lang.EN -> Lang.EN }

    // ---- plans: the speakable form of each shloka, built from the untouched source text ----
    private val plans = HashMap<Int, VersePlan>()
    private fun planFor(i: Int): VersePlan = plans.getOrPut(i) { NarrationText.plan(queue[i], langFor(queue[i])) }
    private fun invalidatePlans() = plans.clear()

    /** The plan of the shloka at [i] in the queue. */
    fun planAt(i: Int): VersePlan = planFor(i.coerceIn(0, queue.lastIndex))

    /** First shloka at or after [from] (or before it, with a negative [step]) that has something to say in this language. */
    private fun firstPlayable(from: Int, step: Int): Int? {
        var i = from
        while (i in queue.indices) { if (!planFor(i).isEmpty) return i; i += step }
        return null
    }

    /** An estimate of how long is left in the chapter, in seconds. */
    val secondsLeft: Int get() {
        if (queue.isEmpty()) return 0
        val now = index.coerceIn(0, queue.lastIndex)
        var s = NarrationText.estimateSeconds(planFor(now), pauseScale, speed) * (1f - progress)
        for (i in now + 1..queue.lastIndex) s += NarrationText.estimateSeconds(planFor(i), pauseScale, speed)
        return s.toInt()
    }

    // ---- the narrator and what it reports ----
    @Volatile private var epoch = 0
    private var seqCounter = 0
    private val seqIndex = java.util.concurrent.ConcurrentHashMap<Int, Int>()
    @Volatile private var currentSeq = -1
    @Volatile private var highestSeq = -1
    private var resumeSeg = 0
    private var resumeOnGain = false
    private var errorStreak = 0
    private var silentMode = false
    private var silentJob: Job? = null
    private var pendingStart: Triple<Int, Int, Int>? = null // (index, seg, epoch)
    private var prepJob: Job? = null

    private val narrator by lazy { Narrator(getApplication(), narratorEvents) }

    private val narratorEvents = object : Narrator.Listener {
        override fun onEngineReady() {
            voicesReady = true
            pendingStart?.let { (i, seg, reqEpoch) ->
                if (reqEpoch == this@AppState.epoch) beginSpeaking(i, seg)
            }
            pendingPreview?.let { (lang, name) -> runPreview(lang, name) }
        }
        override fun onEngineFailed() {
            pendingStart?.let { (i, seg, reqEpoch) ->
                if (reqEpoch == this@AppState.epoch) beginSilent(i, seg, AudioIssue.NO_ENGINE)
            }
        }
        override fun onSegmentStart(epoch: Int, seq: Int, seg: Int) = handleSegmentStart(epoch, seq, seg)
        override fun onRange(epoch: Int, seq: Int, seg: Int, start: Int, end: Int) = handleRange(epoch, seq, seg, end)
        override fun onVerseEnd(epoch: Int, seq: Int) = handleVerseEnd(epoch, seq)
        override fun onSegmentError(epoch: Int, seq: Int, seg: Int) {
            if (epoch != this@AppState.epoch) return
            // One bad piece is skipped; three in a row means the voice is not working.
            if (++errorStreak >= 3) failOver()
        }
    }

    private val focus by lazy {
        FocusController(
            getApplication(),
            onLoss = { temporary -> if (isPlaying) { pause(); resumeOnGain = temporary } },
            onGain = { if (resumeOnGain) { resumeOnGain = false; resume() } },
        )
    }

    fun playVerses(verses: List<Verse>, startAt: Int = 0) {
        if (verses.isEmpty()) return
        queue = verses; invalidatePlans()
        hasSession = true
        startAt(startAt.coerceIn(0, verses.lastIndex), 0)
    }

    fun togglePlay() {
        if (!hasSession) { playVerses(SampleData.adhyaya1, (lastVerse - 1).coerceAtLeast(0)); return }
        when {
            isPlaying -> pause()
            audioStatus == AudioStatus.ENDED -> startAt(0, 0)
            else -> resume()
        }
    }

    fun pausePlayback() { if (isPlaying) pause() }

    /** Called when the app leaves the screen: honours "keep playing in the background". */
    fun onAppBackgrounded() {
        flushPendingSaves()
        if (!keepPlaying) pausePlayback()
    }

    fun next() = moveTo(index + 1)

    /** Like any player: a second press goes back a shloka, but if the shloka is well under way it starts it over. */
    fun previous() = moveTo(if (activeSegment > 0 || progress > 0.25f) index else index - 1)

    fun seekTo(i: Int) = moveTo(i)

    /** Starts saying the current shloka from one of its sentences (tap a sentence in the player). */
    fun playFromSegment(seg: Int) = startAt(index, seg)

    fun toggleLoop() { loop = !loop }

    fun cycleSpeed() {
        val steps = listOf(0.75f, 1.0f, 1.25f, 1.5f)
        speed = steps[(steps.indexOf(speed) + 1) % steps.size]
        save { putFloat("speed", speed) }
        if (isPlaying) startAt(index, activeSegment.coerceAtLeast(0))
    }

    /** After the language, voice or pause length changes: rebuild the plans and carry on from the start of the shloka. */
    fun restartNarration() {
        invalidatePlans()
        if (isPlaying) startAt(index, 0) else { activeSegment = -1; resumeSeg = 0 }
    }

    /** At the end of a chapter: carries straight on into the next one. */
    fun playNextChapter() {
        val v = queue.firstOrNull() ?: return
        SampleData.neighbour(v.skandha, v.adhyaya, 1)?.let { (s, a) -> playVerses(SampleData.versesFor(s, a)) }
    }

    private fun moveTo(i: Int) {
        val target = i.coerceIn(0, queue.lastIndex)
        if (isPlaying) { startAt(target, 0); return }
        index = target; progress = 0f; activeSegment = -1; resumeSeg = 0
        if (hasSession) audioStatus = AudioStatus.PAUSED
    }

    private fun pause() {
        resumeSeg = activeSegment.coerceAtLeast(0)
        prepJob?.cancel()
        silentJob?.cancel(); narrator.stop(); epoch++
        audioStatus = AudioStatus.PAUSED
        focus.release()
    }

    private fun resume() = startAt(index, resumeSeg)

    /** Begins (or restarts) speaking from sentence [seg] of shloka [i]. Anything already queued is dropped. */
    private fun startAt(i: Int, seg: Int) {
        silentJob?.cancel(); narrator.stop(); epoch++
        seqIndex.clear(); currentSeq = -1; highestSeq = -1; errorStreak = 0; silentMode = false
        val first = firstPlayable(i.coerceIn(0, queue.lastIndex), if (i < index) -1 else 1)
        if (first == null) { audioStatus = AudioStatus.ENDED; return }
        index = first; progress = 0f; activeSegment = -1; resumeSeg = seg
        audioIssue = null; audioStatus = AudioStatus.PREPARING
        // The first start in a language can take a few seconds while the voice loads or downloads. If nothing is heard
        // after twenty, say so rather than spin for ever.
        prepJob?.cancel()
        val mine = epoch
        prepJob = viewModelScope.launch { delay(20_000); if (mine == epoch && audioStatus == AudioStatus.PREPARING) failOver() }
        val v = current
        markRead(v.skandha, v.adhyaya, v.num)
        if (!focus.acquire()) { audioStatus = AudioStatus.PAUSED; return } // a phone call has the speaker
        runCatching { PlaybackService.start(getApplication()) }
        narrator.start()
        when {
            narrator.failed -> beginSilent(first, seg, AudioIssue.NO_ENGINE)
            narrator.ready -> beginSpeaking(first, seg)
            else -> pendingStart = Triple(first, seg, epoch)
        }
    }

    private fun beginSpeaking(i: Int, seg: Int) {
        pendingStart = null
        val lang = langFor(queue[i])
        if (narrator.configure(lang, speed, voiceFor(lang)) != ConfigResult.OK) { beginSilent(i, seg, AudioIssue.NO_VOICE); return }
        enqueueVerse(i, seg, flush = true)
    }

    /** The voice is not producing sound. Say why, and let the text follow along so the reader is not left with a dead button. */
    private fun failOver() {
        val lang = langFor(current)
        val issue = if (narrator.installedVoices(lang).isEmpty()) AudioIssue.NO_VOICE else AudioIssue.ERROR
        val seg = activeSegment.coerceAtLeast(0)
        prepJob?.cancel(); silentJob?.cancel(); narrator.stop(); epoch++
        seqIndex.clear(); currentSeq = -1; highestSeq = -1
        audioStatus = AudioStatus.PLAYING
        beginSilent(index, seg, issue)
    }

    private fun enqueueVerse(i: Int, from: Int, flush: Boolean) {
        val seq = ++seqCounter
        seqIndex[seq] = i; highestSeq = seq
        narrator.speakVerse(epoch, seq, planFor(i), from.coerceIn(0, (planFor(i).segments.size - 1).coerceAtLeast(0)), pauseScale, flush)
    }

    private fun nextToPlay(after: Int): Int? = if (loop) after else firstPlayable(after + 1, 1)

    /** Keeps the next shloka queued behind the one being said, so there is no gap between them. */
    private fun ensureLookahead() {
        while (highestSeq < currentSeq + 1) {
            val last = seqIndex[highestSeq] ?: return
            val n = nextToPlay(last) ?: return
            enqueueVerse(n, 0, flush = false)
        }
    }

    private fun handleSegmentStart(epoch: Int, seq: Int, seg: Int) {
        if (epoch != this.epoch) return
        val idx = seqIndex[seq] ?: return
        errorStreak = 0
        prepJob?.cancel()
        if (seq != currentSeq) {
            currentSeq = seq
            if (idx != index) index = idx
            val v = current
            markRead(v.skandha, v.adhyaya, v.num)
            progress = 0f
            if (!silentMode) ensureLookahead()
        }
        activeSegment = seg
        if (audioStatus == AudioStatus.PREPARING) audioStatus = AudioStatus.PLAYING
    }

    private fun handleRange(epoch: Int, seq: Int, seg: Int, end: Int) {
        if (epoch != this.epoch || seq != currentSeq) return
        val p = planFor(seqIndex[seq] ?: return)
        val total = p.speechChars.coerceAtLeast(1)
        val before = p.segments.take(seg).sumOf { it.speech.length }
        progress = ((before + end).toFloat() / total).coerceIn(0f, 1f)
    }

    private fun handleVerseEnd(epoch: Int, seq: Int) {
        if (epoch != this.epoch) return
        // Only the last shloka that was queued ends the chapter; earlier ones are followed by the next.
        if (seq == highestSeq) {
            // "Play through": carry on into the next chapter when the setting says so (never in silent mode, which would run on for nothing).
            val v = queue.lastOrNull()
            val next = if (silentMode || loop || v == null) null
                else SampleData.neighbour(v.skandha, v.adhyaya, 1)?.takeIf { playThrough == PlayThrough.GRANTH || (playThrough == PlayThrough.SKANDHA && it.first == v.skandha) }
            if (next != null) { playVerses(SampleData.versesFor(next.first, next.second)); return }
            audioStatus = AudioStatus.ENDED; activeSegment = -1; progress = 1f; resumeSeg = 0
            silentMode = false; focus.release()
        }
    }

    /**
     * No voice can be used (none installed for this language, or no engine): the text still follows along at a
     * natural pace so the reader is not left with a dead button, and the player says why.
     */
    private fun beginSilent(i: Int, seg: Int, issue: AudioIssue) {
        pendingStart = null; audioIssue = issue; silentMode = true
        val mine = epoch
        silentJob = viewModelScope.launch {
            var idx = i
            var from = seg
            while (isActive && mine == epoch) {
                val seq = ++seqCounter
                seqIndex[seq] = idx; highestSeq = seq
                val p = planFor(idx)
                val total = p.speechChars.coerceAtLeast(1)
                var said = p.segments.take(from).sumOf { it.speech.length }
                for (s in from until p.segments.size) {
                    handleSegmentStart(mine, seq, s)
                    val piece = p.segments[s]
                    val ms = (piece.speech.length / (NarrationText.charsPerSecond(p.lang) * speed) * 1000f + piece.pauseMs * pauseScale).toLong()
                    var t = 0L
                    while (t < ms) {
                        delay(100); t += 100
                        if (!isActive || mine != epoch) return@launch
                        progress = ((said + piece.speech.length * (t.toFloat() / ms).coerceAtMost(1f)) / total).coerceIn(0f, 1f)
                    }
                    said += piece.speech.length
                }
                val n = nextToPlay(idx)
                if (n == null) { handleVerseEnd(mine, seq); return@launch }
                idx = n; from = 0
            }
        }
    }

    override fun onCleared() {
        flushPendingSaves()
        silentJob?.cancel(); sleepJob?.cancel()
        narrator.shutdown(); focus.release()
        super.onCleared()
    }
}
