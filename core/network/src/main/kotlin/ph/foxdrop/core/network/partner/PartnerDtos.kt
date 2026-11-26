package ph.foxdrop.core.network.partner

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ph.foxdrop.core.domain.partner.MyRider
import ph.foxdrop.core.domain.partner.MyStore
import ph.foxdrop.core.domain.partner.PartnerDocument
import ph.foxdrop.core.domain.partner.RiderReview
import ph.foxdrop.core.domain.partner.RiderStatus
import ph.foxdrop.core.domain.partner.RiderSummary
import ph.foxdrop.core.domain.partner.StoreCategory
import ph.foxdrop.core.domain.partner.StoreReview
import ph.foxdrop.core.domain.partner.StoreStatus
import ph.foxdrop.core.domain.partner.StoreSummary
import ph.foxdrop.core.domain.partner.VehicleType
import ph.foxdrop.core.network.LatLngDto
import ph.foxdrop.core.network.NamedRefDto
import ph.foxdrop.core.network.apiEnum

@Serializable
internal data class MyStoreDto(
    val id: String,
    val name: String,
    val category: String,
    val status: String,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    val province: NamedRefDto,
) {
    fun toDomain() =
        MyStore(
            id = id,
            name = name,
            category = apiEnum(category, StoreCategory.More),
            status = apiEnum(status, StoreStatus.Unknown),
            rejectionReason = rejectionReason,
            provinceName = province.name,
        )
}

@Serializable
internal data class OwnerDto(@SerialName("full_name") val fullName: String? = null, val phone: String? = null)

@Serializable
internal data class DocumentDto(val type: String, val url: String) {
    fun toDomain() = PartnerDocument(type, url)
}

@Serializable
internal data class StoreReviewDto(
    val id: String,
    val name: String,
    val category: String,
    val status: String,
    @SerialName("address_line") val addressLine: String = "",
    val city: String = "",
    val owner: OwnerDto? = null,
    @SerialName("created_at") val createdAt: String = "",
    val description: String? = null,
    val barangay: String? = null,
    val phone: String? = null,
    val location: LatLngDto? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    val documents: List<DocumentDto> = emptyList(),
) {
    fun toSummary() =
        StoreSummary(
            id = id,
            name = name,
            category = apiEnum(category, StoreCategory.More),
            status = apiEnum(status, StoreStatus.Unknown),
            addressLine = addressLine,
            city = city,
            ownerName = owner?.fullName,
            ownerPhone = owner?.phone,
            createdAt = createdAt,
        )

    fun toReview() =
        StoreReview(
            summary = toSummary(),
            description = description,
            barangay = barangay,
            phone = phone,
            location = location?.toPoint(),
            rejectionReason = rejectionReason,
            documents = documents.map(DocumentDto::toDomain),
        )
}

@Serializable
internal data class MyRiderDto(
    val id: String,
    val status: String,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    val province: NamedRefDto,
    @SerialName("vehicle_type") val vehicleType: String,
    @SerialName("plate_no") val plateNo: String? = null,
    @SerialName("license_no_last4") val licenseNoLast4: String? = null,
    @SerialName("license_expiry") val licenseExpiry: String? = null,
) {
    fun toDomain() =
        MyRider(
            id = id,
            status = apiEnum(status, RiderStatus.Unknown),
            rejectionReason = rejectionReason,
            provinceName = province.name,
            vehicleType = apiEnum(vehicleType, VehicleType.Motorcycle),
            plateNo = plateNo,
            licenseNoLast4 = licenseNoLast4,
            licenseExpiry = licenseExpiry,
        )
}

@Serializable
internal data class RiderReviewDto(
    val id: String,
    val status: String,
    @SerialName("full_name") val fullName: String? = null,
    val phone: String? = null,
    @SerialName("vehicle_type") val vehicleType: String,
    @SerialName("plate_no") val plateNo: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("license_no_last4") val licenseNoLast4: String? = null,
    @SerialName("license_expiry") val licenseExpiry: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    val documents: List<DocumentDto> = emptyList(),
) {
    fun toSummary() =
        RiderSummary(
            id = id,
            status = apiEnum(status, RiderStatus.Unknown),
            fullName = fullName,
            phone = phone,
            vehicleType = apiEnum(vehicleType, VehicleType.Motorcycle),
            plateNo = plateNo,
            createdAt = createdAt,
        )

    fun toReview() =
        RiderReview(
            summary = toSummary(),
            licenseNoLast4 = licenseNoLast4,
            licenseExpiry = licenseExpiry,
            rejectionReason = rejectionReason,
            documents = documents.map(DocumentDto::toDomain),
        )
}

@Serializable
internal data class RejectRequest(val reason: String)
