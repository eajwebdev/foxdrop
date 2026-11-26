package ph.foxdrop.business.start

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.designsystem.component.Loadable
import ph.foxdrop.core.domain.operator.OperatorApplication
import ph.foxdrop.core.domain.operator.OperatorLicenseRepository
import ph.foxdrop.core.domain.partner.MyStore
import ph.foxdrop.core.domain.partner.StoreOnboardingRepository

/** What this account has already started: a store application and/or an operator application. */
@Immutable
data class GetStartedData(val store: MyStore?, val application: OperatorApplication?)

/**
 * The business app's home until a store or operator role is granted (DECISIONS 64). Shows both paths with the
 * status of anything already submitted.
 */
@HiltViewModel
class GetStartedViewModel @Inject constructor(
    private val stores: StoreOnboardingRepository,
    private val licenses: OperatorLicenseRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<Loadable<GetStartedData>>(Loadable.Loading)
    val state: StateFlow<Loadable<GetStartedData>> = _state.asStateFlow()

    /** Called on every resume, so a status changed on another screen shows at once. */
    fun refresh() {
        if (state.value !is Loadable.Ready) _state.value = Loadable.Loading
        viewModelScope.launch {
            val store = async { stores.myStores() }
            val application = async { licenses.myApplication() }
            val storeResult = store.await()
            val applicationResult = application.await()
            _state.value =
                when {
                    storeResult is FoxResult.Failure -> Loadable.Failed(storeResult.error)

                    applicationResult is FoxResult.Failure -> Loadable.Failed(applicationResult.error)

                    else ->
                        Loadable.Ready(
                            GetStartedData(
                                // stores/mine lists the newest store first
                                store = (storeResult as FoxResult.Success).value.firstOrNull(),
                                application = (applicationResult as FoxResult.Success).value,
                            ),
                        )
                }
        }
    }
}
