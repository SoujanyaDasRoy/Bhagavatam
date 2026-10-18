package com.bhagavatam.app.data

/** Whole minutes still to read for [wordsAfter] words: rounded up, never 0 while words remain. */
fun minutesLeft(wordsAfter: Int, wordsPerMinute: Int = 150): Int =
    if (wordsAfter <= 0) 0 else (wordsAfter + wordsPerMinute - 1) / wordsPerMinute

/** (chapters finished, chapters in the Skandha) for [skandha], from the finished set whose entries are "skandha.adhyaya". */
fun skandhaProgress(finished: Set<String>, chaptersIn: Int, skandha: Int): Pair<Int, Int> =
    finished.count { it.substringBefore('.') == skandha.toString() } to chaptersIn

/** [entry] first, then the older entries without it, at most [max] in all. */
fun pushRecent(list: List<String>, entry: String, max: Int = 5): List<String> = (listOf(entry) + list.filter { it != entry }).take(max)
