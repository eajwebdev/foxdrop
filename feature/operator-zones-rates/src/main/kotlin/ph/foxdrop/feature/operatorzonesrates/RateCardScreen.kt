package ph.foxdrop.feature.operatorzonesrates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import kotlinx.collections.immutable.ImmutableList
import ph.foxdrop.core.common.format.formatPeso
import ph.foxdrop.core.designsystem.component.FoxButton
import ph.foxdrop.core.designsystem.component.FoxButtonState
import ph.foxdrop.core.designsystem.component.FoxCard
import ph.foxdrop.core.designsystem.component.FoxOutlinedButton
import ph.foxdrop.core.designsystem.component.FoxScreenScaffold
import ph.foxdrop.core.designsystem.component.FoxTextField
import ph.foxdrop.core.designsystem.component.Loadable
import ph.foxdrop.core.designsystem.component.LoadableContent
import ph.foxdrop.core.designsystem.component.SkeletonBox
import ph.foxdrop.core.designsystem.component.errorMessage
import ph.foxdrop.core.designsystem.component.problemMessage
import ph.foxdrop.core.designsystem.theme.FoxLayout
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.designsystem.theme.FoxTheme
import ph.foxdrop.core.domain.onboarding.Problem
import ph.foxdrop.core.domain.onboarding.RateCardRules
import ph.foxdrop.core.domain.territory.FeePreview
import ph.foxdrop.core.model.Money

@Composable
fun RateCardRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: RateCardViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RateCardScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::load,
        onFieldChanged = viewModel::onFieldChanged,
        onPublish = viewModel::publish,
        onStoreFeeChanged = viewModel::onStoreFeeChanged,
        onSaveStoreFee = viewModel::saveStoreFee,
        onDismissMessage = viewModel::dismissMessage,
        modifier = modifier,
    )
}

@Suppress("LongParameterList") // one callback per user action keeps the screen stateless for previews and tests
@Composable
fun RateCardScreen(
    state: RateCardState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onFieldChanged: (RateField, String) -> Unit,
    onPublish: () -> Unit,
    onStoreFeeChanged: (String) -> Unit,
    onSaveStoreFee: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val message = state.message?.let { errorMessage(it) }
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            onDismissMessage()
        }
    }
    FoxScreenScaffold(
        title = stringResource(R.string.rates_title),
        onBack = onBack,
        snackbarHostState = snackbar,
        modifier = modifier,
    ) { padding ->
        LoadableContent(
            state = state.content,
            errorTitle = stringResource(R.string.rates_error_title),
            onRetry = onRetry,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.widthIn(max = FoxLayout.MaxFormWidth).fillMaxWidth().padding(FoxSpacing.L),
                    verticalArrangement = Arrangement.spacedBy(FoxSpacing.M),
                ) {
                    RateCardForm(state, onFieldChanged, onPublish)
                    StoreFeeCard(state, onStoreFeeChanged, onSaveStoreFee)
                }
            }
        }
    }
}

