package ph.foxdrop.feature.operatorlicense

import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.domain.media.PhotoUpload
import ph.foxdrop.core.domain.onboarding.OperatorApplicationRules.Field
import ph.foxdrop.core.domain.onboarding.Problem
import ph.foxdrop.core.domain.operator.License
import ph.foxdrop.core.domain.operator.OperatorApplication
import ph.foxdrop.core.domain.province.ProvinceOption

/** The application form (§9.4 step 1): only provinces without a live license can be picked. */
@Immutable
data class ApplyForm(
    val provinces: ImmutableList<ProvinceOption> = persistentListOf(),
    val provinceId: String? = null,
    val businessName: String = "",
    val registrationNumber: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val validId: PhotoUpload? = null,
    val businessPermit: PhotoUpload? = null,
    val errors: Map<Field, Problem> = emptyMap(),
) {
    val selectedProvince: ProvinceOption? get() = provinces.firstOrNull { it.id == provinceId }
}

/** Paying the license fee (first term or renewal): the checkout runs in a Custom Tab while the app polls. */
@Immutable
sealed interface PayState {
    data object Idle : PayState

    data object Starting : PayState

    data object Waiting : PayState

    data object NotPaid : PayState

    data object StillPending : PayState

    data class Failed(val error: DomainError) : PayState
}

@Immutable
sealed interface OperatorLicenseState {
    data object Loading : OperatorLicenseState

    data class Failed(val error: DomainError) : OperatorLicenseState

    /** No application yet, or applying again after a rejection ([previous] shows its reason). */
    data class Apply(
        val form: ApplyForm,
        val submitting: Boolean = false,
        val message: DomainError? = null,
        val previous: OperatorApplication? = null,
    ) : OperatorLicenseState

    data class Status(
        val application: OperatorApplication,
        val license: License?,
        val pay: PayState = PayState.Idle,
        val refreshing: Boolean = false,
    ) : OperatorLicenseState
}

enum class DocumentSlot { ValidId, BusinessPermit }

sealed interface OperatorLicenseAction {
    data object Retry : OperatorLicenseAction

    data object Refresh : OperatorLicenseAction

    data class ProvinceSelected(val provinceId: String) : OperatorLicenseAction

    data class BusinessNameChanged(val value: String) : OperatorLicenseAction

    data class RegistrationNumberChanged(val value: String) : OperatorLicenseAction

    data class ContactPhoneChanged(val value: String) : OperatorLicenseAction

    data class ContactEmailChanged(val value: String) : OperatorLicenseAction

    data class PhotoPicked(val slot: DocumentSlot, val uri: Uri?) : OperatorLicenseAction

    data object Submit : OperatorLicenseAction

    data object DismissMessage : OperatorLicenseAction

    data object ApplyAgain : OperatorLicenseAction

    data object Pay : OperatorLicenseAction
}

sealed interface OperatorLicenseEffect {
    data class OpenCheckout(val url: String) : OperatorLicenseEffect

    /** The operator role was just granted: re-route to the operator home. */
    data object RolesChanged : OperatorLicenseEffect

    data object PhotoUnreadable : OperatorLicenseEffect
}
