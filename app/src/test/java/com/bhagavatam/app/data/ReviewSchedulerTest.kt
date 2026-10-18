package com.bhagavatam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSchedulerTest {
    private val day = 86_400_000L

    @Test fun rememberedMovesToNextStep() {
        assertEquals(1, ReviewScheduler.after(0, true))
        assertEquals(2, ReviewScheduler.after(1, true))
        assertEquals(3, ReviewScheduler.after(2, true))
        assertEquals(4, ReviewScheduler.after(3, true))
    }

    @Test fun failedGoesBackToStart() = assertEquals(0, ReviewScheduler.after(3, false))

    @Test fun learnedStaysLearned() = assertEquals(4, ReviewScheduler.after(4, true))

    @Test fun dueAtAddsTheStepInDays() {
        assertEquals(1_000L + 3 * day, ReviewScheduler.dueAt(1_000L, 1))
        assertEquals(1_000L + 1 * day, ReviewScheduler.dueAt(1_000L, 0))
    }

    @Test fun learnedCardIsNeverDue() = assertFalse(ReviewScheduler.isDue(10_000 * day, ReviewScheduler.dueAt(0, 4), 4))

    @Test fun dueOnceTheTimeHasCome() {
        val due = ReviewScheduler.dueAt(0, 1)
        assertFalse(ReviewScheduler.isDue(2 * day, due, 1))
        assertTrue(ReviewScheduler.isDue(3 * day, due, 1))
    }

    @Test fun clockMovedBackDoesNotMakeEverythingDue() {
        val created = 100 * day
        val due = ReviewScheduler.dueAt(created, 1)
        assertFalse(ReviewScheduler.isDue(created - 2 * day, due, 1))
    }

    @Test fun neverDueMoreThanTwentyOneDaysAhead() {
        // a clock that was far in the future when the card was answered must not hide it for years
        val due = 5_000 * day
        assertTrue(ReviewScheduler.isDue(0, due, 1))
    }
}
