package com.bhagavatam.app.data

/** A curated divine incident or narrative episode in the Bhagavatam. */
data class Episode(
    val s: Int,
    val a: Int,
    val en: String,
    val hi: String,
    val bn: String,
    val category: EpisodeCategory = EpisodeCategory.LILA,
    val keywords: List<String> = emptyList(),
    val descriptionEn: String = "",
    val descriptionHi: String = "",
    val descriptionBn: String = "",
) {
    fun title(l: Lang) = when (l) { Lang.HI -> hi; Lang.BN -> bn; else -> en }
    fun description(l: Lang) = when (l) { Lang.HI -> descriptionHi; Lang.BN -> descriptionBn; else -> descriptionEn }

    fun matchesQuery(query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return false
        if (en.lowercase().contains(q) || hi.lowercase().contains(q) || bn.lowercase().contains(q)) return true
        if (keywords.any { it.lowercase().contains(q) }) return true
        if (descriptionEn.lowercase().contains(q) || descriptionHi.lowercase().contains(q) || descriptionBn.lowercase().contains(q)) return true
        return false
    }
}

enum class EpisodeCategory(val en: String, val hi: String, val bn: String) {
    ALL("All", "सभी", "সব"),
    AVATAR("Avatars", "अवतार", "অবতার"),
    BHAKTA("Great Devotees", "महाभागवत", "মহাভাগবত"),
    LILA("Divine Lilas", "दिव्य लीला", "মধুর লীলা"),
    TEACHING("Teachings", "ज्ञानोपदेश", "তত্ত্বোপদেশ");

    fun label(l: Lang) = when (l) { Lang.HI -> hi; Lang.BN -> bn; else -> en }
}

