package ph.foxdrop.core.common.result

/** Typed errors that cross layer boundaries. Exceptions never reach the UI (§16). */
sealed interface DomainError {
    data object Network : DomainError

    data object Unauthorized : DomainError

    data object NotFound : DomainError

    data object RateLimited : DomainError

    /** The backend URL/key are not set (local.properties missing). */
    data object NotConfigured : DomainError

    data class Validation(val field: String, val reason: String) : DomainError

    data class Conflict(val reason: String) : DomainError

    data class Unknown(val reason: String? = null) : DomainError
}

/** Result of an operation that can fail with a [DomainError]. */
sealed interface FoxResult<out T> {
    data class Success<out T>(val value: T) : FoxResult<T>

    data class Failure(val error: DomainError) : FoxResult<Nothing>
}

inline fun <T, R> FoxResult<T>.map(transform: (T) -> R): FoxResult<R> =
    when (this) {
        is FoxResult.Success -> FoxResult.Success(transform(value))
        is FoxResult.Failure -> this
    }

fun FoxResult<*>.errorOrNull(): DomainError? = (this as? FoxResult.Failure)?.error

inline fun <T> FoxResult<T>.onSuccess(action: (T) -> Unit): FoxResult<T> {
    if (this is FoxResult.Success) action(value)
    return this
}

inline fun <T> FoxResult<T>.onFailure(action: (DomainError) -> Unit): FoxResult<T> {
    if (this is FoxResult.Failure) action(error)
    return this
}
