package com.bhagavatam.app.ui.screens

import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.Verse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageSwitcherTest {
    private fun verse(en: String = "", hi: String = "", bn: String = "") =
        Verse(skandha = 1, adhyaya = 1, num = 1, sa = emptyList(), iast = emptyList(), en = en, hi = hi, bn = bn)

    @Test fun aLanguageIsAvailableWhenAnyVerseHasText() {
        val verses = listOf(verse(en = "a", hi = "b"), verse(en = "c"))
        assertEquals(setOf(Lang.EN, Lang.HI), availableTranslations(verses))
    }

    @Test fun aChapterWithNoBengaliOffersNoBengali() {
        assertFalse(Lang.BN in availableTranslations(listOf(verse(en = "a", hi = "b"))))
    }

    @Test fun emptyChapterOffersNothing() {
        assertTrue(availableTranslations(emptyList()).isEmpty())
    }

    @Test fun pillNamesTheLanguageInItsOwnScript() {
        assertEquals("বাংলা", pillText(Lang.EN, withShloka = false, readLang = Lang.BN))
        assertEquals("हिन्दी", pillText(Lang.BN, withShloka = false, readLang = Lang.HI))
        assertEquals("English", pillText(Lang.HI, withShloka = false, readLang = Lang.EN))
    }

    @Test fun pillWithShlokaIsDescribedInTheAppLanguage() {
        assertEquals("Shloka + translation", pillText(Lang.EN, true, Lang.SA))
        assertEquals("श्लोक + अनुवाद", pillText(Lang.HI, true, Lang.SA))
        assertEquals("শ্লোক + অনুবাদ", pillText(Lang.BN, true, Lang.SA))
    }

    @Test fun pillNeverShowsSanskritAsATranslationName() {
        // Sanskrit reading always comes with the shloka; if the two ever disagree the pill still shows a real language.
        assertEquals("English", pillText(Lang.EN, withShloka = false, readLang = Lang.SA))
    }

    @Test fun cannotPickAnUnavailableLanguage() {
        assertTrue(canPick(setOf(Lang.HI), Lang.HI))
        assertFalse(canPick(setOf(Lang.HI), Lang.BN))
    }

    @Test fun lastTranslationUnderTheShlokaStaysOn() {
        assertFalse(canTurnOff(1))
        assertTrue(canTurnOff(2))
    }

    @Test fun nameInUsesTheAppLanguage() {
        assertEquals("Bengali", nameIn(Lang.EN, Lang.BN))
        assertEquals("बंगाली", nameIn(Lang.HI, Lang.BN))
    }
}
