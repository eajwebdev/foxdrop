package ph.foxdrop.core.location.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ph.foxdrop.core.location.FusedLocationClient
import ph.foxdrop.core.location.LocationClient

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocationModule {
    @Binds
    abstract fun locationClient(impl: FusedLocationClient): LocationClient
}
