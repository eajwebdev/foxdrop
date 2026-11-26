package ph.foxdrop.feature.operatorzonesrates

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.designsystem.component.Loadable
import ph.foxdrop.core.domain.onboarding.Problem
import ph.foxdrop.core.domain.territory.OperatorProvince
import ph.foxdrop.core.domain.territory.TerritoryRepository
import ph.foxdrop.core.domain.territory.Zone
import ph.foxdrop.core.location.LocationClient
import ph.foxdrop.core.location.LocationResult
import ph.foxdrop.core.model.GeoPoint

enum class CenterSource { ProvinceCenter, MyLocation }

/** The "add a zone" sheet: a circle around the province centre or the operator's current position (DECISIONS 58). */
@Immutable
data class ZoneEditor(
    val name: String = "",
    val source: CenterSource = CenterSource.ProvinceCenter,
    val center: GeoPoint? = null,
    val radiusKm: Float = DEFAULT_RADIUS_KM,
    val locating: Boolean = false,
    val locationProblem: LocationResult? = null,
    val nameProblem: Problem? = null,
    val saving: Boolean = false,
) {
    companion object {
        const val DEFAULT_RADIUS_KM = 5f
        const val MIN_RADIUS_KM = 0.5f
        const val MAX_RADIUS_KM = 30f
        const val RADIUS_STEP_KM = 0.5f
    }
}

@Immutable
data class ZonesData(val zones: ImmutableList<Zone>, val province: OperatorProvince)

@Immutable
data class ZonesState(
    val content: Loadable<ZonesData> = Loadable.Loading,
    val editor: ZoneEditor? = null,
    val message: DomainError? = null,
)

sealed interface ZonesAction {
    data object Retry : ZonesAction

    data object AddZone : ZonesAction

    data object DismissEditor : ZonesAction

    data class NameChanged(val value: String) : ZonesAction

    data class RadiusChanged(val km: Float) : ZonesAction

    data object UseProvinceCenter : ZonesAction

    data object UseMyLocation : ZonesAction

    data class LocationPermissionResult(val granted: Boolean) : ZonesAction

    data object Save : ZonesAction

    data class Delete(val zoneId: String) : ZonesAction

    data object DismissMessage : ZonesAction
}

sealed interface ZonesEffect {
    data object RequestLocationPermission : ZonesEffect
}

