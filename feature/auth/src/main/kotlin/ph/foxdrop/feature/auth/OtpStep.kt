package ph.foxdrop.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ph.foxdrop.core.common.validation.PhMobileNumber
import ph.foxdrop.core.designsystem.component.FoxButton
import ph.foxdrop.core.designsystem.component.FoxMark
import ph.foxdrop.core.designsystem.component.FoxOtpField
import ph.foxdrop.core.designsystem.component.FoxTextButton
import ph.foxdrop.core.designsystem.component.SecureScreen
import ph.foxdrop.core.designsystem.theme.FoxLayout
import ph.foxdrop.core.designsystem.theme.FoxSpacing

/** Phone OTP entry. Screenshots are blocked on this step (§17). */
@Composable
internal fun OtpStep(
    state: AuthState,
    onAction: (AuthAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    SecureScreen()
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .imePadding()
                .padding(horizontal = FoxSpacing.L, vertical = FoxSpacing.M),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FoxSpacing.M),
    ) {
        FoxMark(contentDescription = null, modifier = Modifier.height(96.dp).padding(top = FoxSpacing.L))
        Text(
            text = stringResource(R.string.auth_otp_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.auth_otp_subtitle, PhMobileNumber.format(state.otpPhone)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        FoxOtpField(
            value = state.otp,
            onValueChange = { onAction(AuthAction.OtpChanged(it)) },
            contentDescription = stringResource(R.string.auth_otp_field),
            length = AuthReducer.OTP_LENGTH,
            isError = state.otpError,
            isSuccess = state.submit == SubmitStatus.Success,
            shakeTrigger = state.otpShake,
            enabled = !state.isBusy,
            onComplete = { onAction(AuthAction.VerifyOtp) },
            modifier =
                Modifier
                    .widthIn(max = FoxLayout.MaxTextWidth)
                    .fillMaxWidth()
                    .semantics { contentType = ContentType.SmsOtpCode },
        )
        state.message?.let { AuthMessageBanner(it, onDismiss = { onAction(AuthAction.DismissMessage) }) }
        FoxButton(
            text = stringResource(R.string.auth_otp_verify),
            onClick = { onAction(AuthAction.VerifyOtp) },
            enabled = state.otp.length == AuthReducer.OTP_LENGTH,
            state = state.submit.toButtonState(),
            loadingDescription = stringResource(R.string.auth_loading),
            modifier = Modifier.widthIn(max = FoxLayout.MaxButtonWidth).fillMaxWidth(),
        )
        if (state.resendSecondsLeft > 0) {
            Text(
                text = stringResource(R.string.auth_otp_resend_in, state.resendSecondsLeft),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = FoxSpacing.S),
            )
        } else {
            FoxTextButton(
                text = stringResource(R.string.auth_otp_resend),
                onClick = { onAction(AuthAction.ResendOtp) },
                enabled = !state.isBusy,
            )
        }
        FoxTextButton(
            text = stringResource(R.string.auth_otp_change_number),
            onClick = { onAction(AuthAction.ChangeNumber) },
            enabled = !state.isBusy,
        )
    }
}
