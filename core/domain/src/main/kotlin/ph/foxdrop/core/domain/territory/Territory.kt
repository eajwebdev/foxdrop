package ph.foxdrop.core.domain.territory

import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.model.GeoPoint

/** A delivery coverage area (§6). Circles in the app for now (DECISIONS 58). */
data class Zone(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val center: GeoPoint?,
    val radiusKm: Double?,
    val areaKm2: Double,
)

/** A versioned rate card (§6). Money in centavos, shares in basis points. */
data class RateCard(
    val baseFareCentavos: Long,
    val baseKm: Double,
    val ratePerKmCentavos: Long,
    val minFeeCentavos: Long,
    val maxKm: Double,
    val riderShareBps: Int,
    val nightSurchargeCentavos: Long = 0,
    val badWeatherSurchargeCentavos: Long = 0,
    val extraStopCentavos: Long = 0,
    val effectiveFrom: String? = null,
) {
    companion object {
        /** §14 defaults: ₱40 for the first 2 km, ₱10/km after, min ₱40, max 15 km, rider 80 %. */
        val Default =
            RateCard(
                baseFareCentavos = 4_000,
                baseKm = 2.0,
                ratePerKmCentavos = 1_000,
                minFeeCentavos = 4_000,
                maxKm = 15.0,
                riderShareBps = 8_000,
            )
    }
}

/** One row of the server-computed fee preview. Null fees mean the distance is beyond `max_km`. */
data class FeePreview(
    val distanceKm: Double,
    val deliveryFeeCentavos: Long?,
    val riderEarningCentavos: Long?,
    val operatorCutCentavos: Long?,
)

data class OperatorProvince(
    val id: String,
    val name: String,
    val center: GeoPoint?,
    val storeFeeCentavos: Long,
    val storeFeeGraceDays: Int,
)

/** Zones, rate card and province settings of the signed-in operator's province (§9.4 steps 3 and 5). */
interface TerritoryRepository {
    suspend fun zones(): FoxResult<List<Zone>>

    suspend fun createCircleZone(name: String, center: GeoPoint, radiusKm: Double): FoxResult<Zone>

    suspend fun deleteZone(zoneId: String): FoxResult<Unit>

    suspend fun rateCard(): FoxResult<RateCard>

    suspend fun publishRateCard(card: RateCard): FoxResult<RateCard>

    suspend fun previewFees(card: RateCard, distancesKm: List<Double>): FoxResult<List<FeePreview>>

    suspend fun province(): FoxResult<OperatorProvince>

    suspend fun updateStoreFee(storeFeeCentavos: Long): FoxResult<OperatorProvince>
}
