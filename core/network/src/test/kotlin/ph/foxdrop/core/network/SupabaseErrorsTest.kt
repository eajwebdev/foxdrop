package ph.foxdrop.core.network

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult

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
    fun `config hides the key in logs`() {
        val config = SupabaseConfig("https://x.supabase.co", "secret-looking-key")
        assertTrue(config.isConfigured)
        assertFalse(config.toString().contains("secret-looking-key"))
        assertFalse(SupabaseConfig("", "k").isConfigured)
    }
}
