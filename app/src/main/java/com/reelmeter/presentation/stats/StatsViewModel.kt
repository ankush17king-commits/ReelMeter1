package com.reelmeter.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelmeter.domain.model.HistoryData
import com.reelmeter.domain.usecase.ObserveHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface StatsUiState {
    data object Loading : StatsUiState
    data class Error(val message: String) : StatsUiState
    data class Success(val data: com.reelmeter.domain.model.AdvancedStatsData) : StatsUiState
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val observeAdvancedStats: com.reelmeter.domain.usecase.ObserveAdvancedStatsUseCase,
    val usageStatsHelper: com.reelmeter.core.UsageStatsHelper
) : ViewModel() {
    private val selectedPeriod = MutableStateFlow(com.reelmeter.domain.model.StatsPeriod.SEVEN_DAYS)
    private val retryTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatsUiState> = kotlinx.coroutines.flow.combine(selectedPeriod, retryTrigger) { period, _ -> period }
        .flatMapLatest { period ->
            observeAdvancedStats(period).map<com.reelmeter.domain.model.AdvancedStatsData, StatsUiState> { StatsUiState.Success(it) }
        }
        .catch { emit(StatsUiState.Error(it.message ?: "Could not load your history.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState.Loading)

    fun selectPeriod(period: com.reelmeter.domain.model.StatsPeriod) {
        selectedPeriod.value = period
    }

    fun retry() { retryTrigger.value += 1 }
}
