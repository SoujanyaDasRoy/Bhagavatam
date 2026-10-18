package com.bhagavatam.app.data

/** One line of the notebook: a mark or a note on a verse. */
data class NotebookEntry(val ref: String, val kind: AnnKind, val colour: Long, val quote: String, val note: String)

private fun refParts(ref: String): List<Int> = ref.split(".").map { it.toIntOrNull() ?: 0 }

/** Entries grouped by chapter ("skandha.adhyaya"), chapters in book order, entries in verse order. */
fun notebookGroups(entries: List<NotebookEntry>): List<Pair<String, List<NotebookEntry>>> =
    entries.sortedWith(compareBy({ refParts(it.ref).getOrElse(0) { 0 } }, { refParts(it.ref).getOrElse(1) { 0 } }, { refParts(it.ref).getOrElse(2) { 0 } }))
        .groupBy { refParts(it.ref).take(2).joinToString(".") }
        .toList()

/**
 * Plain text of the whole notebook for sharing: a heading per chapter, then each mark with its reference, colour name, quote and note.
 * [colours] and [colourNames] are parallel lists; a colour not in [colours] prints no name.
 */
fun notebookText(
    entries: List<NotebookEntry>,
    colourNames: List<String>,
    colours: List<Long>,
    chapterTitle: (Int, Int) -> String,
): String {
    if (entries.isEmpty()) return ""
    val out = StringBuilder()
    for ((key, list) in notebookGroups(entries)) {
        val p = refParts(key)
        val s = p.getOrElse(0) { 0 }
        val a = p.getOrElse(1) { 0 }
        out.append(if (s == 0) "Mahatmya $a" else "Skandha $s, Adhyaya $a").append(" - ").append(chapterTitle(s, a)).append("\n\n")
        for (e in list) {
            val name = colours.indexOf(e.colour).takeIf { it >= 0 }?.let { colourNames.getOrNull(it) }
            out.append(e.ref)
            if (e.kind == AnnKind.BOOKMARK) out.append(" (bookmark)") else if (name != null) out.append(" (").append(name).append(")")
            out.append("\n").append(e.quote.trim()).append("\n")
            if (e.note.isNotBlank()) out.append(e.note.trim()).append("\n")
            out.append("\n")
        }
    }
    return out.toString().trimEnd() + "\n"
}