object Episodes {
    val all = listOf(
        Episode(
            s = 10, a = 29, en = "Rasa Lila", hi = "रास लीला", bn = "রাসলীলা",
            category = EpisodeCategory.LILA,
            keywords = listOf("gopi", "gopis", "sharad", "flute", "muralidhar", "dance", "vrindavan", "প্রেম", "বাঁশি", "গোপী"),
            descriptionEn = "Krishna's transcendental autumn dance with the Gopis of Vrindavan.",
            descriptionHi = "वृन्दावन में शरद पूर्णिमा की रात्रि में भगवान श्रीकृष्ण की महारास लीला।",
            descriptionBn = "শ্রীকৃষ্ণের মধুর বংশীধ্বনি ও ব্রজগোপীদের সঙ্গে অপ্রাকৃত শারদীয় রাসলীলা।"
        ),
        Episode(
            s = 8, a = 2, en = "Gajendra Moksha", hi = "गजेन्द्र मोक्ष", bn = "গজেন্দ্র মোক্ষ",
            category = EpisodeCategory.BHAKTA,
            keywords = listOf("elephant", "crocodile", "makara", "lotus", "surrender", "sharanagati", "হাতি", "কুমির", "পদ্ম"),
            descriptionEn = "The King of Elephants offers a lotus in total surrender and achieves liberation.",
            descriptionHi = "ग्राह द्वारा पकड़े जाने पर गजेन्द्र की अनन्य प्रार्थना और भगवान द्वारा मोक्ष।",
            descriptionBn = "গ্রাহের আক্রমণে কাতর গজরাজের কাতর প্রার্থনা ও শ্রীহরির দর্শন ও মুক্তি।"
        ),
        Episode(
            s = 7, a = 8, en = "Narasimha Avatar & Prahlada", hi = "नृसिंह अवतार एवं प्रह्लाद", bn = "নৃসিংহ অবতার ও প্রহ্লাদ",
            category = EpisodeCategory.AVATAR,
            keywords = listOf("hiranyakashipu", "pillar", "holika", "demon", "bhakti", "নৃসিংহ", "হিরণ্যকশিপু"),
            descriptionEn = "Lord Narasimha appears from a pillar to protect His supreme child devotee Prahlada.",
            descriptionHi = "स्तम्भ को चीरकर भगवान नृसिंह का प्राकट्य और भक्त प्रह्लाद की रक्षा।",
            descriptionBn = "স্তম্ভ বিদীর্ণ করে নৃসিংহদেবের আবির্ভাব ও পরম ভক্ত বালক প্রহ্লাদকে রক্ষা।"
        ),
        Episode(
            s = 4, a = 8, en = "Dhruva Maharaj's Penance", hi = "ध्रुव चरित्र एवं तपस्या", bn = "ধ্রুব চরিত্র ও তপস্যা",
            category = EpisodeCategory.BHAKTA,
            keywords = listOf("suruchi", "suniti", "uttanapada", "narada", "madhuvana", "pole star", "ধ্রুব", "তপস্যা"),
            descriptionEn = "Five-year-old Prince Dhruva's resolute penance in Madhuvana to attain Lord Vishnu.",
            descriptionHi = "बालक ध्रुव का मधुवन में कठोर तप और अविचल ध्रुवपद की प्राप्ति।",
            descriptionBn = "পাঁচ বছরের বালক ধ্রুবের মধু বনে কঠোর তপস্যা ও চিরন্তন ধ্রুবলোক প্রাপ্তি।"
        ),
        Episode(
            s = 10, a = 3, en = "Birth of Lord Krishna", hi = "श्रीकृष्ण प्राकट्य एवं जन्म", bn = "শ্রীকৃষ্ণের জন্ম ও আবির্ভাব",
            category = EpisodeCategory.AVATAR,
            keywords = listOf("mathura", "vasudeva", "devaki", "kamsa", "gokul", "nanda", "যশোদা", "দেবকী"),
            descriptionEn = "The Supreme Personality of Godhead appears in the prison of Mathura and is carried to Gokul.",
            descriptionHi = "मथुरा के कारागार में चतुर्भुज रूप में भगवान श्रीकृष्ण का पावन अवतार।",
            descriptionBn = "মথুরার কারাগারে শ্রীকৃষ্ণের আবির্ভাব ও গোকুলে বসুদেব কর্তৃক গমন।"
        ),
        Episode(
            s = 10, a = 25, en = "Lifting of Mount Govardhan", hi = "गोवर्धन धारण लीला", bn = "গোবর্ধন ধারণ লীলা",
            category = EpisodeCategory.LILA,
            keywords = listOf("indra", "rain", "giriraj", "mountain", "umbrella", "little finger", "ইন্দ্র", "গিরিরাজ"),
            descriptionEn = "Krishna lifts the Govardhan Hill on His little finger to protect Vraja from torrential rain.",
            descriptionHi = "इन्द्र के कोप से ब्रजवासियों की रक्षा हेतु सात दिन तक गोवर्धन पर्वत को उठाना।",
            descriptionBn = "ইন্দ্রের প্রলয়ঙ্কর বৃষ্টি থেকে ব্রজবাসীদের বাঁচাতে গিরিরাজ গোবর্ধন ধারণ।"
        ),
        Episode(
            s = 10, a = 16, en = "Kaliya Daman", hi = "कालिया नाग दमन", bn = "কালীয় দমন",
            category = EpisodeCategory.LILA,
            keywords = listOf("serpent", "yamuna", "poison", "hoods", "dance", "নাগ", "যমুনা"),
            descriptionEn = "Krishna dances on the hoods of the venomous Kaliya serpent to purify the Yamuna river.",
            descriptionHi = "यमुना के विषैले जल को शुद्ध करने हेतु कालिया नाग के फनों पर नृत्य।",
            descriptionBn = "যমুনার বিষাক্ত জল নির্মল করতে কালীয় নাগের ফণার ওপর নৃত্য ও কৃপা।"
        ),
        Episode(
            s = 10, a = 80, en = "Sudama Vipra & Krishna", hi = "सुदामा चरित्र एवं सख्य", bn = "সুদামা চরিত্র ও সখ্য",
            category = EpisodeCategory.BHAKTA,
            keywords = listOf("friendship", "poha", "beaten rice", "dwaraka", "poverty", "দারুক", "চিঁড়ে", "সুদামা"),
            descriptionEn = "The poor Brahmin Sudama visits Dwaraka; Krishna welcomes him with royal affection.",
            descriptionHi = "दरिद्र सुदामा की भेंट और श्रीकृष्ण द्वारा चावल के दानों के बदले सम्पूर्ण ऐश्वर्य दान।",
            descriptionBn = "দরিদ্র সুধামা বিপ্রের দ্বারকা আগমন এবং শ্রীকৃষ্ণের পরম ভালোবাসা ও বৈভব দান।"
        ),
        Episode(
            s = 8, a = 6, en = "Churning of the Ocean (Samudra Manthan)", hi = "समुद्र मन्थन एवं मोहिनी", bn = "সমুদ্রমন্থন ও মোহিনী",
            category = EpisodeCategory.LILA,
            keywords = listOf("amrita", "mandara", "kurma", "vasuki", "halahala", "shiva", "অমৃত", "বাসুকি"),
            descriptionEn = "Devas and Asuras churn the ocean of milk; Lord Shiva drinks the deadly poison.",
            descriptionHi = "देवताओं और दैत्यों द्वारा क्षीरसागर का मन्थन, हलाहल विष और अमृत प्राकट्य।",
            descriptionBn = "অমৃত লাভের জন্য দেবতা ও অসুরদের ক্ষীরোদসাগর মন্থন ও মোহিনী রূপ।"
        ),
        Episode(
            s = 8, a = 18, en = "Vamana Avatar & King Bali", hi = "वामन अवतार एवं बलि", bn = "বামন অবতার ও রাজা বলি",
            category = EpisodeCategory.AVATAR,
            keywords = listOf("three steps", "trivikrama", "yajna", "shukracharya", "ত্রিবিক্রম", "বলি"),
            descriptionEn = "Lord Vamana requests three paces of land and measures the entire universe.",
            descriptionHi = "बौने ब्राह्मण रूप में तीन पग भूमि मांगकर तीनों लोकों को नापना।",
            descriptionBn = "বামন বেশে বলি রাজার যজ্ঞে তিন পদ ভূমি যাচ্ঞা ও বিশ্বরূপ দর্শন।"
        ),
        Episode(
            s = 6, a = 1, en = "Deliverance of Ajamila", hi = "अजामिल उद्धार", bn = "অজামিল উদ্ধার",
            category = EpisodeCategory.BHAKTA,
            keywords = listOf("narayana", "yamaduta", "vishnuduta", "holy name", "মৃত্যু", "নামমহিমা"),
            descriptionEn = "Calling the holy name 'Narayana' at death rescues Ajamila from the Yamadutas.",
            descriptionHi = "मृत्युकाल में 'नारायण' नाम के उच्चारण मात्र से यमदूतों के बन्धन से मुक्ति।",
            descriptionBn = "মৃত্যুকালে কেবল 'নারায়ণ' নাম উচ্চারণে যমদূতদের হাত থেকে মুক্তি।"
        ),
        Episode(
            s = 1, a = 8, en = "Queen Kunti's Prayers", hi = "कुन्ती स्तुति", bn = "কুন্তীর স্তুতি",
            category = EpisodeCategory.TEACHING,
            keywords = listOf("pandavas", "calamities", "bhakti", "sharanagati", "বিপদ", "ভক্তি"),
            descriptionEn = "Queen Kunti asks for calamities so she may constantly remember Lord Krishna.",
            descriptionHi = "महारानी कुन्ती की अनुपम प्रार्थना: 'हे प्रभु! हमें पद-पद पर विपत्तियाँ दीजिए।'",
            descriptionBn = "মহারানী কুন্তীর অতুলনীয় স্তুতি ও শ্রীকৃষ্ণের প্রতি অনন্য আত্মসমর্পণ।"
        ),
        Episode(
            s = 3, a = 25, en = "Kapila Gita (Sankhya & Bhakti)", hi = "कपिल गीता एवं सांख्य", bn = "কপিল গীতা ও সাংখ্যযোগ",
            category = EpisodeCategory.TEACHING,
            keywords = listOf("devahuti", "kardama", "philosophy", "sankhya", "মোক্ষ", "ভক্তিযোগ"),
            descriptionEn = "Lord Kapila instructs His mother Devahuti in Sankhya philosophy and pure devotional service.",
            descriptionHi = "भगवान कपिल द्वारा अपनी माता देवहूति को सांख्य दर्शन और निष्काम भक्तियोग का उपदेश।",
            descriptionBn = "ভগবান কপিলদেব কর্তৃক মাতা দেবহূতিকে সাংখ্য দর্শন ও ভক্তিযোগের উপদেশ।"
        ),
        Episode(
            s = 11, a = 7, en = "Uddhava Gita & 24 Gurus", hi = "उद्धव गीता एवं २४ गुरु", bn = "উদ্ধব গীতা ও ২৪ জন গুরু",
            category = EpisodeCategory.TEACHING,
            keywords = listOf("avadhuta", "nature", "swan", "dattatreya", "অবধূত", "জ্ঞান"),
            descriptionEn = "Krishna's final sublime teachings to Uddhava including the Avadhuta's 24 nature gurus.",
            descriptionHi = "द्वारका में श्रीकृष्ण द्वारा उद्धवजी को अंतिम उपदेश एवं अवधूत के २४ गुरुओं की कथा।",
            descriptionBn = "উদ্ধবকে প্রদত্ত শ্রীকৃষ্ণের পরম আধ্যাত্মিক উপদেশ ও অবধূতের ২৪ জন গুরু।"
        ),
        Episode(
            s = 9, a = 4, en = "King Ambarisha & Durvasa Muni", hi = "अम्बरीष चरित्र एवं दुर्वासा", bn = "অম্বরীষ চরিত্র ও দুর্বাসা",
            category = EpisodeCategory.BHAKTA,
            keywords = listOf("sudarshana", "ekadashi", "curse", "disc", "সুদর্শন", "একাদশী"),
            descriptionEn = "The power of pure devotion: Sudarshana Chakra protects King Ambarisha from anger.",
            descriptionHi = "परम भक्त राजा अम्बरीष के एकादशी व्रत की रक्षा हेतु सुदर्शन चक्र का दुर्वासा का पीछा करना।",
            descriptionBn = "পরম বৈষ্ণব রাজা অম্বরীষ ও তাঁকে রক্ষায় সুদর্শন চক্রের মহিমা।"
        ),
        Episode(
            s = 3, a = 13, en = "Varaha Avatar (Rescuing the Earth)", hi = "वराह अवतार एवं भू-उद्धार", bn = "বরাহ অবতার ও পৃথিবী উদ্ধার",
            category = EpisodeCategory.AVATAR,
            keywords = listOf("boar", "hiranyaksha", "ocean", "rasatala", "বরাহ", "হিরণ্যাক্ষ"),
            descriptionEn = "Lord Varaha emerges from Brahma's nostril and rescues Mother Earth from the cosmic ocean.",
            descriptionHi = "ब्रह्माजी की नासिका से वराह भगवान का प्राकट्य और रसातल से पृथ्वी का उद्धार।",
            descriptionBn = "ব্রহ্মার নাসারন্ধ্র থেকে বরাহদেবের আবির্ভাব ও পাতাল থেকে ধরণী উদ্ধার।"
        ),
    )
}

fun tr(l: Lang, en: String, hi: String, bn: String) = when (l) { Lang.HI -> hi; Lang.BN -> bn; else -> en }
