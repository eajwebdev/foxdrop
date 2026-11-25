package ph.foxdrop.feature.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.domain.auth.EmailSignUpOutcome

class AuthReducerTest {
    private fun reduce(state: AuthState, vararg actions: AuthAction): AuthNext =
        actions.fold(AuthNext(state)) { next, action -> AuthReducer.reduce(next.state, action) }

    @Test
    fun `phone login with a valid number asks for an OTP`() {
        val next = reduce(AuthState(), AuthAction.PhoneChanged("0917 123 4567"), AuthAction.Submit)
        assertEquals("9171234567", next.state.phone)
        assertEquals(SubmitStatus.Loading, next.state.submit)
        assertEquals(AuthCommand.RequestOtp("+639171234567", fullName = null), next.command)
    }

    @Test
    fun `phone input keeps the 10 national digits however it is typed`() {
        assertEquals("9171234567", reduce(AuthState(), AuthAction.PhoneChanged("+63 917-123-45678")).state.phone)
        assertEquals("9171234567", reduce(AuthState(), AuthAction.PhoneChanged("639171234567")).state.phone)
        assertEquals("917", reduce(AuthState(), AuthAction.PhoneChanged("0917")).state.phone)
        assertEquals("63", reduce(AuthState(), AuthAction.PhoneChanged("63")).state.phone)
    }

    @Test
    fun `invalid or empty phone shows a field error and no request`() {
        val empty = reduce(AuthState(), AuthAction.Submit)
        assertEquals(FieldError.Required, empty.state.errors[AuthField.Phone])
        assertNull(empty.command)

        val invalid = reduce(AuthState(), AuthAction.PhoneChanged("12345"), AuthAction.Submit)
        assertEquals(FieldError.InvalidPhone, invalid.state.errors[AuthField.Phone])
        assertEquals(SubmitStatus.Idle, invalid.state.submit)
    }

    @Test
    fun `editing a field clears its error`() {
        val withError = reduce(AuthState(), AuthAction.Submit).state
        val edited = reduce(withError, AuthAction.PhoneChanged("9")).state
        assertNull(edited.errors[AuthField.Phone])
    }

    @Test
    fun `sign up needs a name and accepted terms`() {
        val next = reduce(AuthState(mode = AuthMode.SignUp, phone = "9171234567"), AuthAction.Submit)
        assertEquals(FieldError.Required, next.state.errors[AuthField.Name])
        assertEquals(FieldError.TermsRequired, next.state.errors[AuthField.Terms])
        assertNull(next.command)

        val ok =
            reduce(
                next.state,
                AuthAction.NameChanged("  Juan Dela Cruz "),
                AuthAction.TermsChanged(true),
                AuthAction.Submit,
            )
        assertEquals(AuthCommand.RequestOtp("+639171234567", "Juan Dela Cruz"), ok.command)
    }

    @Test
    fun `email login validates and signs in`() {
        val start = AuthState(method = AuthMethod.Email)
        val invalid = reduce(start, AuthAction.EmailChanged("juan@"), AuthAction.Submit)
        assertEquals(FieldError.InvalidEmail, invalid.state.errors[AuthField.Email])
        assertEquals(FieldError.Required, invalid.state.errors[AuthField.Password])

        val ok =
            reduce(
                start,
                AuthAction.EmailChanged(" juan@example.ph "),
                AuthAction.PasswordChanged("x"),
                AuthAction.Submit,
            )
        assertEquals(AuthCommand.SignInWithEmail("juan@example.ph", "x"), ok.command)
    }

