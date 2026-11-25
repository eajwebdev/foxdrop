package ph.foxdrop.business.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.persistentListOf
import ph.foxdrop.business.R
import ph.foxdrop.core.designsystem.component.EmptyState
import ph.foxdrop.core.designsystem.component.FoxNavigationShell
import ph.foxdrop.core.designsystem.component.ShellTab
import ph.foxdrop.core.designsystem.preview.FoxDevicePreviews
import ph.foxdrop.core.designsystem.preview.FoxPreviews
import ph.foxdrop.core.designsystem.theme.FoxDropTheme

/**
 * Provincial operator home (§9.4, `03_Provincial_Operator_Workflow.png`): dashboard, stores, riders, orders.
 * Bottom bar on phones, navigation rail on tablets like the dashboard in the reference image.
 * Licensing, territory and onboarding screens arrive in Phase 2; live orders and KPIs in Phases 4–6.
 */
@Composable
fun OperatorHomeScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    FoxNavigationShell(
        tabs =
            persistentListOf(
                ShellTab(Icons.Rounded.Dashboard, stringResource(R.string.tab_dashboard)),
                ShellTab(Icons.Rounded.Storefront, stringResource(R.string.tab_stores)),
                ShellTab(Icons.Rounded.TwoWheeler, stringResource(R.string.tab_riders)),
                ShellTab(Icons.AutoMirrored.Rounded.ReceiptLong, stringResource(R.string.tab_orders)),
                ShellTab(Icons.Rounded.Person, stringResource(R.string.tab_account)),
            ),
        selectedIndex = tab,
        onSelect = { tab = it },
        modifier = modifier,
    ) { index ->
        when (index) {
            0 ->
                EmptyState(
                    icon = Icons.Rounded.Dashboard,
                    title = stringResource(R.string.operator_dashboard_title),
                    message = stringResource(R.string.operator_dashboard_message),
                )

            1 ->
                EmptyState(
                    icon = Icons.Rounded.Storefront,
                    title = stringResource(R.string.operator_stores_title),
                    message = stringResource(R.string.operator_stores_message),
                )

            2 ->
                EmptyState(
                    icon = Icons.Rounded.TwoWheeler,
                    title = stringResource(R.string.operator_riders_title),
                    message = stringResource(R.string.operator_riders_message),
                )

            3 ->
                EmptyState(
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    title = stringResource(R.string.orders_empty_title),
                    message = stringResource(R.string.operator_orders_message),
                )

            else ->
                EmptyState(
                    icon = Icons.Rounded.Person,
                    title = stringResource(R.string.account_title),
                    message = stringResource(R.string.operator_account_message),
                    actionLabel = stringResource(R.string.account_sign_out),
                    onAction = onSignOut,
                )
        }
    }
}

@FoxPreviews
@FoxDevicePreviews
@Composable
private fun OperatorHomePreview() {
    FoxDropTheme { OperatorHomeScreen(onSignOut = {}) }
}
