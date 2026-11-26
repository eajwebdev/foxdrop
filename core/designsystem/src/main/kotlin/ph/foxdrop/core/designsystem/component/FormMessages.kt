package ph.foxdrop.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ph.foxdrop.core.designsystem.R
import ph.foxdrop.core.domain.onboarding.Problem

/** The message under a form field for a [Problem] found by the domain form rules. */
@Composable
fun problemMessage(problem: Problem?): String? =
    when (problem) {
        null -> null
        Problem.Required -> stringResource(R.string.field_required)
        Problem.TooShort -> stringResource(R.string.field_too_short)
        Problem.TooLong -> stringResource(R.string.field_too_long)
        Problem.Invalid -> stringResource(R.string.field_invalid)
        Problem.NotInFuture -> stringResource(R.string.field_not_future)
        Problem.OutOfRange -> stringResource(R.string.field_out_of_range)
    }
