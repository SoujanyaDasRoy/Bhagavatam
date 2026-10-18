package com.bhagavatam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeakersTest {
    private fun v(num: Int, speaker: String? = null, numEnd: Int = num, adhyaya: Int = 1) =
        Verse(1, adhyaya, num, listOf("x"), emptyList(), "en", "hi", speaker = speaker, numEnd = numEnd)

    @Test fun speakerCarriesForward() {
        val l = speakerLabels(listOf(v(1, "श्रीशुक उवाच"), v(2), v(3)))
        assertEquals(listOf("श्रीशुक उवाच", "श्रीशुक उवाच", "श्रीशुक उवाच"), listOf("1.1.1", "1.1.2", "1.1.3").map { l[it] })
    }

    @Test fun newSpeakerStartsNewRun() {
        val l = speakerLabels(listOf(v(1, "सूत उवाच"), v(2), v(3, "शौनक उवाच"), v(4)))
        assertEquals("सूत उवाच", l["1.1.2"]); assertEquals("शौनक उवाच", l["1.1.4"])
    }

    @Test fun beforeFirstSpeakerIsNull() {
        val l = speakerLabels(listOf(v(1), v(2, "सूत उवाच")))
        assertNull(l["1.1.1"]); assertEquals("सूत उवाच", l["1.1.2"])
    }

    @Test fun chapterStartDoesNotInheritFromPreviousChapter() {
        val first = speakerLabels(listOf(v(1, "सूत उवाच", adhyaya = 1)))
        val second = speakerLabels(listOf(v(1, adhyaya = 2)))
        assertEquals("सूत उवाच", first["1.1.1"]); assertNull(second["1.2.1"])
    }

    @Test fun jointVerseHasOneEntry() = assertEquals(1, speakerLabels(listOf(v(5, "सूत उवाच", numEnd = 6))).size)

    @Test fun startsRunOnlyWhereTheSpeakerChanges() {
        val verses = listOf(v(1, "सूत उवाच"), v(2), v(3, "सूत उवाच"), v(4, "शौनक उवाच"))
        val l = speakerLabels(verses)
        assertEquals(listOf(true, false, false, true), verses.map { startsSpeakerRun(l, verses, it) })
    }

    @Test fun knownSpeakerIsNamedInTheAppLanguageUnknownStaysAsPrinted() {
        assertEquals("Shri Shukadeva said", speakerName("श्रीशुक उवाच", Lang.EN))
        assertEquals("श्रीशुक उवाच", speakerName("श्रीशुक उवाच", Lang.HI))
        assertEquals("কেউ উবাচ", speakerName("কেউ উবাচ", Lang.EN))
    }
}
