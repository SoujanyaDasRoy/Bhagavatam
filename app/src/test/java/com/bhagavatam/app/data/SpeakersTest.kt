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

    @Test fun topSpeakersAreNamedInEveryLanguage() {
        assertEquals("Shri Shukadeva said", speakerName("श्रीशुक उवाच", Lang.EN))
        assertEquals("श्रीशुकदेवजी ने कहा", speakerName("श्रीशुक उवाच", Lang.HI))
        assertEquals("শ্রীশুকদেব বললেন", speakerName("श्रीशुक उवाच", Lang.BN))
        assertEquals("श्रीमैत्रेयजी ने कहा", speakerName("मैत्रेय उवाच", Lang.HI))
        assertEquals("The Lord said", speakerName("श्रीभगवानुवाच", Lang.EN))
        assertEquals("श्रीभगवान् ने कहा", speakerName("श्रीभगवानुवाच", Lang.HI))
    }

    @Test fun otherSpeakersAreBuiltFromTheirName() {
        assertEquals("Yudhishthira said", speakerName("युधिष्ठिर उवाच", Lang.EN))
        assertEquals("युधिष्ठिर ने कहा", speakerName("युधिष्ठिर उवाच", Lang.HI))
        assertEquals("যুধিষ্ঠির বললেন", speakerName("युधिष्ठिर उवाच", Lang.BN))
        assertEquals("Prahrada said", speakerName("प्रह्राद उवाच", Lang.EN))
    }

    @Test fun joinedFormsAndPluralsAreRead() {
        assertEquals("The king said", speakerName("राजोवाच", Lang.EN))
        assertEquals("राजा ने कहा", speakerName("राजोवाच", Lang.HI))
        assertEquals("Brahma said", speakerName("ब्रह्मोवाच", Lang.EN))
        assertEquals("Bali said", speakerName("बलिरुवाच", Lang.EN))
        assertEquals("The gods said", speakerName("देवा ऊचुः", Lang.EN))
        assertEquals("देवता बोले", speakerName("देवा ऊचुः", Lang.HI))
        assertEquals("দেবগণ বললেন", speakerName("देवा ऊचुः", Lang.BN))
        assertEquals("गोपी ने कहा", speakerName("गोप्युवाच", Lang.HI))
    }

    @Test fun odiaReadersGetOdiaScript() {
        assertEquals("ମୈତ୍ରେୟ କହିଲେ", speakerName("मैत्रेय उवाच", Lang.OR))
        assertEquals("ଶ୍ରୀଶୁକଦେବ କହିଲେ", speakerName("श्रीशुक उवाच", Lang.OR))
        assertEquals("ବିଦୁର କହିଲେ", speakerName("विदुर उवाच", Lang.OR))
    }

    @Test fun textThatIsNotASpeakerLineStaysAsPrinted() {
        assertEquals("[अस्यार्थः]", speakerName("[अस्यार्थः]", Lang.EN))
        assertEquals("কেউ উবাচ", speakerName("কেউ উবাচ", Lang.EN))
        assertEquals("सूत उवाच", speakerName("सूत उवाच", Lang.SA))
    }
}
