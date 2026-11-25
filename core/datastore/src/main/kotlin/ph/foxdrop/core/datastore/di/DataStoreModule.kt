package ph.foxdrop.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import ph.foxdrop.core.common.dispatcher.Dispatcher
import ph.foxdrop.core.common.dispatcher.FoxDispatchers
import ph.foxdrop.core.datastore.AppPreferencesDataSource
import ph.foxdrop.core.datastore.SecureStore
import ph.foxdrop.core.datastore.createKeystoreAead

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    // File names are listed in each app's data_extraction_rules.xml so they are never backed up.
    private const val SECURE_FILE = "foxdrop_secure"
    private const val PREFERENCES_FILE = "foxdrop_preferences"

    @Provides
    @Singleton
    fun secureStore(
        @ApplicationContext context: Context,
        @Dispatcher(FoxDispatchers.IO) io: CoroutineDispatcher,
    ): SecureStore {
        val dataStore = createStore(context, SECURE_FILE, io)
        return SecureStore(dataStore, lazy { createKeystoreAead(context) }, io)
    }

    @Provides
    @Singleton
    fun appPreferences(
        @ApplicationContext context: Context,
        @Dispatcher(FoxDispatchers.IO) io: CoroutineDispatcher,
    ): AppPreferencesDataSource = AppPreferencesDataSource(createStore(context, PREFERENCES_FILE, io))

    /**
     * A file damaged by a power cut mid-write is replaced with an empty one instead of crashing every launch.
     * Worst case the user sees onboarding again or signs in again.
     */
    private fun createStore(context: Context, name: String, io: CoroutineDispatcher): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = CoroutineScope(io) + SupervisorJob(),
        ) {
            context.preferencesDataStoreFile(name)
        }
}
