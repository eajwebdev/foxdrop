package ph.foxdrop.feature.rideronboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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
import ph.foxdrop.core.domain.onboarding.RiderApplicationRules.Field
import ph.foxdrop.core.domain.partner.MyRider
import ph.foxdrop.core.domain.partner.RiderStatus
import ph.foxdrop.core.domain.partner.VehicleType
import ph.foxdrop.core.media.PhotoSlot
import ph.foxdrop.core.media.rememberPhotoPicker
import ph.foxdrop.feature.rideronboarding.RiderOnboardingAction as Action
import ph.foxdrop.feature.rideronboarding.RiderOnboardingState as State

/**
 * The rider app's home until the rider role is granted (DECISIONS 64). [onRolesChanged] runs when an approved rider
 * starts delivering: the app re-resolves its home and lands on the rider home.
 */
@Composable
fun RiderOnboardingRoute(
    onRolesChanged: () -> Unit,
    modifier: Modifier = Modifier,
    onSignOut: (() -> Unit)? = null,
    viewModel: RiderOnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val unreadable = stringResource(R.string.rider_photo_unreadable)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                RiderOnboardingEffect.PhotoUnreadable -> snackbar.showSnackbar(unreadable)
            }
        }
    }
    var pending by rememberSaveable { mutableStateOf<Field?>(null) }
    val currentViewModel by rememberUpdatedState(viewModel)
    val picker = rememberPhotoPicker { uri -> pending?.let { currentViewModel.onAction(Action.PhotoPicked(it, uri)) } }
    RiderOnboardingScreen(
        state = state,
        onAction = viewModel::onAction,
        onStartDelivering = onRolesChanged,
        onTakePhoto = {
            pending = it
            picker.takePhoto()
        },
        onChoosePhoto = {
            pending = it
            picker.chooseFromGallery()
        },
        onSignOut = onSignOut,
        snackbarHostState = snackbar,
        modifier = modifier,
    )
}

@Suppress("LongParameterList") // stateless for previews and screenshot tests
@Composable
fun RiderOnboardingScreen(
    state: State,
    onAction: (Action) -> Unit,
    onStartDelivering: () -> Unit,
    onTakePhoto: (Field) -> Unit,
    onChoosePhoto: (Field) -> Unit,
    modifier: Modifier = Modifier,
    onSignOut: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val motion = FoxTheme.motion
    FoxScreenScaffold(
        title = stringResource(R.string.rider_title),
        onBack = null,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        actions = {
            if (onSignOut != null) TextButton(onClick = onSignOut) { Text(stringResource(R.string.rider_sign_out)) }
        },
    ) { padding ->
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = { fadeIn(motion.enter()) togetherWith fadeOut(motion.exit()) },
            label = "riderOnboarding",
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { current ->
            when (current) {
                State.Loading -> SkeletonList(
                    stringResource(R.string.rider_title),
                    Modifier.fillMaxSize().padding(FoxSpacing.L),
                )

                is State.Failed ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorState(
                            title = stringResource(R.string.rider_error_title),
                            message = errorMessage(current.error),
                            retryLabel = stringResource(DesignR.string.action_retry),
                            onRetry = { onAction(Action.Retry) },
                        )
                    }

                is State.Apply -> ApplyContent(current, onAction, onTakePhoto, onChoosePhoto)

                is State.Status -> StatusContent(current, onAction, onStartDelivering)
            }
        }
    }
}

