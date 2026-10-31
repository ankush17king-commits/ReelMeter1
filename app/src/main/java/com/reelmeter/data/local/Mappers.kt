package com.reelmeter.data.local

import com.reelmeter.domain.model.DayStat
import com.reelmeter.domain.model.ThemeMode
import com.reelmeter.domain.model.TrackedApp
import com.reelmeter.domain.model.UserSettings
import java.time.LocalDate

fun DailyStatEntity.toDomain() = DayStat(
    day = LocalDate.parse(dayKey),
    perApp = mapOf(
        TrackedApp.INSTAGRAM to instagramCount,
        TrackedApp.YOUTUBE to youtubeCount,
        TrackedApp.FACEBOOK to facebookCount,
        TrackedApp.SNAPCHAT to snapchatCount,
    ),
    limitSnapshot = limitSnapshot,
    warned = warned,
    extensionsUsed = extensionsUsed,
    snoozeUntil = snoozeUntil,
)

fun SettingsEntity.toDomain() = UserSettings(
    dailyLimit = dailyLimit,
    secondsPerReel = secondsPerReel,
    trackedApps = buildSet {
        if (trackInstagram) add(TrackedApp.INSTAGRAM)
        if (trackYoutube) add(TrackedApp.YOUTUBE)
        if (trackFacebook) add(TrackedApp.FACEBOOK)
        if (trackSnapchat) add(TrackedApp.SNAPCHAT)
    },
    remindersEnabled = remindersEnabled,
    strictMode = strictMode,
    themeMode = runCatching { ThemeMode.valueOf(themeMode) }.getOrDefault(ThemeMode.SYSTEM),
    onboardingDone = onboardingDone,
    disclosureAccepted = disclosureAccepted,
    installDay = LocalDate.parse(installDay),
    showFloatingCounter = showFloatingCounter,
    occupation = occupation,
    estimatedDailyReels = estimatedDailyReels,
    primaryGoal = primaryGoal,
    userName = userName,
    selectedGoals = selectedGoalsStr.split(",").filter { it.isNotBlank() }.toSet(),
    estimatedDailyUsage = estimatedDailyUsage,
    unplannedOpenFrequency = unplannedOpenFrequency,
    scrollTimesOfDay = scrollTimesOfDayStr.split(",").filter { it.isNotBlank() }.toSet(),
    scrollAffectsAreas = scrollAffectsAreasStr.split(",").filter { it.isNotBlank() }.toSet(),
    preferredSavedTimeActivities = preferredSavedTimeActivitiesStr.split(",").filter { it.isNotBlank() }.toSet(),
    targetTimeSaved = targetTimeSaved,
    limitReachedBehavior = limitReachedBehaviorStr.split(",").filter { it.isNotBlank() }.toSet(),
    strictnessMode = strictnessMode,
    resetTime = resetTime,
    blackoutEnabled = blackoutEnabled,
    blackoutStartHour = blackoutStartHour,
    blackoutEndHour = blackoutEndHour,
    doomscrollAlertEnabled = doomscrollAlertEnabled,
    doomscrollThreshold = doomscrollThreshold,
    limitLockedUntilEpochDay = limitLockedUntilEpochDay,
)

fun UserSettings.toEntity() = SettingsEntity(
    id = 1,
    dailyLimit = dailyLimit,
    secondsPerReel = secondsPerReel,
    trackInstagram = TrackedApp.INSTAGRAM in trackedApps,
    trackYoutube = TrackedApp.YOUTUBE in trackedApps,
    trackFacebook = TrackedApp.FACEBOOK in trackedApps,
    trackSnapchat = TrackedApp.SNAPCHAT in trackedApps,
    remindersEnabled = remindersEnabled,
    strictMode = strictMode,
    themeMode = themeMode.name,
    onboardingDone = onboardingDone,
    disclosureAccepted = disclosureAccepted,
    installDay = installDay.toString(),
    showFloatingCounter = showFloatingCounter,
    occupation = occupation,
    estimatedDailyReels = estimatedDailyReels,
    primaryGoal = primaryGoal,
    userName = userName,
    selectedGoalsStr = selectedGoals.joinToString(","),
    estimatedDailyUsage = estimatedDailyUsage,
    unplannedOpenFrequency = unplannedOpenFrequency,
    scrollTimesOfDayStr = scrollTimesOfDay.joinToString(","),
    scrollAffectsAreasStr = scrollAffectsAreas.joinToString(","),
    preferredSavedTimeActivitiesStr = preferredSavedTimeActivities.joinToString(","),
    targetTimeSaved = targetTimeSaved,
    limitReachedBehaviorStr = limitReachedBehavior.joinToString(","),
    strictnessMode = strictnessMode,
    resetTime = resetTime,
    blackoutEnabled = blackoutEnabled,
    blackoutStartHour = blackoutStartHour,
    blackoutEndHour = blackoutEndHour,
    doomscrollAlertEnabled = doomscrollAlertEnabled,
    doomscrollThreshold = doomscrollThreshold,
    limitLockedUntilEpochDay = limitLockedUntilEpochDay,
)
