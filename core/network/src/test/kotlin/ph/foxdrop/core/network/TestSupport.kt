package ph.foxdrop.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ph.foxdrop.core.network.session.ApiUser
import ph.foxdrop.core.network.session.SessionState
import ph.foxdrop.core.network.session.SessionStore
import ph.foxdrop.core.network.session.StoredSession

internal class InMemorySessionStore(initial: StoredSession? = null) : SessionStore {
    private val _state = MutableStateFlow<SessionState>(initial?.let(SessionState::Present) ?: SessionState.Absent)
    override val state: StateFlow<SessionState> = _state

    override suspend fun load(): StoredSession? = (_state.value as? SessionState.Present)?.session

    override suspend fun save(session: StoredSession) {
        _state.value = SessionState.Present(session)
    }

    override suspend fun updateUser(token: String, user: ApiUser) {
        load()?.takeIf { it.token == token }?.let { save(it.copy(user = user)) }
    }

    override suspend fun clear() {
        _state.value = SessionState.Absent
    }

    override suspend fun clearIfToken(token: String) {
        if (load()?.token == token) clear()
    }
}

/** A client whose requests are recorded in [requests] and answered by [handler]. */
internal fun mockClient(
    dispatcher: CoroutineDispatcher,
    requests: MutableList<HttpRequestData>,
    store: SessionStore = InMemorySessionStore(StoredSession("1|token", null, ApiUser(id = "u1"))),
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
) = FoxApiClient(
    config = ApiConfig("http://api.test/api/v1/"),
    sessionStore = store,
    engine = {
        MockEngine { request ->
            requests += request
            handler(request)
        }
    },
    io = dispatcher,
)

internal fun MockRequestHandleScope.jsonResponse(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

/** The raw body of a recorded request (JSON text or multipart), for assertions. */
internal suspend fun HttpRequestData.bodyString(): String = body.toByteArray().decodeToString()
