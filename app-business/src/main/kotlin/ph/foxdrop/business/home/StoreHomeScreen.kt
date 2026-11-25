package ph.foxdrop.business.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Person
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
 * Store owner / staff home (§9.3). Bottom bar on phones, navigation rail on tablets (§2). Order board, catalog and
 * wallet arrive in Phases 3 and 5.
 */
@Composable
fun StoreHomeScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    FoxNavigationShell(
        tabs =
            persistentListOf(
                ShellTab(Icons.Rounded.Dashboard, stringResource(R.string.tab_dashboard)),
                ShellTab(Icons.AutoMirrored.Rounded.ReceiptLong, stringResource(R.string.tab_orders)),
                ShellTab(Icons.Rounded.AccountBalanceWallet, stringResource(R.string.tab_wallet)),
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
                    title = stringResource(R.string.dashboard_empty_title),
                    message = stringResource(R.string.dashboard_empty_message),
                )

            1 ->
                EmptyState(
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    title = stringResource(R.string.orders_empty_title),
                    message = stringResource(R.string.orders_empty_message),
                )

            2 ->
                EmptyState(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    title = stringResource(R.string.wallet_empty_title),
                    message = stringResource(R.string.wallet_empty_message),
                )

            else ->
                EmptyState(
                    icon = Icons.Rounded.Person,
                    title = stringResource(R.string.account_title),
                    message = stringResource(R.string.account_message),
                    actionLabel = stringResource(R.string.account_sign_out),
                    onAction = onSignOut,
                )
        }
    }
}

@FoxPreviews
@FoxDevicePreviews
@Composable
private fun StoreHomePreview() {
    FoxDropTheme { StoreHomeScreen(onSignOut = {}) }
}
