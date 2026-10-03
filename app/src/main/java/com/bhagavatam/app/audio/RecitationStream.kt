package com.bhagavatam.app.audio

import com.bhagavatam.app.data.Lang
import java.net.URLEncoder

enum class AudioMode {
    KARAOKE_PAATH,
    YOUTUBE_STREAM
}

object RecitationStream {

    /** Generate accurate search query for full authentic recitation on YouTube. */
    fun getQuery(skandha: Int, chapter: Int, lang: Lang): String = when (lang) {
        Lang.BN -> if (skandha == 0) "শ্রীমদ্ভাগবত মাহাত্ম্য অধ্যায় $chapter বাংলা পাঠ গীতাপ্রেস"
                   else "শ্রীমদ্ভাগবত মহাপুরাণ স্কন্ধ $skandha অধ্যায় $chapter বাংলা পাঠ গীতাপ্রেস"
        Lang.HI -> if (skandha == 0) "श्रीमद्भागवत माहात्म्य अध्याय $chapter गीताप्रेस गोरखपुर पाठ"
                   else "श्रीमद्भागवत महापुराण स्कन्ध $skandha अध्याय $chapter गीताप्रेस गोरखपुर पाठ"
        Lang.EN -> if (skandha == 0) "Shrimad Bhagavatam Mahatmya Chapter $chapter recitation"
                   else "Shrimad Bhagavatam Canto $skandha Chapter $chapter recitation"
        Lang.SA -> if (skandha == 0) "श्रीमद्भागवत माहात्म्य अध्याय $chapter संस्कृत मूल पाठ"
                   else "श्रीमद्भागवत महापुराण स्कन्ध $skandha अध्याय $chapter संस्कृत मूल पाठ"
    }

    /** Embeddable clean streaming URL. */
    fun getEmbedUrl(skandha: Int, chapter: Int, lang: Lang): String {
        val q = URLEncoder.encode(getQuery(skandha, chapter, lang), "UTF-8")
        return "https://www.youtube-nocookie.com/embed?listType=search&list=$q&autoplay=1&playsinline=1&modestbranding=1"
    }

    /** Direct streaming / watch query URL for opening in background or YouTube client. */
    fun getSearchUrl(skandha: Int, chapter: Int, lang: Lang): String {
        val q = URLEncoder.encode(getQuery(skandha, chapter, lang), "UTF-8")
        return "https://www.youtube.com/results?search_query=$q"
    }
}
