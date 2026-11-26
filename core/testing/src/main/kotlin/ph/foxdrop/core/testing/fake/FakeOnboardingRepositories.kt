// Sample data is literal by nature: the numbers are the fixtures.
@file:Suppress("MagicNumber")

package ph.foxdrop.core.testing.fake

import android.net.Uri
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.domain.media.PhotoUpload
import ph.foxdrop.core.domain.operator.ApplicationStatus
import ph.foxdrop.core.domain.operator.License
import ph.foxdrop.core.domain.operator.LicenseStatus
import ph.foxdrop.core.domain.operator.OperatorApplication
import ph.foxdrop.core.domain.operator.OperatorApplicationForm
import ph.foxdrop.core.domain.operator.OperatorLicenseRepository
import ph.foxdrop.core.domain.operator.Payment
import ph.foxdrop.core.domain.operator.PaymentStatus
import ph.foxdrop.core.domain.partner.MyRider
import ph.foxdrop.core.domain.partner.MyStore
import ph.foxdrop.core.domain.partner.PartnerDocument
import ph.foxdrop.core.domain.partner.PartnerReviewRepository
import ph.foxdrop.core.domain.partner.RiderApplicationForm
import ph.foxdrop.core.domain.partner.RiderOnboardingRepository
import ph.foxdrop.core.domain.partner.RiderReview
import ph.foxdrop.core.domain.partner.RiderStatus
import ph.foxdrop.core.domain.partner.RiderSummary
import ph.foxdrop.core.domain.partner.StoreApplicationForm
import ph.foxdrop.core.domain.partner.StoreCategory
import ph.foxdrop.core.domain.partner.StoreOnboardingRepository
import ph.foxdrop.core.domain.partner.StoreReview
import ph.foxdrop.core.domain.partner.StoreStatus
import ph.foxdrop.core.domain.partner.StoreSummary
import ph.foxdrop.core.domain.partner.VehicleType
import ph.foxdrop.core.domain.province.ProvinceOption
import ph.foxdrop.core.domain.province.ProvinceRepository
import ph.foxdrop.core.domain.territory.FeePreview
import ph.foxdrop.core.domain.territory.OperatorProvince
import ph.foxdrop.core.domain.territory.RateCard
import ph.foxdrop.core.domain.territory.TerritoryRepository
import ph.foxdrop.core.domain.territory.Zone
import ph.foxdrop.core.location.LocationClient
import ph.foxdrop.core.location.LocationResult
import ph.foxdrop.core.media.ImageCompressor
import ph.foxdrop.core.model.GeoPoint

/** Sample data shared by feature tests and screenshot tests. */
object SampleData {
    val negros =
        ProvinceOption(
            "neg",
            "Negros Occidental",
            "Region VI (Western Visayas)",
            isServiceable = true,
            isAvailable = false,
        )
    val iloilo =
        ProvinceOption("ilo", "Iloilo", "Region VI (Western Visayas)", isServiceable = false, isAvailable = true)
    val aklan = ProvinceOption("akl", "Aklan", "Region VI (Western Visayas)", isServiceable = false, isAvailable = true)
    val provinces = listOf(aklan, iloilo, negros)

    val photo = PhotoUpload(byteArrayOf(1, 2, 3), "photo.jpg")

    fun license(status: LicenseStatus = LicenseStatus.Active, daysLeft: Int? = 335, canPay: Boolean = true) =
        License(
            id = "lic-1",
            provinceId = "ilo",
            provinceName = "Iloilo",
            status = status,
            startAt = "2026-11-26T00:00:00Z",
            endAt = "2027-11-26T00:00:00Z",
            daysLeft = daysLeft,
            feeCentavos = 1_500_000,
            canPay = canPay,
        )

    fun application(
        status: ApplicationStatus = ApplicationStatus.Submitted,
        license: License? = license(
            LicenseStatus.Applied,
            null,
            false,
        ),
    ) =
        OperatorApplication("app-1", status, "Iloilo", "Gonzaga Logistics", null, license)

    fun payment(status: PaymentStatus = PaymentStatus.Pending) =
        Payment("pay-1", status, 1_500_000, "http://10.0.2.2:8000/dev/checkout/pay-1")

    val zone = Zone("z1", "Metro Bacolod", true, GeoPoint(10.6765, 122.9509), 5.0, 78.5)
    val province = OperatorProvince("neg", "Negros Occidental", GeoPoint(10.6765, 122.9509), 1_500, 7)

    fun preview(card: RateCard, distances: List<Double>) =
        distances.map { km ->
            val extra = kotlin.math.ceil(((km - card.baseKm).coerceAtLeast(0.0)) * 10) / 10
            val fee = maxOf(card.minFeeCentavos, card.baseFareCentavos + (extra * card.ratePerKmCentavos).toLong())
            val rider = fee * card.riderShareBps / 10_000
            if (km > card.maxKm) FeePreview(km, null, null, null) else FeePreview(km, fee, rider, fee - rider)
        }

