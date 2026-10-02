package com.bhagavatam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnnotationAnchorTest {
    private val text = "the lord said: do not grieve. the lord is kind."

    @Test fun storedRangeStillMatches() {
        assertEquals(4 until 13, reanchor(text, 4, 13, "lord said"))
    }

    @Test fun findsTheQuoteAfterTheTextShifted() {
        val shifted = "xxx$text"
        assertEquals(7 until 16, reanchor(shifted, 4, 13, "lord said"))
    }

    @Test fun picksTheCopyNearestTheOldPosition() {
        val first = text.indexOf("the lord")
        val second = text.lastIndexOf("the lord")
        assertEquals(second until second + 8, reanchor(text, second + 1, second + 9, "the lord"))
        assertEquals(first until first + 8, reanchor(text, first, first + 8, "the lord"))
    }

    @Test fun reportsMissingWhenTheQuoteIsGone() {
        assertNull(reanchor(text, 4, 12, "something else"))
        assertNull(reanchor(text, 0, 0, ""))
    }

    @Test fun worksForBengaliAndDevanagari() {
        val bn = "ভগবান বললেন—হে বিদুর ! শোনো।"
        val q = "হে বিদুর"
        assertEquals(bn.indexOf(q) until bn.indexOf(q) + q.length, reanchor(bn, 0, 3, q))
        val hi = "श्रीभगवानुवाच धर्मक्षेत्रे"
        assertEquals(hi.indexOf("धर्म") until hi.indexOf("धर्म") + 4, reanchor(hi, 0, 4, "धर्म"))
    }
}

class WordAtTest {
    @Test fun englishWord() { assertEquals(4 until 8, wordAt("the lord said", 5)) }
    @Test fun caretAtTheEndOfAWord() { assertEquals(4 until 8, wordAt("the lord said", 8)) }
    @Test fun noWordOnPunctuationOnly() { assertNull(wordAt("a . b", 2)) }
    @Test fun devanagariKeepsItsMarks() {
        val t = "धर्मक्षेत्रे कुरुक्षेत्रे"
        assertEquals(0 until 12, wordAt(t, 3))
    }
    @Test fun bengaliKeepsItsMarks() {
        val t = "ভগবান বললেন"
        assertEquals(0 until 5, wordAt(t, 2))
    }
}
