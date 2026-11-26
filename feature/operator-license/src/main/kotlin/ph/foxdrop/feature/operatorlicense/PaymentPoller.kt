package ph.foxdrop.feature.operatorlicense

import javax.inject.Inject
import kotlinx.coroutines.delay
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.domain.operator.OperatorLicenseRepository
import ph.foxdrop.core.domain.operator.Payment
import ph.foxdrop.core.domain.operator.PaymentStatus

/** How a checkout ended, as far as the app can tell (docs/API.md "How a payment finishes"). */
sealed interface PaymentOutcome {
    data object Paid : PaymentOutcome

    /** Declined, cancelled or expired at the provider. */
    data object NotPaid : PaymentOutcome

    /** Still pending after the wait: the user may have closed the page, or the webhook is late. */
    data object StillPending : PaymentOutcome

    data class Error(val error: DomainError) : PaymentOutcome
}

/**
 * Polls `payments/{id}` every 2 s for up to 2 minutes while the user pays in the Custom Tab. Network blips are
 * retried until the time is up; only a final answer stops early.
 */
class PaymentPoller @Inject constructor(private val licenses: OperatorLicenseRepository) {
    suspend fun await(paymentId: String): PaymentOutcome {
        var lastError: DomainError? = null
        repeat(MAX_POLLS) {
            delay(POLL_INTERVAL_MS)
            val result = licenses.payment(paymentId)
            finalOutcome(result)?.let { return it }
            lastError = (result as? FoxResult.Failure)?.error
        }
        return lastError?.let(PaymentOutcome::Error) ?: PaymentOutcome.StillPending
    }

    /** The answer that ends polling, or null to poll again (still pending, or a network blip). */
    private fun finalOutcome(result: FoxResult<Payment>): PaymentOutcome? =
        when (result) {
            is FoxResult.Success ->
                when (result.value.status) {
                    PaymentStatus.Paid -> PaymentOutcome.Paid
                    PaymentStatus.Failed, PaymentStatus.Expired -> PaymentOutcome.NotPaid
                    PaymentStatus.Pending, PaymentStatus.Unknown -> null
                }

            is FoxResult.Failure -> if (result.error ==
                DomainError.Network
            ) {
                null
            } else {
                PaymentOutcome.Error(result.error)
            }
        }

    companion object {
        const val POLL_INTERVAL_MS = 2_000L
        const val MAX_POLLS = 60
    }
}
