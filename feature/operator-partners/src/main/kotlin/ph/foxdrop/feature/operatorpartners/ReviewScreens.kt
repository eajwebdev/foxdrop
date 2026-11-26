package ph.foxdrop.feature.operatorpartners

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import java.util.Locale
import ph.foxdrop.core.designsystem.component.FoxButton
import ph.foxdrop.core.designsystem.component.FoxButtonState
import ph.foxdrop.core.designsystem.component.FoxCard
import ph.foxdrop.core.designsystem.component.FoxOutlinedButton
import ph.foxdrop.core.designsystem.component.FoxScreenScaffold
import ph.foxdrop.core.designsystem.component.FoxTextField
import ph.foxdrop.core.designsystem.component.Loadable
import ph.foxdrop.core.designsystem.component.LoadableContent
import ph.foxdrop.core.designsystem.component.StatusPill
import ph.foxdrop.core.designsystem.component.StatusTone
import ph.foxdrop.core.designsystem.component.errorMessage
import ph.foxdrop.core.designsystem.component.problemMessage
import ph.foxdrop.core.designsystem.theme.FoxLayout
import ph.foxdrop.core.designsystem.theme.FoxShapes
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.domain.partner.PartnerDocument
import ph.foxdrop.core.domain.partner.RiderReview
import ph.foxdrop.core.domain.partner.RiderStatus
import ph.foxdrop.core.domain.partner.StoreReview
import ph.foxdrop.core.domain.partner.StoreStatus

/** Callbacks shared by both review screens. */
data class ReviewActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onApprove: () -> Unit = {},
    val onOpenReject: () -> Unit = {},
    val onReasonChanged: (String) -> Unit = {},
    val onConfirmReject: () -> Unit = {},
    val onDismissReject: () -> Unit = {},
    val onDismissMessage: () -> Unit = {},
)

private fun ReviewViewModel<*>.actions(onBack: () -> Unit) =
    ReviewActions(
        onBack = onBack,
        onRetry = ::load,
        onApprove = ::approve,
        onOpenReject = ::openRejectDialog,
        onReasonChanged = ::onReasonChanged,
        onConfirmReject = ::confirmReject,
        onDismissReject = ::dismissRejectDialog,
        onDismissMessage = ::dismissMessage,
    )

@Composable
fun StoreReviewRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StoreReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = remember(viewModel, onBack) { viewModel.actions(onBack) }
    StoreReviewScreen(state, actions, modifier)
}

@Composable
fun RiderReviewRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RiderReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = remember(viewModel, onBack) { viewModel.actions(onBack) }
    RiderReviewScreen(state, actions, modifier)
}

