package com.reelmeter.domain.logic

import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.reelmeter.domain.model.TrackedApp
import kotlin.math.abs

enum class InstagramSection {
    REELS,
    HOME_FEED,
    MESSAGES,
    SEARCH_EXPLORE,
    PROFILE,
    UNKNOWN
}

/**
 * Multi-signal detector that accurately identifies Reel page transitions
 * while filtering out comment list scrolling, caption clicks, likes, and feed navigation.
 * Privacy Guaranteed: canRetrieveWindowContent="false".
 */
class ReelTransitionDetector(
    private val minTransitionIntervalMs: Long = 1200L
) {
    private val TAG = "ReelMeter"
    private val lastCountTimestamp = HashMap<TrackedApp, Long>()
    private val lastSeenIndexMap = HashMap<TrackedApp, Int>()
    var currentInstagramSection: InstagramSection = InstagramSection.UNKNOWN
        private set

    fun detectInstagramSection(event: AccessibilityEvent): InstagramSection {
        val className = event.className?.toString() ?: ""
        val lowerClass = className.lowercase()

        val textContent = (event.text + listOfNotNull(event.contentDescription)).joinToString(" ").lowercase()

        val section = when {
            lowerClass.contains("direct") || lowerClass.contains("inbox") || textContent.contains("message") -> {
                InstagramSection.MESSAGES
            }
            lowerClass.contains("explore") || textContent.contains("search") -> {
                InstagramSection.SEARCH_EXPLORE
            }
            lowerClass.contains("profile") || textContent.contains("edit profile") -> {
                InstagramSection.PROFILE
            }
            lowerClass.contains("clips") || lowerClass.contains("reels") || textContent.contains("reels") -> {
                InstagramSection.REELS
            }
            lowerClass.contains("mainactivity") || lowerClass.contains("feed") || textContent.contains("home") -> {
                if (textContent.contains("reels")) InstagramSection.REELS else InstagramSection.HOME_FEED
            }
            else -> InstagramSection.UNKNOWN
        }

        if (section != InstagramSection.UNKNOWN && section != currentInstagramSection) {
            currentInstagramSection = section
            when (section) {
                InstagramSection.REELS -> {
                    log("[ReelMeter] Instagram foreground detected")
                    log("[ReelMeter] Reels screen confirmed")
                }
                InstagramSection.HOME_FEED -> log("[ReelMeter] Instagram Home detected")
                InstagramSection.MESSAGES -> {
                    log("[ReelMeter] Instagram Messages detected")
                    log("[ReelMeter] Reel tracking paused")
                }
                InstagramSection.SEARCH_EXPLORE -> log("[ReelMeter] Instagram Search detected")
                InstagramSection.PROFILE -> log("[ReelMeter] Instagram Profile detected")
                else -> Unit
            }
        }
        return currentInstagramSection
    }

    fun isReelContainerEvent(className: String?): Boolean {
        if (className == null) return true
        val lower = className.lowercase()

        // Ignore comment drawers, typing, buttons, text clicks, and reactions
        if (lower.contains("comment") ||
            lower.contains("bottomsheet") ||
            lower.contains("edittext") ||
            lower.contains("button") ||
            lower.contains("actionbar") ||
            lower.contains("dialog")
        ) {
            log("[ReelMeter] Comments scrolling - ignored")
            return false
        }

        return true
    }

    fun isNewReelTransition(
        app: TrackedApp,
        event: AccessibilityEvent,
        nowMillis: Long
    ): Boolean {
        if (app == TrackedApp.INSTAGRAM) {
            val section = detectInstagramSection(event)
            if (section != InstagramSection.REELS && section != InstagramSection.UNKNOWN) {
                return false
            }
        }

        // 1. Debouncing Check
        val prevTimestamp = lastCountTimestamp[app]
        if (prevTimestamp != null && (nowMillis - prevTimestamp) < minTransitionIntervalMs) {
            log("[ReelMeter] Duplicate event ignored")
            return false
        }

        // 2. Class Name Container Check
        val className = event.className?.toString()
        if (!isReelContainerEvent(className)) {
            return false
        }

        // 3. Page Selected Event Check (ViewPager2 / RecyclerView page settlement)
        val toIdx = event.toIndex
        val lastSeenIdx = lastSeenIndexMap[app]

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SELECTED && toIdx >= 0) {
            if (lastSeenIdx == toIdx) {
                log("[ReelMeter] Same Reel detected - no increment")
                return false
            }
            log("[ReelMeter] Reel page selected: index $toIdx")
            lastSeenIndexMap[app] = toIdx
            lastCountTimestamp[app] = nowMillis
            log("[ReelMeter] New Reel confirmed")
            return true
        }

        // 4. Scroll Displacement Check (Filters out partial swipes / half drags)
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val deltaY = event.scrollDeltaY
                if (deltaY != 0 && abs(deltaY) < 250) {
                    log("[ReelMeter] Partial swipe detected - ignored")
                    return false
                }
            }
        }

        log("[ReelMeter] New Reel confirmed")
        lastCountTimestamp[app] = nowMillis
        return true
    }

    private fun log(message: String) {
        runCatching { Log.d(TAG, message) }
    }

    fun resetApp(app: TrackedApp) {
        lastCountTimestamp.remove(app)
        lastSeenIndexMap.remove(app)
        if (app == TrackedApp.INSTAGRAM) {
            currentInstagramSection = InstagramSection.UNKNOWN
        }
    }

    fun reset() {
        lastCountTimestamp.clear()
        lastSeenIndexMap.clear()
        currentInstagramSection = InstagramSection.UNKNOWN
    }
}
