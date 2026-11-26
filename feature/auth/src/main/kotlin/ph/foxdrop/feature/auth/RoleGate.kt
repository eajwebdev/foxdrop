package ph.foxdrop.feature.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.designsystem.component.EmptyState
import ph.foxdrop.core.designsystem.component.ErrorState
import ph.foxdrop.core.designsystem.component.SkeletonList
import ph.foxdrop.core.designsystem.preview.FoxPreviews
import ph.foxdrop.core.designsystem.theme.FoxDropTheme
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.designsystem.theme.FoxTheme
import ph.foxdrop.core.domain.startup.HomeDecision
import ph.foxdrop.core.domain.startup.ResolveHomeUseCase
import ph.foxdrop.core.model.FoxDropApp

@Immutable
sealed interface RoleGateState {
    data object Loading : RoleGateState

    data class Ready(val decision: HomeDecision) : RoleGateState

    data class Failed(val message: AuthMessage) : RoleGateState

    /** The server no longer accepts the session (revoked, refresh failed): the user must sign in again. */
    data object SessionExpired : RoleGateState
}

/** Finds out which home the signed-in user belongs on (§5 role routing). */
@HiltViewModel
class RoleGateViewModel @Inject constructor(
    private val resolveHome: ResolveHomeUseCase,
    private val app: FoxDropApp,
) : ViewModel() {
    private val _state = MutableStateFlow<RoleGateState>(RoleGateState.Loading)
    val state: StateFlow<RoleGateState> = _state.asStateFlow()

    private var loading: Job? = null

    init {
        retry()
    }

    /** Loads (again) and follows updates: saved roles first, then the server's if they lead to another home. */
    fun retry() {
        loading?.cancel()
        _state.value = RoleGateState.Loading
        loading = viewModelScope.launch {
            resolveHome(app).collect { result -> _state.value = result.toGateState() }
        }
    }

    private fun FoxResult<HomeDecision>.toGateState(): RoleGateState =
        when (this) {
            is FoxResult.Success -> RoleGateState.Ready(value)

            is FoxResult.Failure ->
                if (error == DomainError.Unauthorized) {
                    RoleGateState.SessionExpired
                } else {
                    RoleGateState.Failed(error.toMessage())
                }
        }
}

/**
 * Shows [content] for the user's home, or a friendly screen when the account does not belong in this app.
 * [content] also gets
efreshRoles: onboarding flows call it when a role may have been granted (license paid, store
 * or rider approved), so the gate re-reads /me and moves to the new home.
 * Loading shows a skeleton; errors offer a retry (CLAUDE.md: every screen has loading, empty, error).
 */
@Composable
fun RoleGate(
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoleGateViewModel = hiltViewModel(),
    content: @Composable (home: HomeDecision.Home, signOut: () -> Unit, refreshRoles: () -> Unit) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val signOut = rememberSessionSignOut(onSignedOut)
    LaunchedEffect(state) { if (state == RoleGateState.SessionExpired) signOut() }
    RoleGateContent(
        state = state,
        onRetry = viewModel::retry,
        onSignOut = signOut,
        modifier = modifier,
        content = { home, signOutNow -> content(home, signOutNow, viewModel::retry) },
    )
}

@Composable
internal fun RoleGateContent(
    state: RoleGateState,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (home: HomeDecision.Home, signOut: () -> Unit) -> Unit,
) {
    val motion = FoxTheme.motion
    // Crossfade by state kind only, so a Ready → Ready update does not fade the whole home.
    AnimatedContent(
        targetState = state,
        contentKey = { it::class },
        transitionSpec = { fadeIn(motion.enter()) togetherWith fadeOut(motion.exit()) },
        label = "roleGate",
        modifier = modifier,
    ) { current ->
        when (current) {
            RoleGateState.Loading, RoleGateState.SessionExpired ->
                SkeletonList(
                    loadingDescription = stringResource(R.string.auth_loading),
                    modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(FoxSpacing.L),
                )

            is RoleGateState.Failed ->
                Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
                    ErrorState(
                        title = stringResource(R.string.role_gate_error_title),
                        message = stringResource(current.message.textRes()),
                        retryLabel = stringResource(R.string.role_gate_retry),
                        onRetry = onRetry,
                        secondaryLabel = stringResource(R.string.role_sign_out),
                        onSecondary = onSignOut,
                    )
                }

            is RoleGateState.Ready ->
                when (val decision = current.decision) {
                    is HomeDecision.Mismatch -> AccountMismatchScreen(decision = decision, onSignOut = onSignOut)
                    is HomeDecision.Home -> content(decision, onSignOut)
                }
        }
    }
}

/** The account is valid but not for this app: explain where to go, and let the user sign out. */
@Composable
fun AccountMismatchScreen(
    decision: HomeDecision.Mismatch,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (icon, title, message) =
        when (decision) {
            HomeDecision.AdminUseWeb ->
                Triple(Icons.Rounded.AdminPanelSettings, R.string.role_admin_title, R.string.role_admin_message)
        }
    Box(modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
        EmptyState(
            icon = icon,
            title = stringResource(title),
            message = stringResource(message),
            actionLabel = stringResource(R.string.role_sign_out),
            onAction = onSignOut,
        )
    }
}

@FoxPreviews
@Composable
private fun AccountMismatchPreview() {
    FoxDropTheme { AccountMismatchScreen(decision = HomeDecision.AdminUseWeb, onSignOut = {}) }
}

@FoxPreviews
@Composable
private fun RoleGateLoadingPreview() {
    FoxDropTheme { RoleGateContent(state = RoleGateState.Loading, onRetry = {}, onSignOut = {}) { _, _ -> } }
}

@FoxPreviews
@Composable
private fun RoleGateFailedPreview() {
    FoxDropTheme {
        RoleGateContent(state = RoleGateState.Failed(AuthMessage.Network), onRetry = {}, onSignOut = {}) { _, _ -> }
    }
}
