package com.reelmeter.domain

import com.reelmeter.domain.logic.FocusTimerLogic
import com.reelmeter.domain.logic.FocusTimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusTimerLogicTest {

    @Test
    fun `starts running with correct duration`() {
        val now = 100_000L
        val running = FocusTimerLogic.startTimer(15, now)
        assertEquals(now, running.startTimeMs)
        assertEquals(now + 15 * 60_000L, running.endTimeMs)
        assertEquals(15 * 60_000L, running.remainingMs(now))
        assertFalse(running.isFinished(now))
    }

    @Test
    fun `evaluates to locked when running timer finishes`() {
        val now = 100_000L
        val running = FocusTimerLogic.startTimer(1, now) // 1 min = 60,000ms
        val evaluatedBefore = FocusTimerLogic.evaluate(running, now + 30_000L)
        assertTrue(evaluatedBefore is FocusTimerState.Running)

        val evaluatedAfter = FocusTimerLogic.evaluate(running, now + 60_001L)
        assertTrue(evaluatedAfter is FocusTimerState.Locked)
    }

    @Test
    fun `evaluates to idle when unlocking breathing timer finishes`() {
        val now = 100_000L
        val unlocking = FocusTimerLogic.startUnlocking(now)
        assertEquals(60, unlocking.remainingSeconds(now))

        val evaluatedMid = FocusTimerLogic.evaluate(unlocking, now + 30_000L)
        assertTrue(evaluatedMid is FocusTimerState.Unlocking)

        val evaluatedEnd = FocusTimerLogic.evaluate(unlocking, now + 60_001L)
        assertEquals(FocusTimerState.Idle, evaluatedEnd)
    }
}