@Composable
private fun ApplyContent(
    state: State.Apply,
    onAction: (Action) -> Unit,
    onTakePhoto: (Field) -> Unit,
    onChoosePhoto: (Field) -> Unit,
) {
    val form = state.form
    if (form.provinces.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Rounded.TwoWheeler,
                title = stringResource(R.string.rider_no_provinces_title),
                message = stringResource(R.string.rider_no_provinces_message),
            )
        }
        return
    }
    val enabled = !state.submitting
    val photo: @Composable (Field, Int) -> Unit = { field, label ->
        PhotoSlot(
            label = stringResource(label),
            photo = form.photos[field],
            onTakePhoto = { onTakePhoto(field) },
            onChoosePhoto = { onChoosePhoto(field) },
            errorMessage = problemMessage(form.errors[field]),
            enabled = enabled,
        )
    }
    FoxFormColumn {
        Text(stringResource(R.string.rider_apply_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.rider_apply_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.previous?.rejectionReason?.let {
            FoxNotice(stringResource(R.string.rider_apply_again_note, it), StatusTone.Warning)
        }

        ChecklistSection(R.string.rider_step_personal, form.isComplete(ChecklistStep.PersonalDetails)) {
            FoxDropdown(
                label = stringResource(R.string.rider_province),
                placeholder = stringResource(R.string.rider_province_hint),
                options = form.provinces.map { DropdownOption(it.id, it.name) }.toImmutableList(),
                selectedId = form.provinceId,
                onSelect = { onAction(Action.ProvinceSelected(it)) },
                errorMessage = problemMessage(form.errors[Field.Province]),
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            RiderTextField(
                form.fullName,
                R.string.rider_full_name,
                form.errors[Field.FullName],
                enabled,
                KeyboardCapitalization.Words,
            ) {
                onAction(Action.FullNameChanged(it))
            }
            photo(Field.Selfie, R.string.rider_selfie)
        }
        ChecklistSection(R.string.rider_step_valid_id, form.isComplete(ChecklistStep.ValidId)) {
            photo(Field.ValidId, R.string.rider_valid_id)
        }
        ChecklistSection(R.string.rider_step_license, form.isComplete(ChecklistStep.DriversLicense)) {
            RiderTextField(
                form.licenseNo,
                R.string.rider_license_no,
                form.errors[Field.LicenseNo],
                enabled,
                KeyboardCapitalization.Characters,
            ) {
                onAction(Action.LicenseNoChanged(it))
            }
            ExpiryField(form.licenseExpiry, form.errors[Field.LicenseExpiry], enabled) {
                onAction(Action.LicenseExpiryChanged(it))
            }
            photo(Field.DriversLicense, R.string.rider_license_photo)
        }
        ChecklistSection(R.string.rider_step_vehicle, form.isComplete(ChecklistStep.VehicleDetails)) {
            VehicleChips(form.vehicle, enabled) { onAction(Action.VehicleSelected(it)) }
            if (form.needsVehiclePapers) {
                RiderTextField(
                    form.plateNo,
                    R.string.rider_plate,
                    form.errors[Field.PlateNo],
                    enabled,
                    KeyboardCapitalization.Characters,
                ) {
                    onAction(Action.PlateChanged(it))
                }
                photo(Field.OrCr, R.string.rider_or_cr)
                photo(Field.VehiclePhoto, R.string.rider_vehicle_photo)
            } else {
                Text(
                    stringResource(R.string.rider_bicycle_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AnimatedVisibility(visible = state.message != null) {
            state.message?.let { FoxNotice(errorMessage(it), StatusTone.Error) }
        }
        FoxButton(
            text = stringResource(R.string.rider_submit),
            onClick = { onAction(Action.Submit) },
            state = if (state.submitting) FoxButtonState.Loading else FoxButtonState.Idle,
            loadingDescription = stringResource(R.string.rider_submitting),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** One checklist part: ticked once everything in it is filled in. */
@Composable
private fun ChecklistSection(title: Int, done: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val name = stringResource(title)
    val status = stringResource(if (done) R.string.rider_step_done else R.string.rider_step_todo, name)
    FoxCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(FoxSpacing.M), verticalArrangement = Arrangement.spacedBy(FoxSpacing.S)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FoxSpacing.S),
                modifier = Modifier.semantics(mergeDescendants = true) {
                    heading()
                    contentDescription = status
                },
            ) {
                Icon(
                    imageVector = if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (done) FoxTheme.colors.success else MaterialTheme.colorScheme.outline,
                )
                Text(name, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Composable
private fun RiderTextField(
    value: String,
    label: Int,
    problem: Problem?,
    enabled: Boolean,
    capitalization: KeyboardCapitalization,
    onChange: (String) -> Unit,
) {
    FoxTextField(
        value = value,
        onValueChange = onChange,
        label = stringResource(label),
        errorMessage = problemMessage(problem),
        keyboardOptions = KeyboardOptions(capitalization = capitalization),
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VehicleChips(selected: VehicleType, enabled: Boolean, onSelect: (VehicleType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(FoxSpacing.XS)) {
        Text(stringResource(R.string.rider_vehicle), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(FoxSpacing.S)) {
            VehicleType.entries.forEach { vehicle ->
                FilterChip(selected = vehicle == selected, onClick = {
                    onSelect(vehicle)
                }, label = { Text(vehicle.label()) }, enabled = enabled)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpiryField(expiry: LocalDate?, problem: Problem?, enabled: Boolean, onPicked: (LocalDate) -> Unit) {
    var picking by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(FoxSpacing.XXS)) {
        Text(stringResource(R.string.rider_license_expiry), style = MaterialTheme.typography.labelLarge)
        FoxOutlinedButton(
            text =
                expiry?.let { stringResource(R.string.rider_license_expiry_value, it.toString()) }
                    ?: stringResource(R.string.rider_license_expiry_pick),
            onClick = { picking = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        problemMessage(problem)?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
    if (picking) {
        val today = remember { manilaToday() }
        val pickerState =
            rememberDatePickerState(
                initialSelectedDateMillis = expiry?.toUtcMillis(),
                selectableDates =
                    object : SelectableDates {
                        override fun isSelectableDate(
                            utcTimeMillis: Long,
                        ): Boolean = utcTimeMillis.toLocalDate().isAfter(today)

                        override fun isSelectableYear(year: Int): Boolean = year >= today.year
                    },
            )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onPicked(it.toLocalDate()) }
                    picking = false
                }) { Text(stringResource(R.string.rider_date_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) { Text(stringResource(R.string.rider_date_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun StatusContent(state: State.Status, onAction: (Action) -> Unit, onStartDelivering: () -> Unit) {
    val rider = state.rider
    FoxFormColumn {
        FoxCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(FoxSpacing.M), verticalArrangement = Arrangement.spacedBy(FoxSpacing.S)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FoxSpacing.S),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(rider.provinceName, style = MaterialTheme.typography.titleLarge)
                        Text(
                            listOfNotNull(rider.vehicleType.label(), rider.plateNo).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val (label, tone) = statusPill(rider.status)
                    StatusPill(label, tone)
                }
                Text(statusTitle(rider.status), style = MaterialTheme.typography.titleMedium)
                statusMessage(rider)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (rider.status == RiderStatus.Rejected) {
                    rider.rejectionReason?.let {
                        Text(
                            stringResource(R.string.rider_status_rejected_reason, it),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                if (rider.licenseNoLast4 != null && rider.licenseExpiry != null) {
                    Text(
                        stringResource(
                            R.string.rider_license_summary,
                            rider.licenseNoLast4.orEmpty(),
                            rider.licenseExpiry.orEmpty(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        when (rider.status) {
            RiderStatus.Approved -> FoxButton(
                stringResource(R.string.rider_start),
                onStartDelivering,
                Modifier.fillMaxWidth(),
            )

            RiderStatus.Rejected -> FoxButton(stringResource(R.string.rider_fix), {
                onAction(Action.FixAndResubmit)
            }, Modifier.fillMaxWidth())

            else ->
                FoxButton(
                    text = stringResource(R.string.rider_check_status),
                    onClick = { onAction(Action.Refresh) },
                    state = if (state.refreshing) FoxButtonState.Loading else FoxButtonState.Idle,
                    loadingDescription = stringResource(R.string.rider_checking),
                    modifier = Modifier.fillMaxWidth(),
                )
        }
    }
}

@Composable
private fun statusTitle(status: RiderStatus): String =
    stringResource(
        when (status) {
            RiderStatus.Applied, RiderStatus.DocsReview, RiderStatus.Unknown -> R.string.rider_status_pending_title
            RiderStatus.Approved -> R.string.rider_status_approved_title
            RiderStatus.Rejected -> R.string.rider_status_rejected_title
            RiderStatus.Suspended, RiderStatus.Deactivated -> R.string.rider_status_inactive_title
        },
    )

@Composable
private fun statusMessage(rider: MyRider): String? =
    when (rider.status) {
        RiderStatus.Applied, RiderStatus.DocsReview, RiderStatus.Unknown -> stringResource(
            R.string.rider_status_pending_message,
            rider.provinceName,
        )

        RiderStatus.Approved -> stringResource(R.string.rider_status_approved_message, rider.provinceName)

        RiderStatus.Suspended, RiderStatus.Deactivated -> stringResource(
            R.string.rider_status_inactive_message,
            rider.provinceName,
        )

        RiderStatus.Rejected -> null
    }

@Composable
private fun statusPill(status: RiderStatus): Pair<String, StatusTone> =
    when (status) {
        RiderStatus.Applied, RiderStatus.DocsReview, RiderStatus.Unknown -> stringResource(
            R.string.rider_status_pending,
        ) to
            StatusTone.Info

        RiderStatus.Approved -> stringResource(R.string.rider_status_approved) to StatusTone.Success

        RiderStatus.Rejected -> stringResource(R.string.rider_status_rejected) to StatusTone.Error

        RiderStatus.Suspended, RiderStatus.Deactivated -> stringResource(R.string.rider_status_inactive) to
            StatusTone.Warning
    }

@Composable
internal fun VehicleType.label(): String =
    stringResource(
        when (this) {
            VehicleType.Motorcycle -> R.string.vehicle_motorcycle
            VehicleType.Bicycle -> R.string.vehicle_bicycle
            VehicleType.Tricycle -> R.string.vehicle_tricycle
            VehicleType.Car -> R.string.vehicle_car
        },
    )

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
