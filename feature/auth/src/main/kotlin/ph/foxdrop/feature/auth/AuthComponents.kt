package ph.foxdrop.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import ph.foxdrop.core.designsystem.component.FoxOutlinedButton
import ph.foxdrop.core.designsystem.component.FoxTextButton
import ph.foxdrop.core.designsystem.component.FoxTextField
import ph.foxdrop.core.designsystem.theme.FoxShapes
import ph.foxdrop.core.designsystem.theme.FoxSpacing

@Composable
internal fun MethodSwitch(
    method: AuthMethod,
    enabled: Boolean,
    onChange: (AuthMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = mapOf(AuthMethod.Phone to R.string.auth_method_phone, AuthMethod.Email to R.string.auth_method_email)
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        AuthMethod.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = method == option,
                onClick = { onChange(option) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index, AuthMethod.entries.size),
                icon = {},
            ) {
                Text(stringResource(labels.getValue(option)), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
internal fun NameField(
    state: AuthState,
    onAction: (AuthAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    FoxTextField(
        value = state.fullName,
        onValueChange = { onAction(AuthAction.NameChanged(it)) },
        label = stringResource(R.string.auth_field_name),
        leadingIcon = Icons.Rounded.Badge,
        errorMessage = state.errors[AuthField.Name]?.let { errorText(it) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        modifier = modifier.fillMaxWidth().semantics { contentType = ContentType.PersonFullName },
    )
}

@Composable
internal fun PhoneField(
    state: AuthState,
    onAction: (AuthAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    FoxTextField(
        value = state.phone,
        onValueChange = { onAction(AuthAction.PhoneChanged(it)) },
        label = stringResource(R.string.auth_field_phone),
        placeholder = stringResource(R.string.auth_field_phone_placeholder),
        prefix = "+63 ",
        leadingIcon = Icons.Rounded.Phone,
        errorMessage = state.errors[AuthField.Phone]?.let { errorText(it) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onAction(AuthAction.Submit) }),
        modifier = modifier.fillMaxWidth().semantics { contentType = ContentType.PhoneNumber },
    )
}

@Composable
internal fun EmailFields(
    state: AuthState,
    onAction: (AuthAction) -> Unit,
    isSignUp: Boolean,
    modifier: Modifier = Modifier,
) {
    var showPassword by rememberSaveable { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(FoxSpacing.S)) {
        FoxTextField(
            value = state.email,
            onValueChange = { onAction(AuthAction.EmailChanged(it)) },
            label = stringResource(R.string.auth_field_email),
            leadingIcon = Icons.Rounded.Email,
            errorMessage = state.errors[AuthField.Email]?.let { errorText(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.EmailAddress },
        )
        FoxTextField(
            value = state.password,
            onValueChange = { onAction(AuthAction.PasswordChanged(it)) },
            label = stringResource(R.string.auth_field_password),
            leadingIcon = Icons.Rounded.Lock,
            errorMessage = state.errors[AuthField.Password]?.let { errorText(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAction(AuthAction.Submit) }),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription =
                            stringResource(
                                if (showPassword) R.string.auth_hide_password else R.string.auth_show_password,
                            ),
                    )
                }
            },
            modifier =
                Modifier.fillMaxWidth().semantics {
                    contentType = if (isSignUp) ContentType.NewPassword else ContentType.Password
                },
        )
    }
}

@Composable
internal fun TermsRow(
    state: AuthState,
    onAction: (AuthAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val error = state.errors[AuthField.Terms]
    Column(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = state.acceptedTerms,
                        role = Role.Checkbox,
                        onValueChange = { onAction(AuthAction.TermsChanged(it)) },
                    ),
        ) {
            Checkbox(checked = state.acceptedTerms, onCheckedChange = null)
            Text(
                text = stringResource(R.string.auth_terms),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = FoxSpacing.XS),
            )
        }
        if (error != null) {
            Text(
                text = errorText(error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = FoxSpacing.M),
            )
        }
    }
}

@Composable
internal fun AuthMessageBanner(
    message: AuthMessage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isInfo = message == AuthMessage.ConfirmEmail || message == AuthMessage.GoogleSoon
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = FoxShapes.Medium,
        color = if (isInfo) colors.secondaryContainer else colors.errorContainer,
        contentColor = if (isInfo) colors.onSecondaryContainer else colors.onErrorContainer,
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = FoxSpacing.M, end = FoxSpacing.XXS),
        ) {
            Text(
                text = stringResource(message.textRes()),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(vertical = FoxSpacing.S),
            )
            FoxTextButton(text = stringResource(R.string.auth_message_dismiss), onClick = onDismiss)
        }
    }
}

@Composable
internal fun SocialSignIn(
    onGoogle: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FoxSpacing.S),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.auth_or),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = FoxSpacing.S),
            )
            HorizontalDivider(Modifier.weight(1f))
        }
        FoxOutlinedButton(
            text = stringResource(R.string.auth_google),
            onClick = onGoogle,
            enabled = enabled,
            leadingIcon = painterResource(R.drawable.ic_google_logo),
            tintIcon = false, // Google's "G" keeps its own colours (Sign-In branding guidelines)
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun errorText(error: FieldError): String =
    stringResource(
        when (error) {
            FieldError.Required -> R.string.auth_error_required
            FieldError.InvalidPhone -> R.string.auth_error_phone
            FieldError.InvalidEmail -> R.string.auth_error_email
            FieldError.WeakPassword -> R.string.auth_error_password
            FieldError.TermsRequired -> R.string.auth_error_terms
        },
    )

internal fun AuthMessage.textRes(): Int =
    when (this) {
        AuthMessage.NotConfigured -> R.string.auth_message_not_configured
        AuthMessage.Network -> R.string.auth_message_network
        AuthMessage.RateLimited -> R.string.auth_message_rate_limited
        AuthMessage.InvalidCredentials -> R.string.auth_message_invalid
        AuthMessage.WrongCode -> R.string.auth_message_wrong_code
        AuthMessage.CodeNotSent -> R.string.auth_message_code_not_sent
        AuthMessage.ConfirmEmail -> R.string.auth_message_confirm_email
        AuthMessage.GoogleSoon -> R.string.auth_message_google_soon
        AuthMessage.Unknown -> R.string.auth_message_unknown
    }
