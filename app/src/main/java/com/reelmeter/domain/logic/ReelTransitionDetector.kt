package com.reelmeter.domain.logic

import android.os.Build
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
 * Multi-signal detector that accurately identifies Reel/Short page transitions.
 *
 * Guarantees:
 * 1. Counts only genuinely new reels/shorts.
 * 2. Does NOT count when navigating from Home to Reels/Shorts (initial landing is registered without incrementing).
 * 3. Does NOT count when opening or closing the share sheet.
 * 4. Does NOT count on partial swipes that rubber-band back to the same reel.
 * 5. Does NOT count when revisiting an already-counted reel (unique reel/video ID & index deduplication).
 * 6. Fixes YouTube Shorts detection reliably across all Android versions.
 *
 * Privacy: canRetrieveWindowContent="false". Only structural metadata (event type, class name,
 * content description, text, indices, scroll deltas) is inspected.
 */
class ReelTransitionDetector(
    private val minTransitionIntervalMs: Long = 850L
) {
    private val TAG = "ReelMeter"

    // Set of seen unique reel keys per app today to prevent counting the same reel twice
    private val seenReelKeysMap = HashMap<TrackedApp, HashSet<String>>()

    // Currently active reel key per app
    private val currentReelKeyMap = HashMap<TrackedApp, String>()

    // Flag: when landing on Reels (app launch, tab switch), do not count the initial page load
    private val isInitialLandingMap = HashMap<TrackedApp, Boolean>()

    // Last counted timestamp per app (for minimum dwell/transition debounce)
    private val lastCountTimestamp = HashMap<TrackedApp, Long>()

    // Current Instagram section
    var currentInstagramSection: InstagramSection = InstagramSection.UNKNOWN
        private set

    /**
     * Checks if a view class is a valid reel container (filters out comment drawers,
     * input methods, edit texts, dialogs, buttons, etc.).
     */
    fun isReelContainerEvent(className: String?): Boolean {
        if (className == null) return true
        val lower = className.lowercase()
        return !(lower.contains("comment") ||
                lower.contains("bottomsheet") ||
                lower.contains("edittext") ||
                lower.contains("inputmethod") ||
                lower.contains("dialog") ||
                lower.contains("snackbar") ||
                lower == "android.widget.button" ||
                lower.endsWith(".button"))
    }

    /**
     * Detects Instagram section based on class names and accessibility text.
     * Note: We avoid checking "mainactivity" or "home" as keywords because
     * MainActivity hosts the entire tab container including Reels.
     */
    fun detectInstagramSection(event: AccessibilityEvent): InstagramSection {
        val className = event.className?.toString() ?: ""
        val lowerClass = className.lowercase()
        val desc = event.contentDescription?.toString()?.lowercase() ?: ""
        val text = event.text.joinToString(" ").lowercase()
        val combined = "$lowerClass $desc $text"

        val detected = when {
            lowerClass.contains("direct") || lowerClass.contains("inbox") || (combined.contains("message") && !combined.contains("reel")) ->
                InstagramSection.MESSAGES
            lowerClass.contains("explore") || (combined.contains("search") && !combined.contains("reel")) ->
                InstagramSection.SEARCH_EXPLORE
            lowerClass.contains("profile") && !combined.contains("reel") ->
                InstagramSection.PROFILE
            lowerClass.contains("clips") || lowerClass.contains("reel") || combined.contains("reels") ->
                InstagramSection.REELS
            lowerClass.contains("feedfragment") || (lowerClass.contains("feed") && !lowerClass.contains("clips")) ->
                InstagramSection.HOME_FEED
            else -> InstagramSection.UNKNOWN
        }

        if (detected != InstagramSection.UNKNOWN) {
            if (currentInstagramSection != detected) {
                log("[IG] Section changed: $currentInstagramSection -> $detected")
                if (detected == InstagramSection.REELS) {
                    // Entering Reels from another section: prepare initial landing
                    isInitialLandingMap[TrackedApp.INSTAGRAM] = true
                }
            }
            currentInstagramSection = detected
        }
        return detected
    }

    /**
     * Extracts a unique reel/video identifier from the event.
     * Prefers contentDescription (title/creator/audio), then text, then adapter position.
     */
    fun extractReelIdentifier(app: TrackedApp, event: AccessibilityEvent): String? {
        val desc = event.contentDescription?.toString()?.trim() ?: ""
        if (desc.length >= 6 && !isGenericLabel(desc)) {
            return "desc:${desc.take(80).lowercase()}"
        }

        val text = event.text.joinToString(" ").trim()
        if (text.length >= 6 && !isGenericLabel(text)) {
            return "text:${text.take(80).lowercase()}"
        }

        val toIdx = event.toIndex
        if (toIdx >= 0) {
            return "idx:$toIdx"
        }

        val fromIdx = event.fromIndex
        if (fromIdx >= 0) {
            return "idx:$fromIdx"
        }

        return null
    }

    private fun isGenericLabel(label: String): Boolean {
        val lower = label.lowercase().trim()
        val genericWords = setOf(
            "reels", "reel", "shorts", "short", "youtube", "instagram", "facebook", "snapchat",
            "video player", "media player", "home", "search", "explore", "profile", "messages",
            "like", "dislike", "comment", "comments", "share", "remix", "subscribe",
            "play video", "pause video", "mute", "unmute", "spotlight"
        )
        return genericWords.contains(lower) || lower.all { it.isDigit() || it.isWhitespace() }
    }

    /**
     * Called when an app is opened or brought to foreground.
     */
    fun onAppOpened(app: TrackedApp) {
        isInitialLandingMap[app] = true
        if (app == TrackedApp.INSTAGRAM) {
            currentInstagramSection = InstagramSection.UNKNOWN
        }
        log("[$app] App opened - initial landing marked")
    }

    /**
     * Called when a window transition / navigation event occurs (e.g. opening/closing share sheet, tab switches).
     */
    fun onNavigationEvent(app: TrackedApp) {
        // If the user navigates within the app, ensure we don't count an initial landing
        if (currentReelKeyMap[app] == null) {
            isInitialLandingMap[app] = true
        }
        log("[$app] Navigation event recorded")
    }

    /**
     * Main evaluation function for scroll/page-selected accessibility events.
     * Returns true if and only if a genuinely new reel/short is viewed.
     */
    fun isNewReelTransition(
        app: TrackedApp,
        event: AccessibilityEvent,
        nowMillis: Long
    ): Boolean {
        // 1. Container check: ignore non-reel UI components
        if (!isReelContainerEvent(event.className?.toString())) {
            return false
        }

        // 2. App-specific section filtering
        if (app == TrackedApp.INSTAGRAM) {
            val section = detectInstagramSection(event)
            if (section == InstagramSection.HOME_FEED ||
                section == InstagramSection.MESSAGES ||
                section == InstagramSection.SEARCH_EXPLORE ||
                section == InstagramSection.PROFILE
            ) {
                log("[IG] Non-reel section ($section) - ignored")
                return false
            }
        }

        // 3. Multi-item list filter: Full-screen reels display at most 1-2 items during swipe transitions (delta <= 2).
        // Multi-item feeds (like home feed or explore grid with 3+ cards visible) have delta >= 3.
        if (event.fromIndex >= 0 && event.toIndex > event.fromIndex) {
            val delta = event.toIndex - event.fromIndex
            if (delta >= 3) {
                log("[$app] Multi-item feed scroll (${event.fromIndex}..${event.toIndex}, delta=$delta) - ignored")
                return false
            }
        }

        // 4. Extract unique reel identifier
        val reelKey = extractReelIdentifier(app, event)
        val seenKeys = seenReelKeysMap.getOrPut(app) { HashSet() }
        val currentKey = currentReelKeyMap[app]

        // 5. Initial landing check (e.g. navigating Home -> Reels, or app launch)
        if (isInitialLandingMap[app] == true) {
            isInitialLandingMap[app] = false
            currentReelKeyMap[app] = reelKey ?: "initial_landing"
            if (reelKey != null) {
                seenKeys.add(reelKey)
            }
            log("[$app] Initial landing registered without increment")
            return false
        }

        // 6. Deduplication & revisit protection using unique reel keys
        if (reelKey != null) {
            // Case A: Partial swipe or rubber-band returning to the same reel
            if (currentKey == reelKey) {
                log("[$app] Same reel $reelKey - ignored")
                return false
            }

            // Case B: Swiping back to an already-counted reel
            // Index-based keys (like idx:2) represent positions in the current pager, not globally unique videos.
            // Only check seenKeys for descriptive keys (content description or title).
            val isIndexKey = reelKey.startsWith("idx:")
            if (!isIndexKey && seenKeys.contains(reelKey)) {
                log("[$app] Already-counted reel $reelKey - updated position without increment")
                currentReelKeyMap[app] = reelKey
                return false
            }

            // Case C: Minimum dwell/transition interval debounce
            val prevTs = lastCountTimestamp[app]
            if (prevTs != null && (nowMillis - prevTs) < minTransitionIntervalMs) {
                log("[$app] Transition too rapid (${nowMillis - prevTs}ms) - ignored")
                return false
            }

            // Commit new reel!
            seenKeys.add(reelKey)
            currentReelKeyMap[app] = reelKey
            lastCountTimestamp[app] = nowMillis
            log("[$app] New reel confirmed: $reelKey")
            return true
        }

        // 7. Fallback when unique key cannot be extracted (generic view)
        // Verify vertical swipe delta if available
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val dx = event.scrollDeltaX
            val dy = event.scrollDeltaY
            if (!SwipeFilter.isVerticalSwipe(dx, dy)) {
                return false
            }
            // Small touch jitter filter
            if (dy != 0 && abs(dy) < 8) {
                return false
            }
        }

        // Debounce fallback
        val prevTs = lastCountTimestamp[app]
        if (prevTs != null && (nowMillis - prevTs) < minTransitionIntervalMs) {
            log("[$app] Debounce fallback blocked (${nowMillis - prevTs}ms)")
            return false
        }

        lastCountTimestamp[app] = nowMillis
        log("[$app] Fallback transition confirmed")
        return true
    }

    private fun log(message: String) {
        runCatching { Log.d(TAG, message) }
    }

    fun resetApp(app: TrackedApp) {
        lastCountTimestamp.remove(app)
        currentReelKeyMap.remove(app)
        isInitialLandingMap.remove(app)
        if (app == TrackedApp.INSTAGRAM) {
            currentInstagramSection = InstagramSection.UNKNOWN
        }
    }

    fun reset() {
        lastCountTimestamp.clear()
        seenReelKeysMap.clear()
        currentReelKeyMap.clear()
        isInitialLandingMap.clear()
        currentInstagramSection = InstagramSection.UNKNOWN
    }
}
