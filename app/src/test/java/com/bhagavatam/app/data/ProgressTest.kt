package com.bhagavatam.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressTest {
    @Test fun minutesLeftRoundsUpAndNeverZeroWhileWordsRemain() {
        assertEquals(1, minutesLeft(1))
        assertEquals(0, minutesLeft(0))
        assertEquals(3, minutesLeft(301))
        assertEquals(2, minutesLeft(300))
    }

    @Test fun skandhaProgressCountsOnlyThatSkandha() {
        assertEquals(2 to 19, skandhaProgress(setOf("1.1", "1.5", "2.1", "10.3"), 19, 1))
        assertEquals(0 to 10, skandhaProgress(setOf("1.1"), 10, 2))
    }

    @Test fun skandhaProgressIsNotFooledByPrefixes() = assertEquals(1 to 5, skandhaProgress(setOf("1.1", "10.1", "11.1"), 5, 1))

    @Test fun pushRecentKeepsFiveMostRecent() = assertEquals(listOf("f", "e", "d", "c", "b"), pushRecent(listOf("e", "d", "c", "b", "a"), "f"))

    @Test fun pushRecentMovesRepeatToFront() = assertEquals(listOf("c", "a", "b"), pushRecent(listOf("a", "b", "c"), "c"))
}
