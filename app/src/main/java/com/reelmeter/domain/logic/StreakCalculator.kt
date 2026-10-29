package com.reelmeter.domain.logic

import java.time.LocalDate

data class DayResult(val total: Int, val limit: Int)

object StreakCalculator {
    /**
     * Number of consecutive days (ending today) that stayed under the limit.
     * Days without data count as 0 reels. The streak never goes back before [installDay].
     * If today is already at/over the limit the streak is 0.
     */
    fun compute(
        today: LocalDate,
        installDay: LocalDate,
        days: Map<LocalDate, DayResult>,
        currentLimit: Int,
    ): Int {
        var streak = 0
        var d = today
        while (!d.isBefore(installDay)) {
            val result = days[d]
            val total = result?.total ?: 0
            val limit = result?.limit ?: currentLimit
            if (total < limit) streak++ else break
            d = d.minusDays(1)
        }
        return streak
    }
}