@Composable
private fun RateCardForm(state: RateCardState, onFieldChanged: (RateField, String) -> Unit, onPublish: () -> Unit) {
    Text(
        stringResource(R.string.rates_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    state.effectiveFrom?.let {
        Text(stringResource(R.string.rates_effective, it.take(DATE_LENGTH)), style = MaterialTheme.typography.bodySmall)
    }
    val fields = state.fields
    Row(horizontalArrangement = Arrangement.spacedBy(FoxSpacing.S)) {
        NumberField(
            fields.baseFare,
            R.string.rates_base_fare,
            state.problems[RateCardRules.Field.BaseFare],
            Modifier.weight(1f),
        ) {
            onFieldChanged(RateField.BaseFare, it)
        }
        NumberField(
            fields.baseKm,
            R.string.rates_base_km,
            state.problems[RateCardRules.Field.BaseKm],
            Modifier.weight(1f),
        ) {
            onFieldChanged(RateField.BaseKm, it)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(FoxSpacing.S)) {
        NumberField(
            fields.ratePerKm,
            R.string.rates_rate_per_km,
            state.problems[RateCardRules.Field.RatePerKm],
            Modifier.weight(1f),
        ) {
            onFieldChanged(RateField.RatePerKm, it)
        }
        NumberField(
            fields.minFee,
            R.string.rates_min_fee,
            state.problems[RateCardRules.Field.MinFee],
            Modifier.weight(1f),
        ) {
            onFieldChanged(RateField.MinFee, it)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(FoxSpacing.S)) {
        NumberField(
            fields.maxKm,
            R.string.rates_max_km,
            state.problems[RateCardRules.Field.MaxKm],
            Modifier.weight(1f),
        ) {
            onFieldChanged(RateField.MaxKm, it)
        }
        NumberField(
            fields.riderSharePercent,
            R.string.rates_rider_share,
            state.problems[RateCardRules.Field.RiderShare],
            Modifier.weight(1f),
        ) {
            onFieldChanged(RateField.RiderShare, it)
        }
    }
    PreviewTable(state.preview)
    AnimatedVisibility(visible = state.justPublished) {
        Text(
            stringResource(R.string.rates_published),
            style = MaterialTheme.typography.bodyMedium,
            color = FoxTheme.colors.success,
        )
    }
    FoxButton(
        text = stringResource(R.string.rates_publish),
        onClick = onPublish,
        state = if (state.publishing) FoxButtonState.Loading else FoxButtonState.Idle,
        loadingDescription = stringResource(R.string.rates_publishing),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NumberField(value: String, label: Int, problem: Problem?, modifier: Modifier, onChange: (String) -> Unit) {
    FoxTextField(
        value = value,
        onValueChange = onChange,
        label = stringResource(label),
        errorMessage = problemMessage(problem),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
private fun PreviewTable(preview: Loadable<ImmutableList<FeePreview>>) {
    FoxCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(FoxSpacing.M), verticalArrangement = Arrangement.spacedBy(FoxSpacing.XS)) {
            Text(stringResource(R.string.rates_preview_title), style = MaterialTheme.typography.titleMedium)
            PreviewRow(
                stringResource(R.string.rates_preview_distance),
                stringResource(R.string.rates_preview_fee),
                stringResource(R.string.rates_preview_rider),
                stringResource(R.string.rates_preview_operator),
                header = true,
            )
            HorizontalDivider()
            when (preview) {
                Loadable.Loading -> repeat(PREVIEW_ROWS) {
                    SkeletonBox(Modifier.fillMaxWidth().padding(vertical = FoxSpacing.XXS))
                }

                is Loadable.Failed -> Text(
                    stringResource(R.string.rates_preview_error),
                    color = MaterialTheme.colorScheme.error,
                )

                is Loadable.Ready ->
                    preview.value.forEach { row ->
                        val beyond = stringResource(R.string.rates_preview_beyond)
                        PreviewRow(
                            stringResource(R.string.rates_preview_km, row.distanceKm.km()),
                            row.deliveryFeeCentavos?.let { Money(it).formatPeso() } ?: beyond,
                            row.riderEarningCentavos?.let { Money(it).formatPeso() } ?: "–",
                            row.operatorCutCentavos?.let { Money(it).formatPeso() } ?: "–",
                        )
                    }
            }
        }
    }
}

@Composable
private fun PreviewRow(distance: String, fee: String, rider: String, operator: String, header: Boolean = false) {
    val style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
    Row(Modifier.fillMaxWidth()) {
        Text(distance, style = style, modifier = Modifier.weight(1f))
        Text(
            fee,
            style = style,
            fontWeight = if (header) null else FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.2f),
        )
        Text(rider, style = style, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text(operator, style = style, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StoreFeeCard(state: RateCardState, onStoreFeeChanged: (String) -> Unit, onSaveStoreFee: () -> Unit) {
    FoxCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(FoxSpacing.M), verticalArrangement = Arrangement.spacedBy(FoxSpacing.S)) {
            Text(stringResource(R.string.store_fee_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.store_fee_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NumberField(
                state.storeFee,
                R.string.store_fee_amount,
                state.storeFeeProblem,
                Modifier.fillMaxWidth(),
                onStoreFeeChanged,
            )
            AnimatedVisibility(visible = state.storeFeeSaved) {
                Text(
                    stringResource(R.string.store_fee_saved),
                    style = MaterialTheme.typography.bodySmall,
                    color = FoxTheme.colors.success,
                )
            }
            FoxOutlinedButton(
                text = stringResource(if (state.savingStoreFee) R.string.store_fee_saving else R.string.store_fee_save),
                onClick = onSaveStoreFee,
                enabled = !state.savingStoreFee,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private const val DATE_LENGTH = 10
private const val PREVIEW_ROWS = 4

private fun Double.km(): String = if (this % 1.0 == 0.0) toLong().toString() else String.format(Locale.US, "%.1f", this)
