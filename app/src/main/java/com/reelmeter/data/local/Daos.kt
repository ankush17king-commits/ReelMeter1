package com.reelmeter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReelEventDao {
    @Insert suspend fun insert(event: ReelEventEntity)
    @Query("DELETE FROM reel_events WHERE timestamp < :before") suspend fun deleteBefore(before: Long)
    @Query("DELETE FROM reel_events") suspend fun deleteAll()
}

@Dao
interface DailyStatDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: DailyStatEntity)

    @Query("UPDATE daily_stats SET instagramCount = instagramCount + 1 WHERE dayKey = :day")
    suspend fun incInstagram(day: String)

    @Query("UPDATE daily_stats SET youtubeCount = youtubeCount + 1 WHERE dayKey = :day")
    suspend fun incYoutube(day: String)

    @Query("UPDATE daily_stats SET facebookCount = facebookCount + 1 WHERE dayKey = :day")
    suspend fun incFacebook(day: String)

    @Query("UPDATE daily_stats SET snapchatCount = snapchatCount + 1 WHERE dayKey = :day")
    suspend fun incSnapchat(day: String)

    @Query("UPDATE daily_stats SET limitSnapshot = :limit WHERE dayKey = :day")
    suspend fun updateLimit(day: String, limit: Int)

    @Query("UPDATE daily_stats SET warned = 1 WHERE dayKey = :day")
    suspend fun markWarned(day: String)

    @Query(
        "UPDATE daily_stats SET extensionsUsed = extensionsUsed + 1, snoozeUntil = :until " +
            "WHERE dayKey = :day AND extensionsUsed < :max"
    )
    suspend fun useExtension(day: String, until: Long, max: Int): Int

    @Query("SELECT * FROM daily_stats WHERE dayKey = :day") suspend fun get(day: String): DailyStatEntity?
    @Query("SELECT * FROM daily_stats WHERE dayKey = :day") fun observe(day: String): Flow<DailyStatEntity?>

    @Query("SELECT * FROM daily_stats WHERE dayKey BETWEEN :from AND :to ORDER BY dayKey DESC")
    fun observeRange(from: String, to: String): Flow<List<DailyStatEntity>>

    @Query("SELECT * FROM daily_stats ORDER BY dayKey DESC") fun observeAll(): Flow<List<DailyStatEntity>>
    @Query("DELETE FROM daily_stats") suspend fun deleteAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1") fun observe(): Flow<SettingsEntity?>
    @Query("SELECT * FROM settings WHERE id = 1") suspend fun get(): SettingsEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(entity: SettingsEntity)
}
