package ph.foxdrop.core.domain.startup

import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import ph.foxdrop.core.domain.auth.AuthRepository
import ph.foxdrop.core.domain.auth.SessionStatus
import ph.foxdrop.core.domain.preferences.AppPreferencesRepository

/** Where the app opens after the splash screen (§11A.1). */
enum class StartDestination {
    /** First launch on this device: branded intro → onboarding → auth. */
    Intro,

    /** Onboarding done, nobody signed in. */
    Auth,

    /** Signed in: straight to home (no fake delay). */
    Home,
}

/**
 * Decides the first screen. Never waits longer than [timeout] so the splash screen is held for at most 1.5 s;
 * if the session is still unknown by then, the user is treated as signed out.
 */
class ResolveStartDestinationUseCase @Inject constructor(
    private val preferences: AppPreferencesRepository,
    private val auth: AuthRepository,
) {
    suspend operator fun invoke(timeout: Duration = DEFAULT_TIMEOUT): StartDestination {
        val resolved =
            withTimeoutOrNull(timeout) {
                combine(preferences.hasCompletedOnboarding, auth.sessionStatus) { onboarded, session ->
                    onboarded to session
                }.filterNot { (_, session) -> session == SessionStatus.Unknown }
                    .first()
            }
        val onboarded = resolved?.first ?: preferences.hasCompletedOnboarding.first()
        val session = resolved?.second ?: SessionStatus.SignedOut
        return when {
            session is SessionStatus.SignedIn -> StartDestination.Home
            !onboarded -> StartDestination.Intro
            else -> StartDestination.Auth
        }
    }

    companion object {
        val DEFAULT_TIMEOUT: Duration = 1_500.milliseconds
    }
}