@Composable
fun StoreReviewScreen(state: ReviewState<StoreReview>, actions: ReviewActions, modifier: Modifier = Modifier) {
    val review = (state.review as? Loadable.Ready)?.value
    val status = review?.summary?.status
    val decidedMessage =
        when (status) {
            StoreStatus.Rejected -> stringResource(R.string.review_rejected)
            null -> null
            else -> stringResource(R.string.review_approved_store)
        }
    ReviewScaffold(
        title = stringResource(R.string.review_store_title),
        state = state,
        actions = actions,
        pending = status == StoreStatus.Submitted,
        decidedMessage = decidedMessage,
        modifier = modifier,
    ) { store ->
        val summary = store.summary
        Header(
            summary.name,
            "${summary.category.label()} · ${summary.city}",
            summary.status.label(),
            summary.status.tone(),
        )
        store.rejectionReason?.let { RejectedNote(it) }
        store.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        DetailCard {
            Detail(
                stringResource(R.string.review_owner),
                listOfNotNull(summary.ownerName, summary.ownerPhone).joinToString(" · "),
            )
            store.phone?.let { Detail(stringResource(R.string.review_contact), it) }
            Detail(
                stringResource(R.string.review_address),
                listOfNotNull(summary.addressLine, store.barangay, summary.city).joinToString(", "),
            )
            store.location?.let {
                Text(
                    stringResource(R.string.review_location, it.lat.coordinate(), it.lng.coordinate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Documents(store.documents)
    }
}

@Composable
fun RiderReviewScreen(state: ReviewState<RiderReview>, actions: ReviewActions, modifier: Modifier = Modifier) {
    val review = (state.review as? Loadable.Ready)?.value
    val status = review?.summary?.status
    val decidedMessage =
        when (status) {
            RiderStatus.Rejected -> stringResource(R.string.review_rejected)
            null -> null
            else -> stringResource(R.string.review_approved_rider)
        }
    ReviewScaffold(
        title = stringResource(R.string.review_rider_title),
        state = state,
        actions = actions,
        pending = status == RiderStatus.Applied || status == RiderStatus.DocsReview,
        decidedMessage = decidedMessage,
        modifier = modifier,
    ) { rider ->
        val summary = rider.summary
        Header(
            summary.fullName ?: summary.phone.orEmpty(),
            summary.phone.orEmpty(),
            summary.status.label(),
            summary.status.tone(),
        )
        rider.rejectionReason?.let { RejectedNote(it) }
        DetailCard {
            Detail(stringResource(R.string.review_vehicle), summary.vehicleType.label())
            summary.plateNo?.let { Detail(stringResource(R.string.review_plate), it) }
            rider.licenseNoLast4?.let { last4 ->
                Detail(
                    stringResource(R.string.review_license),
                    stringResource(R.string.review_license_value, last4, rider.licenseExpiry?.day().orEmpty()),
                )
            }
        }
        Documents(rider.documents)
    }
}

@Suppress("LongParameterList") // the two review screens differ in each of these
@Composable
private fun <T> ReviewScaffold(
    title: String,
    state: ReviewState<T>,
    actions: ReviewActions,
    pending: Boolean,
    decidedMessage: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(T) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val error = state.message?.let { errorMessage(it) }
    LaunchedEffect(error) {
        if (error != null) {
            snackbar.showSnackbar(error)
            actions.onDismissMessage()
        }
    }
    LaunchedEffect(state.decided, decidedMessage) {
        if (state.decided && decidedMessage != null) snackbar.showSnackbar(decidedMessage)
    }
    FoxScreenScaffold(
        title = title,
        onBack = actions.onBack,
        snackbarHostState = snackbar,
        modifier = modifier,
        bottomBar = {
            when {
                state.rejectDialog != null -> RejectPanel(state.rejectDialog, actions)
                pending -> DecisionBar(state.deciding, actions)
            }
        },
    ) { padding ->
        LoadableContent(
            state = state.review,
            errorTitle = stringResource(R.string.review_error_title),
            onRetry = actions.onRetry,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { review ->
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.widthIn(max = FoxLayout.MaxFormWidth).fillMaxWidth().padding(FoxSpacing.L),
                    verticalArrangement = Arrangement.spacedBy(FoxSpacing.M),
                ) {
                    content(review)
                }
            }
        }
    }
}

@Composable
private fun DecisionBar(deciding: Boolean, actions: ReviewActions) {
    Box(Modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.Center) {
        Row(
            Modifier.widthIn(max = FoxLayout.MaxFormWidth).fillMaxWidth().padding(FoxSpacing.M),
            horizontalArrangement = Arrangement.spacedBy(FoxSpacing.S),
        ) {
            FoxOutlinedButton(
                stringResource(R.string.review_reject),
                actions.onOpenReject,
                Modifier.weight(1f),
                enabled = !deciding,
            )
            FoxButton(
                text = stringResource(R.string.review_approve),
                onClick = actions.onApprove,
                state = if (deciding) FoxButtonState.Loading else FoxButtonState.Idle,
                loadingDescription = stringResource(R.string.review_approving),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Asks for the reason in place of the decision buttons, so it sits next to the documents being judged. */
@Composable
private fun RejectPanel(dialog: RejectDialog, actions: ReviewActions) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.navigationBarsPadding().imePadding(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = FoxLayout.MaxFormWidth).fillMaxWidth().padding(FoxSpacing.M),
                verticalArrangement = Arrangement.spacedBy(FoxSpacing.S),
            ) {
                Text(stringResource(R.string.review_reject_title), style = MaterialTheme.typography.titleMedium)
                FoxTextField(
                    value = dialog.reason,
                    onValueChange = actions.onReasonChanged,
                    label = stringResource(R.string.review_reject_reason),
                    errorMessage = problemMessage(dialog.problem),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(FoxSpacing.S)) {
                    FoxOutlinedButton(
                        stringResource(R.string.review_cancel),
                        actions.onDismissReject,
                        Modifier.weight(1f),
                    )
                    FoxButton(
                        stringResource(R.string.review_reject_confirm),
                        actions.onConfirmReject,
                        Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(title: String, subtitle: String, status: String, tone: StatusTone) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        StatusPill(status, tone)
    }
}

@Composable
private fun RejectedNote(reason: String) {
    Text(
        stringResource(R.string.review_rejected_reason, reason),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    FoxCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(FoxSpacing.M),
            verticalArrangement = Arrangement.spacedBy(FoxSpacing.S),
            content = content,
        )
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Documents(documents: List<PartnerDocument>) {
    Text(stringResource(R.string.review_documents), style = MaterialTheme.typography.titleMedium)
    if (documents.isEmpty()) {
        Text(stringResource(R.string.review_no_documents), style = MaterialTheme.typography.bodyMedium)
    }
    documents.forEach { document ->
        val label = documentLabel(document.type)
        Column(verticalArrangement = Arrangement.spacedBy(FoxSpacing.XS)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            // Signed, short-lived URLs (DECISIONS 60): loaded on demand, never stored by the app.
            SubcomposeAsyncImage(
                model = document.url,
                contentDescription = label,
                contentScale = ContentScale.Fit,
                loading = { ImageFrame() },
                error = { ImageFrame() },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(DOCUMENT_ASPECT)
                        .clip(FoxShapes.Medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .semantics { contentDescription = label },
            )
        }
    }
}

@Composable
private fun ImageFrame() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun Double.coordinate(): String = String.format(Locale.US, "%.5f", this)

private const val DOCUMENT_ASPECT = 4f / 3f
