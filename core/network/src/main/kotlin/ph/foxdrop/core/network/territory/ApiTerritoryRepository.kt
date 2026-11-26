package ph.foxdrop.core.network.territory

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.common.result.map
import ph.foxdrop.core.domain.territory.FeePreview
import ph.foxdrop.core.domain.territory.OperatorProvince
import ph.foxdrop.core.domain.territory.RateCard
import ph.foxdrop.core.domain.territory.TerritoryRepository
import ph.foxdrop.core.domain.territory.Zone
import ph.foxdrop.core.model.GeoPoint
import ph.foxdrop.core.network.FoxApiClient
import ph.foxdrop.core.network.LatLngDto
import ph.foxdrop.core.network.data
import ph.foxdrop.core.network.jsonBody
import ph.foxdrop.core.network.toDto

@Serializable
internal data class ZoneDto(
    val id: String,
    val name: String,
    @SerialName("is_active") val isActive: Boolean = true,
    val center: LatLngDto? = null,
    @SerialName("radius_km") val radiusKm: Double? = null,
    @SerialName("area_km2") val areaKm2: Double = 0.0,
) {
    fun toDomain() = Zone(id, name, isActive, center?.toPoint(), radiusKm, areaKm2)
}

@Serializable
internal data class CircleZoneRequest(
    val name: String,
    val center: LatLngDto,
    @SerialName("radius_km") val radiusKm: Double,
)

@Serializable
internal data class RateCardDto(
    @SerialName("base_fare_centavos") val baseFareCentavos: Long,
    @SerialName("base_km") val baseKm: Double,
    @SerialName("rate_per_km_centavos") val ratePerKmCentavos: Long,
    @SerialName("min_fee_centavos") val minFeeCentavos: Long,
    @SerialName("max_km") val maxKm: Double,
    @SerialName("rider_share_bps") val riderShareBps: Int,
    @SerialName("night_surcharge_centavos") val nightSurchargeCentavos: Long = 0,
    @SerialName("bad_weather_surcharge_centavos") val badWeatherSurchargeCentavos: Long = 0,
    @SerialName("extra_stop_centavos") val extraStopCentavos: Long = 0,
    @SerialName("effective_from") val effectiveFrom: String? = null,
) {
    fun toDomain() =
        RateCard(
            baseFareCentavos = baseFareCentavos,
            baseKm = baseKm,
            ratePerKmCentavos = ratePerKmCentavos,
            minFeeCentavos = minFeeCentavos,
            maxKm = maxKm,
            riderShareBps = riderShareBps,
            nightSurchargeCentavos = nightSurchargeCentavos,
            badWeatherSurchargeCentavos = badWeatherSurchargeCentavos,
            extraStopCentavos = extraStopCentavos,
            effectiveFrom = effectiveFrom,
        )
}

internal fun RateCard.toDto() =
    RateCardDto(
        baseFareCentavos,
        baseKm,
        ratePerKmCentavos,
        minFeeCentavos,
        maxKm,
        riderShareBps,
        nightSurchargeCentavos,
        badWeatherSurchargeCentavos,
        extraStopCentavos,
    )

@Serializable
internal data class PreviewRequest(
    @SerialName("base_fare_centavos") val baseFareCentavos: Long,
    @SerialName("base_km") val baseKm: Double,
    @SerialName("rate_per_km_centavos") val ratePerKmCentavos: Long,
    @SerialName("min_fee_centavos") val minFeeCentavos: Long,
    @SerialName("max_km") val maxKm: Double,
    @SerialName("rider_share_bps") val riderShareBps: Int,
    @SerialName("night_surcharge_centavos") val nightSurchargeCentavos: Long,
    @SerialName("bad_weather_surcharge_centavos") val badWeatherSurchargeCentavos: Long,
    @SerialName("extra_stop_centavos") val extraStopCentavos: Long,
    @SerialName("distances_km") val distancesKm: List<Double>,
)

@Serializable
internal data class FeePreviewDto(
    @SerialName("distance_km") val distanceKm: Double,
    @SerialName("delivery_fee_centavos") val deliveryFeeCentavos: Long? = null,
    @SerialName("rider_earning_centavos") val riderEarningCentavos: Long? = null,
    @SerialName("operator_cut_centavos") val operatorCutCentavos: Long? = null,
) {
    fun toDomain() = FeePreview(distanceKm, deliveryFeeCentavos, riderEarningCentavos, operatorCutCentavos)
}

@Serializable
internal data class OperatorProvinceDto(
    val id: String,
    val name: String,
    val center: LatLngDto? = null,
    @SerialName("store_fee_centavos") val storeFeeCentavos: Long,
    @SerialName("store_fee_grace_days") val storeFeeGraceDays: Int = 0,
) {
    fun toDomain() = OperatorProvince(id, name, center?.toPoint(), storeFeeCentavos, storeFeeGraceDays)
}

@Serializable
internal data class StoreFeeRequest(@SerialName("store_fee_centavos") val storeFeeCentavos: Long)

internal class ApiTerritoryRepository @Inject constructor(private val api: FoxApiClient) : TerritoryRepository {
    override suspend fun zones(): FoxResult<List<Zone>> =
        api.call { http -> http.get("operator/zones").data<List<ZoneDto>>() }
            .map { rows -> rows.map(ZoneDto::toDomain) }

    override suspend fun createCircleZone(name: String, center: GeoPoint, radiusKm: Double): FoxResult<Zone> =
        api.call { http ->
            http.post("operator/zones") { jsonBody(CircleZoneRequest(name, center.toDto(), radiusKm)) }
                .data<ZoneDto>()
        }.map(ZoneDto::toDomain)

    override suspend fun deleteZone(zoneId: String): FoxResult<Unit> =
        api.call { http -> http.delete("operator/zones/$zoneId") }.map { }

    override suspend fun rateCard(): FoxResult<RateCard> =
        api.call { http -> http.get("operator/rate-card").data<RateCardDto>() }
            .map(RateCardDto::toDomain)

    override suspend fun publishRateCard(card: RateCard): FoxResult<RateCard> =
        api.call { http ->
            http.post("operator/rate-cards") { jsonBody(card.toDto()) }.data<RateCardDto>()
        }.map(RateCardDto::toDomain)

    override suspend fun previewFees(card: RateCard, distancesKm: List<Double>): FoxResult<List<FeePreview>> =
        api.call { http ->
            val request =
                PreviewRequest(
                    card.baseFareCentavos,
                    card.baseKm,
                    card.ratePerKmCentavos,
                    card.minFeeCentavos,
                    card.maxKm,
                    card.riderShareBps,
                    card.nightSurchargeCentavos,
                    card.badWeatherSurchargeCentavos,
                    card.extraStopCentavos,
                    distancesKm,
                )
            http.post("operator/rate-cards/preview") { jsonBody(request) }
                .data<List<FeePreviewDto>>()
        }.map { rows -> rows.map(FeePreviewDto::toDomain) }

    override suspend fun province(): FoxResult<OperatorProvince> =
        api.call { http -> http.get("operator/province").data<OperatorProvinceDto>() }
            .map(OperatorProvinceDto::toDomain)

    override suspend fun updateStoreFee(storeFeeCentavos: Long): FoxResult<OperatorProvince> =
        api.call { http ->
            http.patch("operator/province") { jsonBody(StoreFeeRequest(storeFeeCentavos)) }
                .data<OperatorProvinceDto>()
        }.map(OperatorProvinceDto::toDomain)
}
