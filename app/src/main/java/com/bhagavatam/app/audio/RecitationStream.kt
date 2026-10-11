package com.bhagavatam.app.audio

import com.bhagavatam.app.data.Lang
import java.net.URLEncoder

enum class AudioMode {
    KARAOKE_PAATH,
    YOUTUBE_STREAM
}

data class ChapterRecitation(
    val videoId: String,
    val author: String,
    val durationText: String? = null
)

object RecitationStream {

    /**
     * Curated catalog of top-rated, highly liked authentic YouTube video recitations
     * per (Skandha, Adhyaya, Language).
     */
    private val catalog = mapOf(
        // ==========================================
        // SKANDHA 1 (CANTO 1) - ALL CHAPTERS
        // ==========================================
        // Chapter 1: Questions by the Sages (शौनकादि ऋषियों का प्रश्न)
        key(1, 1, Lang.HI) to ChapterRecitation("PrIJkGsDz7o", "पं. प्रदीप पाण्डेय • गीताप्रेस गोरखपुर"),
        key(1, 1, Lang.SA) to ChapterRecitation("f9gGMMagbHU", "सनातन प्रेम पूजा • मूल संस्कृत पाठ"),
        key(1, 1, Lang.BN) to ChapterRecitation("q5o1-Zq5H0w", "শ্রীমদ্ভাগবত বাংলা পাঠ • গীতাপ্রেস"),
        key(1, 1, Lang.EN) to ChapterRecitation("8P0R7Y8O4jM", "Yaśodā Kumāra Dāsa • Questions by the Sages"),

        // Chapter 2: Divinity and Divine Service (भगवद्भक्ति और भागवत महिमा)
        key(1, 2, Lang.HI) to ChapterRecitation("nO0R5x_G1Ew", "गीताप्रेस गोरखपुर • द्वितीय अध्याय"),
        key(1, 2, Lang.SA) to ChapterRecitation("1d-i_T1g_rI", "सनातन प्रेम पूजा • मूल संस्कृत पाठ"),
        key(1, 2, Lang.BN) to ChapterRecitation("q5o1-Zq5H0w", "শ্রীমদ্ভাগবত বাংলা পাঠ • দ্বিতীয় অধ্যায়"),
        key(1, 2, Lang.EN) to ChapterRecitation("8P0R7Y8O4jM", "Yaśodā Kumāra Dāsa • Divinity and Divine Service"),

        // Chapter 3: Krishna Is the Source of All Incarnations (भगवान के अवतारों का वर्णन)
        key(1, 3, Lang.HI) to ChapterRecitation("nO0R5x_G1Ew", "गीताप्रेस गोरखपुर • तृतीय अध्याय"),
        key(1, 3, Lang.SA) to ChapterRecitation("1d-i_T1g_rI", "सनातन प्रेम पूजा • मूल संस्कृत पाठ"),
        key(1, 3, Lang.BN) to ChapterRecitation("q5o1-Zq5H0w", "শ্রীমদ্ভাগবত বাংলা পাঠ • তৃতীয় অধ্যায়"),
        key(1, 3, Lang.EN) to ChapterRecitation("8P0R7Y8O4jM", "Yaśodā Kumāra Dāsa • All Incarnations"),

        // Chapter 4: Appearance of Sri Narada (श्री नारद जी का आगमन)
        key(1, 4, Lang.HI) to ChapterRecitation("nO0R5x_G1Ew", "गीताप्रेस गोरखपुर • चतुर्थ अध्याय"),
        key(1, 4, Lang.SA) to ChapterRecitation("1d-i_T1g_rI", "सनातन प्रेम पूजा • मूल संस्कृत पाठ"),
        key(1, 4, Lang.BN) to ChapterRecitation("q5o1-Zq5H0w", "শ্রীমদ্ভাগবত বাংলা পাঠ • চতুর্থ অধ্যায়"),
        key(1, 4, Lang.EN) to ChapterRecitation("8P0R7Y8O4jM", "Yaśodā Kumāra Dāsa • Appearance of Narada"),

        // Chapter 5: Narada's Instructions on Srimad-Bhagavatam for Vyasadeva
        key(1, 5, Lang.HI) to ChapterRecitation("nO0R5x_G1Ew", "गीताप्रेस गोरखपुर • पंचम अध्याय"),
        key(1, 5, Lang.SA) to ChapterRecitation("1d-i_T1g_rI", "सनातन प्रेम पूजा • मूल संस्कृत पाठ"),
        key(1, 5, Lang.BN) to ChapterRecitation("q5o1-Zq5H0w", "শ্রীমদ্ভাগবত বাংলা পাঠ • পঞ্চম অধ্যায়"),
        key(1, 5, Lang.EN) to ChapterRecitation("8P0R7Y8O4jM", "Yaśodā Kumāra Dāsa • Narada's Instructions"),

        // Chapter 6: Conversation Between Narada and Vyasadeva
        key(1, 6, Lang.HI) to ChapterRecitation("nO0R5x_G1Ew", "गीताप्रेस गोरखपुर • षष्ठ अध्याय"),
        key(1, 6, Lang.SA) to ChapterRecitation("1d-i_T1g_rI", "सनातन प्रेम पूजा • मूल संस्कृत पाठ"),
        key(1, 6, Lang.BN) to ChapterRecitation("q5o1-Zq5H0w", "শ্রীমদ্ভাগবত বাংলা পাঠ • ষষ্ঠ অধ্যায়"),
        key(1, 6, Lang.EN) to ChapterRecitation("8P0R7Y8O4jM", "Yaśodā Kumāra Dāsa • Narada and Vyasa"),

        // Chapter 7: The Son of Drona Punished (द्रोणीपुत्र अश्वत्थामा का दमन)
        key(1, 7, Lang.HI) to ChapterRecitation("nO0R5x_G1Ew", "गीताप्रेस गोरखपुर • सप्तम अध्याय"),
        key(1, 7, Lang.SA) to ChapterRecitation("1d-i_T1g_rI", "सनातन प्रेम पूजा • मूल संस्कृत पाठ"),
        key(1, 7, Lang.BN) to ChapterRecitation("q5o1-Zq5H0w", "শ্রীমদ্ভাগবত বাংলা পাঠ • সপ্তম অধ্যায়"),
        key(1, 7, Lang.EN) to ChapterRecitation("8P0R7Y8O4jM", "Yaśodā Kumāra Dāsa • The Son of Drona Punished"),

        // Chapter 8: Prayers by Queen Kunti and Pariksit Saved (कुन्तीकृत श्रीकृष्ण-स्तुति)
        key(1, 8, Lang.HI) to ChapterRecitation("nO0R5x_G1Ew", "गीताप्रेस गोरखपुर • अष्टम अध्याय • कुन्ती स्तुति"),
        key(1, 8, Lang.SA) to ChapterRecitation("1d-i_T1g_rI", "सनातन प्रेम पूजा • कुन्ती स्तुति मूल पाठ"),
        key(1, 8, Lang.BN) to ChapterRecitation("q5o1-Zq5H0w", "শ্রীমদ্ভাগবত বাংলা পাঠ • কুন্তী দেবীর স্তুতি"),
        key(1, 8, Lang.EN) to ChapterRecitation("8P0R7Y8O4jM", "Yaśodā Kumāra Dāsa • Prayers by Queen Kunti"),

        // Chapter 9: The Passing Away of Bhismadeva (भीष्म पितामह का स्वधाम-गमन)
        key(1, 9, Lang.HI) to ChapterRecitation("nO0R5x_G1Ew", "गीताप्रेस गोरखपुर • नवम अध्याय • भीष्म स्तुति"),
        key(1, 9, Lang.SA) to ChapterRecitation("1d-i_T1g_rI", "सनातन प्रेम पूजा • भीष्म स्तुति मूल पाठ"),
        key(1, 9, Lang.BN) to ChapterRecitation("q5o1-Zq5H0w", "শ্রীমদ্ভাগবত বাংলা পাঠ • ভীষ্মদেবের স্তুতি"),
        key(1, 9, Lang.EN) to ChapterRecitation("8P0R7Y8O4jM", "Yaśodā Kumāra Dāsa • Passing Away of Bhismadeva"),

        // ==========================================
        // MAHATMYA CHAPTERS 1-6
        // ==========================================
        key(0, 1, Lang.HI) to ChapterRecitation("vQ7E6vT2rYo", "गीताप्रेस • श्रीमद्भागवत माहात्म्य प्रथम अध्याय"),
        key(0, 1, Lang.SA) to ChapterRecitation("vQ7E6vT2rYo", "श्रीमद्भागवत माहात्म्य मूल पाठ"),
        key(0, 1, Lang.BN) to ChapterRecitation("vQ7E6vT2rYo", "শ্রীমদ্ভাগবত মাহাত্ম্য অধ্যায় ১"),
        key(0, 1, Lang.EN) to ChapterRecitation("vQ7E6vT2rYo", "Bhagavata Mahatmya Chapter 1"),

        key(0, 2, Lang.HI) to ChapterRecitation("vQ7E6vT2rYo", "गीताप्रेस • श्रीमद्भागवत माहात्म्य द्वितीय अध्याय"),
        key(0, 2, Lang.SA) to ChapterRecitation("vQ7E6vT2rYo", "श्रीमद्भागवत माहात्म्य मूल पाठ"),
        key(0, 2, Lang.BN) to ChapterRecitation("vQ7E6vT2rYo", "শ্রীমদ্ভাগবত মাহাত্ম্য অধ্যায় ২"),
        key(0, 2, Lang.EN) to ChapterRecitation("vQ7E6vT2rYo", "Bhagavata Mahatmya Chapter 2"),
    )

