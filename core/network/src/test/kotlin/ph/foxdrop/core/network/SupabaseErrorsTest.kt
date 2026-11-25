package ph.foxdrop.core.network

import java.io.IOException
import java.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.model.AppRole
import ph.foxdrop.core.model.RoleGrant
import ph.foxdrop.core.network.auth.UserRoleRow
import ph.foxdrop.core.network.auth.appRolesFromAccessToken
import ph.foxdrop.core.network.auth.toGrants

class SupabaseErrorsTest {
    @Test
    fun `maps http status codes`() {
        assertEquals(DomainError.Validation("request", "bad otp"), httpStatusToDomainError(422, "bad otp"))
        assertEquals(DomainError.Validation("request", ""), httpStatusToDomainError(400, null))
        assertEquals(DomainError.Unauthorized, httpStatusToDomainError(401, null))
        assertEquals(DomainError.Unauthorized, httpStatusToDomainError(403, null))
        assertEquals(DomainError.NotFound, httpStatusToDomainError(404, null))
        assertEquals(DomainError.Conflict("taken"), httpStatusToDomainError(409, "taken"))
        assertEquals(DomainError.RateLimited, httpStatusToDomainError(429, null))
        assertEquals(DomainError.Unknown("boom"), httpStatusToDomainError(500, "boom"))
    }

    @Test
    fun `io problems are network errors and others unknown`() = runTest {
        assertEquals(FoxResult.Failure(DomainError.Network), runCatchingSupabase { throw IOException("offline") })
        assertEquals(
            FoxResult.Failure(DomainError.Unknown("IllegalStateException")),
            runCatchingSupabase { throw IllegalStateException("x") },
        )
        assertEquals(FoxResult.Success(1), runCatchingSupabase { 1 })
    }

    @Test(expected = CancellationException::class)
    fun `cancellation is rethrown`() = runTest {
        runCatchingSupabase { throw CancellationException("stop") }
    }

    @Test
    fun `role rows map to grants and unknown roles are ignored`() {
        val grants =
            listOf(
                UserRoleRow("operator", provinceId = "p1"),
                UserRoleRow("store_owner", provinceId = "p1", storeId = "s1"),
                UserRoleRow("future_role"),
            ).toGrants()
        assertEquals(
            listOf(RoleGrant(AppRole.Operator, "p1"), RoleGrant(AppRole.StoreOwner, "p1", "s1")),
            grants,
        )
    }

    @Test
    fun `roles are read from the access token claim`() {
        fun jwt(payload: String) =
            listOf("""{"alg":"HS256"}""", payload, "sig").joinToString(".") { part ->
                if (part == "sig") part else Base64.getUrlEncoder().withoutPadding().encodeToString(part.toByteArray())
            }
        val token =
            jwt(
                """{"sub":"u1","app_roles":[{"role":"customer","province_id":null,"store_id":null},""" +
                    """{"role":"rider","province_id":"p1","store_id":null}]}""",
            )
        assertEquals(
            listOf(RoleGrant(AppRole.Customer), RoleGrant(AppRole.Rider, "p1")),
            appRolesFromAccessToken(token),
        )
        // Hook not enabled: no claim means only the default customer behaviour.
        assertEquals(emptyList<RoleGrant>(), appRolesFromAccessToken(jwt("""{"sub":"u1"}""")))
        assertNull(appRolesFromAccessToken("not-a-jwt"))
    }

    @Test
    fun `config hides the key in logs`() {
        val config = SupabaseConfig("https://x.supabase.co", "secret-looking-key")
        assertTrue(config.isConfigured)
        assertFalse(config.toString().contains("secret-looking-key"))
        assertFalse(SupabaseConfig("", "k").isConfigured)
    }
}
