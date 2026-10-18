package com.bhagavatam.app.state

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingPositionTest {
    private val rows = listOf(1, 2, 3, 5, 8)

    @Test fun indexInsideHeaderGivesFirstVerse() = assertEquals(1, anchorVerseNum(0, 2, rows))
    @Test fun indexMapsToItsVerse() = assertEquals(5, anchorVerseNum(5, 2, rows))
    @Test fun pastTheEndGivesLastVerse() = assertEquals(8, anchorVerseNum(99, 2, rows))
    @Test fun emptyRowsGiveFirstVerse() = assertEquals(1, anchorVerseNum(3, 2, emptyList()))

    @Test fun itemIndexFindsTheVerseOrTheOneBefore() {
        assertEquals(2 + 3, itemIndexFor(5, 2, rows))
        assertEquals(2 + 2, itemIndexFor(4, 2, rows))   // verse 4 is not a row: the row before it (3)
        assertEquals(2 + 0, itemIndexFor(0, 2, rows))   // before the first row: the first row
    }

    @Test fun switchingLanguageKeepsTheVerse() {
        val hindi = listOf(1, 2, 3, 4, 5, 6)
        val joint = listOf(1, 2, 3, 5)                  // another language prints 4-5 together under 5? rows differ
        val top = anchorVerseNum(4, 1, hindi)           // verse 4 at the top in Hindi
        assertEquals(4, top)
        assertEquals(1 + 2, itemIndexFor(top, 1, joint)) // verse 3 is the nearest row at or before 4
    }
}

class ResumeRuleTest {
    @Test fun resumeAppliesOnlyWhenNoVerseWasAskedFor() {
        org.junit.Assert.assertTrue(resumeApplies(null))
        org.junit.Assert.assertFalse(resumeApplies("10.29.5"))
    }
}
