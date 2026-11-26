package ph.foxdrop.core.domain.operator

import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.domain.media.PhotoUpload

/** §5.1 license states. [Unknown] keeps old apps working when the server adds a state. */
enum class LicenseStatus { Applied, PendingPayment, Active, Grace, Expired, Suspended, Rejected, Unknown }

data class License(
    val id: String,
    val provinceId: String,
    val provinceName: String,
    val status: LicenseStatus,
    val startAt: String?,
    val endAt: String?,
    /** Whole days until the end date (negative in GRACE), or null before the first payment. */
    val daysLeft: Int?,
    val feeCentavos: Long,
    /** The server accepts a payment now (first payment or renewal). */
    val canPay: Boolean,
)

enum class ApplicationStatus { Submitted, Approved, Rejected, Withdrawn, Unknown }

data class OperatorApplication(
    val id: String,
    val status: ApplicationStatus,
    val provinceName: String,
    val businessName: String,
    val rejectionReason: String?,
    val license: License?,
)

data class OperatorApplicationForm(
    val provinceId: String,
    val businessName: String,
    val registrationNumber: String?,
    val contactPhone: String,
    val contactEmail: String?,
    val validId: PhotoUpload,
    val businessPermit: PhotoUpload?,
)

enum class PaymentStatus { Pending, Paid, Failed, Expired, Unknown }

data class Payment(
    val id: String,
    val status: PaymentStatus,
    val amountCentavos: Long,
    val checkoutUrl: String,
)

/** Operator application, license and its payments (§5.1, §9.4 steps 1, 2 and 8). */
interface OperatorLicenseRepository {
    suspend fun submitApplication(form: OperatorApplicationForm): FoxResult<OperatorApplication>

    /** The latest application, or null when there is none. */
    suspend fun myApplication(): FoxResult<OperatorApplication?>

    /** The live license the user holds, or null. */
    suspend fun myLicense(): FoxResult<License?>

    suspend fun startPayment(licenseId: String): FoxResult<Payment>

    suspend fun payment(paymentId: String): FoxResult<Payment>
}
