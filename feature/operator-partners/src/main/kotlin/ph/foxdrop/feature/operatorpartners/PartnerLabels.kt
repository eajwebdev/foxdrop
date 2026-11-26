package ph.foxdrop.feature.operatorpartners

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

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
