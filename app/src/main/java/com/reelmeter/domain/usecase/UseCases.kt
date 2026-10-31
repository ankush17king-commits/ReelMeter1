package com.reelmeter.domain.usecase

import com.reelmeter.core.TimeProvider
import com.reelmeter.domain.logic.DayResult
import com.reelmeter.domain.logic.LimitEvaluator
import com.reelmeter.domain.logic.StreakCalculator
import com.reelmeter.domain.model.DashboardData
import com.reelmeter.domain.model.DayBar
import com.reelmeter.domain.model.HistoryData
import com.reelmeter.domain.model.LimitAction
import com.reelmeter.domain.model.Limits
import com.reelmeter.domain.model.MonthSummary
import com.reelmeter.domain.model.RecordResult
import com.reelmeter.domain.model.TrackedApp
import com.reelmeter.domain.model.UserSettings
import com.reelmeter.domain.repository.ReelRepository
import com.reelmeter.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/** Counts one reel and tells the caller what to do next (nothing / warn / show overlay). */
@Singleton
class RecordReelUseCase @Inject constructor(
    private val reels: ReelRepository,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) {
    private val mutex = Mutex()

    /** @return null when [app] is not tracked. */
    suspend operator fun invoke(app: TrackedApp): RecordResult? = mutex.withLock {
        val s = settings.current()
        if (app !in s.trackedApps) return@withLock null
        val now = time.nowMillis()
        val day = time.today() // read on every event => midnight rollover is automatic
        val stat = reels.recordReel(app, now, day, s.dailyLimit)
        val action = LimitEvaluator.decide(
            total = stat.total,
            limit = s.dailyLimit,
            warnedAlready = stat.warned,
            remindersEnabled = s.remindersEnabled,
            snoozeUntil = stat.snoozeUntil,
            now = now,
        )
        if (action == LimitAction.NOTIFY_WARNING) reels.markWarned(day)
        RecordResult(
            app = app,
            appCount = stat.perApp[app] ?: 0,
            total = stat.total,
            limit = s.dailyLimit,
            action = action,
            strictMode = s.strictMode,
            extensionsLeft = (Limits.MAX_EXTENSIONS_PER_DAY - stat.extensionsUsed).coerceAtLeast(0),
        )
    }
}

/** "+5 more minutes" (max 2 per day). */
@Singleton
class ExtendLimitUseCase @Inject constructor(
    private val reels: ReelRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(): Boolean = reels.useExtension(
        day = time.today(),
        snoozeUntil = time.nowMillis() + Limits.EXTENSION_MINUTES * 60_000L,
    )
}

class ObserveDashboardUseCase @Inject constructor(
    private val reels: ReelRepository,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<DashboardData> = time.todayFlow().flatMapLatest { today ->
        combine(
            settings.settings,
            reels.observeDay(today),
            reels.observeRange(today.minusDays(365), today),
        ) { s, todayStat, range ->
            val byDay = range.associateBy { it.day }
            val week = (6 downTo 0).map { offset ->
                val d = today.minusDays(offset.toLong())
                val stat = if (d == today) todayStat else byDay[d]
                val limit = if (d == today) s.dailyLimit else (stat?.limitSnapshot ?: s.dailyLimit)
                DayBar(d, stat?.total ?: 0, limit)
            }
            val results = range.associate {
                it.day to DayResult(it.total, if (it.day == today) s.dailyLimit else it.limitSnapshot)
            }
            DashboardData(
                today = today,
                total = todayStat?.total ?: 0,
                limit = s.dailyLimit,
                perApp = TrackedApp.entries.associateWith { todayStat?.perApp?.get(it) ?: 0 },
                trackedApps = s.trackedApps,
                secondsPerReel = s.secondsPerReel,
                week = week,
                streak = StreakCalculator.compute(today, s.installDay, results, s.dailyLimit),
                userName = s.userName,
            )
        }
    }
}

class ObserveHistoryUseCase @Inject constructor(
    private val reels: ReelRepository,
    private val settings: SettingsRepository,
) {
    operator fun invoke(): Flow<HistoryData> = combine(reels.observeAll(), settings.settings) { all, s ->
        val months = all
            .groupBy { YearMonth.from(it.day) }
            .map { (month, list) ->
                MonthSummary(
                    month = month,
                    totalReels = list.sumOf { it.total },
                    daysTracked = list.count { it.total > 0 },
                    daysOverLimit = list.count { it.total >= it.limitSnapshot },
                )
            }
            .sortedByDescending { it.month }
        HistoryData(days = all.filter { it.total > 0 }, months = months, secondsPerReel = s.secondsPerReel)
    }
}

