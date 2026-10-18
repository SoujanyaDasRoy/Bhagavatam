package com.bhagavatam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OdiaUiTest {
    private fun hasOdia(s: String) = s.any { it in '଀'..'୿' }

    @Test fun everyOdiaStringIsWrittenInOdia() {
        val bad = mutableListOf<String>()
        for (f in Strings::class.java.declaredFields) {
            f.isAccessible = true
            val v = f.get(OdiaStrings)
            val texts = when (v) { is String -> listOf(v); is List<*> -> v.filterIsInstance<String>(); else -> emptyList() }
            for (t in texts) if (!hasOdia(t)) bad += "${f.name}=$t"
        }
        assertTrue("not Odia: $bad", bad.isEmpty())
    }

    @Test fun settingsAndPlayerTextAreOdia() {
        val st = SettingsOr
        assertTrue(listOf(st.appearance, st.version, st.cancel, st.savedHint, st.spacings.first(), st.modes.last()).all(::hasOdia))
        val pl = PlayerOr
        assertTrue(listOf(pl.preparing, pl.play, pl.pause, pl.minutesLeft(3), pl.position(2, 9), pl.qualities.first()).all(::hasOdia))
    }

    @Test fun odiaIsChosenByTheLookups() {
        assertEquals(SettingsOr, settingsTextFor(Lang.OR))
        assertEquals(PlayerOr, playerTextFor(Lang.OR))
        assertEquals(OdiaStrings, stringsFor(Lang.OR))
    }

    @Test fun trUsesTheOdiaTextAndFallsBackToEnglishWhenNoneIsGiven() {
        assertEquals("ଖୋଜନ୍ତୁ", tr(Lang.OR, "Find", "खोजें", "খুঁজুন", "ଖୋଜନ୍ତୁ"))
        assertEquals("Find", tr(Lang.OR, "Find", "खोजें", "খুঁজুন"))
        assertEquals("खोजें", tr(Lang.HI, "Find", "खोजें", "খুঁজুন", "ଖୋଜନ୍ତୁ"))
    }

    @Test fun odiaDigitsAreUsed() = assertEquals("୧୦.୨୯", localDigits("10.29", Lang.OR))
}
