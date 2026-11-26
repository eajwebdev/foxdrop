package ph.foxdrop.feature.operatorlicense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.designsystem.component.Loadable
import ph.foxdrop.core.domain.operator.License
import ph.foxdrop.core.domain.operator.OperatorLicenseRepository

data class LicenseCardState(val license: Loadable<License?> = Loadable.Loading, val pay: PayState = PayState.Idle)

/** The operator dashboard's license card (§9.4 step 8): status, days left, Renew (₱15,000 / year). */
@HiltViewModel
class LicenseCardViewModel @Inject constructor(
    private val licenses: OperatorLicenseRepository,
    private val poller: PaymentPoller,
) : ViewModel() {
    private val _state = MutableStateFlow(LicenseCardState())
    val state: StateFlow<LicenseCardState> = _state.asStateFlow()
    private val _checkout = Channel<String>(Channel.BUFFERED)

    /** Checkout pages to open in a Custom Tab. */
    val checkout: Flow<String> = _checkout.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(license = Loadable.Loading) }
        viewModelScope.launch {
            val license = licenses.myLicense().toLoadable()
            _state.update { it.copy(license = license) }
        }
    }

    fun renew() {
        val license = (state.value.license as? Loadable.Ready)?.value ?: return
        val busy = state.value.pay == PayState.Starting || state.value.pay == PayState.Waiting
        if (busy || !license.canPay) return
        _state.update { it.copy(pay = PayState.Starting) }
        viewModelScope.launch {
            when (val started = licenses.startPayment(license.id)) {
                is FoxResult.Failure -> _state.update { it.copy(pay = PayState.Failed(started.error)) }

                is FoxResult.Success -> {
                    _checkout.send(started.value.checkoutUrl)
                    _state.update { it.copy(pay = PayState.Waiting) }
                    val pay =
                        when (val outcome = poller.await(started.value.id)) {
                            PaymentOutcome.Paid -> PayState.Idle
                            PaymentOutcome.NotPaid -> PayState.NotPaid
                            PaymentOutcome.StillPending -> PayState.StillPending
                            is PaymentOutcome.Error -> PayState.Failed(outcome.error)
                        }
                    val refreshed = licenses.myLicense().toLoadable()
                    _state.update { it.copy(pay = pay, license = refreshed) }
                }
            }
        }
    }

    private fun FoxResult<License?>.toLoadable(): Loadable<License?> =
        when (this) {
            is FoxResult.Success -> Loadable.Ready(value)
            is FoxResult.Failure -> Loadable.Failed(error)
        }
}
