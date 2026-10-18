package com.bhagavatam.app.data

/*
 * Checks for text that came out of the page scans wrong, found with tools/qa/scan_text_quality.py.
 * A reader should see "not available" rather than a line of stray signs, and a chapter title should say what the chapter is about.
 */

private val ODIA_NEVER = Regex("[§୰୲-୷]")
private val PAGE_SYMBOL_RUN = Regex("॥\\s*॥\\s*॥|॥॥")
private val WHITESPACE = Regex("\\s+")

/** True for an Odia translation that is page symbols or half-read letters: mostly doubtful words, runs of ॥, or signs Odia never uses. */
fun odiaLooksGarbled(text: String): Boolean {
    if (text.isBlank()) return false
    val marks = text.split("[?]").size - 1
    val words = text.split(WHITESPACE).count { it.isNotBlank() }
    if (marks >= 3 && words > 0 && marks.toDouble() / words > 0.2) return true
    if (ODIA_NEVER.containsMatchIn(text)) return true
    if (PAGE_SYMBOL_RUN.containsMatchIn(text)) {
        val letters = text.count { it in 'ଅ'..'ହ' }
        return letters < text.length * 0.3
    }
    return false
}

private val ORDINALS = listOf(
    "प्रथम", "द्वितीय", "तृतीय", "चतुर्थ", "पञ्चम", "पंचम", "षष्ठ", "सप्तम", "अष्टम", "नवम", "दशम", "एकादश", "द्वादश", "त्रयोदश", "चतुर्दश", "पञ्चदश", "षोडश", "सप्तदश", "अष्टादश", "एकोनविंश", "विंश",
    "প্রথম", "দ্বিতীয়", "তৃতীয়", "চতুর্থ", "পঞ্চম", "ষষ্ঠ", "সপ্তম", "অষ্টম", "নবম", "দশম", "একাদশ", "দ্বাদশ", "ত্রয়োদশ", "চতুর্দশ", "পঞ্চদশ", "ষোড়শ", "সপ্তদশ", "অষ্টাদশ",
    "ପ୍ରଥମ", "ଦ୍ୱିତୀୟ", "ତୃତୀୟ", "ଚତୁର୍ଥ", "ପଞ୍ଚମ", "ଷଷ୍ଠ", "ସପ୍ତମ", "ଅଷ୍ଟମ", "ନବମ", "ଦଶମ", "ଏକାଦଶ", "ଦ୍ୱାଦଶ",
)
private val ADHYAYA = listOf("अध्याय", "অধ্যায়", "ଅଧ୍ଯାୟ", "ଅଧ୍ୟାୟ")
private val ORDINAL_FRONT = Regex("^(?:${ORDINALS.joinToString("|")})\\s+(?:${ADHYAYA.joinToString("|")})\\s*[-:.।]*\\s*")

/** The title with a leading "fifth chapter" removed; empty when nothing else is left (the book printed only the number), or when it is only brackets. */
fun cleanChapterTitle(title: String): String {
    // the same letter is stored as one code point (য় U+09DF) or two (U+09AF U+09BC); NFC makes them one form
    var t = java.text.Normalizer.normalize(title, java.text.Normalizer.Form.NFC).trim()
    if (t.length >= 2 && (t.first() == '(' && t.last() == ')' || t.first() == '[' && t.last() == ']')) t = t.substring(1, t.length - 1).trim()
    return ORDINAL_FRONT.replaceFirst(t, "").trim()
}
