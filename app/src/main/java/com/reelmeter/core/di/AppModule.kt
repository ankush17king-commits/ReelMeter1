package com.reelmeter.core.di

import android.content.Context
import androidx.room.Room
import com.reelmeter.core.SystemTimeProvider
import com.reelmeter.core.TimeProvider
import com.reelmeter.data.local.AppDatabase
import com.reelmeter.data.local.DailyStatDao
import com.reelmeter.data.local.ReelEventDao
import com.reelmeter.data.local.SettingsDao
import com.reelmeter.data.repository.ReelRepositoryImpl
import com.reelmeter.data.repository.SettingsRepositoryImpl
import com.reelmeter.domain.repository.ReelRepository
import com.reelmeter.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds abstract fun reelRepository(impl: ReelRepositoryImpl): ReelRepository
    @Binds abstract fun settingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds abstract fun timeProvider(impl: SystemTimeProvider): TimeProvider
    @Binds abstract fun leaderboardRepository(impl: com.reelmeter.data.repository.FakeLeaderboardRepository): com.reelmeter.domain.repository.LeaderboardRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "reelmeter.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun reelEventDao(db: AppDatabase): ReelEventDao = db.reelEventDao()
    @Provides fun dailyStatDao(db: AppDatabase): DailyStatDao = db.dailyStatDao()
    @Provides fun settingsDao(db: AppDatabase): SettingsDao = db.settingsDao()
}
