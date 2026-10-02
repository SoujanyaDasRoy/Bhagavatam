package com.bhagavatam.app.data

/** Labels for the Settings screen, in the three app languages. */
data class SettingsText(
    val appearance: String, val matchPhone: String, val matchPhoneNote: String, val lightLook: String, val darkLook: String,
    val look: String,
    val textSize: String, val sample: String, val lineSpacing: String, val spacings: List<String>, val showDaily: String, val homeSec: String,
    val readingSec: String, val listeningSec: String, val librarySec: String, val aboutSec: String,
    val textsFrom: String, val version: String, val contentVersion: String,
    val theme: String, val modes: List<String>, val preferences: String, val pinchHint: String,
    // Your data
    val dataSec: String, val clearProgress: String, val clearProgressMsg: String, val clearSearches: String, val clearSearchesMsg: String,
    val clearSaved: String, val clearSavedMsg: String, val resetSettings: String, val resetSettingsMsg: String,
    val cancel: String, val doClear: String, val doRemove: String, val doReset: String,
    val nothingSaved: String, val savedHint: String,
)

val SettingsEn = SettingsText(
    appearance = "Appearance", matchPhone = "Match phone", matchPhoneNote = "Switch between a light and a dark look with your phone's setting.",
    lightLook = "Light look", darkLook = "Dark look", look = "Look",
    textSize = "Text size", sample = "The Lord is the source of all that exists.", lineSpacing = "Line spacing",
    spacings = listOf("Compact", "Normal", "Airy"), showDaily = "Show shloka of the day", homeSec = "Home",
    readingSec = "Reading", listeningSec = "Listening", librarySec = "Library", aboutSec = "About",
    textsFrom = "Texts from", version = "App version", contentVersion = "Text database",
    theme = "Theme", modes = listOf("System", "Light", "Dark"), preferences = "Preferences",
    pinchHint = "In the reader, pinch to resize the text and swipe sideways to change chapter.",
    dataSec = "Your data", clearProgress = "Clear reading progress",
    clearProgressMsg = "Finished chapters and your place in the text will be reset. Saved verses stay.",
    clearSearches = "Clear recent searches", clearSearchesMsg = "Your search history will be removed.",
    clearSaved = "Remove all saved verses", clearSavedMsg = "All bookmarks and highlights will be removed.",
    resetSettings = "Reset settings", resetSettingsMsg = "Theme, text size and listening options go back to their defaults. Languages and your progress are kept.",
    cancel = "Cancel", doClear = "Clear", doRemove = "Remove", doReset = "Reset",
    nothingSaved = "Nothing saved yet", savedHint = "Press and hold a shloka in the reader to bookmark it.",
)
val SettingsHi = SettingsText(
    appearance = "रूप-रंग", matchPhone = "फ़ोन के अनुसार", matchPhoneNote = "फ़ोन की सेटिंग के अनुसार हल्का या गहरा रूप अपने-आप बदलेगा।",
    lightLook = "हल्का रूप", darkLook = "गहरा रूप", look = "रूप",
    textSize = "अक्षर का आकार", sample = "भगवान ही सबके मूल कारण हैं।", lineSpacing = "पंक्तियों की दूरी",
    spacings = listOf("कम", "सामान्य", "अधिक"), showDaily = "आज का श्लोक दिखाएँ", homeSec = "होम",
    readingSec = "पठन", listeningSec = "श्रवण", librarySec = "संग्रह", aboutSec = "जानकारी",
    textsFrom = "पाठ का स्रोत", version = "ऐप संस्करण", contentVersion = "पाठ डेटाबेस",
    theme = "थीम", modes = listOf("फ़ोन के अनुसार", "हल्का", "गहरा"), preferences = "पसंद",
    pinchHint = "रीडर में दो उँगलियों से पिंच करके अक्षर का आकार बदलें और बगल में स्वाइप करके अध्याय बदलें।",
    dataSec = "आपका डेटा", clearProgress = "पठन प्रगति मिटाएँ",
    clearProgressMsg = "पूरे किए अध्याय और पढ़ने की आपकी जगह मिट जाएगी। सहेजे गए श्लोक बने रहेंगे।",
    clearSearches = "हाल की खोज मिटाएँ", clearSearchesMsg = "आपकी खोज का इतिहास हट जाएगा।",
    clearSaved = "सभी सहेजे श्लोक हटाएँ", clearSavedMsg = "सभी बुकमार्क और हाइलाइट हट जाएँगे।",
    resetSettings = "सेटिंग्स रीसेट करें", resetSettingsMsg = "थीम, अक्षर का आकार और श्रवण विकल्प शुरुआती स्थिति में लौट आएँगे। भाषाएँ और प्रगति बनी रहेंगी।",
    cancel = "रद्द करें", doClear = "मिटाएँ", doRemove = "हटाएँ", doReset = "रीसेट करें",
    nothingSaved = "अभी कुछ सहेजा नहीं गया", savedHint = "रीडर में किसी श्लोक को दबाए रखें, वह सहेज लिया जाएगा।",
)
val SettingsBn = SettingsText(
    appearance = "চেহারা", matchPhone = "ফোনের সঙ্গে মিলিয়ে", matchPhoneNote = "ফোনের সেটিং অনুযায়ী হালকা বা গাঢ় রূপ নিজে থেকেই বদলাবে।",
    lightLook = "হালকা রূপ", darkLook = "গাঢ় রূপ", look = "রূপ",
    textSize = "লেখার আকার", sample = "ভগবানই সকল কিছুর মূল কারণ।", lineSpacing = "লাইনের ফাঁক",
    spacings = listOf("কম", "স্বাভাবিক", "বেশি"), showDaily = "আজকের শ্লোক দেখান", homeSec = "হোম",
    readingSec = "পাঠ", listeningSec = "শ্রবণ", librarySec = "সংগ্রহ", aboutSec = "পরিচিতি",
    textsFrom = "পাঠের উৎস", version = "অ্যাপের সংস্করণ", contentVersion = "পাঠ ডেটাবেস",
    theme = "থিম", modes = listOf("ফোন অনুযায়ী", "হালকা", "গাঢ়"), preferences = "পছন্দ",
    pinchHint = "পাঠে দুই আঙুলে পিঞ্চ করে লেখার আকার বদলান আর পাশে সোয়াইপ করে অধ্যায় বদলান।",
    dataSec = "আপনার ডেটা", clearProgress = "পাঠের অগ্রগতি মুছুন",
    clearProgressMsg = "শেষ করা অধ্যায় আর পড়ার আপনার জায়গা মুছে যাবে। সংরক্ষিত শ্লোক থাকবে।",
    clearSearches = "সাম্প্রতিক খোঁজা মুছুন", clearSearchesMsg = "আপনার খোঁজার ইতিহাস সরে যাবে।",
    clearSaved = "সব সংরক্ষিত শ্লোক সরান", clearSavedMsg = "সব বুকমার্ক আর হাইলাইট সরে যাবে।",
    resetSettings = "সেটিংস রিসেট করুন", resetSettingsMsg = "থিম, লেখার আকার আর শ্রবণ বিকল্প আগের অবস্থায় ফিরবে। ভাষা আর অগ্রগতি থাকবে।",
    cancel = "বাতিল", doClear = "মুছুন", doRemove = "সরান", doReset = "রিসেট করুন",
    nothingSaved = "এখনও কিছু সংরক্ষিত নেই", savedHint = "পাঠে কোনো শ্লোক চেপে ধরে রাখলে সেটি সংরক্ষিত হবে।",
)
fun settingsTextFor(l: Lang) = when (l) { Lang.HI -> SettingsHi; Lang.BN -> SettingsBn; else -> SettingsEn }
