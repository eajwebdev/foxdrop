package ph.foxdrop.core.network.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ph.foxdrop.core.network.SupabaseConfig
import ph.foxdrop.core.network.auth.AuthRemoteDataSource
import ph.foxdrop.core.network.auth.SupabaseAuthRemoteDataSource

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkModule {
    @Binds
    abstract fun authRemoteDataSource(impl: SupabaseAuthRemoteDataSource): AuthRemoteDataSource

    companion object {
        @Provides
        @Singleton
        fun supabaseConfig(): SupabaseConfig = SupabaseConfig.fromBuildConfig()
    }
}
