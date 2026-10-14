package com.bhagavatam.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.bhagavatam.app.ui.components.skGradient
import com.bhagavatam.app.ui.theme.AppDark
import com.bhagavatam.app.ui.theme.AppLight
import com.bhagavatam.app.ui.theme.AppPothi
import com.bhagavatam.app.ui.theme.AppRatri
import com.bhagavatam.app.ui.theme.MahatmyaHue
import com.bhagavatam.app.ui.theme.SkandhaHues
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ContrastTest {
    private fun ratio(a: Color, b: Color): Float {
        val (hi, lo) = if (a.luminance() > b.luminance()) a to b else b to a
        return (hi.luminance() + 0.05f) / (lo.luminance() + 0.05f)
    }

    @Test
    fun tileGradientsHoldCreamText() {
        val cream = Color(0xFFFFF4DC)
        for (hue in SkandhaHues + MahatmyaHue) for (stop in skGradient(hue)) {
            assertTrue("cream text on $stop is ${ratio(cream, stop)}", ratio(cream, stop) >= 4.5f)
        }
    }

    @Test
    fun textTokensReach4_5() {
        for ((name, c) in listOf("light" to AppLight, "dark" to AppDark, "ratri" to AppRatri, "pothi" to AppPothi)) {
            assertTrue("$name ink", ratio(c.ink, c.bg) >= 4.5f)
            assertTrue("$name secondary", ratio(c.inkSecondary, c.bg) >= 4.5f)
            assertTrue("$name secondary on surface", ratio(c.inkSecondary, c.surface) >= 4.5f)
            assertTrue("$name accent", ratio(c.accent, c.bg) >= 4.5f)
            assertTrue("$name gold", ratio(c.gold, c.bg) >= 4.5f)
            assertTrue("$name button text", ratio(c.onAccent, c.accent) >= 4.5f)
        }
    }

    /** No new text under 12 sp: Apple's floor is 11 pt and the app's Indic floor is 15 sp. */
    @Test
    fun noTinyText() {
        val bad = mutableListOf<String>()
        File("src/main/java").walkTopDown().filter { it.extension == "kt" }.forEach { f ->
            f.readLines().forEachIndexed { i, l ->
                if (Regex("fontSize = (9|10|11)(\\.[0-9])?\\.sp").containsMatchIn(l)) bad += "${f.name}:${i + 1}"
            }
        }
        assertTrue("text under 12 sp at $bad", bad.isEmpty())
    }
}