    @Test
    fun `email sign up requires a strong password`() {
        val start =
            AuthState(
                mode = AuthMode.SignUp,
                method = AuthMethod.Email,
                fullName = "Juan",
                email = "juan@example.ph",
                acceptedTerms = true,
            )
        val weak = reduce(start, AuthAction.PasswordChanged("short"), AuthAction.Submit)
        assertEquals(FieldError.WeakPassword, weak.state.errors[AuthField.Password])

        val strong = reduce(start, AuthAction.PasswordChanged("foxdrop2026"), AuthAction.Submit)
        assertEquals(AuthCommand.SignUpWithEmail("Juan", "juan@example.ph", "foxdrop2026"), strong.command)
    }

    @Test
    fun `OTP sent moves to the code step and starts the resend timer`() {
        val loading = reduce(AuthState(phone = "9171234567"), AuthAction.Submit).state
        val next = reduce(loading, AuthAction.OtpRequested(error = null))
        assertEquals(AuthStep.Otp, next.state.step)
        assertEquals("+639171234567", next.state.otpPhone)
        assertEquals(AuthReducer.RESEND_SECONDS, next.state.resendSecondsLeft)
        assertEquals(AuthCommand.StartResendTimer, next.command)
    }

    @Test
    fun `OTP request failure shows a message and stays on the form`() {
        val loading = reduce(AuthState(phone = "9171234567"), AuthAction.Submit).state
        val next = reduce(loading, AuthAction.OtpRequested(DomainError.RateLimited)).state
        assertEquals(AuthStep.Form, next.step)
        assertEquals(AuthMessage.RateLimited, next.message)
        assertEquals(SubmitStatus.Idle, next.submit)
    }

    @Test
    fun `verify only runs with six digits`() {
        val otpStep = AuthState(step = AuthStep.Otp, otpPhone = "+639171234567")
        assertNull(reduce(otpStep, AuthAction.OtpChanged("123"), AuthAction.VerifyOtp).command)

        val next = reduce(otpStep, AuthAction.OtpChanged("12a34567"), AuthAction.VerifyOtp)
        assertEquals("123456", next.state.otp)
        assertEquals(AuthCommand.VerifyOtp("+639171234567", "123456"), next.command)
    }

    @Test
    fun `wrong code clears the boxes and shakes`() {
        val verifying =
            AuthState(step = AuthStep.Otp, otpPhone = "+639171234567", otp = "123456", submit = SubmitStatus.Loading)
        val next = reduce(
            verifying,
            AuthAction.OtpVerified(DomainError.Validation("request", "Token has expired or is invalid")),
        ).state
        assertTrue(next.otpError)
        assertEquals("", next.otp)
        assertEquals(1, next.otpShake)
        assertEquals(AuthMessage.WrongCode, next.message)
    }

    @Test
    fun `correct code succeeds and finishes sign in`() {
        val verifying = AuthState(step = AuthStep.Otp, otp = "123456", submit = SubmitStatus.Loading)
        val next = reduce(verifying, AuthAction.OtpVerified(null))
        assertEquals(SubmitStatus.Success, next.state.submit)
        assertEquals(AuthCommand.FinishSignIn, next.command)
    }

    @Test
    fun `network error on verify keeps the code`() {
        val verifying = AuthState(step = AuthStep.Otp, otp = "123456", submit = SubmitStatus.Loading)
        val next = reduce(verifying, AuthAction.OtpVerified(DomainError.Network)).state
        assertEquals("123456", next.otp)
        assertFalse(next.otpError)
        assertEquals(AuthMessage.Network, next.message)
    }

    @Test
    fun `resend waits for the countdown`() {
        val waiting = AuthState(step = AuthStep.Otp, otpPhone = "+639171234567", resendSecondsLeft = 2)
        assertNull(reduce(waiting, AuthAction.ResendOtp).command)
        val ticked = reduce(waiting, AuthAction.ResendTick, AuthAction.ResendTick, AuthAction.ResendTick).state
        assertEquals(0, ticked.resendSecondsLeft)
        assertEquals(AuthCommand.RequestOtp("+639171234567", null), reduce(ticked, AuthAction.ResendOtp).command)
    }

