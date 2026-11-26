package ph.foxdrop.feature.operatorpartners

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material3.FilterChip
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
import kotlinx.collections.immutable.ImmutableList
import ph.foxdrop.core.designsystem.component.EmptyState
import ph.foxdrop.core.designsystem.component.FoxCard
import ph.foxdrop.core.designsystem.component.Loadable
import ph.foxdrop.core.designsystem.component.LoadableContent
import ph.foxdrop.core.designsystem.component.StatusPill
import ph.foxdrop.core.designsystem.component.StatusTone
import ph.foxdrop.core.designsystem.component.label
import ph.foxdrop.core.designsystem.component.tone
import ph.foxdrop.core.designsystem.theme.FoxLayout
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.domain.partner.RiderSummary
import ph.foxdrop.core.domain.partner.StoreSummary

/** The operator home's Stores tab: store applications by status (§9.4 step 4). */
@Composable
fun StoreApprovalsRoute(
    onOpenStore: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StoreListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    StoreApprovalsScreen(
        state,
        onFilter = viewModel::select,
        onRetry = viewModel::refresh,
        onOpenStore = onOpenStore,
        modifier = modifier,
    )
}

/** The operator home's Riders tab: rider applications by status. */
@Composable
fun RiderApprovalsRoute(
    onOpenRider: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RiderListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    RiderApprovalsScreen(
        state,
        onFilter = viewModel::select,
        onRetry = viewModel::refresh,
        onOpenRider = onOpenRider,
        modifier = modifier,
    )
}

@Composable
fun StoreApprovalsScreen(
    state: PartnerListState<StoreSummary>,
    onFilter: (PartnerFilter) -> Unit,
    onRetry: () -> Unit,
    onOpenStore: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PartnerList(
        title = stringResource(R.string.partners_stores_title),
        filter = state.filter,
        items = state.items,
        onFilter = onFilter,
        onRetry = onRetry,
        emptyIcon = Icons.Rounded.Storefront,
        emptyTitle = stringResource(R.string.partners_stores_empty_title),
        emptyMessage = stringResource(R.string.partners_stores_empty_message),
        key = { it.id },
        modifier = modifier,
    ) { store ->
        PartnerRow(
            icon = Icons.Rounded.Storefront,
            title = store.name,
            subtitle = "${store.category.label()} · ${store.city}",
            detail = listOfNotNull(
                store.ownerName,
                stringResource(R.string.partners_submitted_on, store.createdAt.day()),
            ).joinToString(" · "),
            status = store.status.label(),
            tone = store.status.tone(),
            onClick = { onOpenStore(store.id) },
        )
    }
}

@Composable
fun RiderApprovalsScreen(
    state: PartnerListState<RiderSummary>,
    onFilter: (PartnerFilter) -> Unit,
    onRetry: () -> Unit,
    onOpenRider: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PartnerList(
        title = stringResource(R.string.partners_riders_title),
        filter = state.filter,
        items = state.items,
        onFilter = onFilter,
        onRetry = onRetry,
        emptyIcon = Icons.Rounded.TwoWheeler,
        emptyTitle = stringResource(R.string.partners_riders_empty_title),
        emptyMessage = stringResource(R.string.partners_riders_empty_message),
        key = { it.id },
        modifier = modifier,
    ) { rider ->
        PartnerRow(
            icon = Icons.Rounded.TwoWheeler,
            title = rider.fullName ?: rider.phone.orEmpty(),
            subtitle = listOfNotNull(rider.vehicleType.label(), rider.plateNo).joinToString(" · "),
            detail = stringResource(R.string.partners_submitted_on, rider.createdAt.day()),
            status = rider.status.label(),
            tone = rider.status.tone(),
            onClick = { onOpenRider(rider.id) },
        )
    }
}

@Suppress("LongParameterList") // shared by the store and rider lists; every value differs between them
@Composable
private fun <T> PartnerList(
    title: String,
    filter: PartnerFilter,
    items: Loadable<ImmutableList<T>>,
    onFilter: (PartnerFilter) -> Unit,
    onRetry: () -> Unit,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptyMessage: String,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    row: @Composable (T) -> Unit,
) {
    Column(modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(start = FoxSpacing.L, end = FoxSpacing.L, top = FoxSpacing.L)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(FoxSpacing.S),
                modifier = Modifier.padding(top = FoxSpacing.S),
            ) {
                PartnerFilter.entries.forEach { option ->
                    FilterChip(selected = filter == option, onClick = {
                        onFilter(option)
                    }, label = { Text(option.label()) })
                }
            }
        }
        LoadableContent(
            state = items,
            errorTitle = stringResource(R.string.partners_error_title),
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize(),
        ) { list ->
            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(icon = emptyIcon, title = emptyTitle, message = emptyMessage)
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    LazyColumn(
                        modifier = Modifier.widthIn(max = FoxLayout.MaxFormWidth).fillMaxWidth(),
                        contentPadding = PaddingValues(FoxSpacing.L),
                        verticalArrangement = Arrangement.spacedBy(FoxSpacing.S),
                    ) {
                        items(list, key = key) { row(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PartnerRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    detail: String,
    status: String,
    tone: StatusTone,
    onClick: () -> Unit,
) {
    FoxCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(FoxSpacing.M), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = FoxSpacing.S)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusPill(status, tone)
        }
    }
}

@Composable
private fun PartnerFilter.label(): String =
    stringResource(
        when (this) {
            PartnerFilter.Pending -> R.string.partners_filter_pending
            PartnerFilter.Approved -> R.string.partners_filter_approved
            PartnerFilter.Rejected -> R.string.partners_filter_rejected
        },
    )
