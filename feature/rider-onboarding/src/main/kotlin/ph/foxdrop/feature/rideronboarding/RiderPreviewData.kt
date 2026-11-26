package ph.foxdrop.feature.rideronboarding

import androidx.compose.runtime.Composable
import java.time.LocalDate
import kotlinx.collections.immutable.persistentListOf
import ph.foxdrop.core.designsystem.preview.FoxPreviews
import ph.foxdrop.core.designsystem.theme.FoxDropTheme
import ph.foxdrop.core.domain.media.PhotoUpload
import ph.foxdrop.core.domain.onboarding.Problem
import ph.foxdrop.core.domain.onboarding.RiderApplicationRules.Field
import ph.foxdrop.core.domain.partner.MyRider
import ph.foxdrop.core.domain.partner.RiderStatus
import ph.foxdrop.core.domain.partner.VehicleType
import ph.foxdrop.core.domain.province.ProvinceOption

internal object RiderPreviewData {
    private val photo = PhotoUpload(byteArrayOf(1), "photo.jpg")
    val provinces =
        persistentListOf(
            ProvinceOption("neg", "Negros Occidental", "Region VI", isServiceable = true, isAvailable = false),
        )

    /** Personal details and ID done, license half done, vehicle papers missing. */
    val partial =
        RiderForm(
            provinces = provinces,
            provinceId = "neg",
            fullName = "Paolo Reyes",
            licenseNo = "N01-23-456789",
            licenseExpiry = LocalDate.of(2028, 1, 31),
            plateNo = "NEG 1234",
            photos = mapOf(Field.Selfie to photo, Field.ValidId to photo),
            errors = mapOf(Field.DriversLicense to Problem.Required, Field.OrCr to Problem.Required),
        )

    val bicycle = RiderForm(provinces = provinces, provinceId = "neg", vehicle = VehicleType.Bicycle)

    fun rider(status: RiderStatus, reason: String? = null) =
        MyRider("r1", status, reason, "Negros Occidental", VehicleType.Motorcycle, "NEG 1234", "6789", "2028-01-31")
}

@FoxPreviews
@Composable
private fun RiderChecklistPreview() {
    FoxDropTheme {
        RiderOnboardingScreen(RiderOnboardingState.Apply(RiderPreviewData.partial), {}, {}, {}, {})
    }
}

@FoxPreviews
@Composable
private fun RiderStatusPreview() {
    FoxDropTheme {
        RiderOnboardingScreen(RiderOnboardingState.Status(RiderPreviewData.rider(RiderStatus.DocsReview)), {
        }, {}, {}, {}, onSignOut = {})
    }
}
