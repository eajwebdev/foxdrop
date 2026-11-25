package ph.foxdrop.core.domain.preferences

import kotlinx.coroutines.flow.Flow

/** Small, non-sensitive app settings stored on the device. */
interface AppPreferencesRepository {
    /** True once the first-launch intro and onboarding were finished on this device. */
    val hasCompletedOnboarding: Flow<Boolean>

    suspend fun setOnboardingCompleted()
}
