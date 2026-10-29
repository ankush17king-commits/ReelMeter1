package com.reelmeter.domain.logic

import com.reelmeter.domain.model.TrackedApp
import kotlin.math.abs

/**
 * A new reel is counted when at least [minCountIntervalMs] has passed
 * since the last counted reel for that app.
 */
class ReelDebouncer(
    private val minCountIntervalMs: Long = 1200L,
) {
    private val lastCount = HashMap<TrackedApp, Long>()

    /** @return true if this scroll event represents a NEW reel. */
    fun onScrollEvent(app: TrackedApp, now: Long): Boolean {
        val previousCount = lastCount[app]
        if (previousCount != null && now - previousCount < minCountIntervalMs) {
            return false
        }
        lastCount[app] = now
        return true
    }

    fun reset() {
        lastCount.clear()
    }
}

/** Ignores horizontal-only scrolls (carousels, stories tray). Deltas are 0 when unavailable (API < 28). */
object SwipeFilter {
    fun isVerticalSwipe(deltaX: Int, deltaY: Int): Boolean =
        (deltaX == 0 && deltaY == 0) || abs(deltaY) >= abs(deltaX) || abs(deltaX) < 20
}
