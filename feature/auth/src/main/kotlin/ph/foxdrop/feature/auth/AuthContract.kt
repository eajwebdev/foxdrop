package ph.foxdrop.feature.auth

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.domain.auth.EmailSignUpOutcome

enum class AuthMode { Login, SignUp }

enum class AuthMethod { Phone, Email }

enum class AuthStep { Form, Otp }

enum class SubmitStatus { Idle, Loading, Success }

enum class AuthField { Name, Phone, Email, Password, Terms }

enum class FieldError { Required, InvalidPhone, InvalidEmail, WeakPassword, TermsRequired }

/** Messages shown in the banner under the form. Text lives in strings.xml (Filipino-ready). */
enum class AuthMessage {
    NotConfigured,
    Network,
    RateLimited,
    InvalidCredentials,
    WrongCode,
    CodeNotSent,
    ConfirmEmail,
    GoogleSoon,
    Unknown,
}

@Immutable
data class AuthState(
    val mode: AuthMode = AuthMode.Login,
    val method: AuthMethod = AuthMethod.Phone,
    val step: AuthStep = AuthStep.Form,
    val fullName: String = "",
    /** Digits typed after the fixed +63 prefix. */
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val acceptedTerms: Boolean = false,
    /** E.164 number the code was sent to. */
    val otpPhone: String = "",
    val otp: String = "",
    val otpError: Boolean = false,
    val otpShake: Int = 0,
    val resendSecondsLeft: Int = 0,
    val submit: SubmitStatus = SubmitStatus.Idle,
    val errors: PersistentMap<AuthField, FieldError> = persistentMapOf(),
    val message: AuthMessage? = null,
) {
    val isBusy: Boolean get() = submit != SubmitStatus.Idle
}

sealed interface AuthAction {
    data class ModeChanged(val mode: AuthMode) : AuthAction

    data class MethodChanged(val method: AuthMethod) : AuthAction

    data class NameChanged(val value: String) : AuthAction

    data class PhoneChanged(val value: String) : AuthAction

    data class EmailChanged(val value: String) : AuthAction

    data class PasswordChanged(val value: String) : AuthAction

    data class TermsChanged(val accepted: Boolean) : AuthAction

    data object Submit : AuthAction

    data class OtpChanged(val value: String) : AuthAction

    data object VerifyOtp : AuthAction

    data object ResendOtp : AuthAction

    data object ChangeNumber : AuthAction

    data object GoogleClicked : AuthAction

    data object DismissMessage : AuthAction

    // Results from the ViewModel (not sent by the UI).
    data class OtpRequested(val error: DomainError?) : AuthAction

    data class OtpVerified(val error: DomainError?) : AuthAction

    data class EmailSignedIn(val error: DomainError?) : AuthAction

    data class EmailSignedUp(val error: DomainError?, val outcome: EmailSignUpOutcome?) : AuthAction

    data object ResendTick : AuthAction
}

/** Work the reducer asks the ViewModel to do. */
sealed interface AuthCommand {
    data class RequestOtp(val phoneE164: String, val fullName: String?) : AuthCommand

    data class VerifyOtp(val phoneE164: String, val code: String) : AuthCommand

    data class SignInWithEmail(val email: String, val password: String) : AuthCommand

    data class SignUpWithEmail(val fullName: String, val email: String, val password: String) : AuthCommand

    data object StartResendTimer : AuthCommand

    data object FinishSignIn : AuthCommand
}

sealed interface AuthEffect {
    data object SignedIn : AuthEffect
}
