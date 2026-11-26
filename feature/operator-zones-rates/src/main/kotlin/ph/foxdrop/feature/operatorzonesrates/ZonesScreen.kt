package ph.foxdrop.feature.operatorzonesrates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import ph.foxdrop.core.designsystem.component.EmptyState
import ph.foxdrop.core.designsystem.component.FoxButton
import ph.foxdrop.core.designsystem.component.FoxButtonState
import ph.foxdrop.core.designsystem.component.FoxCard
import ph.foxdrop.core.designsystem.component.FoxScreenScaffold
import ph.foxdrop.core.designsystem.component.FoxTextButton
import ph.foxdrop.core.designsystem.component.FoxTextField
import ph.foxdrop.core.designsystem.component.LoadableContent
import ph.foxdrop.core.designsystem.component.errorMessage
import ph.foxdrop.core.designsystem.component.problemMessage
import ph.foxdrop.core.designsystem.theme.FoxLayout
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.domain.territory.Zone
import ph.foxdrop.core.location.LocationResult
import ph.foxdrop.core.location.rememberLocationPermissionRequest

@Composable
fun ZonesRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: ZonesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val askLocation = rememberLocationPermissionRequest { viewModel.onAction(ZonesAction.LocationPermissionResult(it)) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ZonesEffect.RequestLocationPermission -> askLocation()
            }
        }
    }
    ZonesScreen(state = state, onAction = viewModel::onAction, onBack = onBack, modifier = modifier)
}

@Composable
fun ZonesScreen(
    state: ZonesState,
    onAction: (ZonesAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val message = state.message?.let { errorMessage(it) }
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            onAction(ZonesAction.DismissMessage)
        }
    }
    FoxScreenScaffold(
        title = stringResource(R.string.zones_title),
        onBack = onBack,
        snackbarHostState = snackbar,
        modifier = modifier,
    ) { padding ->
        LoadableContent(
            state = state.content,
            errorTitle = stringResource(R.string.zones_error_title),
            onRetry = { onAction(ZonesAction.Retry) },
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { data ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    modifier = Modifier.widthIn(max = FoxLayout.MaxFormWidth).fillMaxWidth(),
                    contentPadding = PaddingValues(FoxSpacing.L),
                    verticalArrangement = Arrangement.spacedBy(FoxSpacing.M),
                ) {
                    item {
                        Text(
                            stringResource(R.string.zones_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item {
                        AnimatedVisibility(visible = state.editor != null) {
                            state.editor?.let { ZoneEditorCard(it, onAction) }
                        }
                        if (state.editor == null) {
                            FoxButton(stringResource(R.string.zones_add), {
                                onAction(ZonesAction.AddZone)
                            }, Modifier.fillMaxWidth())
                        }
                    }
                    if (data.zones.isEmpty() && state.editor == null) {
                        item {
                            EmptyState(
                                icon = Icons.Rounded.Map,
                                title = stringResource(R.string.zones_empty_title),
                                message = stringResource(R.string.zones_empty_message),
                            )
                        }
                    }
                    items(data.zones, key = {
                        it.id
                    }) { zone -> ZoneRow(zone, onDelete = { onAction(ZonesAction.Delete(zone.id)) }) }
                }
            }
        }
    }
}

@Composable
private fun ZoneRow(zone: Zone, onDelete: () -> Unit) {
    FoxCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = FoxSpacing.M, top = FoxSpacing.S, bottom = FoxSpacing.S),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = FoxSpacing.S)) {
                Text(zone.name, style = MaterialTheme.typography.titleMedium)
                zone.radiusKm?.let {
                    Text(stringResource(R.string.zones_radius, it.km()), style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    stringResource(R.string.zones_area, String.format(Locale.US, "%.0f", zone.areaKm2)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.zones_delete, zone.name))
            }
        }
    }
}

@Composable
private fun ZoneEditorCard(editor: ZoneEditor, onAction: (ZonesAction) -> Unit) {
    FoxCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(FoxSpacing.M), verticalArrangement = Arrangement.spacedBy(FoxSpacing.S)) {
            Text(stringResource(R.string.zones_editor_title), style = MaterialTheme.typography.titleMedium)
            FoxTextField(
                value = editor.name,
                onValueChange = { onAction(ZonesAction.NameChanged(it)) },
                label = stringResource(R.string.zones_name),
                placeholder = stringResource(R.string.zones_name_hint),
                errorMessage = problemMessage(editor.nameProblem),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.zones_center), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(FoxSpacing.S)) {
                FilterChip(
                    selected = editor.source == CenterSource.ProvinceCenter,
                    onClick = { onAction(ZonesAction.UseProvinceCenter) },
                    label = { Text(stringResource(R.string.zones_center_province)) },
                )
                FilterChip(
                    selected = editor.source == CenterSource.MyLocation,
                    onClick = { onAction(ZonesAction.UseMyLocation) },
                    label = { Text(stringResource(R.string.zones_center_mine)) },
                )
            }
            val centerText =
                when {
                    editor.locating -> stringResource(R.string.zones_locating)

                    editor.locationProblem != null -> locationMessage(editor.locationProblem)

                    editor.center != null ->
                        stringResource(
                            R.string.zones_center_value,
                            String.format(Locale.US, "%.4f", editor.center.lat),
                            String.format(Locale.US, "%.4f", editor.center.lng),
                        )

                    else -> locationMessage(LocationResult.Unavailable)
                }
            Text(
                centerText,
                style = MaterialTheme.typography.bodySmall,
                color = if (editor.locationProblem !=
                    null
                ) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                stringResource(R.string.zones_radius_label, editor.radiusKm.toDouble().km()),
                style = MaterialTheme.typography.labelLarge,
            )
            Slider(
                value = editor.radiusKm,
                onValueChange = { onAction(ZonesAction.RadiusChanged(it)) },
                // Continuous track (59 tick marks would be noise); the ViewModel snaps to 0.5 km.
                valueRange = ZoneEditor.MIN_RADIUS_KM..ZoneEditor.MAX_RADIUS_KM,
            )
            FoxButton(
                text = stringResource(R.string.zones_save),
                onClick = { onAction(ZonesAction.Save) },
                state = if (editor.saving) FoxButtonState.Loading else FoxButtonState.Idle,
                loadingDescription = stringResource(R.string.zones_saving),
                modifier = Modifier.fillMaxWidth(),
            )
            FoxTextButton(stringResource(R.string.zones_cancel), {
                onAction(ZonesAction.DismissEditor)
            }, Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun locationMessage(result: LocationResult): String =
    stringResource(
        when (result) {
            LocationResult.PermissionDenied -> R.string.zones_location_denied
            LocationResult.Disabled -> R.string.zones_location_disabled
            is LocationResult.Found, LocationResult.Unavailable -> R.string.zones_location_unavailable
        },
    )

private fun Double.km(): String = if (this % 1.0 == 0.0) toLong().toString() else String.format(Locale.US, "%.1f", this)
