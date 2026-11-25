package ph.foxdrop.feature.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ph.foxdrop.core.designsystem.component.FoxButton
import ph.foxdrop.core.designsystem.component.FoxTextButton
import ph.foxdrop.core.designsystem.preview.FoxPreviews
import ph.foxdrop.core.designsystem.theme.FoxDropTheme
import ph.foxdrop.core.designsystem.theme.FoxLayout
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.model.FoxDropApp

/** Route: asks Android for location, then continues whatever the answer is. */
@Composable
fun LocationPermissionRoute(
    app: FoxDropApp,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val next by rememberUpdatedState(onContinue)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { next() }
    LocationPermissionScreen(
        app = app,
        onAllow = {
            launcher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        },
        onNotNow = onContinue,
        modifier = modifier,
    )
}

/** Explains why FoxDrop needs location before the system dialog appears (Data Privacy Act: purpose first). */
@Composable
fun LocationPermissionScreen(
    app: FoxDropApp,
    onAllow: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().safeDrawingPadding().padding(FoxSpacing.L),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FoxSpacing.M, Alignment.CenterVertically),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(160.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        ) {
            Icon(
                Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp),
            )
        }
        Text(
            text = stringResource(R.string.location_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text =
                stringResource(
                    if (app == FoxDropApp.Rider) R.string.location_body_rider else R.string.location_body_customer,
                ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = FoxLayout.MaxTextWidth),
        )
        FoxButton(
            text = stringResource(R.string.location_allow),
            onClick = onAllow,
            modifier = Modifier.padding(top = FoxSpacing.M).widthIn(max = FoxLayout.MaxButtonWidth).fillMaxWidth(),
        )
        FoxTextButton(text = stringResource(R.string.location_not_now), onClick = onNotNow)
    }
}

@FoxPreviews
@Composable
private fun LocationPreview() {
    FoxDropTheme { LocationPermissionScreen(app = FoxDropApp.Customer, onAllow = {}, onNotNow = {}) }
}
