package ph.foxdrop.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.designsystem.R

/**
 * A friendly sentence for a failed request. Conflicts and validation errors show the server's own message (it says
 * exactly what is wrong, e.g. "This province already has an operator."); everything else a generic, kind message.
 */
@Composable
fun errorMessage(error: DomainError): String =
    when (error) {
        DomainError.Network -> stringResource(R.string.error_network)
        DomainError.NotConfigured -> stringResource(R.string.error_not_configured)
        DomainError.RateLimited -> stringResource(R.string.error_rate_limited)
        DomainError.Unauthorized -> stringResource(R.string.error_unauthorized)
        DomainError.NotFound -> stringResource(R.string.error_not_found)
        is DomainError.Conflict -> error.reason.ifBlank { stringResource(R.string.error_generic) }
        is DomainError.Validation -> error.reason.ifBlank { stringResource(R.string.error_generic) }
        is DomainError.Unknown -> stringResource(R.string.error_generic)
    }
