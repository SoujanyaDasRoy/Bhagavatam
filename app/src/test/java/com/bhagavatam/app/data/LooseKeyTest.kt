package com.bhagavatam.app.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LooseKeyTest {

    @Test
    fun testAllPairsFromLoosePairsJson() {
        val candidates = listOf(
            File("content/_search/tests/loose_pairs.json"),
            File("../content/_search/tests/loose_pairs.json"),
            File("../../content/_search/tests/loose_pairs.json"),
            File("app/src/test/resources/loose_pairs.json")
        )
        val file = candidates.firstOrNull { it.exists() }
        val jsonText = file?.readText() ?: javaClass.classLoader?.getResourceAsStream("loose_pairs.json")?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalStateException("Could not find loose_pairs.json")

        val rx = Regex("""\{\s*"input":\s*"([^"]*)",\s*"fn":\s*"([^"]*)",\s*"key":\s*"([^"]*)"""")
        val matches = rx.findAll(jsonText).toList()
        assertTrue("Expected non-empty pairs list, found ${matches.size}", matches.isNotEmpty())

        var count = 0
        for (m in matches) {
            val input = m.groupValues[1]
            val fn = m.groupValues[2]
            val expectedKey = m.groupValues[3]

            val actual = if (fn == "indic") {
                LooseKey.looseFromIndic(input)
            } else {
                LooseKey.looseFromRoman(input)
            }
            assertEquals("Mismatch for input='$input' (fn=$fn)", expectedKey, actual)

            // Also verify keyOf matches
            val actualKeyOf = LooseKey.keyOf(input)
            assertEquals("Mismatch in keyOf for input='$input'", expectedKey, actualKeyOf)
            count++
        }
        println("Verified $count pairs with LooseKey: 100% parity with Python.")
    }

    @Test
    fun testMustMatchCrossScript() {
        // Names that must yield identical keys across scripts
        val pairs = listOf(
            "krishna" to "कृष्ण",
            "krishna" to "কৃষ্ণ",
            "yudhishthira" to "युधिष्ठिर",
            "yudhishthira" to "যুধিষ্ঠির",
            "narada" to "नारद",
            "shri" to "श्री",
            "rishi" to "ऋषि",
            "bhagavan" to "भगवान",
            "bhagavan" to "ভগবান"
        )
        for ((roman, indic) in pairs) {
            val kRoman = LooseKey.keyOf(roman)
            val kIndic = LooseKey.keyOf(indic)
            assertEquals("Roman '$roman' ($kRoman) and Indic '$indic' ($kIndic) must match", kRoman, kIndic)
        }
    }

    @Test
    fun testMustNotMatch() {
        val nonPairs = listOf(
            "krishna" to "आकर्षण",
            "narada" to "निरोध",
            "kunti" to "कान्त"
        )
        for ((roman, indic) in nonPairs) {
            val kRoman = LooseKey.keyOf(roman)
            val kIndic = LooseKey.keyOf(indic)
            assertTrue("'$roman' and '$indic' should NOT have the same key: got $kRoman", kRoman != kIndic)
        }
    }
}
