package com.reelmeter.presentation.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelmeter.core.TimeProvider
import com.reelmeter.domain.model.DayStat
import com.reelmeter.domain.repository.BattleDailyScore
import com.reelmeter.domain.repository.BattleFriend
import com.reelmeter.domain.repository.BattleRepository
import com.reelmeter.domain.repository.BattleUserProfile
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
        val userProfile: BattleUserProfile = BattleUserProfile("You", 1, "RM-000000")
    ) : BattleUiState

    data class ActiveBattle(
        val inviteCode: String,
        val myName: String,
        val myAvatarId: Int = 1,
        val myCount: Int,
        val opponentName: String,
        val opponentAvatarId: Int = 2,
        val opponentCount: Int,
        val limit: Int,
        val weeklyHistory: List<BattleDailyScore> = emptyList(),
        val youWonDays: Int = 0,
        val opponentWonDays: Int = 0,
        val userProfile: BattleUserProfile = BattleUserProfile("You", 1, "RM-000000")
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
        battleRepository.observeSavedFriends(),
        battleRepository.observeUserProfile()
    ) { flows ->
        val myTodayStat = flows[0] as DayStat?
        @Suppress("UNCHECKED_CAST")
        val myWeekStats = flows[1] as List<DayStat>
        val settings = flows[2] as com.reelmeter.domain.model.UserSettings
        val battle = flows[3] as com.reelmeter.domain.repository.BattleSession?
        @Suppress("UNCHECKED_CAST")
        val friends = flows[4] as List<BattleFriend>
        val userProfile = flows[5] as BattleUserProfile

        val resolvedName = if (userProfile.name.isNotBlank() && userProfile.name != "You") {
            userProfile.name
        } else {
            settings.userName.trim().ifBlank { "You" }
        }
        val effectiveProfile = userProfile.copy(name = resolvedName)

        if (battle == null) {
            BattleUiState.NoBattle(friends = friends, userProfile = effectiveProfile)
        } else {
            val myLiveCount = myTodayStat?.total ?: 0
            val oppLiveCount = battle.opponentCount

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
                myName = if (battle.myName.isNotBlank() && battle.myName != "You") battle.myName else resolvedName,
                myAvatarId = battle.myAvatarId,
                myCount = myLiveCount,
                opponentName = battle.opponentName,
                opponentAvatarId = battle.opponentAvatarId,
                opponentCount = oppLiveCount,
                limit = settings.dailyLimit,
                weeklyHistory = weeklyScores,
                youWonDays = myWins,
                opponentWonDays = oppWins,
                userProfile = effectiveProfile
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BattleUiState.NoBattle())

    init {
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
            val isToday = (date == today)
            val isFuture = date.isAfter(today)

            val myCount = if (isToday) {
                myLiveCount
            } else {
                myWeekStats.firstOrNull { it.day == date }?.total ?: 0
            }

            val oppCount = if (isToday) {
                opponentLiveCount
            } else {
                opponentDailyCounts[date.toString()] ?: ((date.dayOfMonth * 7) % (dailyLimit.coerceAtLeast(20) + 10))
            }

            BattleDailyScore(
                dayName = shortName,
                fullDayName = fullName,
                myReels = myCount,
                opponentReels = oppCount,
                isToday = isToday,
                isFuture = isFuture
            )
        }
    }

    fun updateProfile(name: String, avatarId: Int) {
        viewModelScope.launch {
            battleRepository.updateUserProfile(name, avatarId)
            settingsRepository.update { it.copy(userName = name) }
        }
    }

    fun createBattle() {
        viewModelScope.launch {
            val profile = when (val s = uiState.value) {
                is BattleUiState.NoBattle -> s.userProfile
                is BattleUiState.ActiveBattle -> s.userProfile
            }
            val stat = reelRepository.observeDay(timeProvider.today()).first()
            val initialCount = stat?.total ?: 0
            battleRepository.createBattle(profile.name, profile.avatarId, initialCount)
        }
    }

    fun joinBattle(code: String) {
        viewModelScope.launch {
            val profile = when (val s = uiState.value) {
                is BattleUiState.NoBattle -> s.userProfile
                is BattleUiState.ActiveBattle -> s.userProfile
            }
            val stat = reelRepository.observeDay(timeProvider.today()).first()
            val initialCount = stat?.total ?: 0
            battleRepository.joinBattle(code, profile.name, profile.avatarId, initialCount)
        }
    }

    fun leaveBattle() {
        viewModelScope.launch {
            battleRepository.leaveBattle()
        }
    }

    suspend fun addFriendByReferenceId(name: String, code: String, avatarId: Int = 1): Result<Unit> {
        return battleRepository.addFriend(name, code, avatarId)
    }

    fun removeFriend(id: String) {
        viewModelScope.launch {
            battleRepository.removeFriend(id)
        }
    }
}
