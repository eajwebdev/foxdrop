package ph.foxdrop.core.domain.province

import ph.foxdrop.core.common.result.FoxResult

/** A province from the public directory (`GET provinces`). */
data class ProvinceOption(
    val id: String,
    val name: String,
    val regionName: String,
    /** Customers can order here (enabled, ACTIVE or GRACE license): stores and riders may apply. */
    val isServiceable: Boolean,
    /** No live license: an operator may apply (§5.1). */
    val isAvailable: Boolean,
)

interface ProvinceRepository {
    suspend fun directory(): FoxResult<List<ProvinceOption>>
}
