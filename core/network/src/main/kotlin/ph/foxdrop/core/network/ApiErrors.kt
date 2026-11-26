package ph.foxdrop.core.network

import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult

/** Runs an API call and turns any failure into a [DomainError]. Cancellation is never swallowed. */
@Suppress("TooGenericExceptionCaught") // Network boundary: everything becomes a typed error.
internal suspend fun <T> runCatchingApi(block: suspend () -> T): FoxResult<T> =
    try {
        FoxResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ResponseException) {
        FoxResult.Failure(httpStatusToDomainError(e.response.status.value, laravelError(e.response.bodyAsText())))
    } catch (e: Exception) {
        FoxResult.Failure(e.toDomainError())
    }

internal fun Throwable.toDomainError(): DomainError =
    when (this) {
        is HttpRequestTimeoutException, is IOException -> DomainError.Network
        else -> DomainError.Unknown(this::class.simpleName)
    }

/** The field and message of a Laravel error body: `{"message": "...", "errors": {"code": ["..."]}}`. */
internal data class ApiErrorBody(val field: String?, val message: String?)

internal fun laravelError(body: String): ApiErrorBody =
    try {
        val root = ApiJson.parseToJsonElement(body).jsonObject
        val firstError = (root["errors"] as? JsonObject)?.entries?.firstOrNull()
        ApiErrorBody(
            field = firstError?.key,
            message = firstError?.value?.jsonArray?.firstOrNull()?.jsonPrimitive?.contentOrNull
                ?: root["message"]?.jsonPrimitive?.contentOrNull,
        )
    } catch (_: IllegalArgumentException) { // includes SerializationException
        ApiErrorBody(null, null)
    }

@Suppress("MagicNumber")
internal fun httpStatusToDomainError(status: Int, body: ApiErrorBody = ApiErrorBody(null, null)): DomainError =
    when (status) {
        400, 422 -> DomainError.Validation(field = body.field ?: "request", reason = body.message.orEmpty())

        401, 403 -> DomainError.Unauthorized

        // The server or one it depends on (e.g. Google) is unreachable: like no signal, try again later.
        502, 504 -> DomainError.Network

        404 -> DomainError.NotFound

        409 -> DomainError.Conflict(body.message.orEmpty())

        429 -> DomainError.RateLimited

        503 -> DomainError.NotConfigured

        else -> DomainError.Unknown(body.message)
    }
