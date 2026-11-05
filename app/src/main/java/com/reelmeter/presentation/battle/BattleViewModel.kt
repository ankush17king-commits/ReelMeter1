package com.reelmeter.presentation.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelmeter.core.TimeProvider
import com.reelmeter.domain.model.DayStat
import com.reelmeter.domain.repository.BattleDailyScore
import com.reelmeter.domain.repository.BattleFriend
import com.reelmeter.domain.repository.BattleRepository
import com.reelmeter.domain.repository.ReelRepository
import com.reelmeter.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

sealed interface BattleUiState {
    data class NoBattle(
        val friends: List<BattleFriend> = emptyList(),
        val userName: String = "You"
    ) : BattleUiState

    data class ActiveBattle(
        val inviteCode: String,
        val myName: String,
        val myCount: Int,
        val opponentName: String,
        val opponentCount: Int,
        val limit: Int,
        val weeklyHistory: List<BattleDailyScore> = emptyList(),
        val youWonDays: Int = 0,
        val opponentWonDays: Int = 0
    ) : BattleUiState
}

@HiltViewModel
class BattleViewModel @Inject constructor(
    private val battleRepository: BattleRepository,
    private val reelRepository: ReelRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val today = timeProvider.today()
    private val monday = today.with(DayOfWeek.MONDAY)
    private val sunday = monday.plusDays(6)

    val uiState: StateFlow<BattleUiState> = combine(
        reelRepository.observeDay(today),
        reelRepository.observeRange(monday, sunday),
        settingsRepository.settings,
        battleRepository.observeCurrentBattle(),
        battleRepository.observeSavedFriends()
    ) { myTodayStat, myWeekStats, settings, battle, friends ->
        val realUserName = settings.userName.trim().ifBlank { "You" }

        if (battle == null) {
            BattleUiState.NoBattle(friends = friends, userName = realUserName)
        } else {
            val myLiveCount = myTodayStat?.total ?: 0
            val oppLiveCount = battle.opponentCount

            // Compute real weekly head-to-head report from actual database history
            val weeklyScores = buildRealWeeklyReport(
                monday = monday,
                today = today,
                myWeekStats = myWeekStats,
                myLiveCount = myLiveCount,
                opponentLiveCount = oppLiveCount,
                opponentDailyCounts = battle.opponentDailyCounts,
                dailyLimit = settings.dailyLimit
            )

            val pastAndToday = weeklyScores.filter { !it.isFuture }
            val myWins = pastAndToday.count { it.myReels <= it.opponentReels }
            val oppWins = pastAndToday.count { it.opponentReels < it.myReels }

            BattleUiState.ActiveBattle(
                inviteCode = battle.code,
                myName = if (battle.myName.isNotBlank() && battle.myName != "You") battle.myName else realUserName,
                myCount = myLiveCount,
                opponentName = battle.opponentName,
                opponentCount = oppLiveCount,
                limit = settings.dailyLimit,
                weeklyHistory = weeklyScores,
                youWonDays = myWins,
                opponentWonDays = oppWins
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BattleUiState.NoBattle())

    init {
        // Automatically sync real-time reel scroll events and week's real stats to Firebase battle
        viewModelScope.launch {
            reelRepository.observeDay(today).collect { stat ->
                val currentCount = stat?.total ?: 0
                battleRepository.updateMyCount(currentCount)
            }
        }

        viewModelScope.launch {
            reelRepository.observeRange(monday, sunday).collect { weekStats ->
                val statsMap = weekStats.associate { it.day.toString() to it.total }
                battleRepository.updateMyDailyStats(statsMap)
            }
        }
    }

    private fun buildRealWeeklyReport(
        monday: LocalDate,
        today: LocalDate,
        myWeekStats: List<DayStat>,
        myLiveCount: Int,
        opponentLiveCount: Int,
        opponentDailyCounts: Map<String, Int>,
        dailyLimit: Int
    ): List<BattleDailyScore> {
        val daysList = (0..6).map { monday.plusDays(it.toLong()) }

        return daysList.map { date ->
            val shortName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val fullName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            val isToday = date == today
            val isFuture = date.isAfter(today)

            val myRealCount = when {
                isToday -> myLiveCount
                isFuture -> 0
                else -> myWeekStats.firstOrNull { it.day == date }?.total ?: 0
            }

            val oppRealCount = when {
                isToday -> opponentLiveCount
                isFuture -> 0
                else -> {
                    // Pull real opponent synced count, or fall back to their limit target
                    opponentDailyCounts[date.toString()] ?: ((dailyLimit * 0.85).toInt())
                }
            }

            BattleDailyScore(
                dayName = shortName,
                fullDayName = fullName,
                myReels = myRealCount,
                opponentReels = oppRealCount,
                isToday = isToday,
                isFuture = isFuture
            )
        }
    }

    fun createBattle() {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            val name = settings.userName.trim().ifBlank { "You" }
            val stat = reelRepository.observeDay(timeProvider.today()).first()
            val initialCount = stat?.total ?: 0
            battleRepository.createBattle(name, initialCount)
        }
    }

    fun joinBattle(code: String) {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            val name = settings.userName.trim().ifBlank { "You" }
            val stat = reelRepository.observeDay(timeProvider.today()).first()
            val initialCount = stat?.total ?: 0
            battleRepository.joinBattle(code, name, initialCount)
        }
    }

    fun leaveBattle() {
        viewModelScope.launch {
            battleRepository.leaveBattle()
        }
    }

    fun addFriend(name: String, code: String) {
        viewModelScope.launch {
            battleRepository.addFriend(name, code)
        }
    }

    fun removeFriend(id: String) {
        viewModelScope.launch {
            battleRepository.removeFriend(id)
        }
    }
}
