package ph.foxdrop.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.designsystem.R
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.designsystem.theme.FoxTheme

/** Something a screen loads: a skeleton while [Loading], an error with retry when [Failed], else [Ready]. */
@Immutable
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>

    data class Failed(val error: DomainError) : Loadable<Nothing>

    data class Ready<T>(val value: T) : Loadable<T>
}

/**
 * The loading / error / content switch every screen needs (CLAUDE.md), with a FoxMotion fade between them. Only a
 * change of kind animates, so refreshed content does not flash.
 */
@Composable
fun <T> LoadableContent(
    state: Loadable<T>,
    errorTitle: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    loadingDescription: String = stringResource(R.string.loading),
    content: @Composable (T) -> Unit,
) {
    val motion = FoxTheme.motion
    AnimatedContent(
        targetState = state,
        contentKey = { it::class },
        transitionSpec = { fadeIn(motion.enter()) togetherWith fadeOut(motion.exit()) },
        label = "loadable",
        modifier = modifier,
    ) { current ->
        when (current) {
            Loadable.Loading ->
                SkeletonList(
                    loadingDescription = loadingDescription,
                    modifier = Modifier.fillMaxSize().padding(FoxSpacing.L),
                )

            is Loadable.Failed ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorState(
                        title = errorTitle,
                        message = errorMessage(current.error),
                        retryLabel = stringResource(R.string.action_retry),
                        onRetry = onRetry,
                    )
                }

            is Loadable.Ready -> content(current.value)
        }
    }
}
