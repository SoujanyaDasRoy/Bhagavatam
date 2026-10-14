package com.bhagavatam.app.data

import java.util.regex.Pattern

data class SearchReference(
    val skandha: Int,
    val chapter: Int,
    val verse: Int? = null
) {
    val display: String
        get() = if (verse != null) "$skandha.$chapter.$verse" else "$skandha.$chapter"
}

data class ParsedQuery(
    val raw: String,
    val reference: SearchReference? = null,
    val phrases: List<String> = emptyList(),
    val terms: List<String> = emptyList(),
    val scope: String = "verse"
) {
    val isEmpty: Boolean
        get() = reference == null && phrases.isEmpty() && terms.isEmpty()
}

/**
 * Parser for user search inputs: scriptural references (0.1 to 12.13), quoted phrases,
 * individual search tokens, and minimum word length checks.
 */
object SearchQuery {
    private val RE_REF = Pattern.compile("^(\\d+)[.:/\\-\\s]+(\\d+)(?:[.:/\\-\\s]+(\\d+))?$")
    private val RE_PHRASE = Pattern.compile("\"([^\"]+)\"")
    private val RE_TOKEN = Pattern.compile("[^\\s\"“”'‘’.,;:!?()]+")

    fun parseReference(input: String): SearchReference? {
        val trimmed = input.trim()
        val m = RE_REF.matcher(trimmed)
        if (!m.matches()) return null
        val skandha = m.group(1)?.toIntOrNull() ?: return null
        if (skandha !in 0..12) return null
        val chapter = m.group(2)?.toIntOrNull() ?: return null
        val verse = m.group(3)?.toIntOrNull()
        return SearchReference(skandha, chapter, verse)
    }

    fun parse(raw: String, scope: String = "verse"): ParsedQuery {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ParsedQuery(raw = raw, scope = scope)

        // 1. Check if the query is a chapter or verse reference (e.g. 1.2 or 10.14.8)
        val ref = parseReference(trimmed)
        if (ref != null) {
            return ParsedQuery(raw = raw, reference = ref, scope = scope)
        }

        // 2. Extract quoted phrases
        val phrases = ArrayList<String>()
        val phraseMatcher = RE_PHRASE.matcher(trimmed)
        val sbWithoutPhrases = StringBuffer()
        while (phraseMatcher.find()) {
            val pText = phraseMatcher.group(1)?.trim()
            if (!pText.isNullOrEmpty()) {
                phrases.add(pText)
            }
            phraseMatcher.appendReplacement(sbWithoutPhrases, " ")
        }
        phraseMatcher.appendTail(sbWithoutPhrases)

        // 3. Extract word tokens from remaining text
        val terms = ArrayList<String>()
        val tokenMatcher = RE_TOKEN.matcher(sbWithoutPhrases.toString())
        while (tokenMatcher.find()) {
            val token = tokenMatcher.group()
            val clean = Normalise.word(token)
            if (clean.length >= 2) {
                terms.add(clean)
            }
        }

        return ParsedQuery(
            raw = raw,
            reference = null,
            phrases = phrases,
            terms = terms.distinct(),
            scope = scope
        )
    }
}
