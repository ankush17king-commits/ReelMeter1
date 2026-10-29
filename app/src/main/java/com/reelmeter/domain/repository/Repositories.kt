package com.reelmeter.domain.repository

import com.reelmeter.domain.model.DayStat
import com.reelmeter.domain.model.TrackedApp
import com.reelmeter.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ReelRepository {
    /** Stores one reel event (app + timestamp only) and increments the day's counter atomically. */
    suspend fun recordReel(app: TrackedApp, timestamp: Long, day: LocalDate, limit: Int): DayStat
    fun observeDay(day: LocalDate): Flow<DayStat?>
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<DayStat>>
    fun observeAll(): Flow<List<DayStat>>
    suspend fun markWarned(day: LocalDate)
    /** @return true if an extension was granted (max 2 per day). */
    suspend fun useExtension(day: LocalDate, snoozeUntil: Long): Boolean
    suspend fun purgeEventsBefore(timestamp: Long)
    suspend fun resetAll()
}

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun current(): UserSettings
    suspend fun update(transform: (UserSettings) -> UserSettings)
}
