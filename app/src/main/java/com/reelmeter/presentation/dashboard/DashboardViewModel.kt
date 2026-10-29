package com.reelmeter.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelmeter.core.PermissionChecker
import com.reelmeter.core.PermissionStatus
import com.reelmeter.domain.model.DashboardData
import com.reelmeter.domain.usecase.ObserveDashboardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Error(val message: String) : DashboardUiState
    data class Success(val data: DashboardData, val permissions: PermissionStatus, val settings: com.reelmeter.domain.model.UserSettings) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeDashboard: ObserveDashboardUseCase,
    private val permissionChecker: PermissionChecker,
    private val settingsRepository: com.reelmeter.domain.repository.SettingsRepository,
    val focusTimerManager: com.reelmeter.service.FocusTimerManager,
) : ViewModel() {

    private val permissions = MutableStateFlow(permissionChecker.current())
    private val retryTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = retryTrigger
        .flatMapLatest {
            val flow: Flow<DashboardUiState> = combine(
                observeDashboard(),
                permissions,
                settingsRepository.settings
            ) { data, perms, settings ->
                DashboardUiState.Success(data, perms, settings)
            }
            flow.catch { emit(DashboardUiState.Error(it.message ?: "Could not load your stats.")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState.Loading)

    fun refreshPermissions() { permissions.value = permissionChecker.current() }
    fun retry() { retryTrigger.value += 1 }
}
