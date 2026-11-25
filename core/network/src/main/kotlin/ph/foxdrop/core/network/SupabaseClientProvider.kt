package ph.foxdrop.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import ph.foxdrop.core.common.dispatcher.Dispatcher
import ph.foxdrop.core.common.dispatcher.FoxDispatchers
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.network.auth.EncryptedSessionManager

/**
 * The single entry point to Supabase for every data source. The client is created lazily on the IO dispatcher
 * (never on the main thread during startup), or not at all when the app has no backend configured — callers then
 * get [DomainError.NotConfigured] and never see a nullable client.
 */
@Singleton
class SupabaseClientProvider @Inject constructor(
    private val config: SupabaseConfig,
    private val sessionManager: EncryptedSessionManager,
    @param:Dispatcher(FoxDispatchers.IO) private val io: CoroutineDispatcher,
) {
    private val client: SupabaseClient? by lazy {
        if (!config.isConfigured) return@lazy null
        createSupabaseClient(supabaseUrl = config.url, supabaseKey = config.anonKey) {
            install(Auth) {
                sessionManager = this@SupabaseClientProvider.sessionManager
                autoLoadFromStorage = true
                alwaysAutoRefresh = true
                // TODO(phase-1): per-app redirect schemes (foxdrop-customer://auth, …) together with the manifest
                // intent-filters and handleDeeplinks() in MainActivity, for email confirmation and Google sign-in.
            }
        }
    }

    /** Runs a Supabase call; every failure becomes a [DomainError]. */
    suspend fun <T> call(block: suspend (SupabaseClient) -> T): FoxResult<T> =
        withContext(io) {
            val supabase = client ?: return@withContext FoxResult.Failure(DomainError.NotConfigured)
            runCatchingSupabase { block(supabase) }
        }

    /** Observes Supabase state; emits [whenNotConfigured] once if there is no backend. */
    fun <T> observe(whenNotConfigured: T, block: (SupabaseClient) -> Flow<T>): Flow<T> =
        flow {
            val supabase = client
            if (supabase == null) emit(whenNotConfigured) else emitAll(block(supabase))
        }.flowOn(io)
}
