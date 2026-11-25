package ph.foxdrop.customer.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.persistentListOf
import ph.foxdrop.core.designsystem.component.EmptyState
import ph.foxdrop.core.designsystem.component.FoxNavigationShell
import ph.foxdrop.core.designsystem.component.ShellTab
import ph.foxdrop.core.designsystem.preview.FoxDevicePreviews
import ph.foxdrop.core.designsystem.preview.FoxPreviews
import ph.foxdrop.core.designsystem.theme.FoxDropTheme
import ph.foxdrop.customer.R

/**
 * Phase 0 shell: real tabs and adaptive navigation, with friendly empty states until store browsing (Phase 3)
 * and order history arrive.
 */
@Composable
fun CustomerHomeScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    FoxNavigationShell(
        tabs =
            persistentListOf(
                ShellTab(Icons.Rounded.Home, stringResource(R.string.tab_home)),
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
                    icon = Icons.Rounded.Storefront,
                    title = stringResource(R.string.home_empty_title),
                    message = stringResource(R.string.home_empty_message),
                )

            1 ->
                EmptyState(
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    title = stringResource(R.string.orders_empty_title),
                    message = stringResource(R.string.orders_empty_message),
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
private fun CustomerHomePreview() {
    FoxDropTheme { CustomerHomeScreen(onSignOut = {}) }
}
