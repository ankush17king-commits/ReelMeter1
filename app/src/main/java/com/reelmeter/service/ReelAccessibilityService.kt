package com.reelmeter.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.reelmeter.core.TimeProvider
import com.reelmeter.domain.logic.ReelDebouncer
import com.reelmeter.domain.logic.SwipeFilter
import com.reelmeter.domain.model.LimitAction
import com.reelmeter.domain.model.RecordResult
import com.reelmeter.domain.model.TrackedApp
import com.reelmeter.domain.repository.ReelRepository
import com.reelmeter.domain.repository.SettingsRepository
import com.reelmeter.domain.usecase.ExtendLimitUseCase
import com.reelmeter.domain.usecase.RecordReelUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PRIVACY: this service only looks at the event type, the package name and the scroll deltas
 * (numbers). It never touches event text, content descriptions, node trees or window content
 * (canRetrieveWindowContent="false" in accessibility_service_config.xml enforces this).
 * Persisted data: app name, timestamp, count.
 */
@AndroidEntryPoint
class ReelAccessibilityService : AccessibilityService() {

    @Inject lateinit var recordReel: RecordReelUseCase
    @Inject lateinit var extendLimit: ExtendLimitUseCase
    @Inject lateinit var time: TimeProvider
    @Inject lateinit var notifications: NotificationHelper
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var reelRepository: ReelRepository
    @Inject lateinit var focusTimerManager: FocusTimerManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val debouncer = ReelDebouncer()
    private lateinit var overlay: OverlayController
    private lateinit var floatingCounter: FloatingCounterController
    private var activeTrackedApp: TrackedApp? = null
    private var sessionReelCount: Int = 0
    private var lastSwipeTimestamp: Long = 0L

