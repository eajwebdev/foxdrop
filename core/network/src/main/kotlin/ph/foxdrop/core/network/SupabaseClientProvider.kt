package ph.foxdrop.core.network

import android.content.Intent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ph.foxdrop.core.common.dispatcher.Dispatcher
import ph.foxdrop.core.common.dispatcher.FoxDispatchers
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.model.FoxDropApp
import ph.foxdrop.core.network.auth.EncryptedSessionManager

/** Where Supabase Auth sends the user back after email confirmation / OAuth: `foxdrop-customer://auth` etc. */
data class AuthRedirect(val scheme: String) {
    fun matches(intent: Intent?): Boolean {
        val uri = intent?.data ?: return false
        return intent.action == Intent.ACTION_VIEW && uri.scheme == scheme && uri.host == HOST
    }

    companion object {
        const val HOST = "auth"

        /** One scheme per app, so a link opens the right app without a chooser. Must match the app's manifest. */
        fun forApp(app: FoxDropApp) = AuthRedirect(scheme = "foxdrop-" + app.name.lowercase())
    }
}

/**
 * The single entry point to Supabase for every data source. The client is created lazily on the IO dispatcher
 * (never on the main thread during startup), or not at all when the app has no backend configured — callers then
 * get [DomainError.NotConfigured] and never see a nullable client.
 */
@Singleton
class SupabaseClientProvider @Inject constructor(
    private val config: SupabaseConfig,
    private val sessionManager: EncryptedSessionManager,
    private val redirect: AuthRedirect,
    @param:Dispatcher(FoxDispatchers.IO) private val io: CoroutineDispatcher,
) {
    /** App-lifetime scope for work that must outlive the screen that started it (auth redirects). */
    private val redirectScope = CoroutineScope(SupervisorJob() + io)

    private val client: SupabaseClient? by lazy {
        if (!config.isConfigured) return@lazy null
        createSupabaseClient(supabaseUrl = config.url, supabaseKey = config.anonKey) {
            install(Auth) {
                sessionManager = this@SupabaseClientProvider.sessionManager
                autoLoadFromStorage = true
                alwaysAutoRefresh = true
                scheme = redirect.scheme
                host = AuthRedirect.HOST
            }
            install(Postgrest)
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

    /**
     * Imports the session from an auth redirect (email confirmation link). Only `foxdrop-<app>://auth` links are
     * touched; every other intent is ignored (§17: deep-link parameters are validated). The client is created on the
     * IO dispatcher, never on the main thread. Returns whether the intent was an auth redirect.
     */
    fun handleAuthRedirect(intent: Intent?): Boolean {
        if (!redirect.matches(intent)) return false
        val redirectIntent = Intent(intent) // a copy: the caller may clear the original's data
        redirectScope.launch { client?.handleDeeplinks(redirectIntent) }
        return true
    }
}
