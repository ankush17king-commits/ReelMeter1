package com.reelmeter.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelmeter.core.PermissionChecker
import com.reelmeter.core.PermissionStatus
import com.reelmeter.domain.model.ThemeMode
import com.reelmeter.domain.model.TrackedApp
import com.reelmeter.domain.model.UserSettings
import com.reelmeter.domain.repository.SettingsRepository
import com.reelmeter.domain.usecase.ResetDataUseCase
import com.reelmeter.domain.usecase.UpdateSettingsUseCase
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
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Error(val message: String) : SettingsUiState
    data class Success(val settings: UserSettings, val permissions: PermissionStatus) : SettingsUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val updateSettings: UpdateSettingsUseCase,
    private val resetData: ResetDataUseCase,
    private val permissionChecker: PermissionChecker,
) : ViewModel() {

    private val permissions = MutableStateFlow(permissionChecker.current())
    private val retryTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SettingsUiState> = retryTrigger
        .flatMapLatest {
            val flow: Flow<SettingsUiState> = combine(settingsRepository.settings, permissions) { s, p ->
                SettingsUiState.Success(s, p)
            }
            flow.catch { emit(SettingsUiState.Error(it.message ?: "Could not load settings.")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState.Loading)

    fun refreshPermissions() { permissions.value = permissionChecker.current() }
    fun retry() { retryTrigger.value += 1 }

    fun setLimit(value: Int) = edit { it.copy(dailyLimit = value) }
    fun setSecondsPerReel(value: Int) = edit { it.copy(secondsPerReel = value) }
    fun setReminders(on: Boolean) = edit { it.copy(remindersEnabled = on) }
    fun setStrictMode(on: Boolean) = edit { it.copy(strictMode = on) }
    fun setFloatingCounter(on: Boolean) = edit { it.copy(showFloatingCounter = on) }
    fun setBlackoutEnabled(on: Boolean) = edit { it.copy(blackoutEnabled = on) }
    fun setBlackoutHours(start: Int, end: Int) = edit { it.copy(blackoutStartHour = start, blackoutEndHour = end) }
    fun setDoomscrollAlert(on: Boolean) = edit { it.copy(doomscrollAlertEnabled = on) }
    fun setDoomscrollThreshold(value: Int) = edit { it.copy(doomscrollThreshold = value) }
    fun setTheme(mode: ThemeMode) = edit { it.copy(themeMode = mode) }
    fun setTracked(app: TrackedApp, tracked: Boolean) =
        edit { it.copy(trackedApps = if (tracked) it.trackedApps + app else it.trackedApps - app) }

    fun reset(onDone: () -> Unit) {
        viewModelScope.launch { resetData(); onDone() }
    }

    private fun edit(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { updateSettings(transform) }
    }
}
