package com.bhagavatam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotebookTextTest {
    private fun e(ref: String, quote: String = "q", note: String = "", colour: Long = 1L, kind: AnnKind = AnnKind.HIGHLIGHT) =
        NotebookEntry(ref, kind, colour, quote, note)

    private val names = listOf("Important", "Story", "Doubt", "Memorise")
    private val colours = listOf(0xFFE8B931L, 0xFF5FAE6BL, 0xFF4F9BD9L, 0xFFD9577EL)

    @Test fun groupsAreInBookOrder() {
        val g = notebookGroups(listOf(e("10.29.1"), e("1.2.3"), e("1.2.1")))
        assertEquals(listOf("1.2", "10.29"), g.map { it.first })
        assertEquals(listOf("1.2.1", "1.2.3"), g[0].second.map { it.ref })
    }

    @Test fun exportKeepsLongNoteAndIndicText() {
        val note = "ଓଡ଼ିଆ ".repeat(500)
        val out = notebookText(listOf(e("8.2.31", quote = "ଗଜେନ୍ଦ୍ର", note = note)), names, colours) { _, _ -> "Gajendra" }
        assertTrue(out.contains(note.trim()))
        assertTrue(out.contains("ଗଜେନ୍ଦ୍ର"))
        assertTrue(out.contains("8.2.31"))
    }

    @Test fun exportNamesTheColour() {
        val out = notebookText(listOf(e("1.1.1", colour = colours[3])), names, colours) { _, _ -> "T" }
        assertTrue(out.contains("Memorise"))
    }

    @Test fun exportHasNoDashes() {
        val out = notebookText(listOf(e("1.1.1")), names, colours) { _, _ -> "Title" }
        assertTrue(!out.contains('—') && !out.contains('–'))
    }

    @Test fun emptyGivesEmptyString() {
        assertEquals("", notebookText(emptyList(), names, colours) { _, _ -> "" })
    }
}
