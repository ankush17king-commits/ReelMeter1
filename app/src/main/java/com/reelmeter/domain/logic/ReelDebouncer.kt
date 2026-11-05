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
    /**
     * Returns true if the scroll looks like a vertical swipe (reel navigation).
     *
     * Rules:
     * - If both deltas are 0 (API < 28, values unavailable) → allow (can't determine direction).
     * - If vertical delta is non-zero, verify vertical movement dominates horizontal.
     * - If deltaY is 0 (common on apps like YouTube that don't report vertical delta),
     *   only reject if deltaX is a significant horizontal swipe (>= 40px).
     */
    fun isVerticalSwipe(deltaX: Int, deltaY: Int): Boolean {
        if (deltaX == 0 && deltaY == 0) return true
        if (deltaY != 0) {
            return abs(deltaY) >= abs(deltaX)
        }
        return abs(deltaX) <= 120
    }
}
