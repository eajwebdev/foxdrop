package ph.foxdrop.feature.operatorpartners

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
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

/**
 * A review list of one partner kind. Refreshed on every resume, so a partner reviewed on the detail screen moves lists
 * at once; a newer refresh (resume or filter tap) cancels the one still running.
 */
abstract class PartnerListViewModel<T> : ViewModel() {
    private val _state = MutableStateFlow(PartnerListState<T>())
    val state: StateFlow<PartnerListState<T>> = _state.asStateFlow()
    private var loading: Job? = null

    protected abstract suspend fun fetch(filter: PartnerFilter): FoxResult<List<T>>

    fun select(filter: PartnerFilter) {
        _state.update { it.copy(filter = filter) }
        refresh()
    }

    fun refresh() {
        val filter = state.value.filter
        if (state.value.items !is Loadable.Ready) _state.update { it.copy(items = Loadable.Loading) }
        loading?.cancel()
        loading =
            viewModelScope.launch {
                val items =
                    when (val result = fetch(filter)) {
                        is FoxResult.Success -> Loadable.Ready(result.value.toImmutableList())
                        is FoxResult.Failure -> Loadable.Failed(result.error)
                    }
                _state.update { if (it.filter == filter) it.copy(items = items) else it }
            }
    }
}

/** The province's stores by review status. */
@HiltViewModel
class StoreListViewModel @Inject constructor(private val reviews: PartnerReviewRepository) :
    PartnerListViewModel<StoreSummary>() {
    override suspend fun fetch(filter: PartnerFilter) = reviews.stores(filter.storeStatus())
}

/** The province's riders by review status. */
@HiltViewModel
class RiderListViewModel @Inject constructor(private val reviews: PartnerReviewRepository) :
    PartnerListViewModel<RiderSummary>() {
    override suspend fun fetch(filter: PartnerFilter) = reviews.riders(filter.riderStatus())
}
