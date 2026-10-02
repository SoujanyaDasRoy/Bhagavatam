package com.bhagavatam.app.audio

import android.app.Application
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.SampleData
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Debug builds only. Started with `adb shell am start -n com.bhagavatam.app/.MainActivity --ez narration_probe true`.
 *
 * Checks the narration layer against the real text and the real engine without needing anyone to listen:
 * 1. runs the planner over every shloka and reports anything odd (over-long pieces, leftover diacritics, brackets);
 * 2. lists the voices the engine actually has;
 * 3. synthesises representative passages to WAV files, both as planned (sentence by sentence) and as one block, with
 *    timings, so they can be measured (length, silence, level) off the phone.
 * Everything is written to the logcat tag NarrationProbe and to files/probe inside the app.
 */
object NarrationProbe {
    private const val TAG = "NarrationProbe"

    fun run(app: Application) {
        thread(name = "narration-probe") {
            runCatching { corpus() }.onFailure { Log.e(TAG, "corpus failed", it) }
            runCatching { synth(app) }.onFailure { Log.e(TAG, "synth failed", it) }
            Log.i(TAG, "DONE")
        }
    }

    // ---------------------------------------------------------------- 1. the whole text

    private fun corpus() {
        val all = SampleData.allVerses
        Log.i(TAG, "verses=${all.size}")
        for (lang in listOf(Lang.EN, Lang.HI, Lang.SA)) {
            var dropped = 0; var segs = 0; var empty = 0; var maxLen = 0; var tiny = 0; var noStop = 0; var leftover = 0; var bracket = 0; var marks = 0; var introCount = 0
            val longest = ArrayList<String>(); val oddChars = ArrayList<String>(); val seen = HashSet<Char>()
            for (v in all) {
                val plan = NarrationText.plan(v, lang)
                if (lang != Lang.SA && v.hasText(lang)) {
                    val text = when (lang) { Lang.HI -> v.hi; Lang.BN -> v.bn; else -> v.en }
                    dropped += NarrationText.sentences(text, lang).size - plan.segments.size
                }
                if (plan.isEmpty) { empty++; continue }
                for (s in plan.segments) {
                    segs++
                    maxLen = maxOf(maxLen, s.speech.length)
                    if (s.speech.length < 12 && s.kind == SegKind.SENTENCE) tiny++
                    if (lang == Lang.EN && s.kind == SegKind.SENTENCE && s.speech.last() !in ".!?,:;") noStop++
                    if (s.speech.any { it == '(' || it == ')' || it == '[' || it == ']' }) bracket++
                    if (s.speech.any { it == '*' || it == '†' || it == '‡' || it == '‌' || it == '‍' }) marks++
                    if (lang == Lang.EN) s.speech.filter { it.code > 0x7f && it != '’' }.forEach { ch -> if (seen.add(ch)) oddChars.add("U+%04X %c in: %s".format(ch.code, ch, s.speech.take(60))); leftover++ }
                    if (s.kind == SegKind.SENTENCE && s.pauseMs == NarrationText.PAUSE_INTRO) introCount++
                    if (s.speech.length > 200 && longest.size < 5) longest.add(s.speech.take(110))
                }
            }
            Log.i(TAG, "corpus[$lang] verses=${all.size} emptyPlans=$empty segments=$segs avgPerVerse=${"%.2f".format(segs.toFloat() / (all.size - empty).coerceAtLeast(1))} maxLen=$maxLen tiny=$tiny noTerminal=$noStop brackets=$bracket marks=$marks leftoverNonAscii=$leftover intros=$introCount droppedUnspeakable=$dropped")
            longest.forEach { Log.i(TAG, "corpus[$lang] long: $it") }
            oddChars.take(20).forEach { Log.i(TAG, "corpus[$lang] odd: $it") }
        }
    }

    // ------------------------------------------------------------ 2 and 3. the engine

    private class Case(val name: String, val lang: Lang, val source: String, val plan: VersePlan)

    private fun cases(): List<Case> {
        val all = SampleData.allVerses
        val out = ArrayList<Case>()
        fun en(name: String, v: com.bhagavatam.app.data.Verse?) { if (v != null) out += Case(name, Lang.EN, v.en, NarrationText.plan(v, Lang.EN)) }
        fun hi(name: String, v: com.bhagavatam.app.data.Verse?) { if (v != null) out += Case(name, Lang.HI, v.hi, NarrationText.plan(v, Lang.HI)) }
        en("english_prose", SampleData.verse("1.3.2"))
        en("english_long_sentence", SampleData.verse("1.1.1"))
        en("english_proper_nouns", all.firstOrNull { it.en.contains("Yudhiṣṭhira") && it.en.contains("Kṛṣṇa") && it.en.contains("Dhṛtarāṣṭra") } ?: all.firstOrNull { it.en.contains("Kṛṣṇa") && it.en.contains("Pāṇḍ") })
        en("english_dialogue", all.firstOrNull { it.skandha == 1 && it.en.startsWith("Sūta says") })
        en("english_paragraph", all.firstOrNull { NarrationText.plan(it, Lang.EN).segments.size >= 4 && it.skandha == 1 })
        hi("hindi_passage", SampleData.verse("1.3.1"))
        hi("hindi_dialogue", all.firstOrNull { it.skandha == 1 && it.hi.contains("कहते हैं") })
        SampleData.verse("1.1.1")?.let { out += Case("sanskrit_verse", Lang.SA, it.sa.joinToString("\n"), NarrationText.plan(it, Lang.SA)) }
        SampleData.verse("1.3.1")?.let { out += Case("sanskrit_verse_with_speaker", Lang.SA, it.sa.joinToString("\n"), NarrationText.plan(it, Lang.SA)) }
        val bn = "শ্রীমদ্ভাগবত মহাপুরাণ ভগবানের লীলাকথা বর্ণনা করে। এটি শান্তভাবে পাঠ করা হয়, যাতে প্রতিটি শব্দ স্পষ্ট শোনা যায়।"
        out += Case("bengali_passage", Lang.BN, bn, VersePlan(Lang.BN, NarrationText.sentences(bn, Lang.BN).mapIndexed { i, sp ->
            val said = NarrationText.speech(bn.substring(sp.start, sp.end), Lang.BN)
            Segment(said, sp.start, sp.end, sp.pauseMs, SegKind.SENTENCE)
        }))
        val end =SampleData.chapterEnd(1, 8, Lang.EN).colophon
        if (end.isNotBlank()) out += Case("chapter_transition_colophon", Lang.EN, end, VersePlan(Lang.EN, listOf(Segment(NarrationText.speech(end, Lang.EN), 0, end.length, 900, SegKind.SENTENCE))))
        return out
    }

