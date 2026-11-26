package ph.foxdrop.feature.rideronboarding

import android.net.Uri
import androidx.compose.runtime.Immutable
import java.time.LocalDate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.domain.media.PhotoUpload
import ph.foxdrop.core.domain.onboarding.Problem
import ph.foxdrop.core.domain.onboarding.RiderApplicationRules
import ph.foxdrop.core.domain.onboarding.RiderApplicationRules.Field
import ph.foxdrop.core.domain.partner.MyRider
import ph.foxdrop.core.domain.partner.VehicleType
import ph.foxdrop.core.domain.province.ProvinceOption

/** The four parts of the registration checklist (rider workflow step 1). */
enum class ChecklistStep { PersonalDetails, ValidId, DriversLicense, VehicleDetails }

/** Rider registration (§5.3): only provinces with a live operator can be picked. */
@Immutable
data class RiderForm(
    val provinces: ImmutableList<ProvinceOption> = persistentListOf(),
    val provinceId: String? = null,
    val fullName: String = "",
    val vehicle: VehicleType = VehicleType.Motorcycle,
    val plateNo: String = "",
    val licenseNo: String = "",
    val licenseExpiry: LocalDate? = null,
    val photos: Map<Field, PhotoUpload> = emptyMap(),
    val errors: Map<Field, Problem> = emptyMap(),
) {
    val needsVehiclePapers: Boolean get() = RiderApplicationRules.needsVehiclePapers(vehicle)

    /** A step is ticked when everything it asks for is filled in (the server still checks it all). */
    fun isComplete(step: ChecklistStep): Boolean =
        when (step) {
            ChecklistStep.PersonalDetails -> provinceId != null && fullName.isNotBlank() && Field.Selfie in photos

            ChecklistStep.ValidId -> Field.ValidId in photos

            ChecklistStep.DriversLicense -> licenseNo.isNotBlank() && licenseExpiry != null &&
                Field.DriversLicense in photos

            ChecklistStep.VehicleDetails ->
                !needsVehiclePapers || (plateNo.isNotBlank() && Field.OrCr in photos && Field.VehiclePhoto in photos)
        }
}

@Immutable
sealed interface RiderOnboardingState {
    data object Loading : RiderOnboardingState

    data class Failed(val error: DomainError) : RiderOnboardingState

    /** A new registration, or fixing a rejected one ([previous] shows the reason). */
    data class Apply(
        val form: RiderForm,
        val submitting: Boolean = false,
        val message: DomainError? = null,
        val previous: MyRider? = null,
    ) : RiderOnboardingState

    data class Status(val rider: MyRider, val refreshing: Boolean = false) : RiderOnboardingState
}

sealed interface RiderOnboardingAction {
    data object Retry : RiderOnboardingAction

    data object Refresh : RiderOnboardingAction

    data class ProvinceSelected(val provinceId: String) : RiderOnboardingAction

    data class FullNameChanged(val value: String) : RiderOnboardingAction

    data class VehicleSelected(val vehicle: VehicleType) : RiderOnboardingAction

    data class PlateChanged(val value: String) : RiderOnboardingAction

    data class LicenseNoChanged(val value: String) : RiderOnboardingAction

    data class LicenseExpiryChanged(val date: LocalDate) : RiderOnboardingAction

    /** [document] is one of the photo fields of [RiderApplicationRules.Field]. */
    data class PhotoPicked(val document: Field, val uri: Uri?) : RiderOnboardingAction

    data object Submit : RiderOnboardingAction

    data object DismissMessage : RiderOnboardingAction

    data object FixAndResubmit : RiderOnboardingAction
}

sealed interface RiderOnboardingEffect {
    data object PhotoUnreadable : RiderOnboardingEffect
}
