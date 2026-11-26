package ph.foxdrop.business.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.persistentListOf
import ph.foxdrop.business.R
import ph.foxdrop.core.designsystem.component.EmptyState
import ph.foxdrop.core.designsystem.component.FoxCard
import ph.foxdrop.core.designsystem.component.FoxFormColumn
import ph.foxdrop.core.designsystem.component.FoxNavigationShell
import ph.foxdrop.core.designsystem.component.FoxOutlinedButton
import ph.foxdrop.core.designsystem.component.FoxTextButton
import ph.foxdrop.core.designsystem.component.ShellTab
import ph.foxdrop.core.designsystem.preview.FoxDevicePreviews
import ph.foxdrop.core.designsystem.preview.FoxPreviews
import ph.foxdrop.core.designsystem.theme.FoxDropTheme
import ph.foxdrop.core.designsystem.theme.FoxSpacing

/** The operator tabs (§9.4). */
enum class OperatorTab { Dashboard, Stores, Riders, Orders, Account }

/**
 * Provincial operator home (§9.4, `03_Provincial_Operator_Workflow.png`): dashboard, stores, riders, orders, account.
 * Bottom bar on phones, navigation rail on tablets like the dashboard in the reference image. The license card and
 * the approval lists come from their features through the slots, so this screen stays previewable; live orders and
 * KPIs arrive in Phases 4–6.
 */
@Suppress("LongParameterList") // the app wires one slot or callback per tab
@Composable
fun OperatorHomeScreen(
    onSignOut: () -> Unit,
    onOpenZones: () -> Unit,
    onOpenRates: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: OperatorTab = OperatorTab.Dashboard,
    licenseCard: @Composable () -> Unit = {},
    stores: @Composable () -> Unit = {},
    riders: @Composable () -> Unit = {},
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab.ordinal) }
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
        when (OperatorTab.entries[index]) {
            OperatorTab.Dashboard -> Dashboard(licenseCard)

            OperatorTab.Stores -> stores()

            OperatorTab.Riders -> riders()

            OperatorTab.Orders ->
                EmptyState(
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    title = stringResource(R.string.orders_empty_title),
                    message = stringResource(R.string.operator_orders_message),
                )

            OperatorTab.Account -> OperatorAccount(onOpenZones, onOpenRates, onSignOut)
        }
    }
}

@Composable
private fun Dashboard(licenseCard: @Composable () -> Unit) {
    FoxFormColumn(Modifier.statusBarsPadding()) {
        Text(stringResource(R.string.operator_dashboard_title), style = MaterialTheme.typography.headlineSmall)
        licenseCard()
        Text(
            stringResource(R.string.operator_dashboard_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OperatorAccount(onOpenZones: () -> Unit, onOpenRates: () -> Unit, onSignOut: () -> Unit) {
    FoxFormColumn(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(stringResource(R.string.operator_account_title), style = MaterialTheme.typography.headlineSmall)
        FoxCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(FoxSpacing.M), verticalArrangement = Arrangement.spacedBy(FoxSpacing.S)) {
                Text(stringResource(R.string.operator_province_settings), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.operator_province_settings_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FoxOutlinedButton(
                    text = stringResource(R.string.operator_zones),
                    onClick = onOpenZones,
                    leadingIcon = rememberVectorPainter(Icons.Rounded.Map),
                    modifier = Modifier.fillMaxWidth(),
                )
                FoxOutlinedButton(
                    text = stringResource(R.string.operator_rates),
                    onClick = onOpenRates,
                    leadingIcon = rememberVectorPainter(Icons.Rounded.Payments),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        FoxTextButton(
            stringResource(R.string.account_sign_out),
            onSignOut,
            Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@FoxPreviews
@FoxDevicePreviews
@Composable
private fun OperatorHomePreview() {
    FoxDropTheme { OperatorHomeScreen(onSignOut = {}, onOpenZones = {}, onOpenRates = {}) }
}

@FoxPreviews
@Composable
private fun OperatorAccountPreview() {
    FoxDropTheme {
        OperatorHomeScreen(onSignOut = {}, onOpenZones = {}, onOpenRates = {}, initialTab = OperatorTab.Account)
    }
}
