package com.bhagavatam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineMeaningTest {
    @Test fun bengaliWordStemsDropCaseEndings() {
        val s = OnlineMeaning.stems("জগতের", Lang.BN)
        assertTrue("জগত" in s)
        assertTrue("জগৎ" in s)
    }

    @Test fun bengaliToDevanagariIsPositionalAndTriesBothBa() {
        val v = OnlineMeaning.bengaliToDevanagari("প্রসন্ন")
        assertEquals(listOf("प्रसन्न"), v)
        val b = OnlineMeaning.bengaliToDevanagari("ভগবান")
        assertTrue("भगवान" in b && "भगबान" in b)
    }

    @Test fun finalTaBecomesHalfTa() {
        assertEquals(listOf("जगत्"), OnlineMeaning.bengaliToDevanagari("জগৎ"))
    }

    @Test fun englishStemsStripSuffix() {
        assertTrue("lord" in OnlineMeaning.stems("lords", Lang.EN))
    }
}
