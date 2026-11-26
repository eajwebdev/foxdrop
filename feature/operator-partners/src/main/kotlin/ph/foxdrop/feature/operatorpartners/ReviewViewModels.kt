package ph.foxdrop.feature.operatorpartners

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.designsystem.component.Loadable
import ph.foxdrop.core.domain.onboarding.Problem
import ph.foxdrop.core.domain.onboarding.RejectionReason
import ph.foxdrop.core.domain.partner.PartnerReviewRepository
import ph.foxdrop.core.domain.partner.RiderReview
import ph.foxdrop.core.domain.partner.StoreReview

/** The reject dialog: a reason the applicant will read. */
@Immutable
data class RejectDialog(val reason: String = "", val problem: Problem? = null)

@Immutable
data class ReviewState<T>(
    val review: Loadable<T> = Loadable.Loading,
    val deciding: Boolean = false,
    val rejectDialog: RejectDialog? = null,
    val message: DomainError? = null,
    /** Set after an approve/reject succeeded; the screen confirms it and the list will refresh. */
    val decided: Boolean = false,
)

/**
 * Approve or reject one application (§9.4 step 4). The server checks the reviewer runs this province (or is an
 * admin overriding) and records every decision in the audit log.
 */
abstract class ReviewViewModel<T>(private val id: String) : ViewModel() {
    private val _state = MutableStateFlow(ReviewState<T>())
    val state: StateFlow<ReviewState<T>> = _state.asStateFlow()

    protected abstract suspend fun fetch(id: String): FoxResult<T>

    protected abstract suspend fun approve(id: String): FoxResult<T>

    protected abstract suspend fun reject(id: String, reason: String): FoxResult<T>

    fun load() {
        _state.update { it.copy(review = Loadable.Loading) }
        viewModelScope.launch {
            val review =
                when (val result = fetch(id)) {
                    is FoxResult.Success -> Loadable.Ready(result.value)
                    is FoxResult.Failure -> Loadable.Failed(result.error)
                }
            _state.update { it.copy(review = review) }
        }
    }

    fun approve() = decide { approve(id) }

    fun openRejectDialog() = _state.update { it.copy(rejectDialog = RejectDialog()) }

    fun dismissRejectDialog() = _state.update { it.copy(rejectDialog = null) }

    fun onReasonChanged(reason: String) = _state.update {
        it.copy(rejectDialog = it.rejectDialog?.copy(reason = reason, problem = null))
    }

    fun confirmReject() {
        val dialog = state.value.rejectDialog ?: return
        val problem = RejectionReason.validate(dialog.reason)
        if (problem != null) {
            _state.update { it.copy(rejectDialog = dialog.copy(problem = problem)) }
            return
        }
        _state.update { it.copy(rejectDialog = null) }
        decide { reject(id, dialog.reason.trim()) }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    private fun decide(call: suspend () -> FoxResult<T>) {
        if (state.value.deciding) return
        _state.update { it.copy(deciding = true, message = null) }
        viewModelScope.launch {
            when (val result = call()) {
                is FoxResult.Success -> _state.update {
                    it.copy(deciding = false, review = Loadable.Ready(result.value), decided = true)
                }

                is FoxResult.Failure -> _state.update { it.copy(deciding = false, message = result.error) }
            }
        }
    }
}

@HiltViewModel
class StoreReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reviews: PartnerReviewRepository,
) : ReviewViewModel<StoreReview>(checkNotNull(savedStateHandle.get<String>("storeId"))) {
    init {
        load()
    }

    override suspend fun fetch(id: String) = reviews.store(id)

    override suspend fun approve(id: String) = reviews.approveStore(id)

    override suspend fun reject(id: String, reason: String) = reviews.rejectStore(id, reason)
}

@HiltViewModel
class RiderReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reviews: PartnerReviewRepository,
) : ReviewViewModel<RiderReview>(checkNotNull(savedStateHandle.get<String>("riderId"))) {
    init {
        load()
    }

    override suspend fun fetch(id: String) = reviews.rider(id)

    override suspend fun approve(id: String) = reviews.approveRider(id)

    override suspend fun reject(id: String, reason: String) = reviews.rejectRider(id, reason)
}
