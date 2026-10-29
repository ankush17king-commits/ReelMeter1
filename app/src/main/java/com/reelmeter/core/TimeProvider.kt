package com.reelmeter.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Abstraction over the clock so day-rollover and limit logic are testable. */
interface TimeProvider {
    fun nowMillis(): Long
    fun today(): LocalDate

    /** Emits today's date and re-emits right after midnight (checked at least every minute). */
    fun todayFlow(): Flow<LocalDate> = flow {
        while (true) {
            val now = LocalDateTime.now()
            emit(now.toLocalDate())
            val untilMidnight = Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis()
            delay(minOf(untilMidnight + 1_000L, 60_000L))
        }
    }.distinctUntilChanged()
}

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun today(): LocalDate = LocalDate.now()
}
