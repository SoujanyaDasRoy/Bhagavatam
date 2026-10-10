package com.bhagavatam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQueryTest {

    @Test
    fun testParseReference() {
        val ref1 = SearchQuery.parseReference("1.2")
        assertNotNull(ref1)
        assertEquals(1, ref1!!.skandha)
        assertEquals(2, ref1.chapter)
        assertNull(ref1.verse)
        assertEquals("1.2", ref1.display)

        val ref2 = SearchQuery.parseReference("10.14.8")
        assertNotNull(ref2)
        assertEquals(10, ref2!!.skandha)
        assertEquals(14, ref2.chapter)
        assertEquals(8, ref2.verse)
        assertEquals("10.14.8", ref2.display)

        val ref3 = SearchQuery.parseReference("0.1")
        assertNotNull(ref3)
        assertEquals(0, ref3!!.skandha)
        assertEquals(1, ref3.chapter)

        val ref4 = SearchQuery.parseReference("12.13.23")
        assertNotNull(ref4)
        assertEquals(12, ref4!!.skandha)
        assertEquals(13, ref4.chapter)
        assertEquals(23, ref4.verse)

        // Invalid skandhas
        assertNull(SearchQuery.parseReference("13.1"))
        assertNull(SearchQuery.parseReference("99.1"))
        assertNull(SearchQuery.parseReference("Krishna"))
    }

    @Test
    fun testParseQuotedPhrasesAndTerms() {
        val q1 = SearchQuery.parse("\"Rasa Lila\"")
        assertEquals(listOf("Rasa Lila"), q1.phrases)
        assertTrue(q1.terms.isEmpty())

        val q2 = SearchQuery.parse("Krishna \"supreme lord\" Arjuna")
        assertEquals(listOf("supreme lord"), q2.phrases)
        assertEquals(listOf("krishna", "arjuna"), q2.terms)

        // Minimum length filtering (drops 1-letter words)
        val q3 = SearchQuery.parse("a in the Krishna")
        assertEquals(listOf("in", "the", "krishna"), q3.terms)

        // Punctuation stripped from terms
        val q4 = SearchQuery.parse("Krishna, Arjuna! Dharma?")
        assertEquals(listOf("krishna", "arjuna", "dharma"), q4.terms)
    }

    @Test
    fun testParseScriptureReferenceQuery() {
        val q = SearchQuery.parse("10.29.1")
        assertNotNull(q.reference)
        assertEquals(10, q.reference!!.skandha)
        assertEquals(29, q.reference!!.chapter)
        assertEquals(1, q.reference!!.verse)
        assertTrue(q.terms.isEmpty())
    }
}
