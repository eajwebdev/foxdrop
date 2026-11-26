package ph.foxdrop.feature.auth

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.validation.EmailAddress
import ph.foxdrop.core.common.validation.Password
import ph.foxdrop.core.common.validation.PhMobileNumber
import ph.foxdrop.core.domain.auth.EmailSignUpOutcome

/** New state plus optional work for the ViewModel. */
data class AuthNext(val state: AuthState, val command: AuthCommand? = null)

/**
 * Pure state machine for the auth screen (§16 MVI): no coroutines, no Android, fully unit-tested.
 */
object AuthReducer {
    const val OTP_LENGTH = 6

    /** Matches the server's wait between codes (backend config foxdrop.auth.otp.resend_after_seconds). */
    const val RESEND_SECONDS = 60
    private const val MIN_NAME_LENGTH = 2

    fun reduce(state: AuthState, action: AuthAction): AuthNext =
        when (action) {
            is AuthAction.ModeChanged -> edit(state) { copy(mode = action.mode) }

            is AuthAction.MethodChanged -> edit(state) { copy(method = action.method) }

            is AuthAction.NameChanged -> edit(state) { copy(fullName = action.value).clear(AuthField.Name) }

            is AuthAction.PhoneChanged -> edit(state) {
                copy(phone = PhMobileNumber.nationalDigits(action.value)).clear(AuthField.Phone)
            }

            is AuthAction.EmailChanged -> edit(state) { copy(email = action.value.trim()).clear(AuthField.Email) }

            is AuthAction.PasswordChanged -> edit(state) { copy(password = action.value).clear(AuthField.Password) }

            is AuthAction.TermsChanged -> edit(state) { copy(acceptedTerms = action.accepted).clear(AuthField.Terms) }

            AuthAction.Submit -> submit(state)

            is AuthAction.OtpChanged -> otpChanged(state, action.value)

            AuthAction.VerifyOtp -> verify(state)

            AuthAction.ResendOtp ->
                if (state.resendSecondsLeft > 0 || state.isBusy) {
                    AuthNext(state)
                } else {
                    AuthNext(
                        state.copy(submit = SubmitStatus.Loading, message = null),
                        AuthCommand.RequestOtp(state.otpPhone, nameForSignUp(state)),
                    )
                }

            // The resend countdown keeps running: going back must not become a way around the SMS cooldown.
            AuthAction.ChangeNumber -> AuthNext(state.copy(step = AuthStep.Form, otp = "", otpError = false))

            AuthAction.GoogleClicked -> google(state)

            is AuthAction.GoogleCredentialReceived ->
                if (state.submit == SubmitStatus.Loading) {
                    AuthNext(state, AuthCommand.SignInWithGoogle(action.idToken))
                } else {
                    AuthNext(state)
                }

            AuthAction.GoogleCancelled -> AuthNext(state.copy(submit = SubmitStatus.Idle))

            AuthAction.GoogleUnavailable -> AuthNext(
                state.copy(submit = SubmitStatus.Idle, message = AuthMessage.GoogleFailed),
            )

            is AuthAction.GoogleSignedIn -> signedIn(state, action.error)

            is AuthAction.SmsCodeReceived ->
                if (state.step == AuthStep.Otp) {
                    otpChanged(state, action.code).let { reduce(it.state, AuthAction.VerifyOtp) }
                } else {
                    AuthNext(state)
                }

            AuthAction.DismissMessage -> AuthNext(state.copy(message = null))

            is AuthAction.OtpRequested -> otpRequested(state, action.error)

            is AuthAction.OtpVerified -> otpVerified(state, action.error)

            is AuthAction.EmailSignedIn -> signedIn(state, action.error)

            is AuthAction.EmailSignedUp -> emailSignedUp(state, action)

            AuthAction.ResendTick -> AuthNext(
                state.copy(resendSecondsLeft = (state.resendSecondsLeft - 1).coerceAtLeast(0)),
            )
        }

    /** Field edits are ignored while a request is running so the request matches what is on screen. */
    private inline fun edit(state: AuthState, change: AuthState.() -> AuthState): AuthNext =
        if (state.isBusy) AuthNext(state) else AuthNext(state.change().copy(message = null))

    private fun AuthState.clear(field: AuthField) = copy(errors = errors.removing(field))

    private fun google(state: AuthState): AuthNext =
        when {
            !state.googleEnabled -> AuthNext(state.copy(message = AuthMessage.GoogleSoon))
            state.isBusy -> AuthNext(state)
            else -> AuthNext(state.copy(submit = SubmitStatus.Loading, message = null), AuthCommand.LaunchGoogle)
        }

    private fun signedIn(state: AuthState, error: DomainError?): AuthNext =
        if (error == null) {
            AuthNext(state.copy(submit = SubmitStatus.Success), AuthCommand.FinishSignIn)
        } else {
            AuthNext(state.copy(submit = SubmitStatus.Idle, message = error.toMessage()))
        }

    private fun submit(state: AuthState): AuthNext {
        val errors = validate(state)
        return when {
            state.isBusy -> AuthNext(state)

            errors.isNotEmpty() -> AuthNext(state.copy(errors = errors, message = null))

            // Same number while the code just sent is still valid: go back to the code instead of sending another SMS.
            state.method == AuthMethod.Phone && state.resendSecondsLeft > 0 && phoneE164(state) == state.otpPhone ->
                AuthNext(state.copy(step = AuthStep.Otp, message = null))

            else ->
                AuthNext(
                    state.copy(errors = persistentMapOf(), submit = SubmitStatus.Loading, message = null),
                    submitCommand(state),
                )
        }
    }

