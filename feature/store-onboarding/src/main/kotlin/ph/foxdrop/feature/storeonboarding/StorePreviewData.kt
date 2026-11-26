package ph.foxdrop.feature.storeonboarding

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.persistentListOf
import ph.foxdrop.core.designsystem.preview.FoxPreviews
import ph.foxdrop.core.designsystem.theme.FoxDropTheme
import ph.foxdrop.core.domain.media.PhotoUpload
import ph.foxdrop.core.domain.onboarding.Problem
import ph.foxdrop.core.domain.onboarding.StoreApplicationRules.Field
import ph.foxdrop.core.domain.partner.MyStore
import ph.foxdrop.core.domain.partner.StoreCategory
import ph.foxdrop.core.domain.partner.StoreStatus
import ph.foxdrop.core.domain.province.ProvinceOption
import ph.foxdrop.core.model.GeoPoint

internal object StorePreviewData {
    val provinces =
        persistentListOf(
            ProvinceOption("neg", "Negros Occidental", "Region VI", isServiceable = true, isAvailable = false),
        )

    val filled =
        StoreForm(
            provinces = provinces,
            provinceId = "neg",
            name = "Mama's Kitchen",
            category = StoreCategory.Food,
            addressLine = "12 Lacson St.",
            barangay = "Barangay 17",
            city = "Bacolod City",
            phone = "9170000007",
            location = GeoPoint(10.674, 122.953),
            businessPermit = PhotoUpload(byteArrayOf(1), "business_permit.jpg"),
        )

    val invalid =
        StoreForm(
            provinces = provinces,
            errors = mapOf(
                Field.Province to Problem.Required,
                Field.Name to Problem.Required,
                Field.Location to Problem.Required,
            ),
        )

    fun store(status: StoreStatus, reason: String? = null) =
        MyStore("s1", "Mama's Kitchen", StoreCategory.Food, status, reason, "Negros Occidental")
}

@FoxPreviews
@Composable
private fun StoreFormPreview() {
    FoxDropTheme {
        StoreOnboardingScreen(StoreOnboardingState.Apply(StorePreviewData.filled), {}, {}, {}, {}, {})
    }
}

@FoxPreviews
@Composable
private fun StoreStatusPreview() {
    FoxDropTheme {
        StoreOnboardingScreen(StoreOnboardingState.Status(StorePreviewData.store(StoreStatus.Submitted)), {
        }, {}, {}, {}, {})
    }
}
