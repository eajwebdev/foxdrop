package ph.foxdrop.feature.operatorlicense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.common.validation.PhMobileNumber
import ph.foxdrop.core.domain.onboarding.OperatorApplicationRules
import ph.foxdrop.core.domain.operator.ApplicationStatus
import ph.foxdrop.core.domain.operator.LicenseStatus
import ph.foxdrop.core.domain.operator.OperatorApplication
import ph.foxdrop.core.domain.operator.OperatorApplicationForm
import ph.foxdrop.core.domain.operator.OperatorLicenseRepository
import ph.foxdrop.core.domain.province.ProvinceRepository
import ph.foxdrop.core.media.ImageCompressor
import ph.foxdrop.feature.operatorlicense.OperatorLicenseAction as Action
import ph.foxdrop.feature.operatorlicense.OperatorLicenseState as State

/**
 * Becoming a provincial operator (§9.4 steps 1–2, DECISIONS 55): apply for an available province, wait for the
 * admin, then pay the ₱15,000 license. A paid license grants the operator role, so the app re-routes.
 */
@HiltViewModel
class OperatorLicenseViewModel @Inject constructor(
    private val licenses: OperatorLicenseRepository,
    private val provinces: ProvinceRepository,
    private val compressor: ImageCompressor,
    private val poller: PaymentPoller,
) : ViewModel() {
    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()
    private val _effects = Channel<OperatorLicenseEffect>(Channel.BUFFERED)
    private var refreshJob: Job? = null
    val effects: Flow<OperatorLicenseEffect> = _effects.receiveAsFlow()

    init {
        load()
    }

    @Suppress("CyclomaticComplexMethod") // one branch per user action
    fun onAction(action: Action) {
        when (action) {
            Action.Retry -> load()

            Action.Refresh -> refresh(quiet = false)

            Action.Poll -> refresh(quiet = true)

            is Action.ProvinceSelected -> editForm {
                copy(provinceId = action.provinceId).without(OperatorApplicationRules.Field.Province)
            }

            is Action.BusinessNameChanged -> editForm {
                copy(businessName = action.value).without(OperatorApplicationRules.Field.BusinessName)
            }

            is Action.RegistrationNumberChanged -> editForm { copy(registrationNumber = action.value) }

            is Action.ContactPhoneChanged ->
                editForm {
                    copy(
                        contactPhone = PhMobileNumber.nationalDigits(action.value),
                    ).without(OperatorApplicationRules.Field.ContactPhone)
                }

            is Action.ContactEmailChanged -> editForm {
                copy(contactEmail = action.value).without(OperatorApplicationRules.Field.ContactEmail)
            }

            is Action.PhotoPicked -> onPhoto(action)

            Action.Submit -> submit()

            Action.DismissMessage -> _state.update { if (it is State.Apply) it.copy(message = null) else it }

            Action.ApplyAgain -> (state.value as? State.Status)?.let { openForm(previous = it.application) }

            Action.Pay -> pay()
        }
    }

    private fun load() {
        _state.value = State.Loading
        viewModelScope.launch {
            when (val result = licenses.myApplication()) {
                is FoxResult.Failure -> _state.value = State.Failed(result.error)
                is FoxResult.Success -> show(result.value)
            }
        }
    }

    private suspend fun show(application: OperatorApplication?) {
        if (application ==
            null
        ) {
            openFormNow(previous = null)
        } else {
            _state.value = State.Status(application)
        }
    }

    private fun openForm(previous: OperatorApplication?) {
        _state.value = State.Loading
        viewModelScope.launch { openFormNow(previous) }
    }

    private suspend fun openFormNow(previous: OperatorApplication?) {
        _state.value =
            when (val result = provinces.directory()) {
                is FoxResult.Failure -> State.Failed(result.error)

                is FoxResult.Success -> {
                    val available = result.value.filter { it.isAvailable }.sortedBy { it.name }.toImmutableList()
                    State.Apply(ApplyForm(provinces = available), previous = previous)
                }
            }
    }

    private fun refresh(quiet: Boolean) {
        val current = state.value as? State.Status ?: return if (quiet) Unit else load()
        if (quiet && refreshJob?.isActive == true) return // the last check is still on its way
        if (!quiet) {
            refreshJob?.cancel() // a manual refresh wins over a background check still on its way
            _state.value = current.copy(refreshing = true)
        }
        refreshJob = viewModelScope.launch {
            when (val result = licenses.myApplication()) {
                is FoxResult.Failure -> _state.update { (it as? State.Status)?.copy(refreshing = false) ?: it }

                is FoxResult.Success -> {
                    val application = result.value ?: return@launch openFormNow(previous = null)
                    // Update the latest state: the payment poller may have changed it while this was loading.
                    _state.update {
                        (it as? State.Status)?.copy(
                            application = application,
                            refreshing = false,
                        )
                            ?: it
                    }
                    if (application.license?.status ==
                        LicenseStatus.Active
                    ) {
                        _effects.send(OperatorLicenseEffect.RolesChanged)
                    }
                }
            }
        }
    }

    private fun editForm(change: ApplyForm.() -> ApplyForm) {
        _state.update {
            if (it is State.Apply &&
                !it.submitting
            ) {
                it.copy(form = it.form.change(), message = null)
            } else {
                it
            }
        }
    }

    private fun onPhoto(action: Action.PhotoPicked) {
        val uri = action.uri ?: return
        viewModelScope.launch {
            val fileName = if (action.slot == DocumentSlot.ValidId) "valid_id.jpg" else "business_permit.jpg"
            val photo = compressor.compress(uri, fileName)
            if (photo == null) {
                _effects.send(OperatorLicenseEffect.PhotoUnreadable)
                return@launch
            }
            editForm {
                when (action.slot) {
                    DocumentSlot.ValidId -> copy(validId = photo).without(OperatorApplicationRules.Field.ValidId)
                    DocumentSlot.BusinessPermit -> copy(businessPermit = photo)
                }
            }
        }
    }

    private fun submit() {
        val current = state.value as? State.Apply ?: return
        if (current.submitting) return
        val form = current.form
        val errors =
            OperatorApplicationRules.validate(
                form.provinceId,
                form.businessName,
                form.contactPhone,
                form.contactEmail,
                form.validId != null,
            )
        if (errors.isNotEmpty()) {
            _state.value = current.copy(form = form.copy(errors = errors))
            return
        }
        _state.value = current.copy(submitting = true, message = null)
        viewModelScope.launch {
            val request =
                OperatorApplicationForm(
                    provinceId = checkNotNull(form.provinceId),
                    businessName = form.businessName,
                    registrationNumber = form.registrationNumber.ifBlank { null },
                    contactPhone = checkNotNull(PhMobileNumber.normalize(form.contactPhone)),
                    contactEmail = form.contactEmail.ifBlank { null },
                    validId = checkNotNull(form.validId),
                    businessPermit = form.businessPermit,
                )
            _state.value =
                when (val result = licenses.submitApplication(request)) {
                    is FoxResult.Success -> State.Status(result.value)
                    is FoxResult.Failure -> current.copy(submitting = false, message = result.error)
                }
        }
    }

    private fun pay() {
        val current = state.value as? State.Status ?: return
        val license = current.application.license ?: return
        if (current.pay == PayState.Starting || current.pay == PayState.Waiting || !license.canPay) return
        _state.value = current.copy(pay = PayState.Starting)
        viewModelScope.launch {
            when (val started = licenses.startPayment(license.id)) {
                is FoxResult.Failure -> setPay(PayState.Failed(started.error))

                is FoxResult.Success -> {
                    _effects.send(OperatorLicenseEffect.OpenCheckout(started.value.checkoutUrl))
                    setPay(PayState.Waiting)
                    when (val outcome = poller.await(started.value.id)) {
                        PaymentOutcome.Paid -> {
                            setPay(PayState.Idle)
                            refresh(quiet = false)
                        }

                        PaymentOutcome.NotPaid -> setPay(PayState.NotPaid)

                        PaymentOutcome.StillPending -> setPay(PayState.StillPending)

                        is PaymentOutcome.Error -> setPay(PayState.Failed(outcome.error))
                    }
                }
            }
        }
    }

    private fun setPay(pay: PayState) {
        _state.update { (it as? State.Status)?.copy(pay = pay) ?: it }
    }

    private fun ApplyForm.without(field: OperatorApplicationRules.Field): ApplyForm = copy(errors = errors - field)
}

/** Whether this application can lead to a license right now. */
internal val OperatorApplication.isClosed: Boolean
    get() = status == ApplicationStatus.Rejected || status == ApplicationStatus.Withdrawn
