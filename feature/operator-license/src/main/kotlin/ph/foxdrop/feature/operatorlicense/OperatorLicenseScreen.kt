package ph.foxdrop.feature.operatorlicense

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import ph.foxdrop.core.common.format.formatPeso
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
import ph.foxdrop.core.designsystem.theme.FoxLayout
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.designsystem.theme.FoxTheme
import ph.foxdrop.core.domain.onboarding.OperatorApplicationRules.Field
import ph.foxdrop.core.domain.operator.ApplicationStatus
import ph.foxdrop.core.domain.operator.LicenseStatus
import ph.foxdrop.core.domain.operator.OperatorApplication
import ph.foxdrop.core.media.PhotoSlot
import ph.foxdrop.core.media.R as MediaR
import ph.foxdrop.core.media.rememberPhotoPicker
import ph.foxdrop.core.model.Money
import ph.foxdrop.core.payments.CheckoutLauncher
import ph.foxdrop.feature.operatorlicense.OperatorLicenseAction as Action
import ph.foxdrop.feature.operatorlicense.OperatorLicenseState as State

/** How often the status screen re-checks while the admin decides (DECISIONS 80): every 10 s, slowing to 60 s. */
internal const val STATUS_POLL_MILLIS = 10_000L
internal const val STATUS_POLL_MAX_MILLIS = 60_000L

/**
 * "Become a provincial operator" (§9.4 steps 1–2): the application form, then its status and the license payment.
 * [onRolesChanged] runs when the paid license made this account an operator.
 */
@Composable
fun OperatorLicenseRoute(
    onBack: () -> Unit,
    onRolesChanged: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OperatorLicenseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val unreadable = stringResource(MediaR.string.media_photo_unreadable)
    val currentOnRolesChanged by rememberUpdatedState(onRolesChanged)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is OperatorLicenseEffect.OpenCheckout -> CheckoutLauncher.open(context, effect.url)
                OperatorLicenseEffect.RolesChanged -> currentOnRolesChanged()
                OperatorLicenseEffect.PhotoUnreadable -> snackbar.showSnackbar(unreadable)
            }
        }
    }
    // While the admin decides (or records an offline payment), check again every few seconds, only while visible.
    val waitingForAdmin = (state as? State.Status)?.waitingForAdmin == true
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(waitingForAdmin, lifecycleOwner) {
        if (waitingForAdmin) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Quick at first (an admin often decides while the applicant watches), then gently slower.
                var wait = STATUS_POLL_MILLIS
                while (true) {
                    delay(wait)
                    viewModel.onAction(Action.Poll)
                    wait = (wait * 3 / 2).coerceAtMost(STATUS_POLL_MAX_MILLIS)
                }
            }
        }
    }
    val validIdPicker = rememberPhotoPicker { viewModel.onAction(Action.PhotoPicked(DocumentSlot.ValidId, it)) }
    val permitPicker = rememberPhotoPicker { viewModel.onAction(Action.PhotoPicked(DocumentSlot.BusinessPermit, it)) }
    OperatorLicenseScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        onTakePhoto = { slot ->
            if (slot ==
                DocumentSlot.ValidId
            ) {
                validIdPicker.takePhoto()
            } else {
                permitPicker.takePhoto()
            }
        },
        onChoosePhoto = { slot ->
            if (slot == DocumentSlot.ValidId) validIdPicker.chooseFromGallery() else permitPicker.chooseFromGallery()
        },
        snackbarHostState = snackbar,
        modifier = modifier,
    )
}

@Composable
fun OperatorLicenseScreen(
    state: State,
    onAction: (Action) -> Unit,
    onBack: () -> Unit,
    onTakePhoto: (DocumentSlot) -> Unit,
    onChoosePhoto: (DocumentSlot) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val motion = FoxTheme.motion
    FoxScreenScaffold(
        title = stringResource(R.string.license_title),
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    ) { padding ->
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = { fadeIn(motion.enter()) togetherWith fadeOut(motion.exit()) },
            label = "operatorLicense",
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { current ->
            when (current) {
                State.Loading ->
                    SkeletonList(stringResource(R.string.license_title), Modifier.fillMaxSize().padding(FoxSpacing.L))

                is State.Failed ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorState(
                            title = stringResource(R.string.license_error_title),
                            message = errorMessage(current.error),
                            retryLabel = stringResource(R.string.license_check_status),
                            onRetry = { onAction(Action.Retry) },
                        )
                    }

                is State.Apply -> ApplyContent(current, onAction, onTakePhoto, onChoosePhoto)

                is State.Status -> StatusContent(current, onAction)
            }
        }
    }
}

