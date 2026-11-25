package ph.foxdrop.core.network

import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult

/** Runs a Supabase call and turns any failure into a [DomainError]. Cancellation is never swallowed. */
@Suppress("TooGenericExceptionCaught") // Boundary to a third-party SDK: everything becomes a typed error.
internal suspend fun <T> runCatchingSupabase(block: suspend () -> T): FoxResult<T> =
    try {
        FoxResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        FoxResult.Failure(e.toDomainError())
    }

internal fun Throwable.toDomainError(): DomainError =
    when (this) {
        is RestException -> httpStatusToDomainError(statusCode, error)
        is HttpRequestTimeoutException, is HttpRequestException, is IOException -> DomainError.Network
        else -> DomainError.Unknown(this::class.simpleName)
    }

@Suppress("MagicNumber")
internal fun httpStatusToDomainError(status: Int, reason: String?): DomainError =
    when (status) {
        400, 422 -> DomainError.Validation(field = "request", reason = reason.orEmpty())
        401, 403 -> DomainError.Unauthorized
        404 -> DomainError.NotFound
        409 -> DomainError.Conflict(reason.orEmpty())
        429 -> DomainError.RateLimited
        else -> DomainError.Unknown(reason)
    }
