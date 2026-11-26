package ph.foxdrop.feature.storeonboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import kotlinx.collections.immutable.toImmutableList
import ph.foxdrop.core.designsystem.R as DesignR
import ph.foxdrop.core.designsystem.component.DropdownOption
import ph.foxdrop.core.designsystem.component.EmptyState
import ph.foxdrop.core.designsystem.component.ErrorState
import ph.foxdrop.core.designsystem.component.FoxButton
import ph.foxdrop.core.designsystem.component.FoxButtonState
import ph.foxdrop.core.designsystem.component.FoxCard
import ph.foxdrop.core.designsystem.component.FoxDropdown
import ph.foxdrop.core.designsystem.component.FoxFormColumn
import ph.foxdrop.core.designsystem.component.FoxNotice
import ph.foxdrop.core.designsystem.component.FoxOutlinedButton
import ph.foxdrop.core.designsystem.component.FoxScreenScaffold
import ph.foxdrop.core.designsystem.component.FoxTextField
import ph.foxdrop.core.designsystem.component.SkeletonList
import ph.foxdrop.core.designsystem.component.StatusPill
import ph.foxdrop.core.designsystem.component.StatusTone
import ph.foxdrop.core.designsystem.component.errorMessage
import ph.foxdrop.core.designsystem.component.problemMessage
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.designsystem.theme.FoxTheme
import ph.foxdrop.core.domain.onboarding.Problem
import ph.foxdrop.core.domain.onboarding.StoreApplicationRules.Field
import ph.foxdrop.core.domain.partner.MyStore
import ph.foxdrop.core.domain.partner.StoreCategory
import ph.foxdrop.core.domain.partner.StoreStatus
import ph.foxdrop.core.location.LocationResult
import ph.foxdrop.core.location.rememberLocationPermissionRequest
import ph.foxdrop.core.media.PhotoSlot
import ph.foxdrop.core.media.rememberPhotoPicker
import ph.foxdrop.feature.storeonboarding.StoreOnboardingAction as Action
import ph.foxdrop.feature.storeonboarding.StoreOnboardingState as State

/**
 * "Open a store" from the business app's Get started screen. [onRolesChanged] runs when the owner opens an approved
 * store: the store role now exists, so the app re-resolves its home.
 */
@Composable
fun StoreOnboardingRoute(
    onBack: () -> Unit,
    onRolesChanged: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StoreOnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val unreadable = stringResource(R.string.store_photo_unreadable)
    val askLocation = rememberLocationPermissionRequest { viewModel.onAction(Action.LocationPermissionResult(it)) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                StoreOnboardingEffect.RequestLocationPermission -> askLocation()
                StoreOnboardingEffect.PhotoUnreadable -> snackbar.showSnackbar(unreadable)
            }
        }
    }
    val permitPicker = rememberPhotoPicker { viewModel.onAction(Action.PhotoPicked(StoreDocument.BusinessPermit, it)) }
    val idPicker = rememberPhotoPicker { viewModel.onAction(Action.PhotoPicked(StoreDocument.ValidId, it)) }
    StoreOnboardingScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        onOpenStore = onRolesChanged,
        onTakePhoto = { if (it == StoreDocument.ValidId) idPicker.takePhoto() else permitPicker.takePhoto() },
        onChoosePhoto = {
            if (it ==
                StoreDocument.ValidId
            ) {
                idPicker.chooseFromGallery()
            } else {
                permitPicker.chooseFromGallery()
            }
        },
        snackbarHostState = snackbar,
        modifier = modifier,
    )
}

@Suppress("LongParameterList") // stateless for previews and screenshot tests
@Composable
fun StoreOnboardingScreen(
    state: State,
    onAction: (Action) -> Unit,
    onBack: () -> Unit,
    onOpenStore: () -> Unit,
    onTakePhoto: (StoreDocument) -> Unit,
    onChoosePhoto: (StoreDocument) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val motion = FoxTheme.motion
    FoxScreenScaffold(
        title = stringResource(R.string.store_title),
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    ) { padding ->
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = { fadeIn(motion.enter()) togetherWith fadeOut(motion.exit()) },
            label = "storeOnboarding",
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { current ->
            when (current) {
                State.Loading -> SkeletonList(
                    stringResource(R.string.store_title),
                    Modifier.fillMaxSize().padding(FoxSpacing.L),
                )

                is State.Failed ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorState(
                            title = stringResource(R.string.store_error_title),
                            message = errorMessage(current.error),
                            retryLabel = stringResource(DesignR.string.action_retry),
                            onRetry = { onAction(Action.Retry) },
                        )
                    }

                is State.Apply -> ApplyContent(current, onAction, onTakePhoto, onChoosePhoto)

                is State.Status -> StatusContent(current, onAction, onOpenStore)
            }
        }
    }
}

