package ph.foxdrop.feature.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.collections.immutable.persistentListOf
import ph.foxdrop.core.designsystem.component.FoxButton
import ph.foxdrop.core.designsystem.component.FoxButtonState
import ph.foxdrop.core.designsystem.component.FoxCard
import ph.foxdrop.core.designsystem.component.FoxWordmark
import ph.foxdrop.core.designsystem.component.PillTabSwitch
import ph.foxdrop.core.designsystem.component.foxWordmarkPainter
import ph.foxdrop.core.designsystem.motion.sharedAxisXEnter
import ph.foxdrop.core.designsystem.motion.sharedAxisXExit
import ph.foxdrop.core.designsystem.preview.FoxPreviews
import ph.foxdrop.core.designsystem.theme.FoxDropTheme
import ph.foxdrop.core.designsystem.theme.FoxLayout
import ph.foxdrop.core.designsystem.theme.FoxMotion
import ph.foxdrop.core.designsystem.theme.FoxShapes
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.designsystem.theme.FoxTheme
import ph.foxdrop.feature.auth.google.GoogleSignInResult
import ph.foxdrop.feature.auth.google.requestGoogleIdToken
import ph.foxdrop.feature.auth.sms.SmsCodeAutofill

/**
 * Route entry: gets the ViewModel, runs the platform parts the ViewModel cannot (Google account picker, SMS consent),
 * and forwards the one-time "signed in" effect.
 */
@Composable
fun AuthRoute(
    onSignedIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnSignedIn by rememberUpdatedState(onSignedIn)
    val activityContext = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AuthEffect.SignedIn -> currentOnSignedIn()

                is AuthEffect.LaunchGoogleSignIn -> {
                    val result =
                        try {
                            requestGoogleIdToken(activityContext, effect.clientId, effect.hashedNonce)
                        } catch (cancelled: CancellationException) {
                            // Rotation or leaving the screen kills the picker; the ViewModel survives, so reset it.
                            viewModel.onAction(AuthAction.GoogleCancelled)
                            throw cancelled
                        }
                    viewModel.onAction(
                        when (result) {
                            is GoogleSignInResult.Token -> AuthAction.GoogleCredentialReceived(result.idToken)
                            GoogleSignInResult.Cancelled -> AuthAction.GoogleCancelled
                            GoogleSignInResult.Unavailable -> AuthAction.GoogleUnavailable
                        },
                    )
                }
            }
        }
    }
    if (state.step == AuthStep.Otp) {
        SmsCodeAutofill(onCode = { viewModel.onAction(AuthAction.SmsCodeReceived(it)) })
    }
    AuthScreen(state = state, onAction = viewModel::onAction, modifier = modifier)
}

