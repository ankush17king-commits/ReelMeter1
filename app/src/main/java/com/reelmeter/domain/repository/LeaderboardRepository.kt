package com.reelmeter.domain.repository

import kotlinx.coroutines.flow.Flow

data class LeaderboardUser(
    val id: String,
    val nickname: String,
    val avatarColorHex: String,
    val reelsToday: Int,
    val streakDays: Int,
    val isYou: Boolean = false,
)

interface LeaderboardRepository {
    fun observeParticipation(): Flow<Boolean>
    fun observeLeaderboard(): Flow<List<LeaderboardUser>>
    suspend fun joinLeaderboard(nickname: String)
    suspend fun leaveLeaderboard()
    suspend fun addFriendByCode(code: String): Boolean
    fun getMyInviteCode(): String
}
