package ph.foxdrop.feature.operatorpartners

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.persistentListOf
import ph.foxdrop.core.designsystem.component.Loadable
import ph.foxdrop.core.designsystem.preview.FoxPreviews
import ph.foxdrop.core.designsystem.theme.FoxDropTheme
import ph.foxdrop.core.domain.partner.PartnerDocument
import ph.foxdrop.core.domain.partner.RiderReview
import ph.foxdrop.core.domain.partner.RiderStatus
import ph.foxdrop.core.domain.partner.RiderSummary
import ph.foxdrop.core.domain.partner.StoreCategory
import ph.foxdrop.core.domain.partner.StoreReview
import ph.foxdrop.core.domain.partner.StoreStatus
import ph.foxdrop.core.domain.partner.StoreSummary
import ph.foxdrop.core.domain.partner.VehicleType
import ph.foxdrop.core.model.GeoPoint

internal object PartnerPreviewData {
    val store =
        StoreSummary(
            "s1", "Mama's Kitchen", StoreCategory.Food, StoreStatus.Submitted, "Lacson St.", "Bacolod City",
            "Rosa Lacson", "+639170000007", "2026-11-26T02:00:00Z",
        )
    val stores =
        persistentListOf(
            store,
            store.copy(
                id = "s2",
                name = "Botika ni Ana",
                category = StoreCategory.Medicine,
                ownerName = null,
                city = "Silay City",
            ),
        )
    val storeReview =
        StoreReview(
            store,
            "Home-style burgers, fries and inasal.",
            "Barangay 17",
            "+639170000007",
            GeoPoint(10.674, 122.953),
            null,
            listOf(
                PartnerDocument("BUSINESS_PERMIT", "https://example.test/permit"),
                PartnerDocument("VALID_ID", "https://example.test/id"),
            ),
        )
    val rider =
        RiderSummary(
            "r1",
            RiderStatus.DocsReview,
            "Paolo Reyes",
            "+639170000003",
            VehicleType.Motorcycle,
            "NEG 1234",
            "2026-11-26T02:00:00Z",
        )
    val riders =
        persistentListOf(
            rider,
            rider.copy(id = "r2", fullName = "Lito Cruz", vehicleType = VehicleType.Bicycle, plateNo = null),
        )
    val riderReview =
        RiderReview(
            rider,
            "4567",
            "2028-01-31",
            null,
            listOf(
                PartnerDocument("VALID_ID", "https://example.test/id"),
                PartnerDocument("SELFIE", "https://example.test/selfie"),
            ),
        )
}

@FoxPreviews
@Composable
private fun StoreApprovalsPreview() {
    FoxDropTheme {
        StoreApprovalsScreen(PartnerListState(items = Loadable.Ready(PartnerPreviewData.stores)), {}, {}, {})
    }
}

@FoxPreviews
@Composable
private fun RiderApprovalsEmptyPreview() {
    FoxDropTheme {
        RiderApprovalsScreen(PartnerListState(items = Loadable.Ready(persistentListOf())), {}, {}, {})
    }
}

@FoxPreviews
@Composable
private fun StoreReviewPreview() {
    FoxDropTheme {
        StoreReviewScreen(ReviewState(Loadable.Ready(PartnerPreviewData.storeReview)), ReviewActions())
    }
}

@FoxPreviews
@Composable
private fun RiderReviewRejectPreview() {
    FoxDropTheme {
        RiderReviewScreen(
            ReviewState(Loadable.Ready(PartnerPreviewData.riderReview), rejectDialog = RejectDialog()),
            ReviewActions(),
        )
    }
}
