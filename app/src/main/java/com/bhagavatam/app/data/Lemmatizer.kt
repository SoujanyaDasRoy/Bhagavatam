package com.bhagavatam.app.data

/**
 * Suffix-stripping lemmatizer for Indic and English words.
 * Serves both search and the offline dictionary. Kept in step with content/_dict/coverage_real.py (SUFFIX, VERB, stems),
 * which builds the dictionary's book tables with the same rules.
 */
object Lemmatizer {
    private val suffixesByCode = mapOf(
        "bn" to listOf("গুলোর", "গুলো", "দের", "েরা", "ের", "কে", "তে", "েই", "টি", "রা", "র", "ে", "ই", "ও"),
        "hi" to listOf("लोगोंको", "लोगोंके", "लोगोंने", "लोगों", "लोग", "ियों", "यों", "ाओं", "ओं", "ें", "ों", "ने", "को", "का", "की", "के", "से", "में", "पर", "तक", "ा", "ी", "े"),
        "sa" to listOf("ाणाम्", "ानाम्", "ेभ्यः", "ाभ्याम्", "ायाः", "ीनाम्", "ेषु", "ैः", "स्य", "ाय", "ेन", "ात्", "ानि", "ाः", "ान्", "ाम्", "ायै", "ीम्", "ीः", "ौ", "ो", "े", "ः", "म्", "ं"),
        "or" to listOf("ମାନଙ୍କର", "ମାନଙ୍କୁ", "ମାନେ", "ଙ୍କର", "ଙ୍କୁ", "ଙ୍କ", "ରେ", "କୁ", "ର", "ଟି"),
        "en" to listOf("ing", "ed", "es", "ly", "s"),
    ).mapValues { (_, list) -> list.sortedByDescending { it.length } }

    /** Verb forms back to the dictionary form: Hindi infinitive (-ना), Odia verbal noun (-ିବା), Bengali verbal noun (-া). */
    private val verbEndings = mapOf(
        "hi" to ("ना" to listOf("ायेंगे", "ायेगा", "ायेगी", "ाएँगे", "ाएगा", "ाएगी", "ायँगे", "ायगा", "ायगी", "यँगे", "यगा", "यगी",
            "येगा", "येगी", "ूँगा", "ूँगी", "ेंगे", "ेंगी", "ेगा", "ेगी", "ोगे", "ोगी", "कर", "ते", "ता", "ती",
            "ने", "नी", "ें", "ो", "े", "ा", "ी", "य", "ये", "या", "यी", "ँ")),
        "or" to ("ିବା" to listOf("ୁଥାନ୍ତି", "ୁଥିଲେ", "ୁଥିଲା", "ୁଥିବା", "ୁଥାଏ", "ୁଛନ୍ତି", "ୁଛି", "ୁଛ", "ିଥିବାରୁ", "ିଥିଲେ", "ିଥିଲା",
            "ିଥିବା", "ିଛନ୍ତି", "ିଛି", "ିବାରୁ", "ିବେ", "ିବ", "ିଲେ", "ିଲା", "ିଲି", "ିଲ", "ନ୍ତି", "ନ୍ତୁ", "ି", "ୁ")),
        "bn" to ("া" to listOf("েছিলেন", "েছিল", "েছেন", "েছে", "েননি", "েনি", "লেন", "ছিলেন", "ছিল", "ছেন", "বেন", "তেন",
            "বে", "তে", "েন", "ুন", "ে", "ো", "ি", "ল", "ত")),
    )

    /**
     * Stems of [w]: known inflections and case endings stripped (longest first, up to two deep: जीवोंको -> जीवों -> जीव),
     * then verb forms put back to the dictionary form (जायगा -> जाना). For Bengali, also the "ৎ" spelling of a final "ত" stem.
     */
    fun stems(w: String, lang: Lang): List<String> = stems(w, lang.code)

    /**
     * Stems of [w] given a two-letter language code ("en", "hi", "bn", "or", "sa").
     */
    fun stems(w: String, langCode: String): List<String> {
        val out = ArrayList<String>()
        for (st in strip(w, langCode)) {
            out.add(st)
            out.addAll(strip(st, langCode))
        }
        verbEndings[langCode]?.let { (rep, ends) ->
            for (end in ends) if (w.length > end.length + 1 && w.endsWith(end)) out.add(w.dropLast(end.length) + rep)
        }
        return out.distinct()
    }

    private fun strip(w: String, langCode: String): List<String> {
        val out = ArrayList<String>()
        for (suf in suffixesByCode[langCode] ?: return out) {
            if (w.length > suf.length + 1 && w.endsWith(suf)) {
                val stem = w.dropLast(suf.length)
                out.add(stem)
                if (langCode == "bn" && stem.endsWith("ত")) out.add(stem.dropLast(1) + "ৎ")
            }
        }
        return out
    }
}