    @Test
    fun `change number goes back to the form and keeps the cooldown`() {
        val next = reduce(
            AuthState(step = AuthStep.Otp, otp = "12", resendSecondsLeft = 9),
            AuthAction.ChangeNumber,
        ).state
        assertEquals(AuthStep.Form, next.step)
        assertEquals("", next.otp)
        assertEquals(9, next.resendSecondsLeft)
    }

    @Test
    fun `same number during the cooldown returns to the code without a new SMS`() {
        val back = AuthState(phone = "9171234567", otpPhone = "+639171234567", resendSecondsLeft = 20)
        val same = reduce(back, AuthAction.Submit)
        assertEquals(AuthStep.Otp, same.state.step)
        assertNull(same.command)

        val other = reduce(back, AuthAction.PhoneChanged("9181234567"), AuthAction.Submit)
        assertEquals(AuthCommand.RequestOtp("+639181234567", null), other.command)
    }

    @Test
    fun `a refused number says the code could not be sent`() {
        val loading = reduce(AuthState(phone = "9171234567"), AuthAction.Submit).state
        val next = reduce(loading, AuthAction.OtpRequested(DomainError.Validation("request", "invalid phone"))).state
        assertEquals(AuthMessage.CodeNotSent, next.message)
    }

    @Test
    fun `edits are ignored while a request runs`() {
        val busy = AuthState(phone = "9171234567", submit = SubmitStatus.Loading)
        assertEquals(busy, reduce(busy, AuthAction.PhoneChanged("9000000000")).state)
        assertEquals(busy, reduce(busy, AuthAction.Submit).state)
        assertEquals(busy, reduce(busy, AuthAction.OtpChanged("1")).state)
    }

    @Test
    fun `email sign up that needs confirmation returns to login with a message`() {
        val loading =
            AuthState(
                mode = AuthMode.SignUp,
                method = AuthMethod.Email,
                password = "foxdrop2026",
                submit = SubmitStatus.Loading,
            )
        val next = reduce(loading, AuthAction.EmailSignedUp(null, EmailSignUpOutcome.ConfirmationEmailSent)).state
        assertEquals(AuthMode.Login, next.mode)
        assertEquals("", next.password)
        assertEquals(AuthMessage.ConfirmEmail, next.message)

        val signedIn = reduce(loading, AuthAction.EmailSignedUp(null, EmailSignUpOutcome.SignedIn))
        assertEquals(AuthCommand.FinishSignIn, signedIn.command)

        val failed = reduce(loading, AuthAction.EmailSignedUp(DomainError.Conflict("exists"), null)).state
        assertEquals(AuthMessage.Unknown, failed.message)
    }

    @Test
    fun `email sign in results`() {
        val loading = AuthState(method = AuthMethod.Email, submit = SubmitStatus.Loading)
        assertEquals(AuthCommand.FinishSignIn, reduce(loading, AuthAction.EmailSignedIn(null)).command)
        assertEquals(
            AuthMessage.InvalidCredentials,
            reduce(loading, AuthAction.EmailSignedIn(DomainError.Unauthorized)).state.message,
        )
        assertEquals(
            AuthMessage.NotConfigured,
            reduce(loading, AuthAction.EmailSignedIn(DomainError.NotConfigured)).state.message,
        )
    }

    @Test
    fun `google and dismiss messages`() {
        val google = reduce(AuthState(), AuthAction.GoogleClicked).state
        assertEquals(AuthMessage.GoogleSoon, google.message)
        assertNull(reduce(google, AuthAction.DismissMessage).state.message)
    }

    @Test
    fun `mode and method switches`() {
        val next = reduce(
            AuthState(),
            AuthAction.ModeChanged(AuthMode.SignUp),
            AuthAction.MethodChanged(AuthMethod.Email),
        ).state
        assertEquals(AuthMode.SignUp, next.mode)
        assertEquals(AuthMethod.Email, next.method)
    }
}
