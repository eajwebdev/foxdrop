package ph.foxdrop.feature.operatorlicense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ph.foxdrop.core.common.format.formatPeso
import ph.foxdrop.core.designsystem.component.EmptyState
import ph.foxdrop.core.designsystem.component.FoxButton
import ph.foxdrop.core.designsystem.component.FoxButtonState
import ph.foxdrop.core.designsystem.component.FoxCard
import ph.foxdrop.core.designsystem.component.FoxNotice
import ph.foxdrop.core.designsystem.component.LoadableContent
import ph.foxdrop.core.designsystem.component.StatusPill
import ph.foxdrop.core.designsystem.component.StatusTone
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.domain.operator.License
import ph.foxdrop.core.domain.operator.LicenseStatus
import ph.foxdrop.core.model.Money
import ph.foxdrop.core.payments.CheckoutLauncher

/** The operator dashboard's license card with Renew (§9.4 step 8). */
@Composable
fun LicenseCardRoute(modifier: Modifier = Modifier, viewModel: LicenseCardViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) { viewModel.checkout.collect { CheckoutLauncher.open(context, it) } }
    LicenseCard(state = state, onRenew = viewModel::renew, onRetry = viewModel::load, modifier = modifier)
}

@Composable
fun LicenseCard(
    state: LicenseCardState,
    onRenew: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LoadableContent(
        state = state.license,
        errorTitle = stringResource(R.string.license_error_title),
        onRetry = onRetry,
        modifier = modifier,
    ) { license ->
        if (license == null) {
            EmptyState(
                icon = Icons.Rounded.Verified,
                title = stringResource(R.string.license_none_title),
                message = stringResource(R.string.license_none_message),
            )
        } else {
            LicenseDetails(license, state.pay, onRenew)
        }
    }
}

@Composable
private fun LicenseDetails(license: License, pay: PayState, onRenew: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(FoxSpacing.M), modifier = Modifier.padding(FoxSpacing.L)) {
        when (license.status) {
            LicenseStatus.Grace -> FoxNotice(stringResource(R.string.license_grace_banner), StatusTone.Warning)
            LicenseStatus.Expired -> FoxNotice(stringResource(R.string.license_expired_banner), StatusTone.Error)
            else -> Unit
        }
        FoxCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(FoxSpacing.M), verticalArrangement = Arrangement.spacedBy(FoxSpacing.XS)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FoxSpacing.S),
                ) {
                    Text(
                        stringResource(R.string.license_card_title, license.provinceName),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    StatusPill(licenseLabel(license.status), licenseTone(license.status))
                }
                license.daysLeft?.takeIf { it >= 0 }?.let {
                    Text(
                        pluralStringResource(R.plurals.license_days_left, it, it),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                license.endAt?.let {
                    Text(
                        stringResource(R.string.license_valid_until, it.take(DATE_LENGTH)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        PayNotice(pay)
        if (license.canPay) {
            FoxButton(
                text = stringResource(R.string.license_renew, Money(license.feeCentavos).formatPeso()),
                onClick = onRenew,
                state = if (pay == PayState.Starting ||
                    pay == PayState.Waiting
                ) {
                    FoxButtonState.Loading
                } else {
                    FoxButtonState.Idle
                },
                loadingDescription = stringResource(R.string.license_paying),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private const val DATE_LENGTH = 10 // "2027-11-26" from an ISO timestamp

@Composable
private fun licenseLabel(status: LicenseStatus): String =
    stringResource(
        when (status) {
            LicenseStatus.Applied -> R.string.license_state_applied
            LicenseStatus.PendingPayment -> R.string.license_state_pending_payment
            LicenseStatus.Active -> R.string.license_state_active
            LicenseStatus.Grace -> R.string.license_state_grace
            LicenseStatus.Expired -> R.string.license_state_expired
            LicenseStatus.Suspended -> R.string.license_state_suspended
            LicenseStatus.Rejected -> R.string.license_state_rejected
            LicenseStatus.Unknown -> R.string.license_state_unknown
        },
    )

private fun licenseTone(status: LicenseStatus): StatusTone =
    when (status) {
        LicenseStatus.Active -> StatusTone.Success
        LicenseStatus.Grace, LicenseStatus.PendingPayment -> StatusTone.Warning
        LicenseStatus.Expired, LicenseStatus.Suspended, LicenseStatus.Rejected -> StatusTone.Error
        LicenseStatus.Applied, LicenseStatus.Unknown -> StatusTone.Info
    }