    private fun submitCommand(state: AuthState): AuthCommand =
        when (state.method) {
            AuthMethod.Phone ->
                AuthCommand.RequestOtp(
                    checkNotNull(phoneE164(state)) { "validate() accepts only valid numbers" },
                    nameForSignUp(state),
                )

            AuthMethod.Email ->
                when (state.mode) {
                    AuthMode.Login -> AuthCommand.SignInWithEmail(state.email, state.password)
                    AuthMode.SignUp -> AuthCommand.SignUpWithEmail(state.fullName.trim(), state.email, state.password)
                }
        }

    fun validate(state: AuthState): PersistentMap<AuthField, FieldError> {
        var errors = persistentMapOf<AuthField, FieldError>()
        val signUp = state.mode == AuthMode.SignUp
        if (signUp &&
            state.fullName.trim().length < MIN_NAME_LENGTH
        ) {
            errors = errors.putting(AuthField.Name, FieldError.Required)
        }
        when (state.method) {
            AuthMethod.Phone ->
                if (phoneE164(state) == null) {
                    errors =
                        errors.putting(
                            AuthField.Phone,
                            if (state.phone.isEmpty()) FieldError.Required else FieldError.InvalidPhone,
                        )
                }

            AuthMethod.Email -> {
                when {
                    state.email.isEmpty() -> errors = errors.putting(AuthField.Email, FieldError.Required)

                    !EmailAddress.isValid(state.email) ->
                        errors =
                            errors.putting(AuthField.Email, FieldError.InvalidEmail)
                }
                when {
                    state.password.isEmpty() -> errors = errors.putting(AuthField.Password, FieldError.Required)

                    signUp && !Password.isStrongEnough(state.password) ->
                        errors = errors.putting(AuthField.Password, FieldError.WeakPassword)
                }
            }
        }
        if (signUp && !state.acceptedTerms) errors = errors.putting(AuthField.Terms, FieldError.TermsRequired)
        return errors
    }

    private fun phoneE164(state: AuthState): String? = PhMobileNumber.normalize(state.phone)

    private fun nameForSignUp(state: AuthState): String? =
        state.fullName.trim().takeIf { state.mode == AuthMode.SignUp && it.isNotEmpty() }

    private fun otpChanged(state: AuthState, value: String): AuthNext {
        if (state.isBusy) return AuthNext(state)
        val digits = value.filter(Char::isDigit).take(OTP_LENGTH)
        return AuthNext(state.copy(otp = digits, otpError = false, message = null))
    }

    private fun verify(state: AuthState): AuthNext =
        if (state.isBusy || state.otp.length != OTP_LENGTH) {
            AuthNext(state)
        } else {
            AuthNext(
                state.copy(submit = SubmitStatus.Loading, otpError = false, message = null),
                AuthCommand.VerifyOtp(state.otpPhone, state.otp),
            )
        }

    private fun otpRequested(state: AuthState, error: DomainError?): AuthNext =
        if (error == null) {
            AuthNext(
                state.copy(
                    step = AuthStep.Otp,
                    otpPhone = phoneE164(state) ?: state.otpPhone,
                    otp = "",
                    otpError = false,
                    submit = SubmitStatus.Idle,
                    resendSecondsLeft = RESEND_SECONDS,
                ),
                AuthCommand.StartResendTimer,
            )
        } else {
            // A 400/422 here means the number or SMS sending was refused — not "wrong credentials".
            val message = if (error is DomainError.Validation) AuthMessage.CodeNotSent else error.toMessage()
            AuthNext(state.copy(submit = SubmitStatus.Idle, message = message))
        }

    private fun otpVerified(state: AuthState, error: DomainError?): AuthNext =
        when (error) {
            null -> AuthNext(state.copy(submit = SubmitStatus.Success), AuthCommand.FinishSignIn)

            is DomainError.Validation, DomainError.Unauthorized ->
                AuthNext(
                    state.copy(
                        submit = SubmitStatus.Idle,
                        otp = "",
                        otpError = true,
                        otpShake = state.otpShake + 1,
                        message = AuthMessage.WrongCode,
                    ),
                )

            else -> AuthNext(state.copy(submit = SubmitStatus.Idle, message = error.toMessage()))
        }

    private fun emailSignedUp(state: AuthState, action: AuthAction.EmailSignedUp): AuthNext =
        when {
            action.error != null -> AuthNext(state.copy(submit = SubmitStatus.Idle, message = action.error.toMessage()))

            action.outcome == EmailSignUpOutcome.ConfirmationEmailSent ->
                AuthNext(
                    state.copy(
                        submit = SubmitStatus.Idle,
                        mode = AuthMode.Login,
                        password = "",
                        message = AuthMessage.ConfirmEmail,
                    ),
                )

            else -> AuthNext(state.copy(submit = SubmitStatus.Success), AuthCommand.FinishSignIn)
        }
}

/** The one mapping from domain errors to auth/role-gate messages. */
internal fun DomainError.toMessage(): AuthMessage =
    when (this) {
        DomainError.NotConfigured -> AuthMessage.NotConfigured
        DomainError.Network -> AuthMessage.Network
        DomainError.RateLimited -> AuthMessage.RateLimited
        DomainError.Unauthorized, is DomainError.Validation -> AuthMessage.InvalidCredentials
        else -> AuthMessage.Unknown
    }
