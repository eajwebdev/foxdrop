package ph.foxdrop.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import ph.foxdrop.core.common.dispatcher.Dispatcher
import ph.foxdrop.core.common.dispatcher.FoxDispatchers
import ph.foxdrop.core.data.auth.DefaultAuthRepository
import ph.foxdrop.core.data.preferences.DefaultAppPreferencesRepository
import ph.foxdrop.core.domain.auth.AuthRepository
import ph.foxdrop.core.domain.preferences.AppPreferencesRepository

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun authRepository(impl: DefaultAuthRepository): AuthRepository

    @Binds
    abstract fun appPreferencesRepository(impl: DefaultAppPreferencesRepository): AppPreferencesRepository

    companion object {
        @Provides
        @Dispatcher(FoxDispatchers.IO)
        fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

        @Provides
        @Dispatcher(FoxDispatchers.Default)
        fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
    }
}
