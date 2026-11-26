package ph.foxdrop.business.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ph.foxdrop.business.R
import ph.foxdrop.core.designsystem.component.FoxButton
import ph.foxdrop.core.designsystem.component.FoxCard
import ph.foxdrop.core.designsystem.component.FoxFormColumn
import ph.foxdrop.core.designsystem.component.FoxTextButton
import ph.foxdrop.core.designsystem.component.Loadable
import ph.foxdrop.core.designsystem.component.LoadableContent
import ph.foxdrop.core.designsystem.component.StatusPill
import ph.foxdrop.core.designsystem.component.StatusTone
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.domain.operator.ApplicationStatus
import ph.foxdrop.core.domain.operator.LicenseStatus
import ph.foxdrop.core.domain.operator.OperatorApplication
import ph.foxdrop.core.domain.partner.MyStore
import ph.foxdrop.core.domain.partner.StoreStatus

@Composable
fun GetStartedRoute(
    onOpenStore: () -> Unit,
    onBecomeOperator: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GetStartedViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    GetStartedScreen(
        state,
        onOpenStore,
        onBecomeOperator,
        onRetry = viewModel::refresh,
        onSignOut = onSignOut,
        modifier = modifier,
    )
}

/** "Get started" (DECISIONS 64): open a store, or apply to run FoxDrop in a province. */
@Composable
fun GetStartedScreen(
    state: Loadable<GetStartedData>,
    onOpenStore: () -> Unit,
    onBecomeOperator: () -> Unit,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LoadableContent(
        state = state,
        errorTitle = stringResource(R.string.start_error_title),
        onRetry = onRetry,
        modifier = modifier.fillMaxSize().statusBarsPadding(),
    ) { data ->
        FoxFormColumn {
            Text(stringResource(R.string.start_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.start_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PathCard(
                icon = Icons.Rounded.Storefront,
                title = stringResource(R.string.start_store_title),
                message = stringResource(R.string.start_store_message),
                status = data.store?.let { storeStatus(it) },
                action = stringResource(
                    if (data.store ==
                        null
                    ) {
                        R.string.start_store_action
                    } else {
                        R.string.start_view_status
                    },
                ),
                onAction = onOpenStore,
            )
            PathCard(
                icon = Icons.Rounded.Map,
                title = stringResource(R.string.start_operator_title),
                message = stringResource(R.string.start_operator_message),
                status = data.application?.let { applicationStatus(it) },
                action = stringResource(
                    if (data.application ==
                        null
                    ) {
                        R.string.start_operator_action
                    } else {
                        R.string.start_view_status
                    },
                ),
                onAction = onBecomeOperator,
            )
            FoxTextButton(
                stringResource(R.string.account_sign_out),
                onSignOut,
                Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Suppress("LongParameterList") // one card per path, every value differs
@Composable
private fun PathCard(
    icon: ImageVector,
    title: String,
    message: String,
    status: Pair<String, StatusTone>?,
    action: String,
    onAction: () -> Unit,
) {
    FoxCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(FoxSpacing.M), verticalArrangement = Arrangement.spacedBy(FoxSpacing.S)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FoxSpacing.S),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                status?.let { (label, tone) -> StatusPill(label, tone) }
            }
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FoxButton(action, onAction, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun storeStatus(store: MyStore): Pair<String, StatusTone> =
    when (store.status) {
        StoreStatus.Active, StoreStatus.Approved -> stringResource(R.string.start_status_approved) to StatusTone.Success
        StoreStatus.Rejected -> stringResource(R.string.start_status_rejected) to StatusTone.Error
        else -> stringResource(R.string.start_status_review) to StatusTone.Info
    }

@Composable
private fun applicationStatus(application: OperatorApplication): Pair<String, StatusTone> =
    when {
        application.license?.status == LicenseStatus.PendingPayment -> stringResource(R.string.start_status_pay) to
            StatusTone.Warning

        application.status == ApplicationStatus.Approved -> stringResource(R.string.start_status_approved) to
            StatusTone.Success

        application.status == ApplicationStatus.Rejected -> stringResource(R.string.start_status_rejected) to
            StatusTone.Error

        application.status == ApplicationStatus.Withdrawn -> stringResource(R.string.start_status_withdrawn) to
            StatusTone.Neutral

        else -> stringResource(R.string.start_status_review) to StatusTone.Info
    }
