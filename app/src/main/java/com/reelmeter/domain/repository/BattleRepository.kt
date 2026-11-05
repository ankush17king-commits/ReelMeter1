package com.reelmeter.domain.repository

import kotlinx.coroutines.flow.Flow

data class BattleDailyScore(
    val dayName: String,     // "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
    val fullDayName: String, // "Monday", "Tuesday", etc.
    val myReels: Int,
    val opponentReels: Int,
    val isToday: Boolean = false,
    val isFuture: Boolean = false
) {
    val youWon: Boolean get() = myReels <= opponentReels
}

data class BattleFriend(
    val id: String,
    val name: String,
    val code: String,
    val wins: Int = 0,
    val losses: Int = 0,
    val lastActive: String = "Today"
)

data class BattleSession(
    val code: String,
    val isCreator: Boolean,
    val myName: String,
    val opponentName: String,
    val opponentCount: Int,
    val isOpponentJoined: Boolean,
    val opponentDailyCounts: Map<String, Int> = emptyMap()
)

interface BattleRepository {
    fun observeCurrentBattle(): Flow<BattleSession?>
    fun observeSavedFriends(): Flow<List<BattleFriend>>
    suspend fun createBattle(creatorName: String, myInitialCount: Int): Result<String>
    suspend fun joinBattle(code: String, myName: String, myInitialCount: Int): Result<Unit>
    suspend fun updateMyCount(count: Int)
    suspend fun updateMyDailyStats(statsMap: Map<String, Int>)
    suspend fun leaveBattle()
    suspend fun addFriend(name: String, code: String)
    suspend fun removeFriend(id: String)
}
