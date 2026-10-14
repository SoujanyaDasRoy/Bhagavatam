package com.bhagavatam.app.data

/**
 * Suffix-stripping lemmatizer for Indic and English words.
 * Serves both search and the offline dictionary.
 */
object Lemmatizer {
    private val suffixes = mapOf(
        Lang.BN to listOf("গুলোর", "গুলো", "দের", "েরা", "ের", "কে", "তে", "েই", "টি", "রা", "র", "ে", "ই", "ও"),
        Lang.HI to listOf("ाओं", "ओं", "ें", "ों", "ने", "को", "का", "की", "के", "से", "में", "पर", "ा", "ी", "े"),
        Lang.SA to listOf("स्य", "ाय", "ेन", "ेषु", "ानि", "ाः", "ः", "म्", "ं"),
        Lang.EN to listOf("ing", "ed", "es", "ly", "s"),
    )

    private val suffixesByCode = mapOf(
        "bn" to listOf("গুলোর", "গুলো", "দের", "েরা", "ের", "কে", "তে", "েই", "টি", "রা", "র", "ে", "ই", "ও"),
        "hi" to listOf("ाओं", "ओं", "ें", "ों", "ने", "को", "का", "की", "के", "से", "में", "पर", "ा", "ी", "े"),
        "sa" to listOf("स्य", "ाय", "ेन", "ेषु", "ानि", "ाः", "ः", "म्", "ं"),
        "or" to listOf("ମାନଙ୍କର", "ମାନଙ୍କୁ", "ମାନେ", "ଙ୍କର", "ଙ୍କୁ", "ଙ୍କ", "ରେ", "କୁ", "ର", "ଟି"),
        "en" to listOf("ing", "ed", "es", "ly", "s"),
    )

    /**
     * Stems of [w]: known inflections and case endings stripped (longest first).
     * For Bengali, also generates the "ৎ" spelling of a final "ত" stem.
     */
    fun stems(w: String, lang: Lang): List<String> = stems(w, lang.code)

    /**
     * Stems of [w] given a two-letter language code ("en", "hi", "bn", "or", "sa").
     */
    fun stems(w: String, langCode: String): List<String> {
        val out = ArrayList<String>()
        val list = suffixesByCode[langCode] ?: return emptyList()
        for (suf in list.sortedByDescending { it.length }) {
            if (w.length > suf.length + 1 && w.endsWith(suf)) {
                val stem = w.dropLast(suf.length)
                out.add(stem)
                if (langCode == "bn" && stem.endsWith("ত")) {
                    out.add(stem.dropLast(1) + "ৎ")
                }
            }
        }
        return out.distinct()
    }
}
