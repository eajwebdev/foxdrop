package ph.foxdrop.rider.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.TwoWheeler
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
import ph.foxdrop.feature.auth.rememberSessionSignOut
import ph.foxdrop.rider.R

@Composable
fun RiderHomeRoute(
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RiderHomeScreen(onSignOut = rememberSessionSignOut(onSignedOut), modifier = modifier)
}

/** Phase 0 shell. Rider registration, Go Online and jobs arrive in Phase 4. */
@Composable
fun RiderHomeScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    FoxNavigationShell(
        tabs =
            persistentListOf(
                ShellTab(Icons.Rounded.TwoWheeler, stringResource(R.string.tab_jobs)),
                ShellTab(Icons.Rounded.Savings, stringResource(R.string.tab_earnings)),
                ShellTab(Icons.Rounded.Person, stringResource(R.string.tab_account)),
            ),
        selectedIndex = tab,
        onSelect = { tab = it },
        modifier = modifier,
    ) { index ->
        when (index) {
            0 ->
                EmptyState(
                    icon = Icons.Rounded.TwoWheeler,
                    title = stringResource(R.string.jobs_empty_title),
                    message = stringResource(R.string.jobs_empty_message),
                )

            1 ->
                EmptyState(
                    icon = Icons.Rounded.Savings,
                    title = stringResource(R.string.earnings_empty_title),
                    message = stringResource(R.string.earnings_empty_message),
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
private fun RiderHomePreview() {
    FoxDropTheme { RiderHomeScreen(onSignOut = {}) }
}