    fun isInsideBlackoutWindow(nowHour: Int, startHour: Int, endHour: Int): Boolean {
        return if (startHour > endHour) {
            nowHour >= startHour || nowHour < endHour
        } else {
            nowHour in startHour until endHour
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlay = OverlayController(this)
        floatingCounter = FloatingCounterController(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            val app = TrackedApp.fromPackage(packageName)
            activeTrackedApp = app
            if (app != null) {
                scope.launch { updateFloatingCounterState() }
            } else {
                floatingCounter.dismiss()
            }
            return
        }

        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_SELECTED
        ) return
        if (!::overlay.isInitialized || overlay.isShowing) return
        val app = TrackedApp.fromPackage(packageName) ?: return
        activeTrackedApp = app // Keep activeTrackedApp current on all reel events

        val dx: Int
        val dy: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            dx = event.scrollDeltaX; dy = event.scrollDeltaY
        } else {
            dx = 0; dy = 0
        }
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && !SwipeFilter.isVerticalSwipe(dx, dy)) return
        if (!debouncer.onScrollEvent(app, time.nowMillis())) return

        scope.launch { handleCounted(app) }
    }

    private suspend fun updateFloatingCounterState() {
        val focusState = focusTimerManager.timerState.value
        if ((focusState is com.reelmeter.domain.logic.FocusTimerState.Running || focusState is com.reelmeter.domain.logic.FocusTimerState.Locked) && activeTrackedApp != null) {
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                floatingCounter.dismiss()
                performGlobalAction(GLOBAL_ACTION_HOME)
                overlay.show(
                    OverlayConfig(
                        total = 0,
                        limit = 0,
                        strictMode = true,
                        extensionsLeft = 0,
                        onCloseApp = { overlay.dismiss(); performGlobalAction(GLOBAL_ACTION_HOME) },
                        onExtend = { overlay.dismiss(); performGlobalAction(GLOBAL_ACTION_HOME) },
                        onDone = { overlay.dismiss(); performGlobalAction(GLOBAL_ACTION_HOME) }
                    )
                )
            }
            return
        }

        val settings = settingsRepository.current()
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)

        if (settings.blackoutEnabled && isInsideBlackoutWindow(currentHour, settings.blackoutStartHour, settings.blackoutEndHour) && activeTrackedApp != null) {
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                floatingCounter.dismiss()
                performGlobalAction(GLOBAL_ACTION_HOME)
                overlay.show(
                    OverlayConfig(
                        total = 0,
                        limit = 0,
                        strictMode = true,
                        extensionsLeft = 0,
                        onCloseApp = { overlay.dismiss(); performGlobalAction(GLOBAL_ACTION_HOME) },
                        onExtend = { overlay.dismiss(); performGlobalAction(GLOBAL_ACTION_HOME) },
                        onDone = { overlay.dismiss(); performGlobalAction(GLOBAL_ACTION_HOME) }
                    )
                )
            }
            return
        }

        val today = time.today()
        val stat = reelRepository.observeDay(today).firstOrNull()
        val total = stat?.total ?: 0
        val limit = settings.dailyLimit

        if (total >= limit && activeTrackedApp != null) {
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                floatingCounter.dismiss()
                showOverlay(
                    RecordResult(
                        app = activeTrackedApp ?: TrackedApp.INSTAGRAM,
                        appCount = 0,
                        total = total,
                        limit = limit,
                        action = LimitAction.SHOW_OVERLAY,
                        strictMode = settings.strictMode,
                        extensionsLeft = (com.reelmeter.domain.model.Limits.MAX_EXTENSIONS_PER_DAY - (stat?.extensionsUsed ?: 0)).coerceAtLeast(0),
                    )
                )
            }
            return
        }

        kotlinx.coroutines.withContext(Dispatchers.Main) {
            if (!settings.showFloatingCounter || activeTrackedApp == null) {
                floatingCounter.dismiss()
            } else {
                floatingCounter.showOrUpdate(total, limit)
            }
        }
    }

    private suspend fun handleCounted(app: TrackedApp) {
        val result = runCatching { recordReel(app) }.getOrNull() ?: return

        val now = time.nowMillis()
        if (now - lastSwipeTimestamp > 5 * 60 * 1000L) {
            sessionReelCount = 0
        }
        lastSwipeTimestamp = now
        sessionReelCount++

        // Update home screen widgets immediately
        ReelMeterWidgetProvider.updateAllWidgets(this@ReelAccessibilityService)

        // Update floating counter if enabled and user is currently inside this tracked app
        val settings = settingsRepository.current()

        if (settings.doomscrollAlertEnabled && sessionReelCount >= settings.doomscrollThreshold) {
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(
                    this@ReelAccessibilityService,
                    "👁️ Eye Rest Warning: $sessionReelCount reels in a row! Time for a short break.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }

        kotlinx.coroutines.withContext(Dispatchers.Main) {
            if (settings.showFloatingCounter && activeTrackedApp == app) {
                floatingCounter.showOrUpdate(result.total, result.limit)
            } else {
                floatingCounter.dismiss()
            }

            when (result.action) {
                LimitAction.NOTIFY_WARNING -> notifications.showWarning(result.total, result.limit)
                LimitAction.SHOW_OVERLAY -> showOverlay(result)
                LimitAction.NONE -> Unit
            }
        }
    }

    private fun showOverlay(result: RecordResult) {
        floatingCounter.dismiss()
        performGlobalAction(GLOBAL_ACTION_HOME)
        overlay.show(
            OverlayConfig(
                total = result.total,
                limit = result.limit,
                strictMode = result.strictMode,
                extensionsLeft = result.extensionsLeft,
                onCloseApp = { overlay.dismiss(); performGlobalAction(GLOBAL_ACTION_HOME) },
                onExtend = {
                    scope.launch { if (extendLimit()) overlay.dismiss() }
                },
                onDone = { overlay.dismiss(); performGlobalAction(GLOBAL_ACTION_HOME) },
            )
        )
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (::overlay.isInitialized) overlay.dismiss()
        if (::floatingCounter.isInitialized) floatingCounter.dismiss()
        scope.cancel()
        super.onDestroy()
    }
}
