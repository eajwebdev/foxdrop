package ph.foxdrop.core.network.di

import android.os.Build
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Qualifier
import javax.inject.Singleton
import ph.foxdrop.core.model.FoxDropApp
import ph.foxdrop.core.network.ApiConfig
import ph.foxdrop.core.network.auth.ApiAuthRemoteDataSource
import ph.foxdrop.core.network.auth.ApiRolesRemoteDataSource
import ph.foxdrop.core.network.auth.AuthRemoteDataSource
import ph.foxdrop.core.network.auth.RolesRemoteDataSource
import ph.foxdrop.core.network.session.EncryptedSessionStore
import ph.foxdrop.core.network.session.SessionStore

/** Label of this phone's sign-in token, e.g. "Samsung SM-A155F · FoxDrop Rider", shown in a future device list. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DeviceName

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkModule {
    @Binds
    abstract fun authRemoteDataSource(impl: ApiAuthRemoteDataSource): AuthRemoteDataSource

    @Binds
    abstract fun rolesRemoteDataSource(impl: ApiRolesRemoteDataSource): RolesRemoteDataSource

    @Binds
    abstract fun sessionStore(impl: EncryptedSessionStore): SessionStore

    companion object {
        @Provides
        @Singleton
        fun apiConfig(): ApiConfig = ApiConfig.fromBuildConfig()

        @Provides
        @Singleton
        fun httpEngine(): HttpClientEngine = OkHttp.create()

        /** Each app module binds its own [FoxDropApp] (AppIdentityModule). */
        @Provides
        @DeviceName
        fun deviceName(app: FoxDropApp): String =
            "${Build.MANUFACTURER.replaceFirstChar(
                Char::uppercase,
            )} ${Build.MODEL} · FoxDrop ${app.name}".take(MAX_DEVICE_NAME)

        private const val MAX_DEVICE_NAME = 120
    }
}
