package com.bhagavatam.app.data

import com.bhagavatam.app.audio.NarrationText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VerseDataTest {

    private fun verse(bn: String = "", bnFrom: Int? = null, hi: String = "", en: String = "") =
        Verse(skandha = 1, adhyaya = 1, num = 8, sa = emptyList(), iast = emptyList(), en = en, hi = hi, bn = bn, bnFrom = bnFrom)

    // ---- Bengali ----

    @Test fun bengaliTextIsUsedWhenPresent() {
        val v = verse(bn = "ব্যাসদেব বললেন।")
        assertTrue(v.hasText(Lang.BN))
        assertEquals("ব্যাসদেব বললেন।", v.translation(Lang.BN))
    }

    @Test fun aJointVerseWithoutItsOwnTextPointsToTheOneThatCarriesIt() {
        val v = verse(bn = "", bnFrom = 7)
        assertFalse(v.hasText(Lang.BN))
        assertTrue(v.translation(Lang.BN).contains("7"))
    }

    @Test fun aVerseWithNoBengaliSaysItIsNotAddedYet() {
        val v = verse()
        assertFalse(v.hasText(Lang.BN))
        assertTrue(v.translation(Lang.BN).isNotBlank())
    }

    @Test fun bengaliNarrationSplitsAtTheDanda() {
        val v = verse(bn = "তিনি এলেন। তারপর তিনি চলে গেলেন। কেন গেলেন?")
        val plan = NarrationText.plan(v, Lang.BN)
        assertEquals(3, plan.segments.size)
        assertTrue(plan.segments.last().pauseMs == NarrationText.PAUSE_VERSE_END)
    }

    @Test fun bengaliNarrationOfAnEmptyVerseIsEmpty() {
        assertTrue(NarrationText.plan(verse(bnFrom = 7), Lang.BN).isEmpty)
    }

    // ---- dashes in the printed text ----

    @Test fun anUnspacedEmDashBecomesASpacedHyphen() {
        assertEquals("ব্যাসদেব বললেন - শৌনক", "ব্যাসদেব বললেন—শৌনক".tidyDashes())
    }

    @Test fun aRangeBetweenDigitsBecomesABareHyphen() {
        assertEquals("verses 5-6", "verses 5–6".tidyDashes())
    }

    @Test fun aLineWithoutDashesIsLeftAlone() {
        val t = "Nothing to change here."
        assertEquals(t, t.tidyDashes())
    }

    // ---- a chapter heading that ran into the first verse ----

    @Test fun removesTheTitleFromTheStartOfTheText() {
        val title = "श्रीसूतजीसे शौनकादि ऋषियोंका प्रश्न"
        assertEquals("शौनक उवाच", stripLeadingTitle("$title\nशौनक उवाच", title))
    }

    @Test fun toleratesAHyphenAndAJoinerInThePrintedHeading() {
        val title = "मोहिनीरूपसे भगवान्‌के द्वारा अमृत बाँटा जाना"
        val printed = "मोहिनी-रूपसे भगवान् के द्वारा अमृत बाँटा जाना\nश्रीशुक उवाच"
        assertEquals("श्रीशुक उवाच", stripLeadingTitle(printed, title))
    }

    @Test fun leavesTextThatDoesNotStartWithTheTitle() {
        val text = "जिससे इस जगत् की सृष्टि, स्थिति और प्रलय होते हैं"
        assertEquals(text, stripLeadingTitle(text, "श्रीसूतजीसे शौनकादि ऋषियोंका प्रश्न"))
    }

    @Test fun aShortTitleIsNeverStripped() {
        assertEquals("ॐ नमः", stripLeadingTitle("ॐ नमः", "ॐ"))
    }
}