    fun myStore(status: StoreStatus = StoreStatus.Submitted, reason: String? = null) =
        MyStore("s1", "Mama's Kitchen", StoreCategory.Food, status, reason, "Negros Occidental")

    fun myRider(status: RiderStatus = RiderStatus.DocsReview, reason: String? = null) =
        MyRider("r1", status, reason, "Negros Occidental", VehicleType.Motorcycle, "NEG 1234", "4567", "2028-01-31")

    val storeSummary =
        StoreSummary(
            "s1", "Mama's Kitchen", StoreCategory.Food, StoreStatus.Submitted, "Lacson St.", "Bacolod City",
            "Rosa Lacson", "+639170000007", "2026-11-26T02:00:00Z",
        )
    val storeReview =
        StoreReview(
            storeSummary,
            "Home-style burgers, fries and inasal.",
            "Barangay 17",
            "+639170000007",
            GeoPoint(10.674, 122.953),
            null,
            listOf(
                PartnerDocument("BUSINESS_PERMIT", "https://example.test/permit"),
                PartnerDocument("VALID_ID", "https://example.test/id"),
            ),
        )
    val riderSummary =
        RiderSummary(
            "r1",
            RiderStatus.DocsReview,
            "Paolo Reyes",
            "+639170000003",
            VehicleType.Motorcycle,
            "NEG 1234",
            "2026-11-26T02:00:00Z",
        )
    val riderReview =
        RiderReview(
            riderSummary,
            "4567",
            "2028-01-31",
            null,
            listOf(
                PartnerDocument("VALID_ID", "https://example.test/id"),
                PartnerDocument("SELFIE", "https://example.test/selfie"),
            ),
        )
}

private fun <T> DomainError?.or(value: T): FoxResult<T> = if (this ==
    null
) {
    FoxResult.Success(value)
} else {
    FoxResult.Failure(this)
}

class FakeProvinceRepository(var provinces: List<ProvinceOption> = SampleData.provinces) : ProvinceRepository {
    var error: DomainError? = null

    override suspend fun directory(): FoxResult<List<ProvinceOption>> = error.or(provinces)
}

/** Records calls; [payments] are returned one by one by [payment] (the last one repeats). */
class FakeOperatorLicenseRepository : OperatorLicenseRepository {
    val calls = mutableListOf<String>()
    var application: OperatorApplication? = null
    var license: License? = null
    var submitError: DomainError? = null
    var loadError: DomainError? = null
    var paymentError: DomainError? = null
    var payments: MutableList<Payment> = mutableListOf(SampleData.payment(PaymentStatus.Paid))
    var submitted: OperatorApplicationForm? = null

    override suspend fun submitApplication(form: OperatorApplicationForm): FoxResult<OperatorApplication> {
        calls += "submit:${form.provinceId}"
        submitted = form
        return submitError.or(SampleData.application()).also { if (it is FoxResult.Success) application = it.value }
    }

    override suspend fun myApplication(): FoxResult<OperatorApplication?> = loadError.or(application)

    override suspend fun myLicense(): FoxResult<License?> = loadError.or(license)

    override suspend fun startPayment(licenseId: String): FoxResult<Payment> {
        calls += "pay:$licenseId"
        return paymentError.or(SampleData.payment())
    }

    override suspend fun payment(paymentId: String): FoxResult<Payment> {
        calls += "poll:$paymentId"
        val next = if (payments.size > 1) payments.removeAt(0) else payments.first()
        return FoxResult.Success(next)
    }
}

class FakeTerritoryRepository : TerritoryRepository {
    val calls = mutableListOf<String>()
    var zones = mutableListOf(SampleData.zone)
    var card = RateCard.Default
    var province = SampleData.province
    var error: DomainError? = null
    var writeError: DomainError? = null

    override suspend fun zones(): FoxResult<List<Zone>> = error.or(zones.toList())

    override suspend fun createCircleZone(name: String, center: GeoPoint, radiusKm: Double): FoxResult<Zone> {
        calls += "zone:$name:$radiusKm"
        val zone = Zone("z${zones.size + 1}", name, true, center, radiusKm, Math.PI * radiusKm * radiusKm)
        return writeError.or(zone).also { if (it is FoxResult.Success) zones += zone }
    }

    override suspend fun deleteZone(zoneId: String): FoxResult<Unit> {
        calls += "delete:$zoneId"
        return writeError.or(Unit).also { if (it is FoxResult.Success) zones.removeAll { z -> z.id == zoneId } }
    }

    override suspend fun rateCard(): FoxResult<RateCard> = error.or(card)

