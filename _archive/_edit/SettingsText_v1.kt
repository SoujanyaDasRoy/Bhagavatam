package com.bhagavatam.app.data

/** Labels for the Settings screen, in the three app languages. */
data class SettingsText(
    val appearance: String, val matchPhone: String, val matchPhoneNote: String, val lightLook: String, val darkLook: String,
    val look: String, val accentColour: String, val accents: List<String>,
    val textSize: String, val sample: String, val lineSpacing: String, val spacings: List<String>, val showDaily: String, val homeSec: String,
    val readingSec: String, val listeningSec: String, val librarySec: String, val aboutSec: String,
    val textsFrom: String, val version: String, val contentVersion: String,
)

val SettingsEn = SettingsText(
    appearance = "Appearance", matchPhone = "Match phone", matchPhoneNote = "Switch between a light and a dark look with your phone's setting.",
    lightLook = "Light look", darkLook = "Dark look", look = "Look", accentColour = "Accent colour",
    accents = listOf("Saffron", "Teal", "Indigo", "Rose"),
    textSize = "Text size", sample = "The Lord is the source of all that exists.", lineSpacing = "Line spacing",
    spacings = listOf("Compact", "Normal", "Airy"), showDaily = "Show shloka of the day", homeSec = "Home",
    readingSec = "Reading", listeningSec = "Listening", librarySec = "Library", aboutSec = "About",
    textsFrom = "Texts from", version = "App version", contentVersion = "Text database",
)
val SettingsHi = SettingsText(
    appearance = "रूप-रंग", matchPhone = "फ़ोन के अनुसार", matchPhoneNote = "फ़ोन की सेटिंग के अनुसार हल्का या गहरा रूप अपने-आप बदलेगा।",
    lightLook = "हल्का रूप", darkLook = "गहरा रूप", look = "रूप", accentColour = "मुख्य रंग",
    accents = listOf("केसरी", "फ़िरोज़ी", "नीला", "गुलाबी"),
    textSize = "अक्षर का आकार", sample = "भगवान ही सबके मूल कारण हैं।", lineSpacing = "पंक्तियों की दूरी",
    spacings = listOf("कम", "सामान्य", "अधिक"), showDaily = "आज का श्लोक दिखाएँ", homeSec = "होम",
    readingSec = "पठन", listeningSec = "श्रवण", librarySec = "संग्रह", aboutSec = "जानकारी",
    textsFrom = "पाठ का स्रोत", version = "ऐप संस्करण", contentVersion = "पाठ डेटाबेस",
)
val SettingsBn = SettingsText(
    appearance = "চেহারা", matchPhone = "ফোনের সঙ্গে মিলিয়ে", matchPhoneNote = "ফোনের সেটিং অনুযায়ী হালকা বা গাঢ় রূপ নিজে থেকেই বদলাবে।",
    lightLook = "হালকা রূপ", darkLook = "গাঢ় রূপ", look = "রূপ", accentColour = "মূল রং",
    accents = listOf("গেরুয়া", "ফিরোজা", "নীল", "গোলাপি"),
    textSize = "লেখার আকার", sample = "ভগবানই সকল কিছুর মূল কারণ।", lineSpacing = "লাইনের ফাঁক",
    spacings = listOf("কম", "স্বাভাবিক", "বেশি"), showDaily = "আজকের শ্লোক দেখান", homeSec = "হোম",
    readingSec = "পাঠ", listeningSec = "শ্রবণ", librarySec = "সংগ্রহ", aboutSec = "পরিচিতি",
    textsFrom = "পাঠের উৎস", version = "অ্যাপের সংস্করণ", contentVersion = "পাঠ ডেটাবেস",
)
fun settingsTextFor(l: Lang) = when (l) { Lang.HI -> SettingsHi; Lang.BN -> SettingsBn; else -> SettingsEn }
