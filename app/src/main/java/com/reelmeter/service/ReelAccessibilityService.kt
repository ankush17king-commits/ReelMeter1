package com.reelmeter.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.reelmeter.core.TimeProvider
import kotlin.math.abs
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
    private val transitionDetector = com.reelmeter.domain.logic.ReelTransitionDetector()
    private val lastKnownTotalMap = HashMap<java.time.LocalDate, Int>()
    private lateinit var overlay: OverlayController
    private lateinit var floatingCounter: FloatingCounterController
    private var activeTrackedApp: TrackedApp? = null
    private var sessionReelCount: Int = 0
    private var lastSwipeTimestamp: Long = 0L

    // Debounce handler: prevents transient window events from flickering the counter
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val dismissCounterRunnable = Runnable {
        if (activeTrackedApp == null) {
            floatingCounter.dismiss()
        }
    }

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
            // Ignore transient system overlays (volume bar, systemui, toasts, keyboard) to prevent flickering
            if (packageName == "android" || packageName == "com.android.systemui" || packageName.contains("inputmethod")) {
                return
            }

            val previousApp = activeTrackedApp
            val app = TrackedApp.fromPackage(packageName)

            if (app != null) {
                // Cancel any pending dismiss — user is still inside a tracked app
                mainHandler.removeCallbacks(dismissCounterRunnable)
                activeTrackedApp = app

                if (previousApp != app) {
                    android.util.Log.d("ReelMeter", "[ReelMeter] ${app.label} opened")
                    transitionDetector.onAppOpened(app)
                    scope.launch { updateFloatingCounterState() }
                } else {
                    transitionDetector.onNavigationEvent(app)
                    if (!floatingCounter.isShowing) {
                        scope.launch { updateFloatingCounterState() }
                    }
                }
            } else if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                // Only act on definitive foreground-app transitions (not transient window changes).
                // Schedule the dismiss with a short delay so that brief system overlays (e.g.
                // share sheets, permission dialogs) that immediately return focus to Instagram
                // don't cause the counter to flash off and back on.
                activeTrackedApp = null
                if (previousApp != null) {
                    android.util.Log.d("ReelMeter", "[ReelMeter] ${previousApp.label} closed")
                    android.util.Log.d("ReelMeter", "[ReelMeter] Reel tracking paused")
                    android.util.Log.d("ReelMeter", "[ReelMeter] Session saved")
                    transitionDetector.resetApp(previousApp)
                }
                // Debounce: wait 800 ms before dismissing; if a tracked-app event arrives
                // in that window the runnable is cancelled and the counter stays visible.
                mainHandler.removeCallbacks(dismissCounterRunnable)
                mainHandler.postDelayed(dismissCounterRunnable, 800L)
            }
            // TYPE_WINDOWS_CHANGED for non-tracked packages is intentionally ignored here —
            // those events fire for every floating window/toast/notification and are too noisy
            // to use for counter visibility decisions.
            return
        }

        // ── Scroll / Selected events (reel counting) ───────────────────────────
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_SELECTED
        ) return
        if (!::overlay.isInitialized || overlay.isShowing) return
        val app = TrackedApp.fromPackage(packageName) ?: return

        // Cancel any pending dismiss — a scroll means the user is actively inside the app
        mainHandler.removeCallbacks(dismissCounterRunnable)
        activeTrackedApp = app

        // Ensure floating counter is active when user is in a tracked app
        if (!floatingCounter.isShowing) {
            scope.launch { updateFloatingCounterState() }
        }

        // Pre-filter: horizontal-only scrolls never count (story trays, carousels)
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val dx = event.scrollDeltaX
            val dy = event.scrollDeltaY
            // For YouTube Shorts, ViewPager2 consumes vertical scroll and may report dy=0 with small touch drift dx.
            // Do NOT discard YouTube Shorts scrolls unless dx is a massive horizontal swipe (e.g. > 180px)
            if (app != TrackedApp.YOUTUBE && !SwipeFilter.isVerticalSwipe(dx, dy)) return
            if (app == TrackedApp.YOUTUBE && abs(dx) > 180 && dy == 0) return
        }

        if (!transitionDetector.isNewReelTransition(app, event, time.nowMillis())) return

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
        val dbTotal = stat?.total ?: 0
        val inMemTotal = lastKnownTotalMap[today] ?: 0
        val total = maxOf(dbTotal, inMemTotal)
        val limit = settings.dailyLimit
        val snoozeUntil = stat?.snoozeUntil ?: 0L
        val now = time.nowMillis()

        if (total >= limit && now >= snoozeUntil && activeTrackedApp != null) {
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
        val today = time.today()
        val currentMax = lastKnownTotalMap[today] ?: 0
        val effectiveTotal = maxOf(result.total, currentMax)
        lastKnownTotalMap[today] = effectiveTotal

        android.util.Log.d("ReelMeter", "[ReelMeter] Counter persisted: $effectiveTotal")
        android.util.Log.d("ReelMeter", "[ReelMeter] UI updated: $effectiveTotal")

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
            // Post the toast via the main handler without going through withContext — avoids
            // creating a temporary window context switch that can race with our counter state.
            mainHandler.post {
                android.widget.Toast.makeText(
                    this@ReelAccessibilityService,
                    "👁️ Eye Rest Warning: $sessionReelCount reels in a row! Time for a short break.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }

        kotlinx.coroutines.withContext(Dispatchers.Main) {
            if (settings.showFloatingCounter && activeTrackedApp != null) {
                // Show/update with effective total — don't dismiss just because activeTrackedApp
                // temporarily changed app reference mid-dispatch (race condition guard).
                floatingCounter.showOrUpdate(result.total, result.limit)
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
        mainHandler.removeCallbacks(dismissCounterRunnable)
        if (::overlay.isInitialized) overlay.dismiss()
        if (::floatingCounter.isInitialized) floatingCounter.dismiss()
        scope.cancel()
        super.onDestroy()
    }
}
