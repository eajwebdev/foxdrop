package ph.foxdrop.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Non-sensitive device preferences (plain DataStore). */
class AppPreferencesDataSource(private val dataStore: DataStore<Preferences>) {
    val hasCompletedOnboarding: Flow<Boolean> = dataStore.data.map { it[ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingCompleted() {
        dataStore.edit { it[ONBOARDING_DONE] = true }
    }

    private companion object {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
    }
}
