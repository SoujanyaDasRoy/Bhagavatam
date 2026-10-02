package com.bhagavatam.app.audio

import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.Verse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NarrationTextTest {

    private fun spans(text: String, lang: Lang) = NarrationText.sentences(text, lang).map { text.substring(it.start, it.end) }

    private fun verse(en: String = "", hi: String = "", sa: List<String> = emptyList(), speaker: String? = null) =
        Verse(skandha = 1, adhyaya = 1, num = 1, sa = sa, iast = emptyList(), en = en, hi = hi, speaker = speaker)

    // ---- sentences ----

    @Test fun splitsEnglishAtSentenceEnds() {
        val t = "He came to the hermitage. Then he left it behind! Why did he go?"
        assertEquals(listOf("He came to the hermitage.", "Then he left it behind!", "Why did he go?"), spans(t, Lang.EN))
    }

    @Test fun doesNotSplitAfterAbbreviationsOrBeforeLowercase() {
        val t = "That is, viz. the Lord himself. Then he left, etc. and returned."
        assertEquals(listOf("That is, viz. the Lord himself.", "Then he left, etc. and returned."), spans(t, Lang.EN))
    }

    // The engine already leaves about 0.8 s of silence after every English utterance, so cutting after
    // "Suta says:" would leave a long hole. English keeps the introduction with its sentence.
    @Test fun keepsAnEnglishDialogueIntroWithItsSentence() {
        val t = "Sūta says : In the beginning the Lord wished to create. He assumed a form."
        assertEquals(listOf("Sūta says : In the beginning the Lord wished to create.", "He assumed a form."), spans(t, Lang.EN))
    }

    @Test fun doesNotTreatAnOrdinaryColonAsADialogueIntro() {
        val t = "He gave three gifts: gold, grain and cattle."
        assertEquals(listOf(t), spans(t, Lang.EN))
    }

    @Test fun splitsHindiAtDanda() {
        val t = "ये सब अवतार हैं। परंतु कृष्ण स्वयं भगवान हैं।"
        assertEquals(listOf("ये सब अवतार हैं।", "परंतु कृष्ण स्वयं भगवान हैं।"), spans(t, Lang.HI))
    }

    @Test fun splitsOffAHindiDialogueIntro() {
        val t = "श्रीसूतजी कहते हैं - सृष्टिके आदिमें भगवान् ने इच्छा की । उन्होंने रूप धारण किया ।"
        val s = spans(t, Lang.HI)
        assertEquals(3, s.size)
        assertTrue(s[0].startsWith("श्रीसूतजी कहते हैं"))
    }

    @Test fun cutsAVeryLongSentenceAtClauseBoundaries() {
        val clause = "the sage, whose mind was fixed on the Lord"
        val t = (1..18).joinToString(", ") { clause } + " came to the hermitage."
        val s = spans(t, Lang.EN)
        assertTrue(s.size > 1)
        assertTrue("every piece is short enough for the engine to keep one steady contour", s.all { it.length <= 380 })
        assertEquals("nothing is lost or reordered", t.replace(" ", ""), s.joinToString("").replace(" ", ""))
    }

    @Test fun mergesATinyFragmentIntoThePreviousSentence() {
        assertEquals(listOf("He came and sat down. Yes."), spans("He came and sat down. Yes.", Lang.EN))
    }

    // ---- speech text (the shown text is never changed) ----

    @Test fun anglicisesTransliteratedNames() {
        val out = NarrationText.speech("Śrī Kṛṣṇa met Yudhiṣṭhira at Dvārakā and Ṛṣi Vyāsa.", Lang.EN)
        assertEquals("Shree Krishna met Yudhishthira at Dvaraka and Rishi Vyasa.", out)
    }

    @Test fun turnsParentheticalsIntoSpokenAsides() {
        val out = NarrationText.speech("the Lord (Sri Krsna) smiled (gently) at her.", Lang.EN)
        assertEquals("the Lord, Sri Krsna, smiled, gently, at her.", out)
    }

    @Test fun readsTheSpacedHyphenAsAPause() {
        assertEquals("it returns, because He is present.", NarrationText.speech("it returns - because He is present.", Lang.EN))
    }

    @Test fun dropsFootnoteMarksAndQuoteMarks() {
        assertEquals("Soota replied. It is so.", NarrationText.speech("Sūta* replied.† “It is so.”", Lang.EN))
    }

    @Test fun keepsApostrophesInsideWords() {
        assertEquals("the sun's rays", NarrationText.speech("the sun’s rays", Lang.EN))
    }

    @Test fun addsTerminalPunctuationWhenMissing() {
        assertTrue(NarrationText.plan(verse(en = "He came and sat down"), Lang.EN).segments.last().speech.endsWith("."))
    }

    @Test fun cleansHindiJoinersAndMarks() {
        val out = NarrationText.speech("भगवान्‌के अवतार*", Lang.HI)
        assertFalse(out.contains('‌'))
        assertFalse(out.contains('*'))
        assertTrue(out.startsWith("भगवान्के"))
    }

    @Test fun removesTheClosingVerseMarkerFromASanskritLine() {
        val out = NarrationText.speechSanskritLine("सत्यं परं धीमहि ॥ १ ॥")
        assertEquals("सत्यं परं धीमहि", out)
    }

    @Test fun removesTheDandaFromASanskritLine() {
        assertEquals("जगृहे पौरुषं रूपं भगवान्महदादिभिः", NarrationText.speechSanskritLine("जगृहे पौरुषं रूपं भगवान्महदादिभिः ।"))
    }

    // ---- repairs for what the printed text carries ----

    @Test fun joinsLineWrapHyphensInEnglishSpeech() {
        assertEquals("who is self-conscious and self-effulgent.", NarrationText.speech("who is self-conscious and self- effulgent.", Lang.EN))
    }

    @Test fun joinsLineWrapHyphensInSanskritSpeech() {
        assertEquals("सकलजीवनिकायावासस्य भगवतो", NarrationText.speechSanskritLine("सकलजीव- निकायावासस्य भगवतो"))
    }

    @Test fun saysNothingForTextGarbledByTheOldFont() {
        assertEquals("", NarrationText.speech("∞ﬂt YI¬EaEEt AˇEAA÷E⁄U", Lang.EN))
    }

    @Test fun dropsDevanagariLettersFromEnglishSpeech() {
        assertEquals("the letters and are sounds.", NarrationText.speech("the letters त and प are sounds.", Lang.EN))
    }

    @Test fun sanskritPausesFollowTheDandas() {
        val v = verse(sa = listOf("क ख", "ग घ ।", "ङ च ॥ १ ॥"))
        val pauses = NarrationText.plan(v, Lang.SA).segments.map { it.pauseMs }
        assertEquals(listOf(NarrationText.PAUSE_PADA_CONT, NarrationText.PAUSE_PADA_END, NarrationText.PAUSE_SHLOKA_END), pauses)
    }

    @Test fun cutsAVeryLongSanskritLineIntoBreaths() {
        val line = (1..40).joinToString(" ") { "धर्मक्षेत्रे" }
        val plan = NarrationText.plan(verse(sa = listOf(line)), Lang.SA)
        assertTrue(plan.segments.size > 1)
        assertTrue(plan.segments.all { it.speech.length <= 150 && it.start == 0 })
    }

    @Test fun theEnginesOwnTrailingSilenceIsKnownPerLanguage() {
        assertTrue(NarrationText.tailMs(Lang.EN) > NarrationText.tailMs(Lang.SA))
    }

    // ---- plans ----

    @Test fun planKeepsRangesThatPointBackIntoTheShownText() {
        val text = "He came. Then he left."
        val plan = NarrationText.plan(verse(en = text), Lang.EN)
        assertEquals(listOf("He came.", "Then he left."), plan.segments.map { text.substring(it.start, it.end) })
    }

    @Test fun planPausesLongerAtTheEndOfTheVerse() {
        val plan = NarrationText.plan(verse(en = "He came. Then he left."), Lang.EN)
        assertTrue(plan.segments.last().pauseMs > plan.segments.first().pauseMs)
    }

    @Test fun sanskritPlanIsOneSegmentPerLineWithTheSpeakerFirst() {
        val plan = NarrationText.plan(verse(sa = listOf("तेने ब्रह्म हृदा य आदिकवये", "सत्यं परं धीमहि ॥ १ ॥"), speaker = "सूत उवाच"), Lang.SA)
        assertEquals(listOf(SegKind.SPEAKER, SegKind.SHLOKA, SegKind.SHLOKA), plan.segments.map { it.kind })
        assertEquals(listOf(-1, 0, 1), plan.segments.map { it.start })
        assertFalse(plan.segments.last().speech.contains('॥'))
    }

    @Test fun planDropsSegmentsWithNothingToSay() {
        val plan = NarrationText.plan(verse(en = "He came. * Then he left."), Lang.EN)
        assertTrue(plan.segments.all { it.speech.isNotBlank() })
    }

    @Test fun emptyTranslationGivesAnEmptyPlan() {
        assertTrue(NarrationText.plan(verse(), Lang.EN).isEmpty)
    }

    @Test fun estimateGrowsWithTextAndPauseScale() {
        val short = NarrationText.plan(verse(en = "He came."), Lang.EN)
        val long = NarrationText.plan(verse(en = "He came and sat down by the river and thought for a long while about the world."), Lang.EN)
        assertTrue(NarrationText.estimateSeconds(long) > NarrationText.estimateSeconds(short))
        assertTrue(NarrationText.estimateSeconds(short, 1.5f) > NarrationText.estimateSeconds(short, 1f))
    }
}
