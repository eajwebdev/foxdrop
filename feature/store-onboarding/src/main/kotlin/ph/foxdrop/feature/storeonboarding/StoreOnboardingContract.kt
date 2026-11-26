package ph.foxdrop.feature.storeonboarding

import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.domain.media.PhotoUpload
import ph.foxdrop.core.domain.onboarding.Problem
import ph.foxdrop.core.domain.onboarding.StoreApplicationRules.Field
import ph.foxdrop.core.domain.partner.MyStore
import ph.foxdrop.core.domain.partner.StoreCategory
import ph.foxdrop.core.domain.province.ProvinceOption
import ph.foxdrop.core.location.LocationResult
import ph.foxdrop.core.model.GeoPoint

/** "Open a store" (§5.2, store workflow step 1): only provinces with a live operator can be picked. */
@Immutable
data class StoreForm(
    val provinces: ImmutableList<ProvinceOption> = persistentListOf(),
    val provinceId: String? = null,
    val name: String = "",
    val category: StoreCategory = StoreCategory.Food,
    val description: String = "",
    val addressLine: String = "",
    val barangay: String = "",
    val city: String = "",
    val phone: String = "",
    val location: GeoPoint? = null,
    val locating: Boolean = false,
    /** Why the last location attempt failed; null after a success. */
    val locationProblem: LocationResult? = null,
    val businessPermit: PhotoUpload? = null,
    val validId: PhotoUpload? = null,
    val errors: Map<Field, Problem> = emptyMap(),
)

@Immutable
sealed interface StoreOnboardingState {
    data object Loading : StoreOnboardingState

    data class Failed(val error: DomainError) : StoreOnboardingState

    /** A new store, or fixing a rejected one ([previous] shows the reason). */
    data class Apply(
        val form: StoreForm,
        val submitting: Boolean = false,
        val message: DomainError? = null,
        val previous: MyStore? = null,
    ) : StoreOnboardingState

    data class Status(val store: MyStore, val refreshing: Boolean = false) : StoreOnboardingState
}

enum class StoreDocument { BusinessPermit, ValidId }

sealed interface StoreOnboardingAction {
    data object Retry : StoreOnboardingAction

    data object Refresh : StoreOnboardingAction

    data class ProvinceSelected(val provinceId: String) : StoreOnboardingAction

    data class NameChanged(val value: String) : StoreOnboardingAction

    data class CategorySelected(val category: StoreCategory) : StoreOnboardingAction

    data class DescriptionChanged(val value: String) : StoreOnboardingAction

    data class AddressChanged(val value: String) : StoreOnboardingAction

    data class BarangayChanged(val value: String) : StoreOnboardingAction

    data class CityChanged(val value: String) : StoreOnboardingAction

    data class PhoneChanged(val value: String) : StoreOnboardingAction

    data object UseMyLocation : StoreOnboardingAction

    data class LocationPermissionResult(val granted: Boolean) : StoreOnboardingAction

    data class PhotoPicked(val document: StoreDocument, val uri: Uri?) : StoreOnboardingAction

    data object Submit : StoreOnboardingAction

    data object DismissMessage : StoreOnboardingAction

    data object FixAndResubmit : StoreOnboardingAction
}

sealed interface StoreOnboardingEffect {
    data object RequestLocationPermission : StoreOnboardingEffect

    data object PhotoUnreadable : StoreOnboardingEffect
}
