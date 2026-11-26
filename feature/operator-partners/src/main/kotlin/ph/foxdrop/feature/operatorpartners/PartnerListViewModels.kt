package ph.foxdrop.feature.operatorpartners

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.designsystem.component.Loadable
import ph.foxdrop.core.domain.partner.PartnerReviewRepository
import ph.foxdrop.core.domain.partner.RiderStatus
import ph.foxdrop.core.domain.partner.RiderSummary
import ph.foxdrop.core.domain.partner.StoreStatus
import ph.foxdrop.core.domain.partner.StoreSummary

/** Which list the operator is looking at. Pending first: that is the work to do (§9.4 step 4). */
enum class PartnerFilter { Pending, Approved, Rejected }

@Immutable
data class PartnerListState<T>(
    val filter: PartnerFilter = PartnerFilter.Pending,
    val items: Loadable<ImmutableList<T>> = Loadable.Loading,
)

internal fun PartnerFilter.storeStatus(): StoreStatus =
    when (this) {
        PartnerFilter.Pending -> StoreStatus.Submitted
        PartnerFilter.Approved -> StoreStatus.Active
        PartnerFilter.Rejected -> StoreStatus.Rejected
    }

internal fun PartnerFilter.riderStatus(): RiderStatus =
    when (this) {
        PartnerFilter.Pending -> RiderStatus.DocsReview
        PartnerFilter.Approved -> RiderStatus.Approved
        PartnerFilter.Rejected -> RiderStatus.Rejected
    }

/** The province's stores by review status. */
@HiltViewModel
class StoreListViewModel @Inject constructor(private val reviews: PartnerReviewRepository) : ViewModel() {
    private val _state = MutableStateFlow(PartnerListState<StoreSummary>())
    val state: StateFlow<PartnerListState<StoreSummary>> = _state.asStateFlow()

    fun select(filter: PartnerFilter) {
        _state.update { it.copy(filter = filter) }
        refresh()
    }

    /** Called on every resume, so a store reviewed on the detail screen moves lists at once. */
    fun refresh() {
        val filter = state.value.filter
        if (state.value.items !is Loadable.Ready) _state.update { it.copy(items = Loadable.Loading) }
        viewModelScope.launch {
            val items =
                when (val result = reviews.stores(filter.storeStatus())) {
                    is FoxResult.Success -> Loadable.Ready(result.value.toImmutableList())
                    is FoxResult.Failure -> Loadable.Failed(result.error)
                }
            _state.update { if (it.filter == filter) it.copy(items = items) else it }
        }
    }
}

/** The province's riders by review status. */
@HiltViewModel
class RiderListViewModel @Inject constructor(private val reviews: PartnerReviewRepository) : ViewModel() {
    private val _state = MutableStateFlow(PartnerListState<RiderSummary>())
    val state: StateFlow<PartnerListState<RiderSummary>> = _state.asStateFlow()

    fun select(filter: PartnerFilter) {
        _state.update { it.copy(filter = filter) }
        refresh()
    }

    fun refresh() {
        val filter = state.value.filter
        if (state.value.items !is Loadable.Ready) _state.update { it.copy(items = Loadable.Loading) }
        viewModelScope.launch {
            val items =
                when (val result = reviews.riders(filter.riderStatus())) {
                    is FoxResult.Success -> Loadable.Ready(result.value.toImmutableList())
                    is FoxResult.Failure -> Loadable.Failed(result.error)
                }
            _state.update { if (it.filter == filter) it.copy(items = items) else it }
        }
    }
}
