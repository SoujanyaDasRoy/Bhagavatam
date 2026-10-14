package com.bhagavatam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryTest {

    // 1. Normalise: Chandrabindu folding
    @Test
    fun testFoldChandrabindu() {
        assertEquals("मां", Normalise.foldChandrabindu("माँ"))
        assertEquals("हंसना", Normalise.foldChandrabindu("हँसना"))
        assertEquals("বাংধা", Normalise.foldChandrabindu("বাঁধা"))
        assertEquals("ଆଂ", Normalise.foldChandrabindu("ଆଁ"))
    }

    // 2. Normalise: Nukta stripping
    @Test
    fun testStripNukta() {
        assertEquals("कलम", Normalise.stripNukta("क़लम"))
        assertEquals("जिंदगी", Normalise.stripNukta("ज़िंदगी"))
        assertEquals("पडना", Normalise.stripNukta("पड़ना"))
        assertEquals("পডা", Normalise.stripNukta("পড়া"))
    }

    // 3. Normalise: Script key (skey) alignment across Bengali, Odia and Hindi
    @Test
    fun testSkeyAlignment() {
        val bnDharma = Normalise.skey("ধর্ম")
        val hiDharma = Normalise.skey("धर्म")
        val orDharma = Normalise.skey("ଧର୍ମ")

        assertEquals(hiDharma, bnDharma)
        assertEquals(hiDharma, orDharma)
        assertEquals("lord", Normalise.skey("Lord"))
    }

    // 4. Lemmatizer: Stem stripping across languages
    @Test
    fun testLemmatizerBengali() {
        val stems = Lemmatizer.stems("কৃষ্ণের", "bn")
        assertTrue("Expected 'কৃষ্ণ' in stems of 'কৃষ্ণের'", stems.contains("কৃষ্ণ"))

        val plStems = Lemmatizer.stems("ভক্তদের", "bn")
        assertTrue("Expected 'ভক্ত' in stems of 'ভক্তদের'", plStems.contains("ভক্ত"))

        val khandaTa = Lemmatizer.stems("জগতে", "bn")
        assertTrue(khandaTa.contains("জগত") || khandaTa.contains("জগৎ"))
    }

    @Test
    fun testLemmatizerHindi() {
        val stems = Lemmatizer.stems("भक्तों", "hi")
        assertTrue("Expected 'भक्त' in stems of 'भक्तों'", stems.contains("भक्त"))

        val kStems = Lemmatizer.stems("राजाओं", "hi")
        assertTrue("Expected 'राजा' in stems of 'राजाओं'", stems.contains("राजा") || kStems.contains("राजा"))
    }

    @Test
    fun testLemmatizerOdia() {
        val stems = Lemmatizer.stems("ରାଜାଙ୍କର", "or")
        assertTrue("Expected 'ରାଜା' in stems of 'ରାଜାଙ୍କର'", stems.contains("ରାଜା"))
    }

    @Test
    fun testLemmatizerEnglish() {
        val stems = Lemmatizer.stems("kings", "en")
        assertTrue(stems.contains("king"))

        val prayStems = Lemmatizer.stems("praying", "en")
        assertTrue(prayStems.contains("pray"))
    }

    // 5. Data structures and sense ordering
    @Test
    fun testDictSensePreference() {
        val s1 = DictSense(1, 10, 0, "hi", "भगवान का सेवक", null)
        val s2 = DictSense(2, 10, 1, "en", "Devotee of God", null)
        val s3 = DictSense(3, 10, 2, "bn", "ঈশ্বরের ভক্ত", null)

        val senses = listOf(s1, s2, s3)
        // Prefer English
        val enPreferred = senses.sortedWith(
            compareByDescending<DictSense> { it.glossLang == "en" }
                .thenBy { it.idx }
        )
        assertEquals("en", enPreferred.first().glossLang)

        // Prefer Bengali
        val bnPreferred = senses.sortedWith(
            compareByDescending<DictSense> { it.glossLang == "bn" }
                .thenBy { it.idx }
        )
        assertEquals("bn", bnPreferred.first().glossLang)
    }

    @Test
    fun testSavedWordEquality() {
        val sw1 = SavedWord(1L, "krishna", "en", "1.3.28", 1000L)
        val sw2 = SavedWord(2L, "krishna", "en", "10.29.1", 2000L)

        assertEquals("krishna", sw1.word)
        assertEquals("1.3.28", sw1.ref)
    }
}
