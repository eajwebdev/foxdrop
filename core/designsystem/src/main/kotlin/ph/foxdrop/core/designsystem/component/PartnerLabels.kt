package ph.foxdrop.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ph.foxdrop.core.designsystem.R
import ph.foxdrop.core.domain.partner.RiderStatus
import ph.foxdrop.core.domain.partner.StoreCategory
import ph.foxdrop.core.domain.partner.StoreStatus
import ph.foxdrop.core.domain.partner.VehicleType

/*
 * One wording and one colour per store/rider value, shared by the applicant's status screens and the operator's
 * review lists so both sides always agree.
 */

@Composable
fun StoreCategory.label(): String =
    stringResource(
        when (this) {
            StoreCategory.Food -> R.string.fox_category_food
            StoreCategory.Groceries -> R.string.fox_category_groceries
            StoreCategory.Medicine -> R.string.fox_category_medicine
            StoreCategory.More -> R.string.fox_category_more
        },
    )

@Composable
fun VehicleType.label(): String =
    stringResource(
        when (this) {
            VehicleType.Motorcycle -> R.string.fox_vehicle_motorcycle
            VehicleType.Bicycle -> R.string.fox_vehicle_bicycle
            VehicleType.Tricycle -> R.string.fox_vehicle_tricycle
            VehicleType.Car -> R.string.fox_vehicle_car
        },
    )

@Composable
fun StoreStatus.label(): String =
    when (this) {
        StoreStatus.Draft, StoreStatus.Submitted -> stringResource(R.string.fox_status_pending)
        StoreStatus.Approved -> stringResource(R.string.fox_status_approved)
        StoreStatus.Active -> stringResource(R.string.fox_status_active)
        StoreStatus.Paused -> stringResource(R.string.fox_status_paused)
        StoreStatus.Rejected -> stringResource(R.string.fox_status_rejected)
        StoreStatus.Suspended, StoreStatus.Closed -> stringResource(R.string.fox_status_inactive)
        else -> name
    }

fun StoreStatus.tone(): StatusTone =
    when (this) {
        StoreStatus.Draft, StoreStatus.Submitted -> StatusTone.Info
        StoreStatus.Approved, StoreStatus.Active -> StatusTone.Success
        StoreStatus.Paused -> StatusTone.Warning
        StoreStatus.Rejected, StoreStatus.Suspended, StoreStatus.Closed -> StatusTone.Error
        else -> StatusTone.Neutral
    }

@Composable
fun RiderStatus.label(): String =
    when (this) {
        RiderStatus.Applied, RiderStatus.DocsReview -> stringResource(R.string.fox_status_pending)
        RiderStatus.Approved -> stringResource(R.string.fox_status_approved)
        RiderStatus.Rejected -> stringResource(R.string.fox_status_rejected)
        RiderStatus.Suspended, RiderStatus.Deactivated -> stringResource(R.string.fox_status_inactive)
        RiderStatus.Unknown -> name
    }

fun RiderStatus.tone(): StatusTone =
    when (this) {
        RiderStatus.Applied, RiderStatus.DocsReview -> StatusTone.Info
        RiderStatus.Approved -> StatusTone.Success
        RiderStatus.Rejected, RiderStatus.Suspended, RiderStatus.Deactivated -> StatusTone.Error
        RiderStatus.Unknown -> StatusTone.Neutral
    }
