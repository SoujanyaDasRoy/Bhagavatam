package com.bhagavatam.app.state

/** The verse number at the top of the screen: [firstVisibleIndex] is a list index, [headItems] the list items before the first verse, [rows] the verse numbers in list order. */
fun anchorVerseNum(firstVisibleIndex: Int, headItems: Int, rows: List<Int>): Int =
    rows.getOrNull((firstVisibleIndex - headItems).coerceAtLeast(0)) ?: rows.lastOrNull() ?: 1

/** The list index to scroll to so [num] (or the row at or before it, when the new language has no row for it) is at the top. */
fun itemIndexFor(num: Int, headItems: Int, rows: List<Int>): Int = headItems + rows.indexOfLast { it <= num }.coerceAtLeast(0)

/** The reader goes back to where it stopped only when nothing asked for a particular verse (the notebook, the word sheet). */
fun resumeApplies(pendingVerse: String?): Boolean = pendingVerse == null
