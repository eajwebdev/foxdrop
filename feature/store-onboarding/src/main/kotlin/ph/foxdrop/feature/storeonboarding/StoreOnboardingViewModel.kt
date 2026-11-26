package ph.foxdrop.feature.storeonboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
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
import ph.foxdrop.core.domain.onboarding.StoreApplicationRules
import ph.foxdrop.core.domain.onboarding.StoreApplicationRules.Field
import ph.foxdrop.core.domain.partner.MyStore
import ph.foxdrop.core.domain.partner.StoreApplicationForm
import ph.foxdrop.core.domain.partner.StoreOnboardingRepository
import ph.foxdrop.core.domain.province.ProvinceRepository
import ph.foxdrop.core.location.LocationClient
import ph.foxdrop.core.location.LocationResult
import ph.foxdrop.core.media.ImageCompressor
import ph.foxdrop.feature.storeonboarding.StoreOnboardingAction as Action
import ph.foxdrop.feature.storeonboarding.StoreOnboardingEffect as Effect
import ph.foxdrop.feature.storeonboarding.StoreOnboardingState as State

/**
 * Opening a store (§5.2): the application, then its review status. The province's operator approves it; once
 * ACTIVE the server grants the store role and the screen offers to open the store home.
 */
@HiltViewModel
class StoreOnboardingViewModel @Inject constructor(
    private val stores: StoreOnboardingRepository,
    private val provinces: ProvinceRepository,
    private val location: LocationClient,
    private val compressor: ImageCompressor,
) : ViewModel() {
    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()
    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects: Flow<Effect> = _effects.receiveAsFlow()

    init {
        load()
    }

    @Suppress("CyclomaticComplexMethod") // one branch per user action
    fun onAction(action: Action) {
        when (action) {
            Action.Retry -> load()

            Action.Refresh -> refresh()

            is Action.ProvinceSelected -> edit { copy(provinceId = action.provinceId).without(Field.Province) }

            is Action.NameChanged -> edit { copy(name = action.value).without(Field.Name) }

            is Action.CategorySelected -> edit { copy(category = action.category) }

            is Action.DescriptionChanged -> edit { copy(description = action.value) }

            is Action.AddressChanged -> edit { copy(addressLine = action.value).without(Field.Address) }

            is Action.BarangayChanged -> edit { copy(barangay = action.value) }

            is Action.CityChanged -> edit { copy(city = action.value).without(Field.City) }

            is Action.PhoneChanged -> edit {
                copy(phone = PhMobileNumber.nationalDigits(action.value)).without(Field.Phone)
            }

            Action.UseMyLocation -> locate()

            is Action.LocationPermissionResult ->
                if (action.granted) {
                    locate()
                } else {
                    edit {
                        copy(locating = false, locationProblem = LocationResult.PermissionDenied)
                    }
                }

            is Action.PhotoPicked -> onPhoto(action)

            Action.Submit -> submit()

            Action.DismissMessage -> _state.update { if (it is State.Apply) it.copy(message = null) else it }

            Action.FixAndResubmit -> (state.value as? State.Status)?.let { openForm(previous = it.store) }
        }
    }

    private fun load() {
        _state.value = State.Loading
        viewModelScope.launch {
            when (val result = stores.myStores()) {
                is FoxResult.Failure -> _state.value = State.Failed(result.error)

                is FoxResult.Success -> {
                    val latest = result.value.lastOrNull()
                    if (latest == null) openFormNow(previous = null) else _state.value = State.Status(latest)
                }
            }
        }
    }

    private fun refresh() {
        val current = state.value as? State.Status ?: return load()
        _state.value = current.copy(refreshing = true)
        viewModelScope.launch {
            val latest = (stores.myStores() as? FoxResult.Success)?.value?.firstOrNull { it.id == current.store.id }
            _state.value = State.Status(latest ?: current.store)
        }
    }

    private fun openForm(previous: MyStore?) {
        _state.value = State.Loading
        viewModelScope.launch { openFormNow(previous) }
    }

    private suspend fun openFormNow(previous: MyStore?) {
        _state.value =
            when (val result = provinces.directory()) {
                is FoxResult.Failure -> State.Failed(result.error)

                is FoxResult.Success -> {
                    val open = result.value.filter { it.isServiceable }.sortedBy { it.name }.toImmutableList()
                    val form = StoreForm(provinces = open, provinceId = open.singleOrNull()?.id)
                    State.Apply(
                        form = if (previous ==
                            null
                        ) {
                            form
                        } else {
                            form.copy(name = previous.name, category = previous.category)
                        },
                        previous = previous,
                    )
                }
            }
    }

    private fun edit(change: StoreForm.() -> StoreForm) {
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

    private fun locate() {
        if (!location.hasPermission()) {
            viewModelScope.launch { _effects.send(Effect.RequestLocationPermission) }
            return
        }
        edit { copy(locating = true, locationProblem = null) }
        viewModelScope.launch {
            when (val result = location.currentLocation()) {
                is LocationResult.Found -> edit {
                    copy(location = result.point, locating = false).without(Field.Location)
                }

                else -> edit { copy(locating = false, locationProblem = result) }
            }
        }
    }

    private fun onPhoto(action: Action.PhotoPicked) {
        val uri = action.uri ?: return
        viewModelScope.launch {
            val fileName = if (action.document == StoreDocument.ValidId) "valid_id.jpg" else "business_permit.jpg"
            val photo = compressor.compress(uri, fileName)
            if (photo == null) {
                _effects.send(Effect.PhotoUnreadable)
                return@launch
            }
            edit {
                when (action.document) {
                    StoreDocument.ValidId -> copy(validId = photo).without(Field.ValidId)
                    StoreDocument.BusinessPermit -> copy(businessPermit = photo).without(Field.BusinessPermit)
                }
            }
        }
    }

    private fun submit() {
        val current = state.value as? State.Apply ?: return
        if (current.submitting) return
        val form = current.form
        val errors =
            StoreApplicationRules.validate(
                provinceId = form.provinceId,
                name = form.name,
                addressLine = form.addressLine,
                city = form.city,
                phone = form.phone,
                hasLocation = form.location != null,
                hasBusinessPermit = form.businessPermit != null,
                hasValidId = form.validId != null,
            )
        if (errors.isNotEmpty()) {
            _state.value = current.copy(form = form.copy(errors = errors))
            return
        }
        val request = form.toRequest()
        _state.value = current.copy(submitting = true, message = null)
        viewModelScope.launch {
            _state.value =
                when (val result = stores.submit(request)) {
                    is FoxResult.Success -> State.Status(result.value)
                    is FoxResult.Failure -> current.copy(submitting = false, message = result.error)
                }
        }
    }

    /** Only called once validation passed, so every required value is there. */
    private fun StoreForm.toRequest(): StoreApplicationForm {
        return StoreApplicationForm(
            provinceId = checkNotNull(provinceId),
            name = name.trim(),
            category = category,
            description = description.trim().ifBlank { null },
            addressLine = addressLine.trim(),
            barangay = barangay.trim().ifBlank { null },
            city = city.trim(),
            phone = PhMobileNumber.normalize(phone),
            location = checkNotNull(location),
            businessPermit = checkNotNull(businessPermit),
            validId = checkNotNull(validId),
        )
    }

    private fun StoreForm.without(field: Field): StoreForm = copy(errors = errors - field)
}
