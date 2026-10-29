package com.reelmeter.domain.logic

import com.reelmeter.domain.model.LimitAction
import com.reelmeter.domain.model.LimitStatus
import kotlin.math.ceil

object LimitEvaluator {
    const val WARNING_FRACTION = 0.8

    fun warningThreshold(limit: Int): Int = ceil(limit * WARNING_FRACTION).toInt().coerceAtLeast(1)

    fun status(total: Int, limit: Int): LimitStatus = when {
        total >= limit -> LimitStatus.REACHED
        total >= warningThreshold(limit) -> LimitStatus.WARNING
        else -> LimitStatus.NORMAL
    }

    /**
     * Decides what to do right after a reel was counted.
     * - at/above the limit -> overlay (unless the user is inside a "+5 minutes" snooze)
     * - at/above 80%       -> one single warning per day (if reminders are on)
     */
    fun decide(
        total: Int,
        limit: Int,
        warnedAlready: Boolean,
        remindersEnabled: Boolean,
        snoozeUntil: Long,
        now: Long,
        strictnessMode: String = "Gentle",
    ): LimitAction {
        val fraction = if (limit > 0) total.toFloat() / limit else 0f
        val warningThresholdFraction = if (strictnessMode == "Balanced" || strictnessMode == "Focus") 0.75f else 0.80f

        return when {
            total >= limit -> if (now >= snoozeUntil) LimitAction.SHOW_OVERLAY else LimitAction.NONE
            fraction >= warningThresholdFraction && !warnedAlready && remindersEnabled -> LimitAction.NOTIFY_WARNING
            else -> LimitAction.NONE
        }
    }
}
