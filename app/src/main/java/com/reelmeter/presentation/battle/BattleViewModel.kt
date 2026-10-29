package com.reelmeter.presentation.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelmeter.core.TimeProvider
import com.reelmeter.domain.repository.ReelRepository
import com.reelmeter.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

sealed interface BattleUiState {
    object NoBattle : BattleUiState
    data class ActiveBattle(
        val inviteCode: String,
        val myCount: Int,
        val opponentName: String,
        val opponentCount: Int,
        val limit: Int
    ) : BattleUiState
}

@HiltViewModel
class BattleViewModel @Inject constructor(
    private val reelRepository: ReelRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _mockBattleState = MutableStateFlow<MockBattle?>(null)
    
    val uiState: StateFlow<BattleUiState> = combine(
        reelRepository.observeDay(timeProvider.today()),
        settingsRepository.settings,
        _mockBattleState
    ) { myStat, settings, battle ->
        if (battle == null) {
            BattleUiState.NoBattle
        } else {
            BattleUiState.ActiveBattle(
                inviteCode = battle.code,
                myCount = myStat?.total ?: 0,
                opponentName = battle.opponentName,
                opponentCount = battle.opponentCount,
                limit = settings.dailyLimit
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BattleUiState.NoBattle)

    init {
        // Mock opponent progress simulation
        viewModelScope.launch {
            while (true) {
                delay(15_000) // update opponent every 15s
                val current = _mockBattleState.value
                if (current != null) {
                    val add = if (Random.nextFloat() > 0.4f) Random.nextInt(1, 4) else 0
                    if (add > 0) {
                        _mockBattleState.value = current.copy(opponentCount = current.opponentCount + add)
                    }
                }
            }
        }
    }

    fun createBattle() {
        _mockBattleState.value = MockBattle(
            code = "RM-" + Random.nextInt(1000, 9999),
            opponentName = "Waiting...", // Will change to a mocked friend
            opponentCount = 0
        )
        // Simulate someone joining
        viewModelScope.launch {
            delay(3000)
            val current = _mockBattleState.value
            if (current != null) {
                _mockBattleState.value = current.copy(opponentName = "Alex")
            }
        }
    }

    fun joinBattle(code: String) {
        _mockBattleState.value = MockBattle(
            code = code,
            opponentName = "Sam",
            opponentCount = Random.nextInt(5, 20) // Mock starting count
        )
    }

    fun leaveBattle() {
        _mockBattleState.value = null
    }

    private data class MockBattle(
        val code: String,
        val opponentName: String,
        val opponentCount: Int
    )
}
