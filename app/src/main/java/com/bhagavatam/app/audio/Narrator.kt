package com.bhagavatam.app.audio

import android.app.Application
import android.content.Intent
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.bhagavatam.app.data.Lang
import java.util.Locale

/** A voice the phone can use for a language, in a form the settings screen can list. */
/** [level] is 0 basic, 1 standard, 2 high, 3 highest; [number] is its place in the list ("Voice 2"). */
data class VoiceOption(val name: String, val number: Int, val level: Int, val needsInternet: Boolean)

enum class ConfigResult { OK, NO_VOICE }

/**
 * Speaks a [VersePlan] with the phone's own text-to-speech engine.
 *
 * Why it is built this way (see docs): Android applies speech rate and pitch to the whole engine, not to single
 * utterances, so the lever for natural rhythm is the silence between pieces. Each segment is queued as its own
 * utterance, followed by a real silent utterance, and the next verse is queued before the current one ends, so the
 * engine never goes quiet between verses.
 *
 * Every utterance id is "epoch:seq:segment" (speech) or "epoch:seq:segment:p" (the silence after it, with a trailing
 * ":L" on the last one of a verse). The epoch lets the caller ignore anything that arrives after a stop or a seek.
 * Callbacks arrive on the main thread.
 */
class Narrator(private val app: Application, private val listener: Listener) {

    interface Listener {
        fun onEngineReady()
        fun onEngineFailed()
        fun onSegmentStart(epoch: Int, seq: Int, seg: Int)
        fun onRange(epoch: Int, seq: Int, seg: Int, start: Int, end: Int)
        fun onVerseEnd(epoch: Int, seq: Int)
        fun onSegmentError(epoch: Int, seq: Int, seg: Int)
    }

    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    var ready = false
        private set
    var failed = false
        private set

    /** The engine starts on first use, not at launch, so the app opens faster. */
    fun start() {
        if (tts != null) return
        tts = TextToSpeech(app) { status ->
            main.post {
                if (status == TextToSpeech.SUCCESS) {
                    tts?.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    tts?.setOnUtteranceProgressListener(progress)
                    ready = true
                    Log.i(TAG, "engine ${tts?.defaultEngine}, ${tts?.voices?.size ?: 0} voices")
                    listener.onEngineReady()
                } else {
                    failed = true
                    listener.onEngineFailed()
                }
            }
        }
    }

