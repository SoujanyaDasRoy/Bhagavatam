package com.bhagavatam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookGlossTest {

    @Test
    fun twoEndingsAreStripped() {
        assertTrue(Lemmatizer.stems("जीवोंको", "hi").contains("जीव"))
    }

    @Test
    fun verbFormsGoBackToTheDictionaryForm() {
        assertTrue(Lemmatizer.stems("जायगा", "hi").contains("जाना"))
        assertTrue(Lemmatizer.stems("କରୁଥାନ୍ତି", "or").contains("କରିବା"))
        assertTrue(Lemmatizer.stems("বললেন", "bn").contains("বলা"))
    }

    @Test
    fun sanskritCaseEndingsAreStripped() {
        assertTrue(Lemmatizer.stems("रामस्य", "sa").contains("राम"))
        assertTrue(Lemmatizer.stems("देवानाम्", "sa").contains("देव"))
    }

    @Test
    fun partsShowEachMeaning() {
        val meanings = mapOf("विशुद्ध" to "pure", "अनुभव" to "experience")
        val senses = BookGloss.senses(BookGloss.Row(hi = "", en = "", parts = "विशुद्ध+अनुभव")) { meanings[it] }
        assertEquals(listOf("विशुद्ध (pure) + अनुभव (experience)"), senses)
    }

    @Test
    fun matchedWordsKeepTheirMeaningAndAPartWithoutOneStaysBare() {
        val senses = BookGloss.senses(BookGloss.Row(hi = "यदुवंशमें|यदुवंशियोंमें", en = "", parts = "ଯଦୁ+ବଂଶରେ")) { if (it == "यदुवंशमें") "in the Yadu dynasty" else null }
        assertEquals(listOf("≈ यदुवंशमें (in the Yadu dynasty), यदुवंशियोंमें", "ଯଦୁ + ବଂଶରେ"), senses)
    }

    @Test
    fun englishMatchesAreListed() {
        assertEquals(listOf("≈ descended, manifestations"), BookGloss.senses(BookGloss.Row(hi = "", en = "descended|manifestations", parts = "")) { null })
    }
}

class ScriptLangTest {
    @Test
    fun aTypedWordIsLookedUpInTheLanguageOfItsScript() {
        assertEquals("or", Dictionary.langOfScript("ହୋଇଥିବାରୁ"))
        assertEquals("bn", Dictionary.langOfScript("কৃষ্ণ"))
        assertEquals("hi", Dictionary.langOfScript("कृष्ण"))
        assertEquals("en", Dictionary.langOfScript("Krishna"))
    }
}
