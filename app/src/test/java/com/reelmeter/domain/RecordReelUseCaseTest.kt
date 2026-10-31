package com.reelmeter.domain

import com.reelmeter.core.TimeProvider
import com.reelmeter.domain.model.DayStat
import com.reelmeter.domain.model.LimitAction
import com.reelmeter.domain.model.Limits
import com.reelmeter.domain.model.TrackedApp
import com.reelmeter.domain.model.UserSettings
import com.reelmeter.domain.repository.ReelRepository
import com.reelmeter.domain.repository.SettingsRepository
import com.reelmeter.domain.usecase.ExtendLimitUseCase
import com.reelmeter.domain.usecase.RecordReelUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private class FakeTime(var now: Long = 1_000_000L, var day: LocalDate = LocalDate.of(2026, 10, 25)) : TimeProvider {
    override fun nowMillis() = now
    override fun today() = day
}

private class FakeSettings(initial: UserSettings) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: Flow<UserSettings> = state
    override suspend fun current() = state.value
    override suspend fun update(transform: (UserSettings) -> UserSettings) { state.value = transform(state.value) }
}

private class FakeReels : ReelRepository {
    val stats = mutableMapOf<LocalDate, DayStat>()

    private fun empty(day: LocalDate, limit: Int) =
        DayStat(day, TrackedApp.entries.associateWith { 0 }, limit, false, 0, 0)

    override suspend fun recordReel(app: TrackedApp, timestamp: Long, day: LocalDate, limit: Int): DayStat {
        val s = stats[day] ?: empty(day, limit)
        val updated = s.copy(perApp = s.perApp + (app to (s.perApp.getValue(app) + 1)), limitSnapshot = limit)
        stats[day] = updated
        return updated
    }
    override fun observeDay(day: LocalDate) = flowOf(stats[day])
    override fun observeTodayEvents(day: LocalDate) = flowOf(emptyList<com.reelmeter.data.local.ReelEventEntity>())
    override fun observeRange(from: LocalDate, to: LocalDate) = flowOf(stats.values.toList())
    override fun observeAll() = flowOf(stats.values.toList())
    override suspend fun markWarned(day: LocalDate) { stats[day] = stats.getValue(day).copy(warned = true) }
    override suspend fun useExtension(day: LocalDate, snoozeUntil: Long): Boolean {
        val s = stats[day] ?: return false
        if (s.extensionsUsed >= Limits.MAX_EXTENSIONS_PER_DAY) return false
        stats[day] = s.copy(extensionsUsed = s.extensionsUsed + 1, snoozeUntil = snoozeUntil)
        return true
    }
    override suspend fun purgeEventsBefore(timestamp: Long) = Unit
    override suspend fun resetAll() { stats.clear() }
}

class RecordReelUseCaseTest {
    private val time = FakeTime()
    private val reels = FakeReels()
    private val settings = FakeSettings(UserSettings(dailyLimit = 10))
    private val record = RecordReelUseCase(reels, settings, time)
    private val extend = ExtendLimitUseCase(reels, time)

    @Test fun `counts per app and in total`() = runTest {
        record(TrackedApp.INSTAGRAM); record(TrackedApp.INSTAGRAM)
        val r = record(TrackedApp.YOUTUBE)!!
        assertEquals(3, r.total)
        assertEquals(1, r.appCount)
        assertEquals(2, reels.stats.getValue(time.day).perApp[TrackedApp.INSTAGRAM])
    }

    @Test fun `untracked apps are ignored`() = runTest {
        settings.update { it.copy(trackedApps = setOf(TrackedApp.YOUTUBE)) }
        assertNull(record(TrackedApp.INSTAGRAM))
        assertTrue(reels.stats.isEmpty())
    }

    @Test fun `warning once at 80 percent then overlay at the limit`() = runTest {
        val actions = (1..10).map { record(TrackedApp.INSTAGRAM)!!.action }
        assertEquals(LimitAction.NONE, actions[6])            // 7
        assertEquals(LimitAction.NOTIFY_WARNING, actions[7])  // 8 = 80%
        assertEquals(LimitAction.NONE, actions[8])            // 9 (already warned)
        assertEquals(LimitAction.SHOW_OVERLAY, actions[9])    // 10 = 100%
    }

    @Test fun `extension suppresses the overlay for 5 minutes`() = runTest {
        repeat(10) { record(TrackedApp.INSTAGRAM) }
        assertTrue(extend())
        time.now += 60_000
        assertEquals(LimitAction.NONE, record(TrackedApp.INSTAGRAM)!!.action)
        time.now += Limits.EXTENSION_MINUTES * 60_000L
        assertEquals(LimitAction.SHOW_OVERLAY, record(TrackedApp.INSTAGRAM)!!.action)
    }

    @Test fun `only two extensions per day`() = runTest {
        repeat(10) { record(TrackedApp.INSTAGRAM) }
        assertTrue(extend()); assertTrue(extend()); assertFalse(extend())
        assertEquals(0, record(TrackedApp.INSTAGRAM)!!.extensionsLeft)
    }

    @Test fun `new day starts from zero with fresh warnings and extensions`() = runTest {
        repeat(10) { record(TrackedApp.INSTAGRAM) }
        extend()
        time.day = time.day.plusDays(1)
        val r = record(TrackedApp.INSTAGRAM)!!
        assertEquals(1, r.total)
        assertEquals(Limits.MAX_EXTENSIONS_PER_DAY, r.extensionsLeft)
        assertEquals(LimitAction.NONE, r.action)
    }

    @Test fun `strict mode flag is passed to the overlay`() = runTest {
        settings.update { it.copy(strictMode = true, dailyLimit = 1) }
        val r = record(TrackedApp.FACEBOOK)!!
        assertEquals(LimitAction.SHOW_OVERLAY, r.action)
        assertTrue(r.strictMode)
    }
}
