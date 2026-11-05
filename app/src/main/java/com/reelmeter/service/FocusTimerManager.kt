package com.reelmeter.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.reelmeter.core.TimeProvider
import com.reelmeter.domain.logic.FocusTimerLogic
import com.reelmeter.domain.logic.FocusTimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusTimerManager @Inject constructor(
    private val timeProvider: TimeProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _timerState = MutableStateFlow<FocusTimerState>(FocusTimerState.Idle)
    val timerState: StateFlow<FocusTimerState> = _timerState.asStateFlow()

    // Tick counter incremented every second so collectors always get a new emission
    // even when the sealed-class state object hasn't structurally changed.
    private val _tickMs = MutableStateFlow(timeProvider.nowMillis())
    /** Always-ticking wall-clock millis. Collect this in the UI instead of System.currentTimeMillis(). */
    val tickMs: StateFlow<Long> = _tickMs.asStateFlow()

    init {
        // Ticking loop: evaluate state transitions AND force a tick every second so
        // the UI can recompose the countdown digits without relying on structural equality.
        scope.launch {
            while (true) {
                delay(500) // 500 ms gives sub-second visual smoothness
                val now = timeProvider.nowMillis()
                _tickMs.value = now

                val current = _timerState.value
                val next = FocusTimerLogic.evaluate(current, now)
                // Always reassign even if structurally equal — StateFlow de-dupes by referential
                // equality for data classes, so force a new object when the timer is active.
                _timerState.value = when {
                    next is FocusTimerState.Running   -> next.copy()
                    next is FocusTimerState.Unlocking -> next.copy()
                    else -> next
                }
            }
        }
    }

    fun startTimer(context: Context, minutes: Int) {
        val now = timeProvider.nowMillis()
        val running = FocusTimerLogic.startTimer(minutes, now)
        _timerState.value = running
        startService(context)
    }

    fun startUnlocking() {
        val now = timeProvider.nowMillis()
        _timerState.value = FocusTimerLogic.startUnlocking(now)
    }

    fun cancelTimer() {
        _timerState.value = FocusTimerState.Idle
    }

    private fun startService(context: Context) {
        val intent = Intent(context, FocusTimerService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }
}
