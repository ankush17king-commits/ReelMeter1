package com.reelmeter.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelmeter.core.PermissionChecker
import com.reelmeter.core.PermissionStatus
import com.reelmeter.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val permissionChecker: PermissionChecker,
    private val updateSettings: UpdateSettingsUseCase,
) : ViewModel() {

    private val _permissions = MutableStateFlow(permissionChecker.current())
    val permissions: StateFlow<PermissionStatus> = _permissions

    fun refreshPermissions() { _permissions.value = permissionChecker.current() }

    /** Allowed even if permissions are missing; the dashboard then shows a "fix it" card. */
    fun finishOnboarding(
        userName: String,
        occupation: String,
        selectedGoals: Set<String>,
        selectedApps: Set<com.reelmeter.domain.model.TrackedApp>,
        estimatedDailyUsage: String,
        unplannedOpenFrequency: String,
        scrollTimesOfDay: Set<String>,
        scrollAffectsAreas: Set<String>,
        preferredSavedTimeActivities: Set<String>,
        targetTimeSaved: String,
        limitReachedBehavior: Set<String>,
        dailyLimit: Int,
        resetTime: String,
        strictnessMode: String,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            updateSettings { 
                it.copy(
                    onboardingDone = true, 
                    disclosureAccepted = true,
                    userName = userName,
                    occupation = occupation,
                    selectedGoals = selectedGoals,
                    trackedApps = selectedApps,
                    estimatedDailyUsage = estimatedDailyUsage,
                    unplannedOpenFrequency = unplannedOpenFrequency,
                    scrollTimesOfDay = scrollTimesOfDay,
                    scrollAffectsAreas = scrollAffectsAreas,
                    preferredSavedTimeActivities = preferredSavedTimeActivities,
                    targetTimeSaved = targetTimeSaved,
                    limitReachedBehavior = limitReachedBehavior,
                    dailyLimit = dailyLimit,
                    resetTime = resetTime,
                    strictnessMode = strictnessMode,
                    strictMode = (strictnessMode == "Focus"),
                ) 
            }
            onDone()
        }
    }
}
