package com.reelmeter

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.reelmeter.domain.repository.SettingsRepository
import com.reelmeter.service.MidnightWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ReelMeterApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var settingsRepository: SettingsRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Make sure the single settings row exists (stores the install day for streaks).
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            settingsRepository.update { it }
        }
        MidnightWorker.schedule(this)
    }
}
