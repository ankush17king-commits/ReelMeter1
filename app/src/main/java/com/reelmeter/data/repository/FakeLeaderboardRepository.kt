package com.reelmeter.data.repository

import com.reelmeter.core.TimeProvider
import com.reelmeter.domain.model.UserSettings
import com.reelmeter.domain.repository.LeaderboardRepository
import com.reelmeter.domain.repository.LeaderboardUser
import com.reelmeter.domain.repository.ReelRepository
import com.reelmeter.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeLeaderboardRepository @Inject constructor(
    private val reelRepository: ReelRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
) : LeaderboardRepository {

    private val isParticipating = MutableStateFlow(false)
    private val nicknameState = MutableStateFlow("You")

    private val sampleFriends = MutableStateFlow(
        listOf(
            LeaderboardUser("1", "Alex", "#7C5CFF", 12, 6),
            LeaderboardUser("2", "Sam", "#2ECC71", 24, 4),
            LeaderboardUser("3", "Jordan", "#FF9F1C", 38, 2),
            LeaderboardUser("4", "Taylor", "#E74C3C", 55, 1),
        )
    )

    override fun observeParticipation(): Flow<Boolean> = isParticipating

    override fun observeLeaderboard(): Flow<List<LeaderboardUser>> {
        val today = timeProvider.today()
        return combine(
            isParticipating,
            reelRepository.observeDay(today),
            nicknameState,
            sampleFriends
        ) { participating, todayStat, nick, friends ->
            if (!participating) return@combine emptyList()

            val myCount = todayStat?.total ?: 0
            val me = LeaderboardUser(
                id = "me",
                nickname = nick,
                avatarColorHex = "#7C5CFF",
                reelsToday = myCount,
                streakDays = 5,
                isYou = true
            )

            (friends + me).sortedBy { it.reelsToday }
        }
    }

    override suspend fun joinLeaderboard(nickname: String) {
        if (nickname.isNotBlank()) nicknameState.value = nickname
        isParticipating.value = true
    }

    override suspend fun leaveLeaderboard() {
        isParticipating.value = false
    }

    override suspend fun addFriendByCode(code: String): Boolean {
        if (code.isBlank()) return false
        val newFriend = LeaderboardUser(
            id = "friend_${System.currentTimeMillis()}",
            nickname = "Friend ($code)",
            avatarColorHex = "#3498DB",
            reelsToday = (10..40).random(),
            streakDays = (1..5).random()
        )
        sampleFriends.value = sampleFriends.value + newFriend
        return true
    }

    override fun getMyInviteCode(): String = "RM-" + (1000..9999).random()
}
