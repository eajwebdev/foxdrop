package ph.foxdrop.feature.operatorzonesrates

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.persistentListOf
import ph.foxdrop.core.designsystem.component.Loadable
import ph.foxdrop.core.designsystem.preview.FoxPreviews
import ph.foxdrop.core.designsystem.theme.FoxDropTheme
import ph.foxdrop.core.domain.territory.FeePreview
import ph.foxdrop.core.domain.territory.OperatorProvince
import ph.foxdrop.core.domain.territory.RateCard
import ph.foxdrop.core.domain.territory.Zone
import ph.foxdrop.core.model.GeoPoint

internal object TerritoryPreviewData {
    val province = OperatorProvince("neg", "Negros Occidental", GeoPoint(10.6765, 122.9509), 1_500, 7)
    val zones =
        persistentListOf(
            Zone("z1", "Metro Bacolod", true, GeoPoint(10.6765, 122.9509), 8.0, 201.0),
            Zone("z2", "Silay City", true, GeoPoint(10.7966, 122.9749), 4.5, 63.6),
        )
    val preview =
        persistentListOf(
            FeePreview(1.0, 4_000, 3_200, 800),
            FeePreview(3.0, 5_000, 4_000, 1_000),
            FeePreview(5.0, 7_000, 5_600, 1_400),
            FeePreview(10.0, 12_000, 9_600, 2_400),
        )

    fun rates(
        preview: Loadable<kotlinx.collections.immutable.ImmutableList<FeePreview>> = Loadable.Ready(
            this.preview,
        ),
    ) =
        RateCardState(
            content = Loadable.Ready(Unit),
            fields = RateFields.from(RateCard.Default),
            preview = preview,
            effectiveFrom = "2026-11-26T00:00:00Z",
            storeFee = "15",
        )
}

@FoxPreviews
@Composable
private fun ZonesPreview() {
    FoxDropTheme {
        ZonesScreen(
            ZonesState(Loadable.Ready(ZonesData(TerritoryPreviewData.zones, TerritoryPreviewData.province))),
            onAction = {},
            onBack = {},
        )
    }
}

@FoxPreviews
@Composable
private fun ZoneEditorPreview() {
    FoxDropTheme {
        ZonesScreen(
            ZonesState(
                Loadable.Ready(ZonesData(persistentListOf(), TerritoryPreviewData.province)),
                editor = ZoneEditor(
                    name = "Metro Bacolod",
                    center = TerritoryPreviewData.province.center,
                    radiusKm = 8f,
                ),
            ),
            onAction = {},
            onBack = {},
        )
    }
}

@FoxPreviews
@Composable
private fun RatesPreview() {
    FoxDropTheme {
        RateCardScreen(TerritoryPreviewData.rates(), {}, {}, { _, _ -> }, {}, {}, {}, {})
    }
}