@Composable
private fun ApplyContent(
    state: State.Apply,
    onAction: (Action) -> Unit,
    onTakePhoto: (StoreDocument) -> Unit,
    onChoosePhoto: (StoreDocument) -> Unit,
) {
    val form = state.form
    if (form.provinces.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Rounded.Map,
                title = stringResource(R.string.store_no_provinces_title),
                message = stringResource(R.string.store_no_provinces_message),
            )
        }
        return
    }
    val enabled = !state.submitting
    FoxFormColumn {
        Text(stringResource(R.string.store_apply_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.store_apply_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.previous?.rejectionReason?.let {
            FoxNotice(stringResource(R.string.store_apply_again_note, it), StatusTone.Warning)
        }
        FoxDropdown(
            label = stringResource(R.string.store_province),
            placeholder = stringResource(R.string.store_province_hint),
            options = form.provinces.map { DropdownOption(it.id, it.name) }.toImmutableList(),
            selectedId = form.provinceId,
            onSelect = { onAction(Action.ProvinceSelected(it)) },
            errorMessage = problemMessage(form.errors[Field.Province]),
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        StoreTextField(form.name, R.string.store_name, form.errors[Field.Name], enabled) {
            onAction(Action.NameChanged(it))
        }
        CategoryChips(form.category, enabled) { onAction(Action.CategorySelected(it)) }
        StoreTextField(form.description, R.string.store_description, null, enabled) {
            onAction(Action.DescriptionChanged(it))
        }
        StoreTextField(form.addressLine, R.string.store_address, form.errors[Field.Address], enabled) {
            onAction(Action.AddressChanged(it))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(FoxSpacing.S)) {
            StoreTextField(form.barangay, R.string.store_barangay, null, enabled, Modifier.weight(1f)) {
                onAction(Action.BarangayChanged(it))
            }
            StoreTextField(form.city, R.string.store_city, form.errors[Field.City], enabled, Modifier.weight(1f)) {
                onAction(Action.CityChanged(it))
            }
        }
        FoxTextField(
            value = form.phone,
            onValueChange = { onAction(Action.PhoneChanged(it)) },
            label = stringResource(R.string.store_phone),
            prefix = "+63 ",
            errorMessage = problemMessage(form.errors[Field.Phone]),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        LocationCard(form, enabled) { onAction(Action.UseMyLocation) }
        Text(stringResource(R.string.store_documents), style = MaterialTheme.typography.titleMedium)
        PhotoSlot(
            label = stringResource(R.string.store_business_permit),
            photo = form.businessPermit,
            onTakePhoto = { onTakePhoto(StoreDocument.BusinessPermit) },
            onChoosePhoto = { onChoosePhoto(StoreDocument.BusinessPermit) },
            errorMessage = problemMessage(form.errors[Field.BusinessPermit]),
            enabled = enabled,
        )
        PhotoSlot(
            label = stringResource(R.string.store_valid_id),
            photo = form.validId,
            onTakePhoto = { onTakePhoto(StoreDocument.ValidId) },
            onChoosePhoto = { onChoosePhoto(StoreDocument.ValidId) },
            errorMessage = problemMessage(form.errors[Field.ValidId]),
            enabled = enabled,
        )
        AnimatedVisibility(visible = state.message != null) {
            state.message?.let { FoxNotice(errorMessage(it), StatusTone.Error) }
        }
        FoxButton(
            text = stringResource(R.string.store_submit),
            onClick = { onAction(Action.Submit) },
            state = if (state.submitting) FoxButtonState.Loading else FoxButtonState.Idle,
            loadingDescription = stringResource(R.string.store_submitting),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StoreTextField(
    value: String,
    label: Int,
    problem: Problem?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit,
) {
    FoxTextField(
        value = value,
        onValueChange = onChange,
        label = stringResource(label),
        errorMessage = problemMessage(problem),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(selected: StoreCategory, enabled: Boolean, onSelect: (StoreCategory) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(FoxSpacing.XS)) {
        Text(stringResource(R.string.store_category), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(FoxSpacing.S)) {
            StoreCategory.entries.forEach { category ->
                FilterChip(
                    selected = category == selected,
                    onClick = { onSelect(category) },
                    label = { Text(category.label()) },
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
private fun LocationCard(form: StoreForm, enabled: Boolean, onLocate: () -> Unit) {
    val problem = problemMessage(form.errors[Field.Location])
    FoxCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(FoxSpacing.M), verticalArrangement = Arrangement.spacedBy(FoxSpacing.S)) {
            Text(stringResource(R.string.store_location), style = MaterialTheme.typography.titleMedium)
            val point = form.location
            val (text, isError) =
                when {
                    form.locating -> stringResource(R.string.store_location_locating) to false

                    form.locationProblem != null -> locationMessage(form.locationProblem) to true

                    point != null ->
                        stringResource(R.string.store_location_value, point.lat.coordinate(), point.lng.coordinate()) to
                            false

                    else -> stringResource(R.string.store_location_help) to (problem != null)
                }
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FoxOutlinedButton(
                text = stringResource(
                    if (point ==
                        null
                    ) {
                        R.string.store_location_use
                    } else {
                        R.string.store_location_update
                    },
                ),
                onClick = onLocate,
                enabled = enabled && !form.locating,
                leadingIcon = rememberVectorPainter(Icons.Rounded.MyLocation),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StatusContent(state: State.Status, onAction: (Action) -> Unit, onOpenStore: () -> Unit) {
    val store = state.store
    FoxFormColumn {
        FoxCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(FoxSpacing.M), verticalArrangement = Arrangement.spacedBy(FoxSpacing.S)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FoxSpacing.S),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(store.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${store.category.label()} · ${store.provinceName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StatusPill(statusLabel(store.status), statusTone(store.status))
                }
                Text(statusTitle(store), style = MaterialTheme.typography.titleMedium)
                statusMessage(store)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                store.rejectionReason?.takeIf { store.status == StoreStatus.Rejected }?.let {
                    Text(
                        stringResource(R.string.store_status_rejected_reason, it),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        when (store.status) {
            StoreStatus.Active, StoreStatus.Approved ->
                FoxButton(stringResource(R.string.store_open), onOpenStore, Modifier.fillMaxWidth())

            StoreStatus.Rejected ->
                FoxButton(stringResource(R.string.store_fix), {
                    onAction(Action.FixAndResubmit)
                }, Modifier.fillMaxWidth())

            else ->
                FoxButton(
                    text = stringResource(R.string.store_check_status),
                    onClick = { onAction(Action.Refresh) },
                    state = if (state.refreshing) FoxButtonState.Loading else FoxButtonState.Idle,
                    loadingDescription = stringResource(R.string.store_checking),
                    modifier = Modifier.fillMaxWidth(),
                )
        }
    }
}

@Composable
private fun statusTitle(store: MyStore): String =
    when (store.status) {
        StoreStatus.Submitted, StoreStatus.Draft -> stringResource(R.string.store_status_submitted_title)
        StoreStatus.Active, StoreStatus.Approved -> stringResource(R.string.store_status_active_title)
        StoreStatus.Rejected -> stringResource(R.string.store_status_rejected_title)
        else -> stringResource(R.string.store_status_other_title, store.status.name)
    }

@Composable
private fun statusMessage(store: MyStore): String? =
    when (store.status) {
        StoreStatus.Submitted, StoreStatus.Draft -> stringResource(
            R.string.store_status_submitted_message,
            store.provinceName,
        )

        StoreStatus.Active, StoreStatus.Approved -> stringResource(
            R.string.store_status_active_message,
            store.name,
            store.provinceName,
        )

        else -> null
    }

@Composable
private fun statusLabel(status: StoreStatus): String =
    when (status) {
        StoreStatus.Active, StoreStatus.Approved -> stringResource(R.string.store_status_active)
        StoreStatus.Rejected -> stringResource(R.string.store_status_rejected)
        StoreStatus.Submitted, StoreStatus.Draft -> stringResource(R.string.store_status_pending)
        else -> status.name
    }

private fun statusTone(status: StoreStatus): StatusTone =
    when (status) {
        StoreStatus.Active, StoreStatus.Approved -> StatusTone.Success
        StoreStatus.Rejected, StoreStatus.Suspended, StoreStatus.Closed -> StatusTone.Error
        StoreStatus.Submitted, StoreStatus.Draft -> StatusTone.Info
        else -> StatusTone.Neutral
    }

@Composable
internal fun StoreCategory.label(): String =
    stringResource(
        when (this) {
            StoreCategory.Food -> R.string.category_food
            StoreCategory.Groceries -> R.string.category_groceries
            StoreCategory.Medicine -> R.string.category_medicine
            StoreCategory.More -> R.string.category_more
        },
    )

@Composable
private fun locationMessage(result: LocationResult): String =
    stringResource(
        when (result) {
            LocationResult.PermissionDenied -> R.string.store_location_denied
            LocationResult.Disabled -> R.string.store_location_disabled
            is LocationResult.Found, LocationResult.Unavailable -> R.string.store_location_unavailable
        },
    )

private fun Double.coordinate(): String = String.format(Locale.US, "%.5f", this)
