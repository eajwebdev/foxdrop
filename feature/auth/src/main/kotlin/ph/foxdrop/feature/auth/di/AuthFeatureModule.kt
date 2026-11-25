package ph.foxdrop.feature.auth.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ph.foxdrop.feature.auth.BuildConfig
import ph.foxdrop.feature.auth.GoogleWebClientId

@Module
@InstallIn(SingletonComponent::class)
object AuthFeatureModule {
    /** Public OAuth client ID (from local.properties). Empty means Google sign-in is not set up yet. */
    @Provides
    @GoogleWebClientId
    fun googleWebClientId(): String = BuildConfig.GOOGLE_WEB_CLIENT_ID
}
