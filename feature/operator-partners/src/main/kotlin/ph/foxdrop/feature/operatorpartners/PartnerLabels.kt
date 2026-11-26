package ph.foxdrop.feature.operatorpartners

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ph.foxdrop.core.designsystem.component.StatusTone
import ph.foxdrop.core.domain.partner.RiderStatus
import ph.foxdrop.core.domain.partner.StoreCategory
import ph.foxdrop.core.domain.partner.StoreStatus
import ph.foxdrop.core.domain.partner.VehicleType

@Composable
internal fun StoreCategory.label(): String =
    stringResource(
        when (this) {
            StoreCategory.Food -> R.string.category_food
            StoreCategory.Groceries -> R.string.category_groceries
            StoreCategory.Medicine -> R.string.category_medicine
            StoreCategory.More -> R.string.category_more
        },
    )

@Composable
internal fun VehicleType.label(): String =
    stringResource(
        when (this) {
            VehicleType.Motorcycle -> R.string.vehicle_motorcycle
            VehicleType.Bicycle -> R.string.vehicle_bicycle
            VehicleType.Tricycle -> R.string.vehicle_tricycle
            VehicleType.Car -> R.string.vehicle_car
        },
    )

@Composable
internal fun StoreStatus.label(): String =
    when (this) {
        StoreStatus.Submitted -> stringResource(R.string.status_pending)
        StoreStatus.Active -> stringResource(R.string.status_active)
        StoreStatus.Approved -> stringResource(R.string.status_approved)
        StoreStatus.Rejected -> stringResource(R.string.status_rejected)
        else -> stringResource(R.string.status_other, name)
    }

internal fun StoreStatus.tone(): StatusTone =
    when (this) {
        StoreStatus.Submitted -> StatusTone.Info
        StoreStatus.Active, StoreStatus.Approved -> StatusTone.Success
        StoreStatus.Rejected, StoreStatus.Suspended, StoreStatus.Closed -> StatusTone.Error
        else -> StatusTone.Neutral
    }

@Composable
internal fun RiderStatus.label(): String =
    when (this) {
        RiderStatus.Applied, RiderStatus.DocsReview -> stringResource(R.string.status_pending)
        RiderStatus.Approved -> stringResource(R.string.status_approved)
        RiderStatus.Rejected -> stringResource(R.string.status_rejected)
        else -> stringResource(R.string.status_other, name)
    }

internal fun RiderStatus.tone(): StatusTone =
    when (this) {
        RiderStatus.Applied, RiderStatus.DocsReview -> StatusTone.Info
        RiderStatus.Approved -> StatusTone.Success
        RiderStatus.Rejected, RiderStatus.Suspended, RiderStatus.Deactivated -> StatusTone.Error
        RiderStatus.Unknown -> StatusTone.Neutral
    }

/** `BUSINESS_PERMIT` → "Business permit"; unknown types fall back to the raw name. */
@Composable
internal fun documentLabel(type: String): String =
    when (type.uppercase()) {
        "VALID_ID" -> stringResource(R.string.document_valid_id)
        "BUSINESS_PERMIT" -> stringResource(R.string.document_business_permit)
        "DRIVERS_LICENSE" -> stringResource(R.string.document_drivers_license)
        "SELFIE" -> stringResource(R.string.document_selfie)
        "OR_CR" -> stringResource(R.string.document_or_cr)
        "VEHICLE_PHOTO" -> stringResource(R.string.document_vehicle_photo)
        else -> type
    }

/** "2026-11-26T02:00:00Z" → "2026-11-26". */
internal fun String.day(): String = take(DAY_LENGTH)

private const val DAY_LENGTH = 10
