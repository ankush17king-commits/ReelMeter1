package com.reelmeter.presentation.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelmeter.domain.repository.LeaderboardRepository
import com.reelmeter.domain.repository.LeaderboardUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LeaderboardUiState {
    data object OptInRequired : LeaderboardUiState
    data class Active(
        val users: List<LeaderboardUser>,
        val inviteCode: String,
    ) : LeaderboardUiState
}

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repository: LeaderboardRepository,
) : ViewModel() {

    val uiState: StateFlow<LeaderboardUiState> = combine(
        repository.observeParticipation(),
        repository.observeLeaderboard()
    ) { participating, list ->
        if (!participating) {
            LeaderboardUiState.OptInRequired
        } else {
            LeaderboardUiState.Active(
                users = list,
                inviteCode = repository.getMyInviteCode()
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LeaderboardUiState.OptInRequired)

    fun join(nickname: String) {
        viewModelScope.launch {
            repository.joinLeaderboard(nickname)
        }
    }

    fun leave() {
        viewModelScope.launch {
            repository.leaveLeaderboard()
        }
    }

    fun addFriend(code: String) {
        viewModelScope.launch {
            repository.addFriendByCode(code)
        }
    }
}
