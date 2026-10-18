package com.bhagavatam.app.ui.screens

import com.bhagavatam.app.data.AnnKind
import com.bhagavatam.app.data.Annotation
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterMarksTest {
    private fun a(id: Long, quote: String, note: String = "", colour: Long = MarkColours[0], kind: AnnKind = AnnKind.HIGHLIGHT) =
        Annotation(id, "1.1.$id", "hi", 0, quote.length, quote, kind, colour, note, 0)

    private val marks = listOf(
        a(1, "Krishna lifted the hill", colour = MarkColours[0]),
        a(2, "the gopis wept", note = "doubt about Krishna", colour = MarkColours[2]),
        a(3, "bookmarked line", colour = 0L, kind = AnnKind.BOOKMARK),
    )

    @Test fun noFilterKeepsEverything() = assertEquals(3, filterMarks(marks, "", 0).size)
    @Test fun searchLooksInQuoteAndNoteIgnoringCase() = assertEquals(listOf(1L, 2L), filterMarks(marks, "KRISHNA", 0).map { it.id })
    @Test fun colourChipKeepsOnlyThatColour() = assertEquals(listOf(2L), filterMarks(marks, "", 3).map { it.id })
    @Test fun aBookmarkNeverMatchesAColourChip() = assertEquals(emptyList<Long>(), filterMarks(marks, "", 4).map { it.id })
    @Test fun searchAndColourTogether() = assertEquals(listOf(1L), filterMarks(marks, "hill", 1).map { it.id })
    @Test fun blankSearchIsNoSearch() = assertEquals(3, filterMarks(marks, "   ", 0).size)
}

class ShareSizeTest {
    @Test fun smallTextFitsAndHugeTextDoesNot() {
        org.junit.Assert.assertTrue(fitsInShareIntent("a".repeat(1000)))
        org.junit.Assert.assertFalse(fitsInShareIntent("ଓ".repeat(300_000)))
    }
}

class GroupMarksTest {
    private fun a(id: Long, ref: String) = Annotation(id, ref, "hi", 0, 1, "q", AnnKind.HIGHLIGHT, 1L, "", 0)

    @Test fun groupsAreInBookOrderAndMarksInVerseOrder() {
        val g = groupMarks(listOf(a(1, "10.29.1"), a(2, "1.2.10"), a(3, "1.2.3")))
        org.junit.Assert.assertEquals(listOf("1.2", "10.29"), g.map { it.first })
        org.junit.Assert.assertEquals(listOf(3L, 2L), g[0].second.map { it.id })
    }

    @Test fun twoThousandMarksGroupQuickly() {
        val marks = (1..2000).map { a(it.toLong(), "${it % 12 + 1}.${it % 30 + 1}.${it % 50 + 1}") }
        val t0 = System.nanoTime()
        groupMarks(marks)
        org.junit.Assert.assertTrue((System.nanoTime() - t0) / 1_000_000 < 500)
    }
}
