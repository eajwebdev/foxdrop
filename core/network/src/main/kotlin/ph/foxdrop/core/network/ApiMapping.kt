package ph.foxdrop.core.network

import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.model.GeoPoint

/** `PENDING_PAYMENT` → the enum entry `PendingPayment`; anything new from a newer server → [unknown]. */
internal inline fun <reified E : Enum<E>> apiEnum(value: String?, unknown: E): E {
    val name = value.orEmpty().lowercase().split('_').joinToString("") { part ->
        part.replaceFirstChar(Char::uppercase)
    }
    return enumValues<E>().firstOrNull { it.name == name } ?: unknown
}

/** The enum entry `PendingPayment` → `PENDING_PAYMENT`. */
internal fun Enum<*>.apiName(): String = name.replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), "_").uppercase()

/** A 404 means "you have none": an empty answer, not an error. */
internal fun <T> FoxResult<T>.notFoundAsNull(): FoxResult<T?> =
    when (this) {
        is FoxResult.Success -> this
        is FoxResult.Failure -> if (error == DomainError.NotFound) FoxResult.Success(null) else this
    }

@kotlinx.serialization.Serializable
internal data class LatLngDto(val lat: Double, val lng: Double) {
    fun toPoint(): GeoPoint = GeoPoint(lat, lng)
}

internal fun GeoPoint.toDto(): LatLngDto = LatLngDto(lat, lng)

@kotlinx.serialization.Serializable
internal data class NamedRefDto(val id: String, val name: String)
