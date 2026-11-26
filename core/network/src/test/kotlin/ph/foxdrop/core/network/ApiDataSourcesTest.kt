package ph.foxdrop.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.domain.auth.SessionStatus
import ph.foxdrop.core.model.AppRole
import ph.foxdrop.core.model.RoleGrant
import ph.foxdrop.core.network.auth.ApiAuthRemoteDataSource
import ph.foxdrop.core.network.auth.ApiRolesRemoteDataSource
import ph.foxdrop.core.network.session.ApiUser
import ph.foxdrop.core.network.session.SessionState
import ph.foxdrop.core.network.session.SessionStore
import ph.foxdrop.core.network.session.StoredSession
import ph.foxdrop.core.network.session.UserRoleRow

private const val USER_JSON =
    """{"id":"u1","full_name":"Liza Gonzaga","phone":"+639170000006","email":null,"avatar_url":null,""" +
        """"roles":[{"role":"customer","province_id":null,"store_id":null},""" +
        """{"role":"operator","province_id":"neg","store_id":null}]}"""

private val signedIn = StoredSession("1|old-token", null, ApiUser(id = "u1", roles = listOf(UserRoleRow("customer"))))

class ApiDataSourcesTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val dispatcher = StandardTestDispatcher()

    private fun client(
        store: SessionStore,
        baseUrl: String = "http://api.test/api/v1/",
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = FoxApiClient(
        config = ApiConfig(baseUrl),
        sessionStore = store,
        engine = {
            MockEngine { request ->
                requests += request
                handler(request)
            }
        },
        io = dispatcher,
    )

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun HttpRequestData.bodyText(): String = (body as OutgoingContent.ByteArrayContent).bytes().decodeToString()

    @Test
    fun `phone sign-in saves the token and roles, and sends the name typed at sign-up`() = runTest(dispatcher) {
        val store = InMemorySessionStore()
        val api = client(store) { request ->
            when (request.url.encodedPath) {
                "/api/v1/auth/otp/send" -> json(
                    """{"expires_in_seconds":300,"resend_after_seconds":60}""",
                    HttpStatusCode.Accepted,
                )

                else -> json(
                    """{"data":$USER_JSON,"token":"2|new-token","expires_at":"2027-02-24T00:00:00+00:00"}""",
                    HttpStatusCode.Created,
                )
            }
        }
        val auth = ApiAuthRemoteDataSource(api, store, deviceName = "Pixel 9 · FoxDrop Business")

        assertEquals(FoxResult.Success(Unit), auth.requestPhoneOtp("+639170000006"))
        assertEquals(FoxResult.Success(Unit), auth.verifyPhoneOtp("+639170000006", "123456", "Liza Gonzaga"))

        val verify = requests.last()
        assertEquals("application/json", verify.headers[HttpHeaders.Accept])
        assertTrue(verify.bodyText().contains("\"full_name\":\"Liza Gonzaga\""))
        assertTrue(verify.bodyText().contains("\"device_name\":\"Pixel 9 · FoxDrop Business\""))
        assertEquals("2|new-token", store.load()?.token)
        assertEquals(SessionStatus.SignedIn("u1"), auth.sessionStatus.first())
    }

    @Test
    fun `a wrong code is a validation error and keeps the user signed out`() = runTest(dispatcher) {
        val store = InMemorySessionStore()
        val api = client(store) {
            json(
                """{"message":"Invalid.","errors":{"code":["That code is wrong or has expired."]}}""",
                HttpStatusCode.UnprocessableEntity,
            )
        }

        val result = ApiAuthRemoteDataSource(api, store, "Pixel").verifyPhoneOtp("+639170000006", "000000", null)

        assertEquals(FoxResult.Failure(DomainError.Validation("code", "That code is wrong or has expired.")), result)
        assertNull(store.load())
    }

    @Test
    fun `signed-in calls carry the token, and a 401 signs the phone out`() = runTest(dispatcher) {
        val store = InMemorySessionStore(signedIn)
        val api = client(store) { json("""{"message":"Unauthenticated."}""", HttpStatusCode.Unauthorized) }

        val result = ApiRolesRemoteDataSource(api, store).fetchRoles()

        assertEquals("Bearer 1|old-token", requests.single().headers[HttpHeaders.Authorization])
        assertEquals(FoxResult.Failure(DomainError.Unauthorized), result)
        assertEquals(SessionState.Absent, store.state.value)
    }

    @Test
    fun `roles come from the server and refresh the saved copy`() = runTest(dispatcher) {
        val store = InMemorySessionStore(signedIn)
        val api = client(store) { json("""{"data":$USER_JSON}""") }

        val roles = ApiRolesRemoteDataSource(api, store).fetchRoles()

        assertEquals(FoxResult.Success(listOf(RoleGrant(AppRole.Customer), RoleGrant(AppRole.Operator, "neg"))), roles)
        assertEquals(2, store.load()?.user?.roles?.size)
    }

    @Test
    fun `offline, the saved roles are still there`() = runTest(dispatcher) {
        val store = InMemorySessionStore(signedIn)
        val roles = ApiRolesRemoteDataSource(client(store) { throw IOException("no signal") }, store)

        assertEquals(FoxResult.Failure(DomainError.Network), roles.fetchRoles())
        assertEquals(listOf(RoleGrant(AppRole.Customer)), roles.savedRoles())
    }

    @Test
    fun `without a session there are no roles to read`() = runTest(dispatcher) {
        val api = client(InMemorySessionStore()) { error("must not be called") }

        assertEquals(
            FoxResult.Failure(DomainError.Unauthorized),
            ApiRolesRemoteDataSource(api, InMemorySessionStore()).fetchRoles(),
        )
    }

    @Test
    fun `sign-out works offline`() = runTest(dispatcher) {
        val store = InMemorySessionStore(signedIn)
        val api = client(store) { throw IOException("no signal") }

        assertEquals(FoxResult.Success(Unit), ApiAuthRemoteDataSource(api, store, "Pixel").signOut())
        assertNull(store.load())
    }

    @Test
    fun `without a server URL nothing is sent`() = runTest(dispatcher) {
        val api = client(InMemorySessionStore(), baseUrl = "") { error("must not be called") }

        val result = ApiAuthRemoteDataSource(
            api,
            InMemorySessionStore(),
            "Pixel",
        ).signInWithEmail("a@b.ph", "secret123")

        assertEquals(FoxResult.Failure(DomainError.NotConfigured), result)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `a late 401 for an old token does not sign out a newer session`() = runTest(dispatcher) {
        val store = InMemorySessionStore(signedIn)
        val newer = signedIn.copy(token = "3|newer-token")
        val api = client(store) {
            store.save(newer) // the user signed in again while this old request was in flight
            json("""{"message":"Unauthenticated."}""", HttpStatusCode.Unauthorized)
        }

        ApiRolesRemoteDataSource(api, store).fetchRoles()

        assertEquals("3|newer-token", store.load()?.token)
    }

    @Test
    fun `roles fetched for a session that was signed out meanwhile are dropped`() = runTest(dispatcher) {
        val store = InMemorySessionStore(signedIn)
        val api = client(store) {
            store.clear() // the user tapped Sign out while /me was loading
            json("""{"data":$USER_JSON}""")
        }

        ApiRolesRemoteDataSource(api, store).fetchRoles()

        assertNull(store.load())
    }
}
