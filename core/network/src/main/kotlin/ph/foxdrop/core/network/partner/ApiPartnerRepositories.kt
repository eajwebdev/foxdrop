package ph.foxdrop.core.network.partner

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import javax.inject.Inject
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.common.result.map
import ph.foxdrop.core.domain.partner.MyRider
import ph.foxdrop.core.domain.partner.MyStore
import ph.foxdrop.core.domain.partner.PartnerReviewRepository
import ph.foxdrop.core.domain.partner.RiderApplicationForm
import ph.foxdrop.core.domain.partner.RiderOnboardingRepository
import ph.foxdrop.core.domain.partner.RiderReview
import ph.foxdrop.core.domain.partner.RiderStatus
import ph.foxdrop.core.domain.partner.RiderSummary
import ph.foxdrop.core.domain.partner.StoreApplicationForm
import ph.foxdrop.core.domain.partner.StoreOnboardingRepository
import ph.foxdrop.core.domain.partner.StoreReview
import ph.foxdrop.core.domain.partner.StoreStatus
import ph.foxdrop.core.domain.partner.StoreSummary
import ph.foxdrop.core.network.DataEnvelope
import ph.foxdrop.core.network.FoxApiClient
import ph.foxdrop.core.network.apiName
import ph.foxdrop.core.network.field
import ph.foxdrop.core.network.jsonBody
import ph.foxdrop.core.network.photo
import ph.foxdrop.core.network.upload

internal class ApiStoreOnboardingRepository @Inject constructor(
    private val api: FoxApiClient,
) : StoreOnboardingRepository {
    override suspend fun myStores(): FoxResult<List<MyStore>> =
        api.call { http -> http.get("stores/mine").body<DataEnvelope<List<MyStoreDto>>>().data }
            .map { rows -> rows.map(MyStoreDto::toDomain) }

    override suspend fun submit(form: StoreApplicationForm): FoxResult<MyStore> =
        api.call { http ->
            http.upload<MyStoreDto>("stores") {
                field("province_id", form.provinceId)
                field("name", form.name.trim())
                field("category", form.category.apiName())
                field("description", form.description?.trim())
                field("address_line", form.addressLine.trim())
                field("barangay", form.barangay?.trim())
                field("city", form.city.trim())
                field("phone", form.phone)
                field("latitude", form.location.lat.toString())
                field("longitude", form.location.lng.toString())
                photo("business_permit", form.businessPermit)
                photo("valid_id", form.validId)
            }
        }.map(MyStoreDto::toDomain)
}

internal class ApiRiderOnboardingRepository @Inject constructor(
    private val api: FoxApiClient,
) : RiderOnboardingRepository {
    override suspend fun me(): FoxResult<MyRider?> =
        api.call { http -> http.get("riders/me").body<DataEnvelope<MyRiderDto?>>().data }.map { it?.toDomain() }

    override suspend fun submit(form: RiderApplicationForm): FoxResult<MyRider> =
        api.call { http ->
            http.upload<MyRiderDto>("riders") {
                field("province_id", form.provinceId)
                field("full_name", form.fullName.trim())
                field("vehicle_type", form.vehicleType.apiName())
                field("plate_no", form.plateNo?.trim()?.uppercase())
                field("license_no", form.licenseNo.trim().uppercase())
                field("license_expiry", form.licenseExpiry)
                photo("valid_id", form.validId)
                photo("drivers_license", form.driversLicense)
                photo("selfie", form.selfie)
                form.orCr?.let { photo("or_cr", it) }
                form.vehiclePhoto?.let { photo("vehicle_photo", it) }
            }
        }.map(MyRiderDto::toDomain)
}

internal class ApiPartnerReviewRepository @Inject constructor(
    private val api: FoxApiClient,
) : PartnerReviewRepository {
    override suspend fun stores(status: StoreStatus): FoxResult<List<StoreSummary>> =
        api.call { http ->
            http.get("operator/stores") { parameter("status", status.apiName()) }
                .body<DataEnvelope<List<StoreReviewDto>>>().data
        }.map { rows -> rows.map(StoreReviewDto::toSummary) }

    override suspend fun store(storeId: String): FoxResult<StoreReview> = storeCall {
        it.get("operator/stores/$storeId")
    }

    override suspend fun approveStore(storeId: String): FoxResult<StoreReview> =
        storeCall { it.post("operator/stores/$storeId/approve") }

    override suspend fun rejectStore(storeId: String, reason: String): FoxResult<StoreReview> =
        storeCall { it.post("operator/stores/$storeId/reject") { jsonBody(RejectRequest(reason.trim())) } }

    override suspend fun riders(status: RiderStatus): FoxResult<List<RiderSummary>> =
        api.call { http ->
            http.get("operator/riders") { parameter("status", status.apiName()) }
                .body<DataEnvelope<List<RiderReviewDto>>>().data
        }.map { rows -> rows.map(RiderReviewDto::toSummary) }

    override suspend fun rider(riderId: String): FoxResult<RiderReview> = riderCall {
        it.get("operator/riders/$riderId")
    }

    override suspend fun approveRider(riderId: String): FoxResult<RiderReview> =
        riderCall { it.post("operator/riders/$riderId/approve") }

    override suspend fun rejectRider(riderId: String, reason: String): FoxResult<RiderReview> =
        riderCall { it.post("operator/riders/$riderId/reject") { jsonBody(RejectRequest(reason.trim())) } }

    private suspend fun storeCall(
        request: suspend (io.ktor.client.HttpClient) -> io.ktor.client.statement.HttpResponse,
    ): FoxResult<StoreReview> =
        api.call { http -> request(http).body<DataEnvelope<StoreReviewDto>>().data }.map(StoreReviewDto::toReview)

    private suspend fun riderCall(
        request: suspend (io.ktor.client.HttpClient) -> io.ktor.client.statement.HttpResponse,
    ): FoxResult<RiderReview> =
        api.call { http -> request(http).body<DataEnvelope<RiderReviewDto>>().data }.map(RiderReviewDto::toReview)
}
