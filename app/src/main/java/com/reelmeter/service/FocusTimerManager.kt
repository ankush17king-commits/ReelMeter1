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

    init {
        // Ticking loop to evaluate state transitions automatically & update UI timer digits
        scope.launch {
            while (true) {
                delay(1000)
                val current = _timerState.value
                val now = timeProvider.nowMillis()
                val evaluated = FocusTimerLogic.evaluate(current, now)
                _timerState.value = evaluated
                if (evaluated is FocusTimerState.Running) {
                    _timerState.value = evaluated.copy()
                } else if (evaluated is FocusTimerState.Unlocking) {
                    _timerState.value = evaluated.copy()
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
