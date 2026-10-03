package com.bhagavatam.app.state

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bhagavatam.app.data.BENGALI_READY
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
import java.util.Locale

enum class PlayThrough { ADHYAYA, SKANDHA, GRANTH }
enum class PackState { DONE, DOWNLOADING, NONE }

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
    fun updateShowHi(on: Boolean) { showHi = on; save { putBoolean("showHi", on) } }
    fun updateShowBn(on: Boolean) { showBn = on; save { putBoolean("showBn", on) } }
    fun updateShowEn(on: Boolean) { showEn = on; save { putBoolean("showEn", on) } }
    fun updateTheme(t: ReaderTheme) { readerTheme = t; save { putString("theme", t.name) } }
    fun updateTextScale(v: Float) { textScale = v; save { putFloat("textScale", v) } }
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

    // ---------- saved verses ----------
    val bookmarks = mutableStateListOf("1.3.28", "10.29.1", "1.1.3")
    val highlights = mutableStateMapOf("1.3.28" to 0xFFD9577E, "1.1.3" to 0xFFE07A1F)

    fun toggleBookmark(ref: String) { if (ref in bookmarks) bookmarks.remove(ref) else bookmarks.add(0, ref) }

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
    var isPlaying by mutableStateOf(false)
        private set
    var progress by mutableStateOf(0f)
        private set
    var speed by mutableStateOf(1.0f)
        private set
    var loop by mutableStateOf(false)
        private set
    var hasSession by mutableStateOf(false)
        private set
    var audioNote by mutableStateOf<String?>(null)
        private set

    val current: Verse get() = queue[index.coerceIn(0, queue.lastIndex)]

    /** Language that is heard: follows the Bhagavatam (paath) language. */
    val audioLang: Lang get() = readLang

    /** Chapter and Skandha titles follow the reading (paath) language: Sanskrit or Hindi -> Hindi titles,
     *  English -> English, Bengali -> Bengali (chapter titles fall back to English until Bengali is extracted). */
    val titleLang: Lang get() = when (readLang) { Lang.SA, Lang.HI -> Lang.HI; Lang.BN -> Lang.BN; Lang.EN -> Lang.EN }

    private var tickJob: Job? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        tts = TextToSpeech(app) { status -> ttsReady = status == TextToSpeech.SUCCESS }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { viewModelScope.launch(Dispatchers.Main) { onVerseFinished() } }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { viewModelScope.launch(Dispatchers.Main) { fallbackToTimer() } }
            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                val len = current.translation(audioLang).length.coerceAtLeast(1)
                viewModelScope.launch(Dispatchers.Main) { progress = (end.toFloat() / len).coerceIn(0f, 1f) }
            }
        })
    }

    fun playVerses(verses: List<Verse>, startAt: Int = 0) {
        if (verses.isEmpty()) return
        queue = verses; index = startAt.coerceIn(0, verses.lastIndex)
        hasSession = true
        startCurrent()
    }

    fun togglePlay() {
        if (!hasSession) { playVerses(SampleData.adhyaya1, (lastVerse - 1).coerceAtLeast(0)); return }
        if (isPlaying) pause() else startCurrent(resume = true)
    }

    fun next() { if (index < queue.lastIndex) { index++; progress = 0f; if (isPlaying) startCurrent() } }
    fun previous() { if (index > 0) index--; progress = 0f; if (isPlaying) startCurrent() }
    fun toggleLoop() { loop = !loop }
    fun cycleSpeed() {
        val steps = listOf(0.75f, 1.0f, 1.25f, 1.5f)
        speed = steps[(steps.indexOf(speed) + 1) % steps.size]
        tts?.setSpeechRate(speed)
    }

    private fun pause() {
        isPlaying = false
        tickJob?.cancel()
        tts?.stop()
    }

    private fun startCurrent(resume: Boolean = false) {
        tickJob?.cancel(); tts?.stop()
        isPlaying = true
        if (!resume) progress = 0f
        val v = current
        markRead(v.skandha, v.adhyaya, v.num)
        if (audioLang == Lang.SA) {
            // Sanskrit recordings are downloaded per Skandha; none ship with the prototype.
            audioNote = stringsFor(uiLang).sanskritAudio + " · demo timing"
            runTimer(durationFor(v))
        } else {
            speakTranslation(v)
        }
    }

    private fun speakTranslation(v: Verse) {
        val engine = tts
        val locale = when (audioLang) { Lang.HI -> Locale("hi", "IN"); Lang.BN -> Locale("bn", "IN"); else -> Locale("en", "IN") }
        val ok = engine != null && ttsReady && engine.setLanguage(locale) >= TextToSpeech.LANG_AVAILABLE
        if (!ok) { fallbackToTimer(); return }
        audioNote = null
        engine!!.setSpeechRate(speed)
        engine.speak(v.translation(audioLang), TextToSpeech.QUEUE_FLUSH, null, v.ref)
    }

    private fun fallbackToTimer() {
        audioNote = "Install this language’s voice in Android text-to-speech settings"
        runTimer(durationFor(current))
    }

    private fun durationFor(v: Verse) = (6f + v.sa.size * 4f) / speed

    private fun runTimer(seconds: Float) {
        tickJob = viewModelScope.launch {
            val stepMs = 100L
            while (isActive && progress < 1f) {
                delay(stepMs)
                progress = (progress + stepMs / 1000f / seconds).coerceAtMost(1f)
            }
            if (isActive) onVerseFinished()
        }
    }

    private fun onVerseFinished() {
        if (!isPlaying) return
        progress = 0f
        when {
            loop -> startCurrent()
            index < queue.lastIndex -> { index++; startCurrent() }
            else -> { isPlaying = false }
        }
    }

    override fun onCleared() {
        tickJob?.cancel()
        tts?.shutdown()
        super.onCleared()
    }
}
