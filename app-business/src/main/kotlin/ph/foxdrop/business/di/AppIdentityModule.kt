package ph.foxdrop.business.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ph.foxdrop.core.model.FoxDropApp

/** Tells shared modules which app is running (auth redirect scheme `foxdrop-business://auth`, routing). */
@Module
@InstallIn(SingletonComponent::class)
object AppIdentityModule {
    @Provides
    fun foxDropApp(): FoxDropApp = FoxDropApp.Business
}
