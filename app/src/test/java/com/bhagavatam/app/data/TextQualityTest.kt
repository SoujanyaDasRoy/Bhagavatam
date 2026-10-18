package com.bhagavatam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextQualityTest {
    @Test fun runsOfPageSymbolsAreGarbled() = assertTrue(odiaLooksGarbled("୧୧୯୯॥ ॥ ॥ (ିତ[?] ତି॥ ୍ରି[?] ୫॥।"))

    @Test fun mostlyDoubtfulWordsAreGarbled() =
        assertTrue(odiaLooksGarbled("ତେ ବାସୁଦେବାୟ ଃମ; ହ ମ୯;,[?] ଠ[?] ୍ଚ୍ଚ[?] କନ୍ଦ[?] ବନ୍େ[?] ବ୍ରଜ୍ସ୍ରିୟମ୍ୱ[?] ବ[?]"))

    @Test fun signsThatNeverOccurInOdiaAreGarbled() = assertTrue(odiaLooksGarbled("ମଶାୟ ନମଃ ॥ ତେ ବାସୁଦେବାୟ ମହ §; ଡତ୍ଛ"))

    @Test fun ordinaryVersesAreKept() {
        assertFalse(odiaLooksGarbled("ଶ୍ରୀଶୁକଦେବଙ୍କର ସର୍ବତ୍ର ବ୍ରହ୍ମ ଅନୁଭବ ସ୍ୱତଃସିଦ୍ଧ ଥିଲା । ତେଣୁ ତାଙ୍କ ନିମନ୍ତେ ଲୌକିକ କର୍ମ ନଥିଲା ॥"))
        assertFalse(odiaLooksGarbled("ଦୈବବଶତଃ[?] ତାକୁ ଏହି ବ୍ରାହ୍ମଣଦେବତା (ଭରତ) ମିଳିଗଲେ । ସେ ଦେଖିଲା, ଏ ଲୋକଟା ବେଶ୍ ହୃଷ୍ଟପୁଷ୍ଟ"))
        assertFalse(odiaLooksGarbled(""))
    }

    @Test fun titleThatIsOnlyAnOrdinalIsNoTitle() {
        assertEquals("", cleanChapterTitle("পঞ্চম অধ্যায়"))
        assertEquals("", cleanChapterTitle("(द्वितीय अध्याय)"))
        assertEquals("", cleanChapterTitle("(পঞ্চদশ অধ্যায়)"))
        assertEquals("", cleanChapterTitle("ଦ୍ୱିତୀୟ ଅଧ୍ଯାୟ"))
        assertEquals("", cleanChapterTitle("   "))
    }

    @Test fun ordinalInFrontOfARealTitleIsDropped() =
        assertEquals("ব্রহ্মার উৎপত্তি", cleanChapterTitle("অষ্টম অধ্যায় ব্রহ্মার উৎপত্তি"))

    @Test fun theSameLetterStoredInTwoWaysIsRecognised() {
        assertEquals("ব্রহ্মার উৎপত্তি", cleanChapterTitle("অষ্টম অধ্যায় ব্রহ্মার উৎপত্তি"))
        assertEquals("", cleanChapterTitle("পঞ্চম অধ্যায়"))
    }

    @Test fun realTitlesAreUntouched() {
        assertEquals("भगवान्का प्राकट्य", cleanChapterTitle("भगवान्का प्राकट्य"))
        assertEquals("विदुरের প্রশ্ন", cleanChapterTitle("विदुरের প্রশ্ন"))
    }
}
