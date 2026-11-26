package ph.foxdrop.feature.operatorzonesrates

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ph.foxdrop.core.common.format.PesoInput
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.designsystem.component.Loadable
import ph.foxdrop.core.domain.onboarding.Problem
import ph.foxdrop.core.domain.onboarding.RateCardRules
import ph.foxdrop.core.domain.territory.FeePreview
import ph.foxdrop.core.domain.territory.RateCard
import ph.foxdrop.core.domain.territory.TerritoryRepository

/** The rate card form as typed: pesos and km as text, the rider share in percent. */
@Immutable
data class RateFields(
    val baseFare: String = "",
    val baseKm: String = "",
    val ratePerKm: String = "",
    val minFee: String = "",
    val maxKm: String = "",
    val riderSharePercent: String = "",
) {
    fun parse(): Parsed =
        Parsed(
            baseFare = PesoInput.parse(baseFare),
            baseKm = baseKm.trim().toDoubleOrNull(),
            ratePerKm = PesoInput.parse(ratePerKm),
            minFee = PesoInput.parse(minFee),
            maxKm = maxKm.trim().toDoubleOrNull(),
            riderShareBps = riderSharePercent.trim().toDoubleOrNull()?.let { (it * BPS_PER_PERCENT).roundToInt() },
        )

    data class Parsed(
        val baseFare: Long?,
        val baseKm: Double?,
        val ratePerKm: Long?,
        val minFee: Long?,
        val maxKm: Double?,
        val riderShareBps: Int?,
    ) {
        val problems: Map<RateCardRules.Field, Problem> = RateCardRules.validate(
            baseFare,
            baseKm,
            ratePerKm,
            minFee,
            maxKm,
            riderShareBps,
        )

        /** The card to publish, or null while any value is missing or invalid. */
        fun toCard(): RateCard? {
            val complete =
                baseFare != null && baseKm != null && ratePerKm != null && minFee != null && maxKm != null &&
                    riderShareBps != null
            return if (problems.isEmpty() && complete) {
                RateCardRules.toCard(baseFare, baseKm, ratePerKm, minFee, maxKm, riderShareBps)
            } else {
                null
            }
        }
    }

    companion object {
        const val BPS_PER_PERCENT = 100

        fun from(card: RateCard) =
            RateFields(
                baseFare = PesoInput.toInput(card.baseFareCentavos),
                baseKm = card.baseKm.clean(),
                ratePerKm = PesoInput.toInput(card.ratePerKmCentavos),
                minFee = PesoInput.toInput(card.minFeeCentavos),
                maxKm = card.maxKm.clean(),
                riderSharePercent = (card.riderShareBps.toDouble() / BPS_PER_PERCENT).clean(),
            )

        private fun Double.clean(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()
    }
}

enum class RateField { BaseFare, BaseKm, RatePerKm, MinFee, MaxKm, RiderShare }

@Immutable
data class RateCardState(
    val content: Loadable<Unit> = Loadable.Loading,
    val fields: RateFields = RateFields(),
    val problems: Map<RateCardRules.Field, Problem> = emptyMap(),
    val preview: Loadable<ImmutableList<FeePreview>> = Loadable.Loading,
    val effectiveFrom: String? = null,
    val publishing: Boolean = false,
    val justPublished: Boolean = false,
    val storeFee: String = "",
    val storeFeeProblem: Problem? = null,
    val savingStoreFee: Boolean = false,
    val storeFeeSaved: Boolean = false,
    val message: DomainError? = null,
)

/**
 * The rate card editor with a live fee preview (§6, §9.4 step 5) and the province store fee. The preview always
 * comes from the server, so the numbers are exactly what customers will pay.
 */
@HiltViewModel
class RateCardViewModel @Inject constructor(private val territory: TerritoryRepository) : ViewModel() {
    private val _state = MutableStateFlow(RateCardState())
    val state: StateFlow<RateCardState> = _state.asStateFlow()
    private var previewJob: Job? = null

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(content = Loadable.Loading) }
        viewModelScope.launch {
            val card = territory.rateCard()
            val province = territory.province()
            when {
                card is FoxResult.Failure -> _state.update { it.copy(content = Loadable.Failed(card.error)) }

                province is FoxResult.Failure -> _state.update { it.copy(content = Loadable.Failed(province.error)) }

                else -> {
                    val current = (card as FoxResult.Success).value
                    _state.update {
                        it.copy(
                            content = Loadable.Ready(Unit),
                            fields = RateFields.from(current),
                            effectiveFrom = current.effectiveFrom,
                            storeFee = PesoInput.toInput((province as FoxResult.Success).value.storeFeeCentavos),
                        )
                    }
                    schedulePreview(delayMs = 0)
                }
            }
        }
    }

    fun onFieldChanged(field: RateField, value: String) {
        _state.update {
            val fields =
                when (field) {
                    RateField.BaseFare -> it.fields.copy(baseFare = value)
                    RateField.BaseKm -> it.fields.copy(baseKm = value)
                    RateField.RatePerKm -> it.fields.copy(ratePerKm = value)
                    RateField.MinFee -> it.fields.copy(minFee = value)
                    RateField.MaxKm -> it.fields.copy(maxKm = value)
                    RateField.RiderShare -> it.fields.copy(riderSharePercent = value)
                }
            it.copy(fields = fields, problems = emptyMap(), justPublished = false)
        }
        schedulePreview(PREVIEW_DEBOUNCE_MS)
    }

    fun publish() {
        val current = state.value
        if (current.publishing) return
        val parsed = current.fields.parse()
        val card = parsed.toCard()
        if (card == null) {
            _state.update { it.copy(problems = parsed.problems) }
            return
        }
        _state.update { it.copy(publishing = true, message = null) }
        viewModelScope.launch {
            when (val result = territory.publishRateCard(card)) {
                is FoxResult.Success ->
                    _state.update {
                        it.copy(publishing = false, justPublished = true, effectiveFrom = result.value.effectiveFrom)
                    }

                is FoxResult.Failure -> _state.update { it.copy(publishing = false, message = result.error) }
            }
        }
    }

    fun onStoreFeeChanged(value: String) {
        _state.update { it.copy(storeFee = value, storeFeeProblem = null, storeFeeSaved = false) }
    }

    fun saveStoreFee() {
        val current = state.value
        if (current.savingStoreFee) return
        val centavos = PesoInput.parse(current.storeFee)
        if (centavos == null || centavos > MAX_STORE_FEE_CENTAVOS) {
            _state.update { it.copy(storeFeeProblem = if (centavos == null) Problem.Invalid else Problem.OutOfRange) }
            return
        }
        _state.update { it.copy(savingStoreFee = true, message = null) }
        viewModelScope.launch {
            when (val result = territory.updateStoreFee(centavos)) {
                is FoxResult.Success ->
                    _state.update {
                        it.copy(
                            savingStoreFee = false,
                            storeFeeSaved = true,
                            storeFee = PesoInput.toInput(result.value.storeFeeCentavos),
                        )
                    }

                is FoxResult.Failure -> _state.update { it.copy(savingStoreFee = false, message = result.error) }
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun schedulePreview(delayMs: Long) {
        previewJob?.cancel()
        previewJob =
            viewModelScope.launch {
                delay(delayMs)
                val card = state.value.fields.parse().toCard() ?: return@launch
                val preview =
                    when (val result = territory.previewFees(card, PREVIEW_DISTANCES_KM)) {
                        is FoxResult.Success -> Loadable.Ready(result.value.toImmutableList())
                        is FoxResult.Failure -> Loadable.Failed(result.error)
                    }
                _state.update { it.copy(preview = preview) }
            }
    }

    companion object {
        const val PREVIEW_DEBOUNCE_MS = 400L
        val PREVIEW_DISTANCES_KM = listOf(1.0, 3.0, 5.0, 10.0)
        const val MAX_STORE_FEE_CENTAVOS = 100_000L
    }
}
