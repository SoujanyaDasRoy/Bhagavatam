package com.bhagavatam.app.state

import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.data.digitsTo
import org.junit.Assert.assertEquals
import org.junit.Test

class LangZoneTest {
    @Test fun frameFollowsTheApp() {
        assertEquals(Lang.EN, zoneLang(LangZone.FRAME, Lang.EN, Lang.BN))
        assertEquals(Lang.HI, zoneLang(LangZone.FRAME, Lang.HI, Lang.EN))
    }

    @Test fun pageFollowsTheReadingLanguage() {
        assertEquals(Lang.BN, zoneLang(LangZone.PAGE, Lang.EN, Lang.BN))
        // with Sanskrit the title language is already the app language, so the page follows the app
        assertEquals(Lang.HI, zoneLang(LangZone.PAGE, Lang.HI, Lang.HI))
    }

    @Test fun digitsFollowTheAppWhateverScriptTheyWerePrintedIn() {
        assertEquals("॥ १२ ॥", digitsTo("॥ ১২ ॥", Lang.HI))
        assertEquals("12 ।", digitsTo("১২ ।", Lang.EN))
        assertEquals("শ্লোক ১২", digitsTo("শ্লোক 12", Lang.BN))
        assertEquals("abc", digitsTo("abc", Lang.HI))
    }
}
