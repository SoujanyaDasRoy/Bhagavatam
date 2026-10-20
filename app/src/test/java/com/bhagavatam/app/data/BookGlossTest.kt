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
    fun aStemWithAMeaningIsNamedAsTheBaseForm() {
        val senses = BookGloss.senses(BookGloss.Row(hi = "", en = "", parts = "", stem = "ଦିଶ")) { if (it == "ଦିଶ") "appear" else null }
        assertEquals(listOf("form of ଦିଶ (appear)"), senses)
    }

    @Test
    fun aMisprintIsExplainedByTheWordItMisses() {
        val senses = BookGloss.senses(BookGloss.Row(hi = "", en = "", parts = "", fix = "ଭାଗବତେ")) { if (it == "ଭାଗବତେ") "in the Bhagavata" else null }
        assertEquals(listOf("probably a misprint of ଭାଗବତେ (in the Bhagavata)"), senses)
    }

    @Test
    fun theVerseIsTheLastResortAndOnlyWhenNothingElseIsKnown() {
        val none = BookGloss.Row(hi = "", en = "", parts = "", ref = "10.29.1")
        assertEquals(listOf("In verse 10.29.1: Krishna looked."), BookGloss.senses(none, verse = { "Krishna looked." }) { null })
        val known = BookGloss.Row(hi = "", en = "descended", parts = "", ref = "10.29.1")
        assertEquals(listOf("≈ descended"), BookGloss.senses(known, verse = { "Krishna looked." }) { null })
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
