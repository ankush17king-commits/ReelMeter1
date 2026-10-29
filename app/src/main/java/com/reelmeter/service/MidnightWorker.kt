package com.reelmeter.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.reelmeter.domain.usecase.PurgeOldEventsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Daily housekeeping right after midnight (survives reboot / battery optimisation).
 * The actual "midnight reset" needs no job: every stat row is keyed by local date and the
 * service reads today's date on every event, so a new day always starts at 0.
 */
@HiltWorker
class MidnightWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val purgeOldEvents: PurgeOldEventsUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        purgeOldEvents()
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    companion object {
        private const val NAME = "midnight_housekeeping"

        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            val next = now.toLocalDate().plusDays(1).atStartOfDay().plusSeconds(5)
            val delay = Duration.between(now, next).toMillis()
            val request = PeriodicWorkRequestBuilder<MidnightWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
