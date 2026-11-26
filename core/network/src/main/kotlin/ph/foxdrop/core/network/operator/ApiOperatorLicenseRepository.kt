package ph.foxdrop.core.network.operator

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.common.result.map
import ph.foxdrop.core.domain.operator.ApplicationStatus
import ph.foxdrop.core.domain.operator.License
import ph.foxdrop.core.domain.operator.LicenseStatus
import ph.foxdrop.core.domain.operator.OperatorApplication
import ph.foxdrop.core.domain.operator.OperatorApplicationForm
import ph.foxdrop.core.domain.operator.OperatorLicenseRepository
import ph.foxdrop.core.domain.operator.Payment
import ph.foxdrop.core.domain.operator.PaymentStatus
import ph.foxdrop.core.network.DataEnvelope
import ph.foxdrop.core.network.FoxApiClient
import ph.foxdrop.core.network.NamedRefDto
import ph.foxdrop.core.network.apiEnum
import ph.foxdrop.core.network.field
import ph.foxdrop.core.network.notFoundAsNull
import ph.foxdrop.core.network.photo
import ph.foxdrop.core.network.upload

@Serializable
internal data class LicenseDto(
    val id: String,
    val province: NamedRefDto,
    val status: String,
    @SerialName("start_at") val startAt: String? = null,
    @SerialName("end_at") val endAt: String? = null,
    @SerialName("days_left") val daysLeft: Int? = null,
    @SerialName("fee_centavos") val feeCentavos: Long = 0,
    @SerialName("can_pay") val canPay: Boolean = false,
) {
    fun toDomain() =
        License(
            id = id,
            provinceId = province.id,
            provinceName = province.name,
            status = apiEnum(status, LicenseStatus.Unknown),
            startAt = startAt,
            endAt = endAt,
            daysLeft = daysLeft,
            feeCentavos = feeCentavos,
            canPay = canPay,
        )
}

@Serializable
internal data class OperatorApplicationDto(
    val id: String,
    val status: String,
    val province: NamedRefDto,
    @SerialName("business_name") val businessName: String,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    val license: LicenseDto? = null,
) {
    fun toDomain() =
        OperatorApplication(
            id = id,
            status = apiEnum(status, ApplicationStatus.Unknown),
            provinceName = province.name,
            businessName = businessName,
            rejectionReason = rejectionReason,
            license = license?.toDomain(),
        )
}

@Serializable
internal data class PaymentDto(
    val id: String,
    val status: String,
    @SerialName("amount_centavos") val amountCentavos: Long,
    @SerialName("checkout_url") val checkoutUrl: String = "",
) {
    fun toDomain() = Payment(id, apiEnum(status, PaymentStatus.Unknown), amountCentavos, checkoutUrl)
}

internal class ApiOperatorLicenseRepository @Inject constructor(
    private val api: FoxApiClient,
) : OperatorLicenseRepository {
    override suspend fun submitApplication(form: OperatorApplicationForm): FoxResult<OperatorApplication> =
        api.call { http ->
            http.upload<OperatorApplicationDto>("operator/applications") {
                field("province_id", form.provinceId)
                field("business_name", form.businessName.trim())
                field("registration_number", form.registrationNumber?.trim())
                field("contact_phone", form.contactPhone)
                field("contact_email", form.contactEmail?.trim())
                photo("valid_id", form.validId)
                form.businessPermit?.let { photo("business_permit", it) }
            }
        }.map(OperatorApplicationDto::toDomain)

    override suspend fun myApplication(): FoxResult<OperatorApplication?> =
        api.call { http -> http.get("operator/applications/mine").body<DataEnvelope<OperatorApplicationDto?>>().data }
            .map { it?.toDomain() }

    override suspend fun myLicense(): FoxResult<License?> =
        api.call { http -> http.get("operator/license").body<DataEnvelope<LicenseDto>>().data.toDomain() }
            .notFoundAsNull()

    override suspend fun startPayment(licenseId: String): FoxResult<Payment> =
        api.call { http -> http.post("licenses/$licenseId/payments").body<DataEnvelope<PaymentDto>>().data }
            .map(PaymentDto::toDomain)

    override suspend fun payment(paymentId: String): FoxResult<Payment> =
        api.call { http -> http.get("payments/$paymentId").body<DataEnvelope<PaymentDto>>().data }
            .map(PaymentDto::toDomain)
}
