package ph.foxdrop.core.data.preferences

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import ph.foxdrop.core.datastore.AppPreferencesDataSource
import ph.foxdrop.core.domain.preferences.AppPreferencesRepository

internal class DefaultAppPreferencesRepository @Inject constructor(
    private val dataSource: AppPreferencesDataSource,
) : AppPreferencesRepository {
    override val hasCompletedOnboarding: Flow<Boolean> = dataSource.hasCompletedOnboarding

    override suspend fun setOnboardingCompleted() = dataSource.setOnboardingCompleted()
}
