package com.reelmeter.domain.model

import java.time.LocalDate
import java.time.YearMonth

enum class TrackedApp(val packageName: String, val label: String) {
    INSTAGRAM("com.instagram.android", "Instagram"),
    YOUTUBE("com.google.android.youtube", "YouTube"),
    FACEBOOK("com.facebook.katana", "Facebook"),
    SNAPCHAT("com.snapchat.android", "Snapchat");

    companion object {
        fun fromPackage(packageName: String): TrackedApp? = entries.firstOrNull { it.packageName == packageName }
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

object Limits {
    const val DEFAULT_DAILY_LIMIT = 100
    const val DEFAULT_SECONDS_PER_REEL = 20
    const val MAX_EXTENSIONS_PER_DAY = 2
    const val EXTENSION_MINUTES = 5
    const val STRICT_COUNTDOWN_SECONDS = 60
    /** Raw swipe events (app, timestamp) are deleted after this many days; daily totals are kept. */
    const val EVENT_RETENTION_DAYS = 30
}

data class UserSettings(
    val dailyLimit: Int = Limits.DEFAULT_DAILY_LIMIT,
    val secondsPerReel: Int = Limits.DEFAULT_SECONDS_PER_REEL,
    val trackedApps: Set<TrackedApp> = TrackedApp.entries.toSet(),
    val remindersEnabled: Boolean = true,
    val strictMode: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val onboardingDone: Boolean = false,
    val disclosureAccepted: Boolean = false,
    val installDay: LocalDate = LocalDate.now(),
    val showFloatingCounter: Boolean = true,
    val occupation: String = "",
    val estimatedDailyReels: Int = 0,
    val primaryGoal: String = "",
    val userName: String = "",
    val selectedGoals: Set<String> = emptySet(),
    val estimatedDailyUsage: String = "",
    val unplannedOpenFrequency: String = "",
    val scrollTimesOfDay: Set<String> = emptySet(),
    val scrollAffectsAreas: Set<String> = emptySet(),
    val preferredSavedTimeActivities: Set<String> = emptySet(),
    val targetTimeSaved: String = "",
    val limitReachedBehavior: Set<String> = emptySet(),
    val strictnessMode: String = "Balanced",
    val resetTime: String = "Midnight",
    val blackoutEnabled: Boolean = false,
    val blackoutStartHour: Int = 23, // 11 PM
    val blackoutEndHour: Int = 7,   // 7 AM
    val doomscrollAlertEnabled: Boolean = true,
    val doomscrollThreshold: Int = 15,
    val limitLockedUntilEpochDay: Long = 0L,
)

data class DayStat(
    val day: LocalDate,
    val perApp: Map<TrackedApp, Int>,
    val limitSnapshot: Int,
    val warned: Boolean,
    val extensionsUsed: Int,
    val snoozeUntil: Long,
) {
    val total: Int get() = perApp.values.sum()
}

enum class LimitAction { NONE, NOTIFY_WARNING, SHOW_OVERLAY }
enum class LimitStatus { NORMAL, WARNING, REACHED }

data class RecordResult(
    val app: TrackedApp,
    val appCount: Int,
    val total: Int,
    val limit: Int,
    val action: LimitAction,
    val strictMode: Boolean,
    val extensionsLeft: Int,
)

data class DayBar(val day: LocalDate, val total: Int, val limit: Int)

data class DashboardData(
    val today: LocalDate,
    val total: Int,
    val limit: Int,
    val perApp: Map<TrackedApp, Int>,
    val trackedApps: Set<TrackedApp>,
    val secondsPerReel: Int,
    val week: List<DayBar>,
    val streak: Int,
    val userName: String = "",
) {
    val secondsLost: Long get() = total.toLong() * secondsPerReel
    val hasAnyDataThisWeek: Boolean get() = week.any { it.total > 0 }
}

data class MonthSummary(
    val month: YearMonth,
    val totalReels: Int,
    val daysTracked: Int,
    val daysOverLimit: Int,
)

enum class StatsPeriod(val label: String, val daysCount: Int) {
    TODAY("Today", 1),
    SEVEN_DAYS("7 Days", 7),
    THIRTY_DAYS("30 Days", 30)
}

data class AppAnalyticsSummary(
    val app: TrackedApp,
    val count: Int,
    val timeMinutes: Long,
    val percentageShare: Double, // 0.0 to 100.0
    val prevWeekCount: Int
)

data class AdvancedStatsData(
    val selectedPeriod: StatsPeriod = StatsPeriod.SEVEN_DAYS,
    val today: LocalDate = LocalDate.now(),
    val totalReelsPeriod: Int = 0,
    val dailyAverageReels: Double = 0.0,
    val bestDay: DayStat? = null,
    val peakDay: DayStat? = null,
    val prevPeriodTotal: Int = 0,
    val percentageChange: Double? = null, // ((prev - curr) / prev) * 100. Positive = reduced usage
    val reelsAvoided: Int = 0,
    val estimatedTimeSavedMinutes: Long = 0,
    val chartBars: List<DayBar> = emptyList(),
    val todayHourlyBars: List<DayBar> = emptyList(),
    val appAnalytics: List<AppAnalyticsSummary> = emptyList(),
    val mostUsedApp: TrackedApp? = null,
    val insights: List<String> = emptyList(),
    val streak: Int = 0,
    val days: List<DayStat> = emptyList(),
    val months: List<MonthSummary> = emptyList(),
    val secondsPerReel: Int = Limits.DEFAULT_SECONDS_PER_REEL,
    val dailyLimit: Int = Limits.DEFAULT_DAILY_LIMIT,
    val hasUsagePermission: Boolean = false,
    val foregroundTimeMapMinutes: Map<TrackedApp, Long> = emptyMap()
)

data class HistoryData(
    val days: List<DayStat>,
    val months: List<MonthSummary>,
    val secondsPerReel: Int,
)
