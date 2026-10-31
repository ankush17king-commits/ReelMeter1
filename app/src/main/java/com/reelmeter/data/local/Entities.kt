package com.reelmeter.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Privacy: only app, timestamp and day. No content of any kind. */
@Entity(tableName = "reel_events", indices = [Index("timestamp")])
data class ReelEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appId: String,
    val timestamp: Long,
    val dayKey: String,
)

@Entity(tableName = "daily_stats")
data class DailyStatEntity(
    @PrimaryKey val dayKey: String, // ISO yyyy-MM-dd (local date)
    val instagramCount: Int = 0,
    val youtubeCount: Int = 0,
    val facebookCount: Int = 0,
    val snapchatCount: Int = 0,
    val limitSnapshot: Int,
    val warned: Boolean = false,
    val extensionsUsed: Int = 0,
    val snoozeUntil: Long = 0,
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val dailyLimit: Int,
    val secondsPerReel: Int,
    val trackInstagram: Boolean,
    val trackYoutube: Boolean,
    val trackFacebook: Boolean,
    val trackSnapchat: Boolean = true,
    val remindersEnabled: Boolean,
    val strictMode: Boolean,
    val themeMode: String,
    val onboardingDone: Boolean,
    val disclosureAccepted: Boolean,
    val installDay: String,
    val showFloatingCounter: Boolean = true,
    val occupation: String = "",
    val estimatedDailyReels: Int = 0,
    val primaryGoal: String = "",
    val userName: String = "",
    val selectedGoalsStr: String = "",
    val estimatedDailyUsage: String = "",
    val unplannedOpenFrequency: String = "",
    val scrollTimesOfDayStr: String = "",
    val scrollAffectsAreasStr: String = "",
    val preferredSavedTimeActivitiesStr: String = "",
    val targetTimeSaved: String = "",
    val limitReachedBehaviorStr: String = "",
    val strictnessMode: String = "Balanced",
    val resetTime: String = "Midnight",
    val blackoutEnabled: Boolean = false,
    val blackoutStartHour: Int = 23,
    val blackoutEndHour: Int = 7,
    val doomscrollAlertEnabled: Boolean = true,
    val doomscrollThreshold: Int = 15,
    val limitLockedUntilEpochDay: Long = 0L,
)
