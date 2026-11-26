package ph.foxdrop.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ph.foxdrop.core.common.dispatcher.Dispatcher
import ph.foxdrop.core.common.dispatcher.FoxDispatchers
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.network.session.SessionState
import ph.foxdrop.core.network.session.SessionStore

/**
 * The single entry point to the FoxDrop API for every data source. Requests run on IO, carry the stored bearer token,
 * and every failure becomes a [DomainError]. A `401` means the server no longer accepts the token (expired, revoked,
 * signed out elsewhere): the session is cleared here once, and every screen that observes the session sends the user
 * back to sign-in.
 */
@Singleton
class FoxApiClient @Inject constructor(
    private val config: ApiConfig,
    private val sessionStore: SessionStore,
    private val engine: dagger.Lazy<HttpClientEngine>,
    @param:Dispatcher(FoxDispatchers.IO) private val io: CoroutineDispatcher,
) {
    private val http: HttpClient by lazy {
        HttpClient(engine.get()) {
            expectSuccess = true
            install(ContentNegotiation) { json(ApiJson) }
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
            }
            defaultRequest {
                url(config.baseUrl)
                contentType(ContentType.Application.Json)
                headers.append("Accept", "application/json")
                (sessionStore.state.value as? SessionState.Present)?.let { bearerAuth(it.session.token) }
            }
        }
    }

    suspend fun <T> call(block: suspend (HttpClient) -> T): FoxResult<T> =
        withContext(io) {
            if (!config.isConfigured) return@withContext FoxResult.Failure(DomainError.NotConfigured)
            val sentToken = sessionStore.load()?.token
            runCatchingApi {
                try {
                    block(http)
                } catch (e: ResponseException) {
                    // Only the session this request was sent with is rejected; a newer sign-in stays.
                    if (e.response.status == HttpStatusCode.Unauthorized && sentToken != null) {
                        sessionStore.clearIfToken(sentToken)
                    }
                    throw e
                }
            }
        }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000L
        const val REQUEST_TIMEOUT_MS = 20_000L
    }
}

/** JSON for API bodies: tolerant of fields a newer server adds. */
internal val ApiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