    override suspend fun publishRateCard(card: RateCard): FoxResult<RateCard> {
        calls += "publish:${card.baseFareCentavos}"
        return writeError.or(card).also { if (it is FoxResult.Success) this.card = card }
    }

    override suspend fun previewFees(card: RateCard, distancesKm: List<Double>): FoxResult<List<FeePreview>> {
        calls += "preview:${card.baseFareCentavos}"
        return error.or(SampleData.preview(card, distancesKm))
    }

    override suspend fun province(): FoxResult<OperatorProvince> = error.or(province)

    override suspend fun updateStoreFee(storeFeeCentavos: Long): FoxResult<OperatorProvince> {
        calls += "storeFee:$storeFeeCentavos"
        return writeError.or(province.copy(storeFeeCentavos = storeFeeCentavos)).also {
            if (it is FoxResult.Success) province = it.value
        }
    }
}

class FakeStoreOnboardingRepository : StoreOnboardingRepository {
    var stores = mutableListOf<MyStore>()
    var error: DomainError? = null
    var submitError: DomainError? = null
    var submitted: StoreApplicationForm? = null

    override suspend fun myStores(): FoxResult<List<MyStore>> = error.or(stores.toList())

    override suspend fun submit(form: StoreApplicationForm): FoxResult<MyStore> {
        submitted = form
        val store = SampleData.myStore().copy(name = form.name)
        return submitError.or(store).also { if (it is FoxResult.Success) stores = mutableListOf(store) }
    }
}

class FakeRiderOnboardingRepository : RiderOnboardingRepository {
    var rider: MyRider? = null
    var error: DomainError? = null
    var submitError: DomainError? = null
    var submitted: RiderApplicationForm? = null

    override suspend fun me(): FoxResult<MyRider?> = error.or(rider)

    override suspend fun submit(form: RiderApplicationForm): FoxResult<MyRider> {
        submitted = form
        val mine = SampleData.myRider().copy(vehicleType = form.vehicleType)
        return submitError.or(mine).also { if (it is FoxResult.Success) rider = mine }
    }
}

class FakePartnerReviewRepository : PartnerReviewRepository {
    val calls = mutableListOf<String>()
    var stores = mutableListOf(SampleData.storeSummary)
    var riders = mutableListOf(SampleData.riderSummary)
    var error: DomainError? = null
    var writeError: DomainError? = null

    override suspend fun stores(status: StoreStatus): FoxResult<List<StoreSummary>> {
        calls += "stores:$status"
        return error.or(stores.filter { it.status == status })
    }

    override suspend fun store(storeId: String): FoxResult<StoreReview> = error.or(SampleData.storeReview)

    override suspend fun approveStore(storeId: String): FoxResult<StoreReview> {
        calls += "approveStore:$storeId"
        return writeError.or(
            SampleData.storeReview.copy(summary = SampleData.storeSummary.copy(status = StoreStatus.Active)),
        )
    }

    override suspend fun rejectStore(storeId: String, reason: String): FoxResult<StoreReview> {
        calls += "rejectStore:$storeId:$reason"
        return writeError.or(
            SampleData.storeReview.copy(
                summary = SampleData.storeSummary.copy(status = StoreStatus.Rejected),
                rejectionReason = reason,
            ),
        )
    }

    override suspend fun riders(status: RiderStatus): FoxResult<List<RiderSummary>> {
        calls += "riders:$status"
        return error.or(riders.filter { it.status == status })
    }

    override suspend fun rider(riderId: String): FoxResult<RiderReview> = error.or(SampleData.riderReview)

    override suspend fun approveRider(riderId: String): FoxResult<RiderReview> {
        calls += "approveRider:$riderId"
        return writeError.or(
            SampleData.riderReview.copy(summary = SampleData.riderSummary.copy(status = RiderStatus.Approved)),
        )
    }

    override suspend fun rejectRider(riderId: String, reason: String): FoxResult<RiderReview> {
        calls += "rejectRider:$riderId:$reason"
        return writeError.or(
            SampleData.riderReview.copy(
                summary = SampleData.riderSummary.copy(status = RiderStatus.Rejected),
                rejectionReason = reason,
            ),
        )
    }
}

/** Returns [result] for every photo (null = "could not read"). */
class FakeImageCompressor(var result: PhotoUpload? = SampleData.photo) : ImageCompressor {
    override suspend fun compress(
        uri: Uri,
        fileName: String,
    ): PhotoUpload? = result?.let { PhotoUpload(it.bytes, fileName) }
}

class FakeLocationClient(
    var result: LocationResult = LocationResult.Found(
        GeoPoint(10.674, 122.953),
    ),
) : LocationClient {
    var permitted = true

    override fun hasPermission(): Boolean = permitted

    override suspend fun currentLocation(): LocationResult = if (permitted) result else LocationResult.PermissionDenied
}