class ObserveAdvancedStatsUseCase @Inject constructor(
    private val reels: ReelRepository,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(period: com.reelmeter.domain.model.StatsPeriod): Flow<com.reelmeter.domain.model.AdvancedStatsData> = time.todayFlow().flatMapLatest { today ->
        combine(
            reels.observeAll(),
            reels.observeTodayEvents(today),
            settings.settings
        ) { all, todayEvents, s ->
            val daysCount = period.daysCount
            val byDay = all.associateBy { it.day }

            // Today 24-hour breakdown
            val hourMap = (0..23).associateWith { h ->
                todayEvents.count { event ->
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = event.timestamp }
                    cal.get(java.util.Calendar.HOUR_OF_DAY) == h
                }
            }
            val todayHourlyBars = (0..23).map { h ->
                DayBar(today, hourMap[h] ?: 0, s.dailyLimit)
            }

            // Current period bars
            val chartBars = ((daysCount - 1) downTo 0).map { offset ->
                val d = today.minusDays(offset.toLong())
                val stat = byDay[d]
                DayBar(d, stat?.total ?: 0, stat?.limitSnapshot ?: s.dailyLimit)
            }

            val currentPeriodStats = (0 until daysCount).map { offset ->
                val d = today.minusDays(offset.toLong())
                byDay[d] ?: com.reelmeter.domain.model.DayStat(d, emptyMap(), s.dailyLimit, false, 0, 0L)
            }

            val totalReelsPeriod = currentPeriodStats.sumOf { it.total }
            val dailyAverage = totalReelsPeriod.toDouble() / daysCount

            // Previous period comparison
            val previousPeriodStats = (daysCount until (daysCount * 2)).map { offset ->
                val d = today.minusDays(offset.toLong())
                byDay[d] ?: com.reelmeter.domain.model.DayStat(d, emptyMap(), s.dailyLimit, false, 0, 0L)
            }
            val prevPeriodTotal = previousPeriodStats.sumOf { it.total }

            val percentageChange = if (prevPeriodTotal > 0) {
                ((prevPeriodTotal - totalReelsPeriod).toDouble() / prevPeriodTotal) * 100.0
            } else null

            val reelsAvoided = (prevPeriodTotal - totalReelsPeriod).coerceAtLeast(0)
            val estimatedTimeSavedMinutes = (reelsAvoided.toLong() * s.secondsPerReel) / 60

            val bestDay = currentPeriodStats.filter { it.total > 0 }.minByOrNull { it.total }
            val peakDay = currentPeriodStats.maxByOrNull { it.total }

            // App breakdown
            val appCounts = TrackedApp.entries.associateWith { app ->
                currentPeriodStats.sumOf { it.perApp[app] ?: 0 }
            }
            val prevAppCounts = TrackedApp.entries.associateWith { app ->
                previousPeriodStats.sumOf { it.perApp[app] ?: 0 }
            }

            val appAnalytics = TrackedApp.entries.map { app ->
                val count = appCounts[app] ?: 0
                val share = if (totalReelsPeriod > 0) (count.toDouble() / totalReelsPeriod) * 100.0 else 0.0
                val timeMins = (count.toLong() * s.secondsPerReel) / 60
                com.reelmeter.domain.model.AppAnalyticsSummary(
                    app = app,
                    count = count,
                    timeMinutes = timeMins,
                    percentageShare = share,
                    prevWeekCount = prevAppCounts[app] ?: 0
                )
            }.sortedByDescending { it.count }

            val mostUsedApp = appAnalytics.firstOrNull { it.count > 0 }?.app

            // Streak computation
            val results = all.associate {
                it.day to DayResult(it.total, if (it.day == today) s.dailyLimit else it.limitSnapshot)
            }
            val streak = StreakCalculator.compute(today, s.installDay, results, s.dailyLimit)

            // Insights list
            val insights = mutableListOf<String>()
            if (percentageChange != null) {
                if (percentageChange > 0) {
                    insights.add("🎉 Great job! You watched ${percentageChange.toInt()}% fewer reels than the previous $daysCount days.")
                } else if (percentageChange < 0) {
                    insights.add("📈 Heads up: Your reel usage increased by ${kotlin.math.abs(percentageChange.toInt())}% compared to the previous period.")
                }
            }
            if (mostUsedApp != null) {
                insights.add("📱 ${mostUsedApp.label} is your most active app.")
            }
            if (bestDay != null) {
                insights.add("🌟 Your best day was ${bestDay.day.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }} with only ${bestDay.total} reels.")
            }

            val months = all
                .groupBy { YearMonth.from(it.day) }
                .map { (month, list) ->
                    MonthSummary(
                        month = month,
                        totalReels = list.sumOf { it.total },
                        daysTracked = list.count { it.total > 0 },
                        daysOverLimit = list.count { it.total >= it.limitSnapshot },
                    )
                }
                .sortedByDescending { it.month }

            com.reelmeter.domain.model.AdvancedStatsData(
                selectedPeriod = period,
                today = today,
                totalReelsPeriod = totalReelsPeriod,
                dailyAverageReels = dailyAverage,
                bestDay = bestDay,
                peakDay = peakDay,
                prevPeriodTotal = prevPeriodTotal,
                percentageChange = percentageChange,
                reelsAvoided = reelsAvoided,
                estimatedTimeSavedMinutes = estimatedTimeSavedMinutes,
                chartBars = chartBars,
                todayHourlyBars = todayHourlyBars,
                appAnalytics = appAnalytics,
                mostUsedApp = mostUsedApp,
                insights = insights,
                streak = streak,
                days = all.filter { it.total > 0 },
                months = months,
                secondsPerReel = s.secondsPerReel,
                dailyLimit = s.dailyLimit
            )
        }
    }
}

class UpdateSettingsUseCase @Inject constructor(private val settings: SettingsRepository) {
    suspend operator fun invoke(transform: (UserSettings) -> UserSettings) = settings.update(transform)
}

class ResetDataUseCase @Inject constructor(private val reels: ReelRepository) {
    suspend operator fun invoke() = reels.resetAll()
}

class PurgeOldEventsUseCase @Inject constructor(
    private val reels: ReelRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke() =
        reels.purgeEventsBefore(time.nowMillis() - Limits.EVENT_RETENTION_DAYS * 86_400_000L)
}
