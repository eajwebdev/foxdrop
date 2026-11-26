package ph.foxdrop.core.network.partner

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
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
import ph.foxdrop.core.network.FoxApiClient
import ph.foxdrop.core.network.apiName
import ph.foxdrop.core.network.data
import ph.foxdrop.core.network.field
import ph.foxdrop.core.network.jsonBody
import ph.foxdrop.core.network.photo
import ph.foxdrop.core.network.upload

internal class ApiStoreOnboardingRepository @Inject constructor(
    private val api: FoxApiClient,
) : StoreOnboardingRepository {
    override suspend fun myStores(): FoxResult<List<MyStore>> =
        api.call { http -> http.get("stores/mine").data<List<MyStoreDto>>() }
            .map { rows -> rows.map(MyStoreDto::toDomain) }

    override suspend fun submit(form: StoreApplicationForm): FoxResult<MyStore> =
        api.call { http ->
            http.upload<MyStoreDto>("stores") {
                field("province_id", form.provinceId)
                field("name", form.name)
                field("category", form.category.apiName())
                field("description", form.description)
                field("address_line", form.addressLine)
                field("barangay", form.barangay)
                field("city", form.city)
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
        api.call { http -> http.get("riders/me").data<MyRiderDto?>() }.map { it?.toDomain() }

    override suspend fun submit(form: RiderApplicationForm): FoxResult<MyRider> =
        api.call { http ->
            http.upload<MyRiderDto>("riders") {
                field("province_id", form.provinceId)
                field("full_name", form.fullName)
                field("vehicle_type", form.vehicleType.apiName())
                field("plate_no", form.plateNo)
                field("license_no", form.licenseNo)
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
                .data<List<StoreReviewDto>>()
        }.map { rows -> rows.map(StoreReviewDto::toSummary) }

    override suspend fun store(storeId: String): FoxResult<StoreReview> =
        review(StoreReviewDto::toReview) { it.get("operator/stores/$storeId") }

    override suspend fun approveStore(storeId: String): FoxResult<StoreReview> =
        review(StoreReviewDto::toReview) { it.post("operator/stores/$storeId/approve") }

    override suspend fun rejectStore(storeId: String, reason: String): FoxResult<StoreReview> =
        review(StoreReviewDto::toReview) {
            it.post("operator/stores/$storeId/reject") { jsonBody(RejectRequest(reason)) }
        }

    override suspend fun riders(status: RiderStatus): FoxResult<List<RiderSummary>> =
        api.call { http ->
            http.get("operator/riders") { parameter("status", status.apiName()) }
                .data<List<RiderReviewDto>>()
        }.map { rows -> rows.map(RiderReviewDto::toSummary) }

    override suspend fun rider(riderId: String): FoxResult<RiderReview> =
        review(RiderReviewDto::toReview) { it.get("operator/riders/$riderId") }

    override suspend fun approveRider(riderId: String): FoxResult<RiderReview> =
        review(RiderReviewDto::toReview) { it.post("operator/riders/$riderId/approve") }

    override suspend fun rejectRider(riderId: String, reason: String): FoxResult<RiderReview> =
        review(RiderReviewDto::toReview) {
            it.post("operator/riders/$riderId/reject") { jsonBody(RejectRequest(reason)) }
        }

    /** One store or rider review call: decodes the `{"data": ...}` dto and maps it with [toReview]. */
    private suspend inline fun <reified D, R> review(
        crossinline toReview: (D) -> R,
        crossinline request: suspend (HttpClient) -> HttpResponse,
    ): FoxResult<R> = api.call { http -> request(http).data<D>() }.map { toReview(it) }
}
