package com.reelmeter.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ReelEventEntity::class, DailyStatEntity::class, SettingsEntity::class],
    version = 6,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reelEventDao(): ReelEventDao
    abstract fun dailyStatDao(): DailyStatDao
    abstract fun settingsDao(): SettingsDao
}
