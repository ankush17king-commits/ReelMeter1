package com.reelmeter.domain.logic

sealed interface FocusTimerState {
    data object Idle : FocusTimerState
    data class Running(val startTimeMs: Long, val endTimeMs: Long, val totalDurationMs: Long) : FocusTimerState {
        fun remainingMs(now: Long): Long = (endTimeMs - now).coerceAtLeast(0L)
        fun isFinished(now: Long): Boolean = now >= endTimeMs
    }
    data class Locked(val lockedAtMs: Long) : FocusTimerState
    data class Unlocking(val breathingUntilMs: Long) : FocusTimerState {
        fun remainingSeconds(now: Long): Int = ((breathingUntilMs - now + 999) / 1000).coerceAtLeast(0).toInt()
        fun isFinished(now: Long): Boolean = now >= breathingUntilMs
    }
}

object FocusTimerLogic {
    fun evaluate(state: FocusTimerState, now: Long): FocusTimerState {
        return when (state) {
            is FocusTimerState.Running -> {
                if (state.isFinished(now)) FocusTimerState.Locked(now) else state
            }
            is FocusTimerState.Unlocking -> {
                if (state.isFinished(now)) FocusTimerState.Idle else state
            }
            else -> state
        }
    }

    fun startTimer(durationMinutes: Int, now: Long): FocusTimerState.Running {
        val durationMs = durationMinutes * 60_000L
        return FocusTimerState.Running(
            startTimeMs = now,
            endTimeMs = now + durationMs,
            totalDurationMs = durationMs
        )
    }

    fun startUnlocking(now: Long): FocusTimerState.Unlocking {
        return FocusTimerState.Unlocking(breathingUntilMs = now + 60_000L)
    }
}
