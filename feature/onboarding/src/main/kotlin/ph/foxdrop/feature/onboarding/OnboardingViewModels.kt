package ph.foxdrop.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ph.foxdrop.core.domain.preferences.AppPreferencesRepository
import ph.foxdrop.core.domain.startup.ResolveStartDestinationUseCase
import ph.foxdrop.core.domain.startup.StartDestination

/** Decides the first screen while the splash is visible. `null` = still deciding (keep the splash). */
@HiltViewModel
class StartupViewModel @Inject constructor(
    resolveStartDestination: ResolveStartDestinationUseCase,
) : ViewModel() {
    val startDestination: StateFlow<StartDestination?> =
        flow { emit(resolveStartDestination()) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: AppPreferencesRepository,
) : ViewModel() {
    private var completing = false

    /**
     * Saves that onboarding is done, then continues. "Skip" and "Get started" can both be tapped quickly; only the
     * first call counts.
     */
    fun complete(then: () -> Unit) {
        if (completing) return
        completing = true
        viewModelScope.launch {
            preferences.setOnboardingCompleted()
            then()
        }
    }
}
