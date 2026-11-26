package ph.foxdrop.feature.rideronboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
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
import ph.foxdrop.core.common.time.ManilaTime
import ph.foxdrop.core.domain.onboarding.RiderApplicationRules
import ph.foxdrop.core.domain.onboarding.RiderApplicationRules.Field
import ph.foxdrop.core.domain.partner.MyRider
import ph.foxdrop.core.domain.partner.RiderApplicationForm
import ph.foxdrop.core.domain.partner.RiderOnboardingRepository
import ph.foxdrop.core.domain.province.ProvinceRepository
import ph.foxdrop.core.media.ImageCompressor
import ph.foxdrop.feature.rideronboarding.RiderOnboardingAction as Action
import ph.foxdrop.feature.rideronboarding.RiderOnboardingEffect as Effect
import ph.foxdrop.feature.rideronboarding.RiderOnboardingState as State

/**
 * Rider registration (§5.3): the checklist, then the review status. The province's operator approves riders; once
 * APPROVED the server grants the rider role and the screen offers to start delivering.
 */
@HiltViewModel
class RiderOnboardingViewModel @Inject constructor(
    private val riders: RiderOnboardingRepository,
    private val provinces: ProvinceRepository,
    private val compressor: ImageCompressor,
) : ViewModel() {
    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()
    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects: Flow<Effect> = _effects.receiveAsFlow()

    /** License expiry is checked against today in Manila; tests pin it. */
    internal var clock: Clock = Clock.systemUTC()

    init {
        load()
    }

    @Suppress("CyclomaticComplexMethod") // one branch per user action
    fun onAction(action: Action) {
        when (action) {
            Action.Retry -> load()

            Action.Refresh -> refresh()

            is Action.ProvinceSelected -> edit { copy(provinceId = action.provinceId).without(Field.Province) }

            is Action.FullNameChanged -> edit { copy(fullName = action.value).without(Field.FullName) }

            is Action.VehicleSelected -> edit {
                copy(vehicle = action.vehicle).without(Field.PlateNo, Field.OrCr, Field.VehiclePhoto)
            }

            is Action.PlateChanged -> edit { copy(plateNo = action.value.uppercase()).without(Field.PlateNo) }

            is Action.LicenseNoChanged -> edit { copy(licenseNo = action.value.uppercase()).without(Field.LicenseNo) }

            is Action.LicenseExpiryChanged -> edit { copy(licenseExpiry = action.date).without(Field.LicenseExpiry) }

            is Action.PhotoPicked -> onPhoto(action)

            Action.Submit -> submit()

            Action.DismissMessage -> _state.update { if (it is State.Apply) it.copy(message = null) else it }

            Action.FixAndResubmit -> (state.value as? State.Status)?.let { openForm(previous = it.rider) }
        }
    }

    private fun load() {
        _state.value = State.Loading
        viewModelScope.launch {
            when (val result = riders.me()) {
                is FoxResult.Failure -> _state.value = State.Failed(result.error)

                is FoxResult.Success -> {
                    val mine = result.value
                    if (mine == null) openFormNow(previous = null) else _state.value = State.Status(mine)
                }
            }
        }
    }

    private fun refresh() {
        val current = state.value as? State.Status ?: return load()
        _state.value = current.copy(refreshing = true)
        viewModelScope.launch {
            val latest = (riders.me() as? FoxResult.Success)?.value
            _state.value = State.Status(latest ?: current.rider)
        }
    }

    private fun openForm(previous: MyRider?) {
        _state.value = State.Loading
        viewModelScope.launch { openFormNow(previous) }
    }

    private suspend fun openFormNow(previous: MyRider?) {
        _state.value =
            when (val result = provinces.directory()) {
                is FoxResult.Failure -> State.Failed(result.error)

                is FoxResult.Success -> {
                    val open = result.value.filter { it.isServiceable }.sortedBy { it.name }.toImmutableList()
                    val form = RiderForm(provinces = open, provinceId = open.singleOrNull()?.id)
                    State.Apply(
                        form = if (previous ==
                            null
                        ) {
                            form
                        } else {
                            form.copy(
                                provinceId = open.firstOrNull { it.id == previous.provinceId }?.id ?: form.provinceId,
                                vehicle = previous.vehicleType,
                                plateNo = previous.plateNo.orEmpty(),
                            )
                        },
                        previous = previous,
                    )
                }
            }
    }

    private fun edit(change: RiderForm.() -> RiderForm) {
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
        val fileName = PHOTO_FILES[action.document] ?: return
        viewModelScope.launch {
            val photo = compressor.compress(uri, fileName)
            if (photo == null) {
                _effects.send(Effect.PhotoUnreadable)
                return@launch
            }
            edit { copy(photos = photos + (action.document to photo)).without(action.document) }
        }
    }

    private fun submit() {
        val current = state.value as? State.Apply ?: return
        if (current.submitting) return
        val form = current.form
        val errors =
            RiderApplicationRules.validate(
                provinceId = form.provinceId,
                fullName = form.fullName,
                vehicle = form.vehicle,
                plateNo = form.plateNo,
                licenseNo = form.licenseNo,
                licenseExpiry = form.licenseExpiry,
                today = manilaToday(clock),
                photos = form.photos.keys,
            )
        if (errors.isNotEmpty()) {
            _state.value = current.copy(form = form.copy(errors = errors))
            return
        }
        val request = form.toRequest()
        _state.value = current.copy(submitting = true, message = null)
        viewModelScope.launch {
            _state.value =
                when (val result = riders.submit(request)) {
                    is FoxResult.Success -> State.Status(result.value)
                    is FoxResult.Failure -> current.copy(submitting = false, message = result.error)
                }
        }
    }

    /** Only called once validation passed, so every required value is there. */
    private fun RiderForm.toRequest(): RiderApplicationForm {
        val papers = needsVehiclePapers
        return RiderApplicationForm(
            provinceId = checkNotNull(provinceId),
            fullName = fullName.trim(),
            vehicleType = vehicle,
            plateNo = if (papers) plateNo.trim() else null,
            licenseNo = licenseNo.trim(),
            licenseExpiry = checkNotNull(licenseExpiry).toString(),
            validId = photos.getValue(Field.ValidId),
            driversLicense = photos.getValue(Field.DriversLicense),
            selfie = photos.getValue(Field.Selfie),
            orCr = if (papers) photos[Field.OrCr] else null,
            vehiclePhoto = if (papers) photos[Field.VehiclePhoto] else null,
        )
    }

    private fun RiderForm.without(vararg fields: Field): RiderForm = copy(errors = errors - fields.toSet())

    private companion object {
        val PHOTO_FILES =
            mapOf(
                Field.ValidId to "valid_id.jpg",
                Field.DriversLicense to "drivers_license.jpg",
                Field.Selfie to "selfie.jpg",
                Field.OrCr to "or_cr.jpg",
                Field.VehiclePhoto to "vehicle_photo.jpg",
            )
    }
}

/** Today in Manila as a [LocalDate]; the expiry picker only offers later days. */
internal fun manilaToday(clock: Clock = Clock.systemUTC()): LocalDate = ManilaTime.now(clock).toLocalDate()
