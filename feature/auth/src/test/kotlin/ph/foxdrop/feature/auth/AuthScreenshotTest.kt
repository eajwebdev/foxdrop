package ph.foxdrop.feature.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.collections.immutable.persistentMapOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import ph.foxdrop.core.testing.screenshot.FoxScreenshotVariant
import ph.foxdrop.core.testing.screenshot.captureFoxScreenshots

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AuthScreenshotTest {
    @Test
    fun login() = captureFoxScreenshots("auth_login") { AuthScreen(AuthState(), onAction = {}) }

    @Test
    fun signUp() =
        captureFoxScreenshots("auth_signup") {
            AuthScreen(
                AuthState(
                    mode = AuthMode.SignUp,
                    fullName = "Juan Dela Cruz",
                    phone = "9171234567",
                    acceptedTerms = true,
                ),
                onAction = {},
            )
        }

    @Test
    fun signUpEmailWithErrors() =
        captureFoxScreenshots(
            "auth_signup_email_errors",
            listOf(FoxScreenshotVariant.PhoneLight, FoxScreenshotVariant.PhoneDark),
        ) {
            AuthScreen(
                AuthState(
                    mode = AuthMode.SignUp,
                    method = AuthMethod.Email,
                    email = "juan@",
                    errors =
                        persistentMapOf(
                            AuthField.Name to FieldError.Required,
                            AuthField.Email to FieldError.InvalidEmail,
                            AuthField.Password to FieldError.Required,
                            AuthField.Terms to FieldError.TermsRequired,
                        ),
                ),
                onAction = {},
            )
        }

    @Test
    fun loading() =
        captureFoxScreenshots("auth_loading", listOf(FoxScreenshotVariant.PhoneLight)) {
            AuthScreen(AuthState(phone = "9171234567", submit = SubmitStatus.Loading), onAction = {})
        }

    @Test
    fun networkError() =
        captureFoxScreenshots("auth_error", listOf(FoxScreenshotVariant.PhoneLight, FoxScreenshotVariant.PhoneDark)) {
            AuthScreen(AuthState(phone = "9171234567", message = AuthMessage.Network), onAction = {})
        }

    @Test
    fun otp() =
        captureFoxScreenshots("auth_otp") {
            AuthScreen(
                AuthState(step = AuthStep.Otp, otpPhone = "+639171234567", otp = "1234", resendSecondsLeft = 24),
                onAction = {},
            )
        }

    @Test
    fun otpWrongCode() =
        captureFoxScreenshots("auth_otp_wrong", listOf(FoxScreenshotVariant.PhoneLight)) {
            AuthScreen(
                AuthState(
                    step = AuthStep.Otp,
                    otpPhone = "+639171234567",
                    otpError = true,
                    message = AuthMessage.WrongCode,
                ),
                onAction = {},
            )
        }

    @Test
    fun otpSuccess() =
        captureFoxScreenshots("auth_otp_success", listOf(FoxScreenshotVariant.PhoneLight)) {
            AuthScreen(
                AuthState(
                    step = AuthStep.Otp,
                    otpPhone = "+639171234567",
                    otp = "123456",
                    submit = SubmitStatus.Success,
                ),
                onAction = {},
            )
        }
}
