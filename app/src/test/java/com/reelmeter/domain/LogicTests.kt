package com.reelmeter.domain

import com.reelmeter.domain.logic.DayResult
import com.reelmeter.domain.logic.Formatters
import com.reelmeter.domain.logic.LimitEvaluator
import com.reelmeter.domain.logic.ReelDebouncer
import com.reelmeter.domain.logic.StreakCalculator
import com.reelmeter.domain.logic.SwipeFilter
import com.reelmeter.domain.model.LimitAction
import com.reelmeter.domain.model.LimitStatus
import com.reelmeter.domain.model.TrackedApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LimitEvaluatorTest {
    private fun decide(total: Int, limit: Int = 100, warned: Boolean = false, reminders: Boolean = true, snooze: Long = 0, now: Long = 1_000) =
        LimitEvaluator.decide(total, limit, warned, reminders, snooze, now)

    @Test fun `warning threshold is 80 percent rounded up`() {
        assertEquals(80, LimitEvaluator.warningThreshold(100))
        assertEquals(8, LimitEvaluator.warningThreshold(10))
        assertEquals(4, LimitEvaluator.warningThreshold(5))
        assertEquals(1, LimitEvaluator.warningThreshold(1))
    }

    @Test fun `status follows thresholds`() {
        assertEquals(LimitStatus.NORMAL, LimitEvaluator.status(79, 100))
        assertEquals(LimitStatus.WARNING, LimitEvaluator.status(80, 100))
        assertEquals(LimitStatus.WARNING, LimitEvaluator.status(99, 100))
        assertEquals(LimitStatus.REACHED, LimitEvaluator.status(100, 100))
    }

    @Test fun `no action below 80 percent`() = assertEquals(LimitAction.NONE, decide(79))

    @Test fun `warns once at 80 percent`() {
        assertEquals(LimitAction.NOTIFY_WARNING, decide(80))
        assertEquals(LimitAction.NONE, decide(81, warned = true))
    }

    @Test fun `no warning when reminders are off`() = assertEquals(LimitAction.NONE, decide(85, reminders = false))

    @Test fun `overlay at 100 percent even after the warning`() {
        assertEquals(LimitAction.SHOW_OVERLAY, decide(100, warned = true))
        assertEquals(LimitAction.SHOW_OVERLAY, decide(120))
    }

    @Test fun `overlay is suppressed during snooze and returns after it`() {
        assertEquals(LimitAction.NONE, decide(100, snooze = 5_000, now = 1_000))
        assertEquals(LimitAction.SHOW_OVERLAY, decide(101, snooze = 5_000, now = 5_000))
    }

    @Test fun `overlay wins over warning for tiny limits`() = assertEquals(LimitAction.SHOW_OVERLAY, decide(1, limit = 1))
}

class ReelDebouncerTest {
    private val app = TrackedApp.INSTAGRAM

    @Test fun `first event counts`() = assertTrue(ReelDebouncer().onScrollEvent(app, 0))

    @Test fun `burst of events from one swipe counts once`() {
        val d = ReelDebouncer(1200L)
        assertTrue(d.onScrollEvent(app, 0))
        listOf(30L, 60L, 120L, 200L, 350L, 500L, 650L, 1000L).forEach { assertFalse(d.onScrollEvent(app, it)) }
    }

    @Test fun `new swipe after minimum interval counts`() {
        val d = ReelDebouncer(1200L)
        assertTrue(d.onScrollEvent(app, 0))
        assertFalse(d.onScrollEvent(app, 200))
        assertTrue(d.onScrollEvent(app, 1_500))
    }

    @Test fun `minimum interval blocks too-fast recounts`() {
        val d = ReelDebouncer(900L)
        assertTrue(d.onScrollEvent(app, 0))
        assertFalse(d.onScrollEvent(app, 700)) // only 700ms after last counted reel
        assertTrue(d.onScrollEvent(app, 1_500))
    }

    @Test fun `apps are debounced independently`() {
        val d = ReelDebouncer()
        assertTrue(d.onScrollEvent(TrackedApp.INSTAGRAM, 0))
        assertTrue(d.onScrollEvent(TrackedApp.YOUTUBE, 10))
        assertFalse(d.onScrollEvent(TrackedApp.INSTAGRAM, 20))
    }

