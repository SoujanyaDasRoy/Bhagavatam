package com.bhagavatam.app.data

/** Words used by the player and the voice sheet, in the three app languages. */
data class PlayerText(
    // states
    val textOnly: String,
    val preparing: String, val paused: String, val pausedHint: String, val ended: String, val replay: String, val nextChapter: String,
    val sanskritViaHindi: String,
    val minutesLeft: (Int) -> String, val hoursLeft: (Int, Int) -> String, val lessThanMinute: String,
    // problems
    val noVoice: (String) -> String, val noEngine: String, val voiceStopped: String, val installVoice: String,
    // control labels
    val closePlayer: String, val prevShloka: String, val nextShloka: String, val play: String, val pause: String,
    val voiceSettings: String, val position: (Int, Int) -> String, val playFromHere: String,
    // voice sheet
    val voiceTitle: String, val voiceWord: String, val pausesTitle: String, val pauseOptions: List<String>, val installMore: String,
    val tapToHear: String, val noVoicesFor: String, val automatic: String, val offline: String, val online: String, val qualities: List<String>,
)

val PlayerEn = PlayerText(
    textOnly = "No sound, text only",
    preparing = "Preparing the voice", paused = "Paused", pausedHint = "Paused. Tap a sentence to play from there.",
    ended = "End of chapter", replay = "Play again", nextChapter = "Next chapter",
    sanskritViaHindi = "Sanskrit, read with the Hindi voice",
    minutesLeft = { "about $it min left" }, hoursLeft = { h, m -> "about $h h $m min left" }, lessThanMinute = "less than a minute left",
    noVoice = { "No $it voice is installed. The text follows along without sound." },
    noEngine = "This phone has no speech engine. The text follows along without sound.",
    voiceStopped = "The voice stopped working.", installVoice = "Install voice",
    closePlayer = "Close player", prevShloka = "Previous shloka", nextShloka = "Next shloka", play = "Play", pause = "Pause",
    voiceSettings = "Voice and pauses", position = { a, b -> "Shloka $a of $b" }, playFromHere = "Play from here",
    voiceTitle = "Narration voice", voiceWord = "Voice", pausesTitle = "Pauses between sentences", pauseOptions = listOf("Shorter", "Normal", "Longer"),
    installMore = "Install more voices", tapToHear = "Tap a voice to hear it.", noVoicesFor = "No voices are installed for this language.",
    automatic = "Automatic (best available)", offline = "works offline", online = "needs internet",
    qualities = listOf("Basic", "Standard", "High quality", "Highest quality"),
)

val PlayerHi = PlayerText(
    textOnly = "बिना आवाज़, केवल पाठ",
    preparing = "आवाज़ तैयार हो रही है", paused = "रुका हुआ", pausedHint = "रुका हुआ। किसी वाक्य को छूकर वहीं से सुनें।",
    ended = "अध्याय पूरा हुआ", replay = "फिर से सुनें", nextChapter = "अगला अध्याय",
    sanskritViaHindi = "संस्कृत, हिन्दी आवाज़ में",
    minutesLeft = { "लगभग $it मिनट शेष" }, hoursLeft = { h, m -> "लगभग $h घंटा $m मिनट शेष" }, lessThanMinute = "एक मिनट से कम शेष",
    noVoice = { "$it की आवाज़ इंस्टॉल नहीं है। पाठ बिना आवाज़ के आगे बढ़ेगा।" },
    noEngine = "इस फ़ोन में बोलकर पढ़ने वाला इंजन नहीं है। पाठ बिना आवाज़ के आगे बढ़ेगा।",
    voiceStopped = "आवाज़ बीच में रुक गई।", installVoice = "आवाज़ इंस्टॉल करें",
    closePlayer = "प्लेयर बंद करें", prevShloka = "पिछला श्लोक", nextShloka = "अगला श्लोक", play = "चलाएँ", pause = "रोकें",
    voiceSettings = "आवाज़ और ठहराव", position = { a, b -> "श्लोक $a / $b" }, playFromHere = "यहाँ से सुनें",
    voiceTitle = "वाचन की आवाज़", voiceWord = "आवाज़", pausesTitle = "वाक्यों के बीच ठहराव", pauseOptions = listOf("कम", "सामान्य", "अधिक"),
    installMore = "और आवाज़ें जोड़ें", tapToHear = "सुनने के लिए किसी आवाज़ को छुएँ।", noVoicesFor = "इस भाषा की कोई आवाज़ इंस्टॉल नहीं है।",
    automatic = "अपने-आप (सबसे अच्छी उपलब्ध)", offline = "बिना इंटरनेट", online = "इंटरनेट चाहिए",
    qualities = listOf("सामान्य", "मानक", "उच्च गुणवत्ता", "सर्वोच्च गुणवत्ता"),
)

val PlayerBn = PlayerText(
    textOnly = "শব্দ ছাড়া, শুধু লেখা",
    preparing = "কণ্ঠ তৈরি হচ্ছে", paused = "বিরতি", pausedHint = "বিরতি। কোনো বাক্যে ছুঁয়ে সেখান থেকে শুনুন।",
    ended = "অধ্যায় শেষ", replay = "আবার শুনুন", nextChapter = "পরের অধ্যায়",
    sanskritViaHindi = "সংস্কৃত, হিন্দি কণ্ঠে",
    minutesLeft = { "প্রায় $it মিনিট বাকি" }, hoursLeft = { h, m -> "প্রায় $h ঘণ্টা $m মিনিট বাকি" }, lessThanMinute = "এক মিনিটের কম বাকি",
    noVoice = { "$it কণ্ঠ ইনস্টল করা নেই। লেখা শব্দ ছাড়াই এগোবে।" },
    noEngine = "এই ফোনে পড়ে শোনানোর ইঞ্জিন নেই। লেখা শব্দ ছাড়াই এগোবে।",
    voiceStopped = "কণ্ঠ মাঝপথে থেমে গেছে।", installVoice = "কণ্ঠ ইনস্টল করুন",
    closePlayer = "প্লেয়ার বন্ধ করুন", prevShloka = "আগের শ্লোক", nextShloka = "পরের শ্লোক", play = "চালান", pause = "থামান",
    voiceSettings = "কণ্ঠ ও বিরতি", position = { a, b -> "শ্লোক $a / $b" }, playFromHere = "এখান থেকে শুনুন",
    voiceTitle = "পাঠের কণ্ঠ", voiceWord = "কণ্ঠ", pausesTitle = "বাক্যের মাঝে বিরতি", pauseOptions = listOf("কম", "স্বাভাবিক", "বেশি"),
    installMore = "আরও কণ্ঠ যোগ করুন", tapToHear = "শুনতে কোনো কণ্ঠে ছুঁয়ে দিন।", noVoicesFor = "এই ভাষার কোনো কণ্ঠ ইনস্টল করা নেই।",
    automatic = "নিজে থেকে (সবচেয়ে ভালোটি)", offline = "ইন্টারনেট ছাড়াই চলে", online = "ইন্টারনেট লাগবে",
    qualities = listOf("সাধারণ", "মানসম্মত", "উচ্চ মান", "সর্বোচ্চ মান"),
)

fun playerTextFor(l: Lang) = when (l) { Lang.HI -> PlayerHi; Lang.BN -> PlayerBn; else -> PlayerEn }
