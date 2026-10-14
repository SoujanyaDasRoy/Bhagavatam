package com.bhagavatam.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TransliterateIastTest {
    @Test fun colophonOpening() {
        assertEquals("iti śrīmadbhāgavate mahāpurāṇe", Transliterate.toIast("इति श्रीमद्भागवते महापुराणे"))
    }

    @Test fun anusvaraVisargaAndNumbers() {
        assertEquals("pāramahaṃsyāṃ saṃhitāyāṃ || 12 ||", Transliterate.toIast("पारमहंस्यां संहितायां ॥ १२ ॥"))
        assertEquals("kṛṣṇaḥ", Transliterate.toIast("कृष्णः"))
    }

    @Test fun conjunctsKeepHalfLetters() {
        assertEquals("nṛsiṃha", Transliterate.toIast("नृसिंह"))
        assertEquals("dharma", Transliterate.toIast("धर्म"))
    }
}