    @Test fun `swipe filter ignores horizontal scrolls`() {
        assertTrue(SwipeFilter.isVerticalSwipe(0, 0))
        assertTrue(SwipeFilter.isVerticalSwipe(5, 300))
        assertFalse(SwipeFilter.isVerticalSwipe(300, 5))
    }
}

class StreakCalculatorTest {
    private val today = LocalDate.of(2026, 10, 25)

    @Test fun `fresh install with no data has a streak of one`() =
        assertEquals(1, StreakCalculator.compute(today, today, emptyMap(), 100))

    @Test fun `counts consecutive days under the limit`() {
        val days = (1L..4L).associate { today.minusDays(it) to DayResult(50, 100) }
        assertEquals(5, StreakCalculator.compute(today, today.minusDays(4), days, 100))
    }

    @Test fun `breaks on a day at or over the limit`() {
        val days = mapOf(
            today.minusDays(1) to DayResult(40, 100),
            today.minusDays(2) to DayResult(100, 100),
            today.minusDays(3) to DayResult(10, 100),
        )
        assertEquals(2, StreakCalculator.compute(today, today.minusDays(10), days, 100))
    }

    @Test fun `streak is zero when today is already over the limit`() {
        val days = mapOf(today to DayResult(130, 100))
        assertEquals(0, StreakCalculator.compute(today, today.minusDays(3), days, 100))
    }

    @Test fun `never counts days before install`() =
        assertEquals(3, StreakCalculator.compute(today, today.minusDays(2), emptyMap(), 100))

    @Test fun `uses each day's own limit`() {
        val days = mapOf(today.minusDays(1) to DayResult(60, 50)) // over a stricter old limit
        assertEquals(1, StreakCalculator.compute(today, today.minusDays(5), days, 100))
    }
}

class FormattersTest {
    @Test fun duration() {
        assertEquals("45s", Formatters.duration(45))
        assertEquals("10m", Formatters.duration(600))
        assertEquals("1h", Formatters.duration(3_600))
        assertEquals("1h 15m", Formatters.duration(4_500))
    }
}

class PercentageReductionTest {
    private fun calcPercentageChange(prev: Int, curr: Int): Double? {
        if (prev <= 0) return null
        return ((prev - curr).toDouble() / prev) * 100.0
    }

    @Test fun `positive reduction when usage decreases`() {
        assertEquals(20.0, calcPercentageChange(100, 80)!!, 0.01)
    }

    @Test fun `negative reduction when usage increases`() {
        assertEquals(-25.0, calcPercentageChange(100, 125)!!, 0.01)
    }

    @Test fun `returns null when previous period has zero usage`() {
        assertEquals(null, calcPercentageChange(0, 50))
    }
}

class BlackoutWindowTest {
    private fun isInsideBlackoutWindow(nowHour: Int, startHour: Int, endHour: Int): Boolean {
        return if (startHour > endHour) {
            nowHour >= startHour || nowHour < endHour
        } else {
            nowHour in startHour until endHour
        }
    }

    @Test fun `overnight blackout window evaluates correctly`() {
        // 11 PM (23) to 7 AM (7)
        assertTrue(isInsideBlackoutWindow(23, 23, 7)) // 11 PM
        assertTrue(isInsideBlackoutWindow(2, 23, 7))  // 2 AM
        assertTrue(isInsideBlackoutWindow(6, 23, 7))  // 6 AM
        assertFalse(isInsideBlackoutWindow(8, 23, 7)) // 8 AM
        assertFalse(isInsideBlackoutWindow(14, 23, 7))// 2 PM
    }

    @Test fun `daytime blackout window evaluates correctly`() {
        // 9 AM (9) to 1 PM (13)
        assertTrue(isInsideBlackoutWindow(9, 9, 13))
        assertTrue(isInsideBlackoutWindow(11, 9, 13))
        assertFalse(isInsideBlackoutWindow(8, 9, 13))
        assertFalse(isInsideBlackoutWindow(13, 9, 13))
    }
}
