package ph.foxdrop.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.common.result.errorOrNull
import ph.foxdrop.core.domain.auth.AuthRepository
import ph.foxdrop.core.domain.auth.SessionStatus
import ph.foxdrop.feature.auth.google.GoogleNonce

/**
 * Runs [AuthReducer] and the work it asks for. The reducer stays pure; this class only does I/O and timers.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository,
    @param:GoogleWebClientId private val googleClientId: String,
) : ViewModel() {
    private val _state = MutableStateFlow(AuthState(googleEnabled = googleClientId.isNotBlank()))
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val effects = Channel<AuthEffect>(Channel.BUFFERED)
    val effect: Flow<AuthEffect> = effects.receiveAsFlow()

    private var resendTimer: Job? = null
    private var signedInSent = false

    /** Raw nonce of the Google request in flight; Google signs its SHA-256, the API checks it against this. */
    private var googleNonce: String? = null

    init {
        // On a slow cold start the stored session can finish loading after the 1.5 s splash cap already showed this
        // screen. Move on as soon as it does — unless a sign-in started here is running, which finishes by itself.
        viewModelScope.launch {
            auth.sessionStatus.filterIsInstance<SessionStatus.SignedIn>().collect {
                if (_state.value.submit == SubmitStatus.Idle) sendSignedIn()
            }
        }
    }

    fun onAction(action: AuthAction) {
        var command: AuthCommand? = null
        _state.update { current ->
            val next = AuthReducer.reduce(current, action)
            command = next.command
            next.state
        }
        command?.let(::run)
    }

    private fun run(command: AuthCommand) {
        when (command) {
            is AuthCommand.RequestOtp ->
                launchResult {
                    AuthAction.OtpRequested(auth.requestPhoneOtp(command.phoneE164, command.fullName).errorOrNull())
                }

            is AuthCommand.VerifyOtp ->
                launchResult {
                    AuthAction.OtpVerified(auth.verifyPhoneOtp(command.phoneE164, command.code).errorOrNull())
                }

            is AuthCommand.SignInWithEmail ->
                launchResult {
                    AuthAction.EmailSignedIn(auth.signInWithEmail(command.email, command.password).errorOrNull())
                }

            is AuthCommand.SignUpWithEmail ->
                launchResult {
                    when (val result = auth.signUpWithEmail(command.fullName, command.email, command.password)) {
                        is FoxResult.Success -> AuthAction.EmailSignedUp(error = null, outcome = result.value)
                        is FoxResult.Failure -> AuthAction.EmailSignedUp(error = result.error, outcome = null)
                    }
                }

            AuthCommand.StartResendTimer -> startResendTimer()

            AuthCommand.LaunchGoogle -> {
                val nonce = GoogleNonce.create()
                googleNonce = nonce.raw
                viewModelScope.launch { effects.send(AuthEffect.LaunchGoogleSignIn(googleClientId, nonce.sha256)) }
            }

            is AuthCommand.SignInWithGoogle -> {
                val rawNonce = googleNonce.orEmpty()
                googleNonce = null
                launchResult {
                    AuthAction.GoogleSignedIn(auth.signInWithGoogle(command.idToken, rawNonce).errorOrNull())
                }
            }

            AuthCommand.FinishSignIn ->
                viewModelScope.launch {
                    delay(SUCCESS_HOLD) // let the ✓ / green boxes show before leaving
                    sendSignedIn()
                }
        }
    }

    private suspend fun sendSignedIn() {
        if (signedInSent) return
        signedInSent = true
        effects.send(AuthEffect.SignedIn)
    }

    private fun launchResult(block: suspend () -> AuthAction) {
        viewModelScope.launch { onAction(block()) }
    }

    private fun startResendTimer() {
        resendTimer?.cancel()
        resendTimer =
            viewModelScope.launch {
                while (_state.value.resendSecondsLeft > 0) {
                    delay(1.seconds)
                    onAction(AuthAction.ResendTick)
                }
            }
    }

    private companion object {
        val SUCCESS_HOLD = 600.milliseconds
    }
}
