package com.reelmeter.data.repository

import androidx.room.withTransaction
import com.reelmeter.data.local.AppDatabase
import com.reelmeter.data.local.DailyStatDao
import com.reelmeter.data.local.DailyStatEntity
import com.reelmeter.data.local.ReelEventDao
import com.reelmeter.data.local.ReelEventEntity
import com.reelmeter.data.local.toDomain
import com.reelmeter.domain.model.DayStat
import com.reelmeter.domain.model.Limits
import com.reelmeter.domain.model.TrackedApp
import com.reelmeter.domain.repository.ReelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReelRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val events: ReelEventDao,
    private val daily: DailyStatDao,
) : ReelRepository {

    override suspend fun recordReel(app: TrackedApp, timestamp: Long, day: LocalDate, limit: Int): DayStat =
        db.withTransaction {
            val key = day.toString()
            daily.insertIfAbsent(DailyStatEntity(dayKey = key, limitSnapshot = limit))
            events.insert(ReelEventEntity(appId = app.name, timestamp = timestamp, dayKey = key))
            when (app) {
                TrackedApp.INSTAGRAM -> daily.incInstagram(key)
                TrackedApp.YOUTUBE -> daily.incYoutube(key)
                TrackedApp.FACEBOOK -> daily.incFacebook(key)
                TrackedApp.SNAPCHAT -> daily.incSnapchat(key)
            }
            daily.updateLimit(key, limit)
            checkNotNull(daily.get(key)).toDomain()
        }

    override fun observeDay(day: LocalDate): Flow<DayStat?> = daily.observe(day.toString()).map { it?.toDomain() }

    override fun observeRange(from: LocalDate, to: LocalDate): Flow<List<DayStat>> =
        daily.observeRange(from.toString(), to.toString()).map { list -> list.map { it.toDomain() } }

    override fun observeAll(): Flow<List<DayStat>> = daily.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun markWarned(day: LocalDate) = daily.markWarned(day.toString())

    override suspend fun useExtension(day: LocalDate, snoozeUntil: Long): Boolean =
        daily.useExtension(day.toString(), snoozeUntil, Limits.MAX_EXTENSIONS_PER_DAY) > 0

    override suspend fun purgeEventsBefore(timestamp: Long) = events.deleteBefore(timestamp)

    override suspend fun resetAll() {
        db.withTransaction {
            events.deleteAll()
            daily.deleteAll()
        }
    }
}