    private fun synth(app: Application) {
        val dir = File(app.filesDir, "probe").apply { deleteRecursively(); mkdirs() }
        val ready = CountDownLatch(1)
        var ok = false
        val done = ConcurrentHashMap<String, Long>()
        val errors = ConcurrentHashMap<String, Int>()
        lateinit var tts: TextToSpeech
        tts = TextToSpeech(app) { status -> ok = status == TextToSpeech.SUCCESS; ready.countDown() }
        if (!ready.await(15, TimeUnit.SECONDS) || !ok) { Log.e(TAG, "engine did not start"); return }
        Log.i(TAG, "engine=${tts.defaultEngine} voices=${tts.voices?.size}")
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { if (utteranceId != null) done[utteranceId] = System.nanoTime() }
            @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) { if (utteranceId != null) errors[utteranceId] = -1 }
            override fun onError(utteranceId: String?, errorCode: Int) { if (utteranceId != null) errors[utteranceId] = errorCode }
        })
        // What the engine really offers, per language.
        for (loc in listOf(Locale("hi", "IN"), Locale("bn", "IN"), Locale("en", "IN"), Locale("sa", "IN"))) {
            val avail = tts.isLanguageAvailable(loc)
            val vs = tts.voices.orEmpty().filter { it.locale.language == loc.language }
            Log.i(TAG, "language ${loc.toLanguageTag()} available=$avail (0 ok, 1 country, 2 variant, -1 missing data, -2 unsupported) voices=${vs.size}")
            vs.sortedByDescending { it.quality }.forEach { Log.i(TAG, "  voice ${it.name} locale=${it.locale} quality=${it.quality} latency=${it.latency} network=${it.isNetworkConnectionRequired} installed=${!it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)} features=${it.features}") }
        }
        val manifest = StringBuilder("case,lang,mode,segment,chars,pauseMs,synthMs,status,file,text\n")
        for (c in cases()) {
            val language = c.lang
            val locale = Narrator.localeFor(language)
            val avail = tts.setLanguage(locale)
            val best = tts.voices.orEmpty().filter { it.locale.language == locale.language && !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
                .sortedWith(compareByDescending<android.speech.tts.Voice> { it.quality }.thenBy { it.isNetworkConnectionRequired }).firstOrNull()
            if (best != null) tts.voice = best
            tts.setSpeechRate(Narrator.baseRate(language)); tts.setPitch(Narrator.basePitch(language))
            Log.i(TAG, "case ${c.name} lang=$language setLanguage=$avail voice=${best?.name} segments=${c.plan.segments.size}")
            fun one(mode: String, index: Int, text: String, pause: Int) {
                val id = "${c.name}:$mode:$index"
                val file = File(dir, "${c.name}__$mode$index.wav")
                val t0 = System.nanoTime()
                val r = tts.synthesizeToFile(text, Bundle(), file, id)
                var waited = 0
                while (!done.containsKey(id) && !errors.containsKey(id) && waited < 60_000) { Thread.sleep(20); waited += 20 }
                val ms = ((done[id] ?: System.nanoTime()) - t0) / 1_000_000
                val status = when { errors.containsKey(id) -> "error${errors[id]}"; !done.containsKey(id) -> "timeout"; r != TextToSpeech.SUCCESS -> "rejected"; else -> "ok" }
                manifest.append("${c.name},$language,$mode,$index,${text.length},$pause,$ms,$status,${file.name},\"${text.replace("\"", "'").replace("\n", " / ").take(70)}\"\n")
            }
            c.plan.segments.forEachIndexed { i, s -> one("seg", i, s.speech, s.pauseMs) }
            // The same passage as one block, which is how the old player sent it.
            val block = if (language == Lang.SA) c.source else NarrationText.speech(c.source, language)
            if (block.isNotBlank()) one("block", 0, block, 0)
        }
        File(dir, "manifest.csv").writeText(manifest.toString())
        tts.shutdown()
        Log.i(TAG, "wrote ${dir.listFiles()?.size} files")
    }
}
