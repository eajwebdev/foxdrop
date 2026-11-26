package ph.foxdrop.feature.operatorlicense

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.persistentListOf
import ph.foxdrop.core.designsystem.component.Loadable
import ph.foxdrop.core.designsystem.preview.FoxPreviews
import ph.foxdrop.core.designsystem.theme.FoxDropTheme
import ph.foxdrop.core.domain.operator.ApplicationStatus
import ph.foxdrop.core.domain.operator.License
import ph.foxdrop.core.domain.operator.LicenseStatus
import ph.foxdrop.core.domain.operator.OperatorApplication
import ph.foxdrop.core.domain.province.ProvinceOption

internal object LicensePreviewData {
    val provinces =
        persistentListOf(
            ProvinceOption("akl", "Aklan", "Region VI", isServiceable = false, isAvailable = true),
            ProvinceOption("ilo", "Iloilo", "Region VI", isServiceable = false, isAvailable = true),
        )

    fun license(status: LicenseStatus, daysLeft: Int? = 335) =
        License(
            id = "lic-1",
            provinceId = "ilo",
            provinceName = "Iloilo",
            status = status,
            startAt = "2026-11-26T00:00:00Z",
            endAt = "2027-11-26T00:00:00Z",
            daysLeft = daysLeft,
            feeCentavos = 1_500_000,
            canPay = true,
        )

    fun application(
        status: ApplicationStatus,
        license: License? = license(LicenseStatus.PendingPayment, null),
        reason: String? = null,
    ) =
        OperatorApplication("app-1", status, "Iloilo", "Gonzaga Logistics", reason, license)
}

@FoxPreviews
@Composable
private fun ApplyPreview() {
    FoxDropTheme {
        OperatorLicenseScreen(
            state = OperatorLicenseState.Apply(ApplyForm(provinces = LicensePreviewData.provinces, provinceId = "ilo")),
            onAction = {},
            onBack = {},
            onTakePhoto = {},
            onChoosePhoto = {},
        )
    }
}

@FoxPreviews
@Composable
private fun ApprovedPreview() {
    FoxDropTheme {
        OperatorLicenseScreen(
            state =
                OperatorLicenseState.Status(
                    LicensePreviewData.application(ApplicationStatus.Approved),
                    LicensePreviewData.license(LicenseStatus.PendingPayment, null),
                ),
            onAction = {},
            onBack = {},
            onTakePhoto = {},
            onChoosePhoto = {},
        )
    }
}

@FoxPreviews
@Composable
private fun LicenseCardPreview() {
    FoxDropTheme {
        val grace = LicensePreviewData.license(LicenseStatus.Grace, -3)
        LicenseCard(LicenseCardState(Loadable.Ready(grace)), onRenew = {}, onRetry = {})
    }
}
