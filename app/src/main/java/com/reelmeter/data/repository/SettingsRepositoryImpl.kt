package com.reelmeter.data.repository

import com.reelmeter.core.TimeProvider
import com.reelmeter.data.local.SettingsDao
import com.reelmeter.data.local.toDomain
import com.reelmeter.data.local.toEntity
import com.reelmeter.domain.model.UserSettings
import com.reelmeter.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dao: SettingsDao,
    private val time: TimeProvider,
) : SettingsRepository {

    private fun default() = UserSettings(installDay = time.today())

    override val settings: Flow<UserSettings> = dao.observe().map { it?.toDomain() ?: default() }

    override suspend fun current(): UserSettings = dao.get()?.toDomain() ?: default()

    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        dao.upsert(transform(current()).toEntity())
    }
}
