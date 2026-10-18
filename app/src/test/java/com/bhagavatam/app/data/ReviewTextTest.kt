package com.bhagavatam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewTextTest {
    private fun verse(num: Int, hi: String = "", en: String = "", enFrom: Int? = null, hiFrom: Int? = null, sa: List<String> = listOf("श्लोक $num । पहला", "दूसरा")) =
        Verse(10, 29, num, sa, emptyList(), en, hi, hiFrom = hiFrom, enFrom = enFrom)

    private val verses = mapOf(
        "10.29.1" to verse(1, hi = "पहला अनुवाद", en = "first translation"),
        "10.29.5" to verse(5, hi = "संयुक्त अनुवाद", en = "joint translation", sa = listOf("पाँचवाँ")),
        "10.29.6" to verse(6, hiFrom = 5, enFrom = 5, sa = listOf("छठा")),
    )
    private val verseOf = { ref: String -> verses[ref] }
    private fun card(kind: String, key: String) = ReviewCard(1, kind, key, 0, 0, 0)

    @Test fun verseCardPromptIsFirstSanskritLine() = assertEquals("श्लोक 1 । पहला", reviewPrompt(card("verse", "10.29.1"), verseOf))

    @Test fun verseCardAnswerIsTheTranslationInTheReadingLanguage() {
        assertEquals("first translation", reviewAnswer(card("verse", "10.29.1"), verseOf, { _, _ -> null }, Lang.EN))
        assertEquals("पहला अनुवाद", reviewAnswer(card("verse", "10.29.1"), verseOf, { _, _ -> null }, Lang.HI))
    }

    @Test fun jointVerseCardUsesTheCarriersText() {
        val a = reviewAnswer(card("verse", "10.29.6"), verseOf, { _, _ -> null }, Lang.EN)
        assertEquals("joint translation", a)
    }

    @Test fun aLanguageWithNoTextFallsBackToAnotherNeverEmpty() {
        val a = reviewAnswer(card("verse", "10.29.1"), verseOf, { _, _ -> null }, Lang.BN)
        assertTrue(!a.isNullOrBlank())
    }

    @Test fun wordCardPromptIsTheWordAndAnswerItsFirstMeaning() {
        val c = card("word", "hi|कृष्ण")
        assertEquals("कृष्ण", reviewPrompt(c, verseOf))
        assertEquals("dark", reviewAnswer(c, verseOf, { w, l -> if (w == "कृष्ण" && l == Lang.HI) "dark" else null }, Lang.EN))
    }

    @Test fun missingVerseGivesNoCard() {
        assertNull(reviewPrompt(card("verse", "9.9.9"), verseOf))
        assertNull(reviewAnswer(card("verse", "9.9.9"), verseOf, { _, _ -> null }, Lang.EN))
    }

    @Test fun wordWithNoMeaningGivesNoAnswer() = assertNull(reviewAnswer(card("word", "hi|xyz"), verseOf, { _, _ -> null }, Lang.EN))

    @Test fun promptNeverNull_forAnExistingVerse() = assertNotNull(reviewPrompt(card("verse", "10.29.5"), verseOf))
}
