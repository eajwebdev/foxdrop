package ph.foxdrop.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ph.foxdrop.core.domain.auth.AuthRepository
import ph.foxdrop.core.domain.auth.SessionStatus

/** Used by home screens: sign out, and notice when the session ends (expired or revoked on the server). */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {
    val signedOut: Flow<Unit> = auth.sessionStatus.filter { it == SessionStatus.SignedOut }.map { }

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }
}

/**
 * For home screens of every app: returns the "sign out" action, and calls [onSignedOut] whenever the session ends
 * (after signing out, or when the server revokes it).
 */
@Composable
fun rememberSessionSignOut(
    onSignedOut: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
): () -> Unit {
    val currentOnSignedOut by rememberUpdatedState(onSignedOut)
    LaunchedEffect(viewModel) { viewModel.signedOut.collect { currentOnSignedOut() } }
    return viewModel::signOut
}