@Composable
private fun ApplyContent(
    state: State.Apply,
    onAction: (Action) -> Unit,
    onTakePhoto: (DocumentSlot) -> Unit,
    onChoosePhoto: (DocumentSlot) -> Unit,
) {
    val form = state.form
    if (form.provinces.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Rounded.Map,
                title = stringResource(R.string.license_no_provinces_title),
                message = stringResource(R.string.license_no_provinces_message),
            )
        }
        return
    }
    val enabled = !state.submitting
    FoxFormColumn {
        Text(stringResource(R.string.license_apply_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.license_apply_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.previous?.rejectionReason?.let { reason ->
            FoxNotice(stringResource(R.string.license_apply_again_note, reason), StatusTone.Warning)
        }
        FoxDropdown(
            label = stringResource(R.string.license_province),
            placeholder = stringResource(R.string.license_province_hint),
            options = form.provinces.map { DropdownOption(it.id, it.name) }.toImmutableList(),
            selectedId = form.provinceId,
            onSelect = { onAction(Action.ProvinceSelected(it)) },
            errorMessage = problemMessage(form.errors[Field.Province]),
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        FoxTextField(
            value = form.businessName,
            onValueChange = { onAction(Action.BusinessNameChanged(it)) },
            label = stringResource(R.string.license_business_name),
            errorMessage = problemMessage(form.errors[Field.BusinessName]),
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        FoxTextField(
            value = form.registrationNumber,
            onValueChange = { onAction(Action.RegistrationNumberChanged(it)) },
            label = stringResource(R.string.license_registration_number),
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        FoxTextField(
            value = form.contactPhone,
            onValueChange = { onAction(Action.ContactPhoneChanged(it)) },
            label = stringResource(R.string.license_contact_phone),
            prefix = "+63 ",
            errorMessage = problemMessage(form.errors[Field.ContactPhone]),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        FoxTextField(
            value = form.contactEmail,
            onValueChange = { onAction(Action.ContactEmailChanged(it)) },
            label = stringResource(R.string.license_contact_email),
            errorMessage = problemMessage(form.errors[Field.ContactEmail]),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.license_documents), style = MaterialTheme.typography.titleMedium)
        PhotoSlot(
            label = stringResource(R.string.license_valid_id),
            photo = form.validId,
            onTakePhoto = { onTakePhoto(DocumentSlot.ValidId) },
            onChoosePhoto = { onChoosePhoto(DocumentSlot.ValidId) },
            errorMessage = problemMessage(form.errors[Field.ValidId]),
            enabled = enabled,
        )
        PhotoSlot(
            label = stringResource(R.string.license_business_permit),
            photo = form.businessPermit,
            onTakePhoto = { onTakePhoto(DocumentSlot.BusinessPermit) },
            onChoosePhoto = { onChoosePhoto(DocumentSlot.BusinessPermit) },
            enabled = enabled,
        )
        AnimatedVisibility(visible = state.message != null) {
            state.message?.let { FoxNotice(errorMessage(it), StatusTone.Error) }
        }
        FoxButton(
            text = stringResource(R.string.license_submit),
            onClick = { onAction(Action.Submit) },
            state = if (state.submitting) FoxButtonState.Loading else FoxButtonState.Idle,
            loadingDescription = stringResource(R.string.license_submitting),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StatusContent(state: State.Status, onAction: (Action) -> Unit) {
    val application = state.application
    val license = state.application.license
    FoxFormColumn {
        FoxCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(FoxSpacing.M), verticalArrangement = Arrangement.spacedBy(FoxSpacing.S)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FoxSpacing.S),
                ) {
                    Text(
                        application.provinceName,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    StatusPill(applicationLabel(application.status), applicationTone(application.status))
                }
                Text(application.businessName, style = MaterialTheme.typography.bodyMedium)
                Text(
                    statusMessage(application),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                application.rejectionReason?.let {
                    Text(
                        stringResource(R.string.license_rejected_reason, it),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                license?.let {
                    Text(
                        stringResource(R.string.license_fee_per_year, Money(it.feeCentavos).formatPeso()),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        PayNotice(state.pay)
        val canPay = license != null && license.canPay && license.status == LicenseStatus.PendingPayment
        if (canPay) {
            FoxButton(
                text = stringResource(R.string.license_pay, Money(license.feeCentavos).formatPeso()),
                onClick = { onAction(Action.Pay) },
                state = if (state.pay == PayState.Starting ||
                    state.pay == PayState.Waiting
                ) {
                    FoxButtonState.Loading
                } else {
                    FoxButtonState.Idle
                },
                loadingDescription = stringResource(R.string.license_paying),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (application.isClosed) {
            FoxButton(text = stringResource(R.string.license_apply_again), onClick = {
                onAction(Action.ApplyAgain)
            }, modifier = Modifier.fillMaxWidth())
        } else {
            FoxOutlinedButton(
                text = stringResource(R.string.license_check_status),
                onClick = { onAction(Action.Refresh) },
                enabled = !state.refreshing,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun PayNotice(pay: PayState) {
    when (pay) {
        PayState.Idle, PayState.Starting -> Unit
        PayState.Waiting -> FoxNotice(stringResource(R.string.license_paying_message), StatusTone.Info)
        PayState.NotPaid -> FoxNotice(stringResource(R.string.license_not_paid), StatusTone.Error)
        PayState.StillPending -> FoxNotice(stringResource(R.string.license_still_pending), StatusTone.Warning)
        is PayState.Failed -> FoxNotice(errorMessage(pay.error), StatusTone.Error)
    }
}

@Composable
private fun applicationLabel(status: ApplicationStatus): String =
    stringResource(
        when (status) {
            ApplicationStatus.Submitted, ApplicationStatus.Unknown -> R.string.license_status_submitted
            ApplicationStatus.Approved -> R.string.license_status_approved
            ApplicationStatus.Rejected -> R.string.license_status_rejected
            ApplicationStatus.Withdrawn -> R.string.license_status_withdrawn
        },
    )

private fun applicationTone(status: ApplicationStatus): StatusTone =
    when (status) {
        ApplicationStatus.Submitted, ApplicationStatus.Unknown -> StatusTone.Info
        ApplicationStatus.Approved -> StatusTone.Success
        ApplicationStatus.Rejected -> StatusTone.Error
        ApplicationStatus.Withdrawn -> StatusTone.Neutral
    }

@Composable
private fun statusMessage(application: OperatorApplication): String =
    when (application.status) {
        ApplicationStatus.Approved -> stringResource(R.string.license_approved_message, application.provinceName)

        ApplicationStatus.Rejected, ApplicationStatus.Withdrawn ->
            stringResource(R.string.license_rejected_message, application.provinceName)

        ApplicationStatus.Submitted, ApplicationStatus.Unknown ->
            stringResource(R.string.license_submitted_message, application.provinceName)
    }