    private val progress = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            val id = parse(utteranceId) ?: return
            if (!id.silence) main.post { listener.onSegmentStart(id.epoch, id.seq, id.seg) }
        }

        override fun onDone(utteranceId: String?) {
            val id = parse(utteranceId) ?: return
            if (id.silence && id.last) main.post { listener.onVerseEnd(id.epoch, id.seq) }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) = onError(utteranceId, -1)

        override fun onError(utteranceId: String?, errorCode: Int) {
            val id = parse(utteranceId) ?: return
            Log.w(TAG, "utterance $utteranceId failed ($errorCode)")
            if (!id.silence) main.post { listener.onSegmentError(id.epoch, id.seq, id.seg) }
            else if (id.last) main.post { listener.onVerseEnd(id.epoch, id.seq) }
        }

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            val id = parse(utteranceId) ?: return
            if (!id.silence) main.post { listener.onRange(id.epoch, id.seq, id.seg, start, end) }
        }
    }

    private data class Id(val epoch: Int, val seq: Int, val seg: Int, val silence: Boolean, val last: Boolean)

    private fun parse(id: String?): Id? {
        val p = id?.split(':') ?: return null
        if (p.size < 3) return null
        val e = p[0].toIntOrNull() ?: return null
        val s = p[1].toIntOrNull() ?: return null
        val g = p[2].toIntOrNull() ?: return null
        return Id(e, s, g, silence = p.size > 3 && p[3] == "p", last = p.size > 4 && p[4] == "L")
    }

    // ------------------------------------------------------------ configuration

    fun installedVoices(lang: Lang): List<Voice> {
        val engine = tts ?: return emptyList()
        val language = localeFor(lang).language
        return runCatching { engine.voices }.getOrNull().orEmpty()
            .filter { it.locale.language == language && !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
            .sortedWith(compareByDescending<Voice> { it.quality }
                .thenByDescending { !it.isNetworkConnectionRequired }
                .thenByDescending { it.locale.country == localeFor(lang).country }
                .thenBy { it.name })
    }

    /** Sets language, voice, rate and pitch for what is about to be said. */
    fun configure(lang: Lang, speed: Float, voiceName: String?): ConfigResult {
        val engine = tts ?: return ConfigResult.NO_VOICE
        val locale = localeFor(lang)
        if (engine.setLanguage(locale) < TextToSpeech.LANG_AVAILABLE) return ConfigResult.NO_VOICE
        val voices = installedVoices(lang)
        val chosen = voices.firstOrNull { it.name == voiceName } ?: voices.maxByOrNull { it.quality } ?: voices.firstOrNull()
        if (chosen != null) runCatching { engine.voice = chosen }
        engine.setSpeechRate(baseRate(lang) * speed)
        engine.setPitch(basePitch(lang))
        return ConfigResult.OK
    }

    fun options(lang: Lang): List<VoiceOption> = installedVoices(lang).mapIndexed { i, v ->
        val level = when {
            v.quality >= Voice.QUALITY_VERY_HIGH -> 3
            v.quality >= Voice.QUALITY_HIGH -> 2
            v.quality >= Voice.QUALITY_NORMAL -> 1
            else -> 0
        }
        VoiceOption(v.name, i + 1, level, v.isNetworkConnectionRequired)
    }

    // ----------------------------------------------------------------- speaking

    /**
     * Queues the plan from segment [from]. With [flush] it replaces whatever was queued; without, it joins the end
     * of the queue, which is how the next verse is lined up behind the current one.
     */
    fun speakVerse(epoch: Int, seq: Int, plan: VersePlan, from: Int, pauseScale: Float, flush: Boolean) {
        val engine = tts ?: return
        val max = TextToSpeech.getMaxSpeechInputLength() - 1
        val segments = plan.segments
        for (i in from until segments.size) {
            val seg = segments[i]
            val mode = if (flush && i == from) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val ok = engine.speak(seg.speech.take(max), mode, Bundle(), "$epoch:$seq:$i")
            if (ok != TextToSpeech.SUCCESS) { main.post { listener.onSegmentError(epoch, seq, i) }; continue }
            // The engine already ends each utterance with some silence of its own, so only the shortfall is added.
            // The last piece always gets a silent utterance (even a short one): its end is how the verse's end is noticed.
            val extra = (seg.pauseMs * pauseScale - NarrationText.tailMs(plan.lang)).toLong()
            val last = i == segments.lastIndex
            if (last || extra >= 60) engine.playSilentUtterance(extra.coerceAtLeast(40L), TextToSpeech.QUEUE_ADD, "$epoch:$seq:$i:p" + if (last) ":L" else "")
        }
    }

    fun stop() { tts?.stop() }

    /** Says a short sample so a voice can be judged before it is chosen. */
    fun preview(lang: Lang, speed: Float, voiceName: String?, sample: String) {
        val engine = tts ?: return
        if (configure(lang, speed, voiceName) != ConfigResult.OK) return
        engine.speak(sample, TextToSpeech.QUEUE_FLUSH, Bundle(), "preview")
    }

    /** For checking synthesis without a speaker: writes what would be said to a file. */
    fun synthesizeToFile(text: String, file: java.io.File, id: String): Int = tts?.synthesizeToFile(text, Bundle(), file, id) ?: TextToSpeech.ERROR

    fun engineLabel(): String = tts?.defaultEngine.orEmpty()

    fun shutdown() { tts?.shutdown(); tts = null; ready = false }

    companion object {
        const val TAG = "Narrator"

        /** Sanskrit has no voice of its own on any engine; it is read with the Hindi voice. */
        fun localeFor(lang: Lang): Locale = when (lang) {
            Lang.HI, Lang.SA -> Locale("hi", "IN")
            Lang.BN -> Locale("bn", "IN")
            Lang.EN -> Locale("en", "IN")
        }

        /** Slightly slower than the engine's default: sacred text is read with meditative cadence. */
        fun baseRate(lang: Lang) = when (lang) { Lang.SA -> 0.85f; Lang.BN -> 0.88f; Lang.EN -> 0.92f; else -> 0.90f }
        fun basePitch(lang: Lang) = when (lang) { Lang.SA -> 0.98f; else -> 1.0f }

        /** Opens the system screen where voices are installed and the engine is chosen. */
        fun settingsIntent(): Intent = Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
