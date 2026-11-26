package ph.foxdrop.core.network

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.model.AppRole
import ph.foxdrop.core.model.RoleGrant
import ph.foxdrop.core.network.session.UserRoleRow
import ph.foxdrop.core.network.session.toGrants

class ApiErrorsTest {
    @Test
    fun `maps http status codes`() {
        val body = ApiErrorBody(field = "code", message = "That code is wrong or has expired.")
        assertEquals(
            DomainError.Validation("code", "That code is wrong or has expired."),
            httpStatusToDomainError(422, body),
        )
        assertEquals(DomainError.Validation("request", ""), httpStatusToDomainError(400))
        assertEquals(DomainError.Unauthorized, httpStatusToDomainError(401))
        assertEquals(DomainError.Unauthorized, httpStatusToDomainError(403))
        assertEquals(DomainError.NotFound, httpStatusToDomainError(404))
        assertEquals(DomainError.RateLimited, httpStatusToDomainError(429))
        assertEquals(DomainError.NotConfigured, httpStatusToDomainError(503))
        assertEquals(DomainError.Unknown("boom"), httpStatusToDomainError(500, ApiErrorBody(null, "boom")))
    }

    @Test
    fun `reads the first field error of a Laravel body, else its message`() {
        assertEquals(
            ApiErrorBody("code", "That code is wrong or has expired."),
            laravelError("""{"message":"x","errors":{"code":["That code is wrong or has expired."]}}"""),
        )
        assertEquals(ApiErrorBody(null, "Too Many Attempts."), laravelError("""{"message":"Too Many Attempts."}"""))
        assertEquals(ApiErrorBody(null, null), laravelError("<html>Bad gateway</html>"))
    }

    @Test
    fun `io problems are network errors and others unknown`() = runTest {
        assertEquals(FoxResult.Failure(DomainError.Network), runCatchingApi { throw IOException("offline") })
        assertEquals(FoxResult.Failure(DomainError.Unknown("IllegalStateException")), runCatchingApi { error("x") })
        assertEquals(FoxResult.Success(1), runCatchingApi { 1 })
    }

    @Test(expected = CancellationException::class)
    fun `cancellation is rethrown`() = runTest {
        runCatchingApi { throw CancellationException("stop") }
    }

    @Test
    fun `role rows map to grants and unknown roles are ignored`() {
        val grants =
            listOf(
                UserRoleRow("operator", provinceId = "p1"),
                UserRoleRow("store_owner", provinceId = "p1", storeId = "s1"),
                UserRoleRow("future_role"),
            ).toGrants()
        assertEquals(listOf(RoleGrant(AppRole.Operator, "p1"), RoleGrant(AppRole.StoreOwner, "p1", "s1")), grants)
    }

    @Test
    fun `base url always ends with a slash`() {
        assertEquals("http://10.0.2.2:8000/api/v1/", ApiConfig(" http://10.0.2.2:8000/api/v1 ").baseUrl)
        assertEquals("http://10.0.2.2:8000/api/v1/", ApiConfig("http://10.0.2.2:8000/api/v1/").baseUrl)
        assertFalse(ApiConfig("").isConfigured)
    }
}