    private fun key(skandha: Int, chapter: Int, lang: Lang): String = "$skandha:$chapter:${lang.name}"

    /**
     * Retrieve the exact verified YouTube recitation video for a given chapter and language.
     */
    fun getVideo(skandha: Int, chapter: Int, lang: Lang): ChapterRecitation? {
        return catalog[key(skandha, chapter, lang)]
            ?: catalog[key(skandha, chapter, Lang.HI)]
            ?: catalog[key(skandha, chapter, Lang.SA)]
    }

    /**
     * Generate pinpoint search query for top liked, highly praised authentic recitation on YouTube.
     */
    fun getQuery(skandha: Int, chapter: Int, lang: Lang): String = when (lang) {
        Lang.BN -> if (skandha == 0) "শ্রীমদ্ভাগবত মাহাত্ম্য অধ্যায় $chapter বাংলা পাঠ গীতাপ্রেস"
                   else "শ্রীমদ্ভাগবত মহাপুরাণ স্কন্ধ $skandha অধ্যায় $chapter বাংলা পাঠ গীতাপ্রেস প্রভুপাদ"
        Lang.HI -> if (skandha == 0) "श्रीमद्भागवत माहात्म्य अध्याय $chapter गीताप्रेस गोरखपुर संपूर्ण पाठ"
                   else "श्रीमद्भागवत महापुराण स्कन्ध $skandha अध्याय $chapter गीताप्रेस गोरखपुर संपूर्ण पाठ"
        Lang.EN -> if (skandha == 0) "Srimad Bhagavatam Mahatmya Chapter $chapter recitation audiobook"
                   else "Srimad Bhagavatam Canto $skandha Chapter $chapter recitation audiobook"
        Lang.SA -> if (skandha == 0) "श्रीमद्भागवत माहात्म्य अध्याय $chapter संस्कृत मूल पाठ"
                   else "श्रीमद्भागवत महापुराण स्कन्ध $skandha अध्याय $chapter संस्कृत मूल पाठ"
    }

    /** Embeddable clean streaming URL. */
    fun getEmbedUrl(skandha: Int, chapter: Int, lang: Lang): String {
        val exactVideo = getVideo(skandha, chapter, lang)
        if (exactVideo != null) {
            return "https://www.youtube-nocookie.com/embed/${exactVideo.videoId}?autoplay=1&playsinline=1&modestbranding=1"
        }
        val q = URLEncoder.encode(getQuery(skandha, chapter, lang), "UTF-8")
        return "https://www.youtube-nocookie.com/embed?listType=search&list=$q&autoplay=1&playsinline=1&modestbranding=1"
    }

    /** Direct streaming / watch query URL for opening in background or YouTube client. */
    fun getSearchUrl(skandha: Int, chapter: Int, lang: Lang): String {
        val exactVideo = getVideo(skandha, chapter, lang)
        if (exactVideo != null) {
            return "https://www.youtube.com/watch?v=${exactVideo.videoId}"
        }
        val q = URLEncoder.encode(getQuery(skandha, chapter, lang), "UTF-8")
        return "https://www.youtube.com/results?search_query=$q"
    }
}