/** Delivery coverage zones of the operator's province (§6, §9.4 step 3). */
@HiltViewModel
class ZonesViewModel @Inject constructor(
    private val territory: TerritoryRepository,
    private val location: LocationClient,
) : ViewModel() {
    private val _state = MutableStateFlow(ZonesState())
    val state: StateFlow<ZonesState> = _state.asStateFlow()
    private val _effects = Channel<ZonesEffect>(Channel.BUFFERED)
    val effects: Flow<ZonesEffect> = _effects.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: ZonesAction) {
        when (action) {
            ZonesAction.Retry -> load()

            ZonesAction.AddZone -> _state.update { it.copy(editor = ZoneEditor(center = provinceCenter())) }

            ZonesAction.DismissEditor -> _state.update { it.copy(editor = null) }

            is ZonesAction.NameChanged -> editor { copy(name = action.value, nameProblem = null) }

            is ZonesAction.RadiusChanged -> editor { copy(radiusKm = snap(action.km)) }

            ZonesAction.UseProvinceCenter ->
                editor { copy(source = CenterSource.ProvinceCenter, center = provinceCenter(), locationProblem = null) }

            ZonesAction.UseMyLocation -> locate()

            is ZonesAction.LocationPermissionResult ->
                if (action.granted) {
                    locate()
                } else {
                    editor {
                        copy(locating = false, locationProblem = LocationResult.PermissionDenied)
                    }
                }

            ZonesAction.Save -> save()

            is ZonesAction.Delete -> delete(action.zoneId)

            ZonesAction.DismissMessage -> _state.update { it.copy(message = null) }
        }
    }

    private fun load() {
        _state.update { it.copy(content = Loadable.Loading) }
        viewModelScope.launch {
            val (zones, province) =
                coroutineScope {
                    val zones = async { territory.zones() }
                    val province = async { territory.province() }
                    zones.await() to province.await()
                }
            val content =
                when {
                    zones is FoxResult.Failure -> Loadable.Failed(zones.error)

                    province is FoxResult.Failure -> Loadable.Failed(province.error)

                    else ->
                        Loadable.Ready(
                            ZonesData(
                                (zones as FoxResult.Success).value.filter { it.isActive }.toImmutableList(),
                                (province as FoxResult.Success).value,
                            ),
                        )
                }
            _state.update { it.copy(content = content) }
        }
    }

    private fun provinceCenter(): GeoPoint? = (state.value.content as? Loadable.Ready)?.value?.province?.center

    private fun editor(change: ZoneEditor.() -> ZoneEditor) {
        _state.update { it.copy(editor = it.editor?.change()) }
    }

    private fun locate() {
        if (!location.hasPermission()) {
            editor { copy(source = CenterSource.MyLocation, locating = false) }
            viewModelScope.launch { _effects.send(ZonesEffect.RequestLocationPermission) }
            return
        }
        editor { copy(source = CenterSource.MyLocation, locating = true, locationProblem = null) }
        viewModelScope.launch {
            when (val result = location.currentLocation()) {
                is LocationResult.Found -> editor { copy(center = result.point, locating = false) }
                else -> editor { copy(locating = false, locationProblem = result) }
            }
        }
    }

    private fun save() {
        val editor = state.value.editor ?: return
        if (editor.saving || editor.locating) return
        val trimmed = editor.name.trim()
        val nameProblem =
            when {
                trimmed.isEmpty() -> Problem.Required
                trimmed.length > MAX_NAME -> Problem.TooLong
                else -> null
            }
        val center = editor.center
        if (nameProblem != null || center == null) {
            editor {
                copy(
                    nameProblem = nameProblem,
                    locationProblem = if (center ==
                        null
                    ) {
                        LocationResult.Unavailable
                    } else {
                        locationProblem
                    },
                )
            }
            return
        }
        editor { copy(saving = true) }
        viewModelScope.launch {
            when (val result = territory.createCircleZone(trimmed, center, editor.radiusKm.toDouble())) {
                is FoxResult.Success ->
                    _state.update { current ->
                        val data = (current.content as? Loadable.Ready)?.value
                        current.copy(
                            editor = null,
                            content =
                                data?.let {
                                    Loadable.Ready(it.copy(zones = (it.zones + result.value).toImmutableList()))
                                }
                                    ?: current.content,
                        )
                    }

                is FoxResult.Failure -> _state.update {
                    it.copy(editor = it.editor?.copy(saving = false), message = result.error)
                }
            }
        }
    }

    private fun delete(zoneId: String) {
        viewModelScope.launch {
            when (val result = territory.deleteZone(zoneId)) {
                is FoxResult.Success ->
                    _state.update { current ->
                        val data = (current.content as? Loadable.Ready)?.value ?: return@update current
                        current.copy(
                            content = Loadable.Ready(
                                data.copy(
                                    zones = data.zones.filterNot {
                                        it.id == zoneId
                                    }.toImmutableList(),
                                ),
                            ),
                        )
                    }

                is FoxResult.Failure -> _state.update { it.copy(message = result.error) }
            }
        }
    }

    private fun snap(km: Float): Float =
        ((km / ZoneEditor.RADIUS_STEP_KM).roundToInt() * ZoneEditor.RADIUS_STEP_KM)
            .coerceIn(ZoneEditor.MIN_RADIUS_KM, ZoneEditor.MAX_RADIUS_KM)

    private companion object {
        const val MAX_NAME = 80
    }
}
