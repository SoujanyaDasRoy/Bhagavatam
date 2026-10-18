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