/** One screen for Log In ⇄ Sign Up and the OTP step (§11A.2). Stateless: previews and tests need no Hilt. */
@Composable
fun AuthScreen(
    state: AuthState,
    onAction: (AuthAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = FoxTheme.motion
    BackHandler(enabled = state.step == AuthStep.Otp && !state.isBusy) { onAction(AuthAction.ChangeNumber) }
    AnimatedContent(
        targetState = state.step,
        transitionSpec = {
            val forward = targetState == AuthStep.Otp
            motion.sharedAxisXEnter(forward) togetherWith motion.sharedAxisXExit(forward)
        },
        modifier = modifier.fillMaxSize(),
        label = "authStep",
    ) { step ->
        when (step) {
            AuthStep.Form -> AuthFormStep(state, onAction)
            AuthStep.Otp -> OtpStep(state, onAction)
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun AuthFormStep(
    state: AuthState,
    onAction: (AuthAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = FoxTheme.motion
    val wordmark = foxWordmarkPainter() // decoded once, not on every Log In ⇄ Sign Up switch
    SharedTransitionLayout(modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = state.mode,
            transitionSpec = {
                fadeIn(motion.enter()) togetherWith fadeOut(motion.exit()) using
                    SizeTransform(clip = false)
            },
            label = "authMode",
        ) { mode ->
            AuthForm(
                mode = mode,
                state = state,
                onAction = onAction,
                wordmark = wordmark,
                sharedScope = this@SharedTransitionLayout,
                visibilityScope = this@AnimatedContent,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun AuthForm(
    mode: AuthMode,
    state: AuthState,
    onAction: (AuthAction) -> Unit,
    wordmark: Painter,
    sharedScope: SharedTransitionScope,
    visibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    val motion = FoxTheme.motion
    val isSignUp = mode == AuthMode.SignUp
    with(sharedScope) {
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
            FoxWordmark(
                contentDescription = stringResource(R.string.auth_logo_description),
                painter = wordmark,
                modifier =
                    Modifier
                        .sharedElement(rememberSharedContentState("auth-logo"), visibilityScope)
                        .height(if (isSignUp) 84.dp else 148.dp)
                        .fillMaxWidth(if (isSignUp) 0.5f else 0.85f),
            )
            with(visibilityScope) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier.animateEnterExit(
                            enter = slideInHorizontally(motion.enter()) { it / 3 } + fadeIn(motion.enter()),
                            exit = slideOutHorizontally(motion.exit()) { -it / 3 } + fadeOut(motion.exit()),
                        ),
                ) {
                    Text(
                        text = stringResource(if (isSignUp) R.string.auth_title_signup else R.string.auth_title_login),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(
                            if (isSignUp) R.string.auth_subtitle_signup else R.string.auth_subtitle_login,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            PillTabSwitch(
                options = persistentListOf(
                    stringResource(R.string.auth_tab_login),
                    stringResource(R.string.auth_tab_signup),
                ),
                selectedIndex = if (isSignUp) 1 else 0,
                onSelect = { onAction(AuthAction.ModeChanged(if (it == 1) AuthMode.SignUp else AuthMode.Login)) },
                modifier =
                    Modifier
                        .sharedElement(rememberSharedContentState("auth-tabs"), visibilityScope)
                        .widthIn(max = FoxLayout.MaxFormWidth)
                        .fillMaxWidth(),
            )
            FoxCard(
                shape = FoxShapes.Large,
                modifier =
                    Modifier
                        .sharedBounds(rememberSharedContentState("auth-card"), visibilityScope)
                        .widthIn(max = FoxLayout.MaxFormWidth)
                        .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(FoxSpacing.M),
                    verticalArrangement = Arrangement.spacedBy(FoxSpacing.S),
                ) {
                    AuthFields(mode = mode, state = state, onAction = onAction, visibilityScope = visibilityScope)
                    FoxButton(
                        text = stringResource(primaryLabel(mode, state.method)),
                        onClick = { onAction(AuthAction.Submit) },
                        state = state.submit.toButtonState(),
                        loadingDescription = stringResource(R.string.auth_loading),
                        modifier =
                            Modifier
                                .sharedBounds(rememberSharedContentState("auth-button"), visibilityScope)
                                .fillMaxWidth()
                                .padding(top = FoxSpacing.XS),
                    )
                }
            }
            SocialSignIn(onGoogle = { onAction(AuthAction.GoogleClicked) }, enabled = !state.isBusy)
        }
    }
}

@Composable
private fun AuthFields(
    mode: AuthMode,
    state: AuthState,
    onAction: (AuthAction) -> Unit,
    visibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    val motion = FoxTheme.motion
    val isSignUp = mode == AuthMode.SignUp
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(FoxSpacing.S)) {
        MethodSwitch(method = state.method, enabled = !state.isBusy, onChange = {
            onAction(AuthAction.MethodChanged(it))
        })
        if (isSignUp) NameField(state, onAction, Modifier.staggered(visibilityScope, motion, index = 0))
        AnimatedContent(
            targetState = state.method,
            transitionSpec = {
                fadeIn(motion.enter()) togetherWith fadeOut(motion.exit()) using
                    SizeTransform(clip = false)
            },
            modifier = Modifier.staggered(visibilityScope, motion, index = 1),
            label = "authMethod",
        ) { method ->
            when (method) {
                AuthMethod.Phone -> PhoneField(state, onAction)
                AuthMethod.Email -> EmailFields(state, onAction, isSignUp)
            }
        }
        if (isSignUp) TermsRow(state, onAction, Modifier.staggered(visibilityScope, motion, index = 2))
        state.message?.let { AuthMessageBanner(it, onDismiss = { onAction(AuthAction.DismissMessage) }) }
    }
}

/** Fields enter one after another (40 ms apart) when switching Log In ⇄ Sign Up (§11A.2). */
private fun Modifier.staggered(scope: AnimatedVisibilityScope, motion: FoxMotion, index: Int): Modifier {
    val delay = motion.staggerDelay(index)
    return with(scope) {
        this@staggered.animateEnterExit(
            enter = fadeIn(motion.enter(delayMillis = delay)) + expandVertically(motion.enter(delayMillis = delay)),
            exit = fadeOut(motion.exit()) + shrinkVertically(motion.exit()),
        )
    }
}

private fun primaryLabel(mode: AuthMode, method: AuthMethod): Int =
    when {
        method == AuthMethod.Phone -> R.string.auth_send_code
        mode == AuthMode.Login -> R.string.auth_login_button
        else -> R.string.auth_signup_button
    }

internal fun SubmitStatus.toButtonState(): FoxButtonState =
    when (this) {
        SubmitStatus.Idle -> FoxButtonState.Idle
        SubmitStatus.Loading -> FoxButtonState.Loading
        SubmitStatus.Success -> FoxButtonState.Success
    }

@FoxPreviews
@Composable
private fun AuthLoginPreview() {
    FoxDropTheme { Surface { AuthScreen(state = AuthState(), onAction = {}) } }
}

@FoxPreviews
@Composable
private fun AuthSignUpPreview() {
    FoxDropTheme {
        Surface {
            AuthScreen(
                state = AuthState(mode = AuthMode.SignUp, fullName = "Juan Dela Cruz", phone = "9171234567"),
                onAction = {},
            )
        }
    }
}

@FoxPreviews
@Composable
private fun AuthOtpPreview() {
    FoxDropTheme {
        Surface {
            val state =
                remember {
                    AuthState(step = AuthStep.Otp, otpPhone = "+639171234567", otp = "1234", resendSecondsLeft = 24)
                }
            AuthScreen(state = state, onAction = {})
        }
    }
}
