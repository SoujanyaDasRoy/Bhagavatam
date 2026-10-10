package com.bhagavatam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class SearchIndexTest {

    private fun encodeVarints(diffs: List<Int>): ByteArray {
        val out = ByteArrayOutputStream()
        for (diff in diffs) {
            var n = diff
            while (n > 0x7F) {
                out.write((n and 0x7F) or 0x80)
                n = n ushr 7
            }
            out.write(n)
        }
        return out.toByteArray()
    }

    private fun encodePosting(ids: List<Int>): ByteArray {
        val sorted = ids.sorted()
        val diffs = ArrayList<Int>()
        var prev = 0
        for (id in sorted) {
            diffs.add(id - prev)
            prev = id
        }
        return encodeVarints(diffs)
    }

    @Test
    fun testDecodePosting() {
        // 1. Empty posting
        assertEquals(emptyList<Int>(), SearchIndex.decodePosting(ByteArray(0)))

        // 2. Single ID
        val single = encodePosting(listOf(42))
        assertEquals(listOf(42), SearchIndex.decodePosting(single))

        // 3. Small sequence
        val small = encodePosting(listOf(1, 5, 20, 25))
        assertEquals(listOf(1, 5, 20, 25), SearchIndex.decodePosting(small))

        // 4. Large IDs spanning multi-byte varints
        val large = encodePosting(listOf(1, 150, 2083, 14000))
        assertEquals(listOf(1, 150, 2083, 14000), SearchIndex.decodePosting(large))
    }

    @Test
    fun testEditsLe1() {
        assertTrue(SearchIndex.editsLe1("krishna", "krishna"))
        assertTrue(SearchIndex.editsLe1("krshna", "krishna")) // deletion
        assertTrue(SearchIndex.editsLe1("krishna", "krshna")) // insertion
        assertTrue(SearchIndex.editsLe1("arjna", "arjuna"))   // insertion
        assertTrue(SearchIndex.editsLe1("krishna", "krishno")) // substitution
        assertTrue(SearchIndex.editsLe1("a", "ab"))
        assertTrue(SearchIndex.editsLe1("ab", "a"))

        assertFalse(SearchIndex.editsLe1("krshn", "krishna")) // 2 deletions
        assertFalse(SearchIndex.editsLe1("krishna", "k"))
        assertFalse(SearchIndex.editsLe1("abc", "xyz"))
    }

    @Test
    fun testNormaliseAndSkey() {
        assertEquals("कृष्ण", Normalise.toDevanagari("কৃষ্ণ"))
        assertEquals("भगबान", Normalise.toDevanagari("ভগবান"))
        assertEquals("भगबान", Normalise.toDevanagari("ଭଗବାନ"))

        assertEquals("मगर", Normalise.exactOf("मगर"))
        assertEquals("krishna", Normalise.exactOf("Krishna"))
        assertEquals("god", Normalise.skey("God"))
        assertEquals("भगबान", Normalise.skey("ভগবান"))
    }

    @Test
    fun testLemmatizerStems() {
        val bnStems = Lemmatizer.stems("ভগবানের", "bn")
        assertTrue(bnStems.contains("ভগবান"))

        val hiStems = Lemmatizer.stems("राजाओं", "hi")
        assertTrue(hiStems.contains("राजा"))

        val enStems = Lemmatizer.stems("kings", "en")
        assertTrue(enStems.contains("king"))
    }

    @Test
    fun testStoryChaptersFromEpisodes() {
        // Rasa Lila occurs in Skandha 10, Chapter 29
        val rasaChapters = SearchIndex.storyChapters("Rasa Lila")
        assertTrue("Rasa Lila should link to chapter 10.29", rasaChapters.contains(10 to 29))

        // Gajendra occurs in Skandha 8, Chapter 2
        val gajendraChapters = SearchIndex.storyChapters("Gajendra")
        assertTrue("Gajendra should link to chapter 8.2", gajendraChapters.contains(8 to 2))
    }
}
