package ph.foxdrop.core.domain.onboarding

import java.time.LocalDate
import ph.foxdrop.core.common.validation.EmailAddress
import ph.foxdrop.core.common.validation.PhMobileNumber
import ph.foxdrop.core.domain.partner.VehicleType
import ph.foxdrop.core.domain.territory.RateCard

/** Why a form field is not accepted yet. The server checks the same rules again (§17). */
enum class Problem { Required, TooShort, TooLong, Invalid, NotInFuture, OutOfRange }

/** Operator application (§9.4 step 1). */
object OperatorApplicationRules {
    enum class Field { Province, BusinessName, ContactPhone, ContactEmail, ValidId }

    fun validate(
        provinceId: String?,
        businessName: String,
        contactPhone: String,
        contactEmail: String,
        hasValidId: Boolean,
    ): Map<Field, Problem> =
        buildMap {
            if (provinceId.isNullOrBlank()) put(Field.Province, Problem.Required)
            text(businessName, min = 2, max = 160)?.let { put(Field.BusinessName, it) }
            phone(contactPhone, required = true)?.let { put(Field.ContactPhone, it) }
            if (contactEmail.isNotBlank() && !EmailAddress.isValid(contactEmail)) {
                put(Field.ContactEmail, Problem.Invalid)
            }
            if (!hasValidId) put(Field.ValidId, Problem.Required)
        }
}

/** Store application (§9.3 onboarding). */
object StoreApplicationRules {
    enum class Field { Province, Name, Address, City, Phone, Location, BusinessPermit, ValidId }

    @Suppress("LongParameterList") // one argument per form field keeps the call sites readable
    fun validate(
        provinceId: String?,
        name: String,
        addressLine: String,
        city: String,
        phone: String,
        hasLocation: Boolean,
        hasBusinessPermit: Boolean,
        hasValidId: Boolean,
    ): Map<Field, Problem> =
        buildMap {
            if (provinceId.isNullOrBlank()) put(Field.Province, Problem.Required)
            text(name, min = 2, max = 120)?.let { put(Field.Name, it) }
            text(addressLine, min = 3, max = 300)?.let { put(Field.Address, it) }
            text(city, min = 2, max = 120)?.let { put(Field.City, it) }
            phone(phone, required = false)?.let { put(Field.Phone, it) }
            if (!hasLocation) put(Field.Location, Problem.Required)
            if (!hasBusinessPermit) put(Field.BusinessPermit, Problem.Required)
            if (!hasValidId) put(Field.ValidId, Problem.Required)
        }
}

/** Rider registration (§5.3, `04_Rider_Workflow.png` step 1). */
object RiderApplicationRules {
    enum class Field {
        Province,
        FullName,
        PlateNo,
        LicenseNo,
        LicenseExpiry,
        ValidId,
        DriversLicense,
        Selfie,
        OrCr,
        VehiclePhoto,
    }

    private val plate = Regex("^[A-Z0-9 -]{2,12}$")

    /** Bicycles have no plate, OR/CR or vehicle photo. */
    fun needsVehiclePapers(vehicle: VehicleType): Boolean = vehicle != VehicleType.Bicycle

    @Suppress("LongParameterList", "CyclomaticComplexMethod") // one argument and one rule per form field
    fun validate(
        provinceId: String?,
        fullName: String,
        vehicle: VehicleType,
        plateNo: String,
        licenseNo: String,
        licenseExpiry: LocalDate?,
        today: LocalDate,
        photos: Set<Field>,
    ): Map<Field, Problem> =
        buildMap {
            if (provinceId.isNullOrBlank()) put(Field.Province, Problem.Required)
            text(fullName, min = 2, max = 120)?.let { put(Field.FullName, it) }
            if (needsVehiclePapers(vehicle)) {
                when {
                    plateNo.isBlank() -> put(Field.PlateNo, Problem.Required)
                    !plate.matches(plateNo.trim().uppercase()) -> put(Field.PlateNo, Problem.Invalid)
                }
            }
            text(licenseNo, min = 5, max = 30)?.let { put(Field.LicenseNo, it) }
            when {
                licenseExpiry == null -> put(Field.LicenseExpiry, Problem.Required)
                !licenseExpiry.isAfter(today) -> put(Field.LicenseExpiry, Problem.NotInFuture)
            }
            requiredPhotos(vehicle).filterNot(photos::contains).forEach { put(it, Problem.Required) }
        }

    fun requiredPhotos(vehicle: VehicleType): List<Field> =
        listOf(Field.ValidId, Field.DriversLicense, Field.Selfie) +
            if (needsVehiclePapers(vehicle)) listOf(Field.OrCr, Field.VehiclePhoto) else emptyList()
}

/** Rate card editor (§6). Amounts already parsed to centavos; null means the text was not a valid amount. */
object RateCardRules {
    enum class Field { BaseFare, BaseKm, RatePerKm, MinFee, MaxKm, RiderShare }

    private const val MAX_KM = 100.0
    private const val MAX_BPS = 10_000

    fun validate(
        baseFare: Long?,
        baseKm: Double?,
        ratePerKm: Long?,
        minFee: Long?,
        maxKm: Double?,
        riderShareBps: Int?,
    ): Map<Field, Problem> =
        buildMap {
            if (baseFare == null) put(Field.BaseFare, Problem.Invalid)
            if (ratePerKm == null) put(Field.RatePerKm, Problem.Invalid)
            if (minFee == null) put(Field.MinFee, Problem.Invalid)
            if (baseKm == null || baseKm !in 0.0..MAX_KM) put(Field.BaseKm, Problem.OutOfRange)
            if (maxKm == null || maxKm <= 0.0 || maxKm > MAX_KM) put(Field.MaxKm, Problem.OutOfRange)
            if (riderShareBps == null || riderShareBps !in 0..MAX_BPS) put(Field.RiderShare, Problem.OutOfRange)
        }

    /** The card to send when [validate] found nothing (the effective date is set by the server). */
    @Suppress("LongParameterList")
    fun toCard(
        baseFare: Long,
        baseKm: Double,
        ratePerKm: Long,
        minFee: Long,
        maxKm: Double,
        riderShareBps: Int,
    ): RateCard =
        RateCard(
            baseFareCentavos = baseFare,
            baseKm = baseKm,
            ratePerKmCentavos = ratePerKm,
            minFeeCentavos = minFee,
            maxKm = maxKm,
            riderShareBps = riderShareBps,
        )
}

/** A reviewer's rejection reason (shown to the applicant). */
object RejectionReason {
    const val MIN = 5
    const val MAX = 300

    fun validate(reason: String): Problem? = text(reason, MIN, MAX)
}

private fun text(value: String, min: Int, max: Int): Problem? {
    val trimmed = value.trim()
    return when {
        trimmed.isEmpty() -> Problem.Required
        trimmed.length < min -> Problem.TooShort
        trimmed.length > max -> Problem.TooLong
        else -> null
    }
}

private fun phone(value: String, required: Boolean): Problem? =
    when {
        value.isBlank() -> if (required) Problem.Required else null
        PhMobileNumber.normalize(value) == null -> Problem.Invalid
        else -> null
    }
