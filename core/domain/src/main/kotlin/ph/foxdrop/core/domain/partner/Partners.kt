package ph.foxdrop.core.domain.partner

import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.domain.media.PhotoUpload
import ph.foxdrop.core.model.GeoPoint

enum class StoreCategory { Food, Groceries, Medicine, More }

/** §5.2 store states (+ REJECTED, DECISIONS 59). */
enum class StoreStatus { Draft, Submitted, Approved, Active, Paused, Suspended, Closed, Rejected, Unknown }

data class MyStore(
    val id: String,
    val name: String,
    val category: StoreCategory,
    val status: StoreStatus,
    val rejectionReason: String?,
    val provinceName: String,
)

data class StoreApplicationForm(
    val provinceId: String,
    val name: String,
    val category: StoreCategory,
    val description: String?,
    val addressLine: String,
    val barangay: String?,
    val city: String,
    val phone: String?,
    val location: GeoPoint,
    val businessPermit: PhotoUpload,
    val validId: PhotoUpload,
)

interface StoreOnboardingRepository {
    suspend fun myStores(): FoxResult<List<MyStore>>

    /** Also resubmits a REJECTED store (the server updates it back to SUBMITTED). */
    suspend fun submit(form: StoreApplicationForm): FoxResult<MyStore>
}

enum class VehicleType { Motorcycle, Bicycle, Tricycle, Car }

/** §5.3 rider states (+ REJECTED). */
enum class RiderStatus { Applied, DocsReview, Approved, Rejected, Suspended, Deactivated, Unknown }

data class MyRider(
    val id: String,
    val status: RiderStatus,
    val rejectionReason: String?,
    val provinceName: String,
    val vehicleType: VehicleType,
    val plateNo: String?,
    val licenseNoLast4: String?,
    val licenseExpiry: String?,
)

data class RiderApplicationForm(
    val provinceId: String,
    val fullName: String,
    val vehicleType: VehicleType,
    val plateNo: String?,
    val licenseNo: String,
    /** `YYYY-MM-DD`. */
    val licenseExpiry: String,
    val validId: PhotoUpload,
    val driversLicense: PhotoUpload,
    val selfie: PhotoUpload,
    val orCr: PhotoUpload?,
    val vehiclePhoto: PhotoUpload?,
)

interface RiderOnboardingRepository {
    /** The rider record, or null when the user has not applied. */
    suspend fun me(): FoxResult<MyRider?>

    /** Also resubmits a REJECTED application. */
    suspend fun submit(form: RiderApplicationForm): FoxResult<MyRider>
}

/** A document photo for a reviewer: [url] is signed and short-lived (DECISIONS 60). */
data class PartnerDocument(val type: String, val url: String)

data class StoreSummary(
    val id: String,
    val name: String,
    val category: StoreCategory,
    val status: StoreStatus,
    val addressLine: String,
    val city: String,
    val ownerName: String?,
    val ownerPhone: String?,
    val createdAt: String,
)

data class StoreReview(
    val summary: StoreSummary,
    val description: String?,
    val barangay: String?,
    val phone: String?,
    val location: GeoPoint?,
    val rejectionReason: String?,
    val documents: List<PartnerDocument>,
)

data class RiderSummary(
    val id: String,
    val status: RiderStatus,
    val fullName: String?,
    val phone: String?,
    val vehicleType: VehicleType,
    val plateNo: String?,
    val createdAt: String,
)

data class RiderReview(
    val summary: RiderSummary,
    val licenseNoLast4: String?,
    val licenseExpiry: String?,
    val rejectionReason: String?,
    val documents: List<PartnerDocument>,
)

/** The operator's partner approvals (§9.4 step 4). Admins may override any province (server side). */
interface PartnerReviewRepository {
    suspend fun stores(status: StoreStatus): FoxResult<List<StoreSummary>>

    suspend fun store(storeId: String): FoxResult<StoreReview>

    suspend fun approveStore(storeId: String): FoxResult<StoreReview>

    suspend fun rejectStore(storeId: String, reason: String): FoxResult<StoreReview>

    suspend fun riders(status: RiderStatus): FoxResult<List<RiderSummary>>

    suspend fun rider(riderId: String): FoxResult<RiderReview>

    suspend fun approveRider(riderId: String): FoxResult<RiderReview>

    suspend fun rejectRider(riderId: String, reason: String): FoxResult<RiderReview>
}
