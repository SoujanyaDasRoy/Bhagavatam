package com.bhagavatam.app.data

/** Well-loved episodes, each pointing at the chapter where it begins. Titles are plain names, not scripture text. */
data class Episode(val s: Int, val a: Int, val en: String, val hi: String, val bn: String) {
    fun title(l: Lang) = when (l) { Lang.HI -> hi; Lang.BN -> bn; else -> en }
}

object Episodes {
    val all = listOf(
        Episode(10, 29, "Rasa Lila", "रास लीला", "রাসলীলা"),
        Episode(8, 2, "Gajendra's prayer", "गजेन्द्र मोक्ष", "গজেন্দ্র মোক্ষ"),
        Episode(7, 8, "Narasimha", "नृसिंह अवतार", "নৃসিংহ অবতার"),
        Episode(4, 8, "Dhruva", "ध्रुव चरित्र", "ধ্রুব চরিত্র"),
        Episode(10, 3, "Krishna's birth", "श्रीकृष्ण जन्म", "শ্রীকৃষ্ণের জন্ম"),
        Episode(8, 6, "Churning of the ocean", "समुद्र मन्थन", "সমুদ্রমন্থন"),
        Episode(10, 25, "Govardhan", "गोवर्धन लीला", "গোবর্ধন লীলা"),
        Episode(1, 8, "Kunti's prayers", "कुन्ती स्तुति", "কুন্তীর স্তুতি"),
        Episode(6, 1, "Ajamila", "अजामिल", "অজামিল"),
        Episode(8, 18, "Vamana", "वामन अवतार", "বামন অবতার"),
        Episode(10, 80, "Sudama", "सुदामा चरित्र", "সুদামা চরিত্র"),
        Episode(11, 7, "Uddhava's teaching", "उद्धव उपदेश", "উদ্ধব উপদেশ"),
    )
}

fun tr(l: Lang, en: String, hi: String, bn: String) = when (l) { Lang.HI -> hi; Lang.BN -> bn; else -> en }
