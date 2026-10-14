package com.bhagavatam.app.ui

import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.ui.screens.parseVoiceQuery
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceQueryTest {
    @Test fun asciiDigits() { val t = parseVoiceQuery("skandha 10 chapter 29", Lang.EN)!!; assertEquals(10 to 29, t.skandha to t.chapter) }
    @Test fun devanagariDigits() { val t = parseVoiceQuery("दशम स्कन्ध १० २९", Lang.HI)!!; assertEquals(10 to 29, t.skandha to t.chapter) }
    @Test fun bengaliDigits() { val t = parseVoiceQuery("১০.২৯", Lang.BN)!!; assertEquals(10 to 29, t.skandha to t.chapter) }
}
