package ph.foxdrop.core.network.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ph.foxdrop.core.model.FoxDropApp
import ph.foxdrop.core.network.AuthRedirect
import ph.foxdrop.core.network.SupabaseConfig
import ph.foxdrop.core.network.auth.AuthRemoteDataSource
import ph.foxdrop.core.network.auth.RolesRemoteDataSource
import ph.foxdrop.core.network.auth.SupabaseAuthRemoteDataSource
import ph.foxdrop.core.network.auth.SupabaseRolesRemoteDataSource

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkModule {
    @Binds
    abstract fun authRemoteDataSource(impl: SupabaseAuthRemoteDataSource): AuthRemoteDataSource

    @Binds
    abstract fun rolesRemoteDataSource(impl: SupabaseRolesRemoteDataSource): RolesRemoteDataSource

    companion object {
        @Provides
        @Singleton
        fun supabaseConfig(): SupabaseConfig = SupabaseConfig.fromBuildConfig()

        /** Each app module binds its own [FoxDropApp] (AppIdentityModule). */
        @Provides
        @Singleton
        fun authRedirect(app: FoxDropApp): AuthRedirect = AuthRedirect.forApp(app)
    }
}
