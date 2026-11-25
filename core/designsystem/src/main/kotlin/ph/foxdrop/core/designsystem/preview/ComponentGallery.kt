package ph.foxdrop.core.designsystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ph.foxdrop.core.designsystem.component.EmptyState
import ph.foxdrop.core.designsystem.component.ErrorState
import ph.foxdrop.core.designsystem.component.FoxButton
import ph.foxdrop.core.designsystem.component.FoxButtonState
import ph.foxdrop.core.designsystem.component.FoxOtpField
import ph.foxdrop.core.designsystem.component.FoxOutlinedButton
import ph.foxdrop.core.designsystem.component.FoxTextButton
import ph.foxdrop.core.designsystem.component.FoxTextField
import ph.foxdrop.core.designsystem.component.FoxWordmark
import ph.foxdrop.core.designsystem.component.PillTabSwitch
import ph.foxdrop.core.designsystem.component.SkeletonList
import ph.foxdrop.core.designsystem.theme.FoxDropTheme
import ph.foxdrop.core.designsystem.theme.FoxSpacing

/** Every design-system component on one page: used for previews and the design-system screenshot test. */
@Composable
fun ComponentGallery(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(FoxSpacing.M),
        verticalArrangement = Arrangement.spacedBy(FoxSpacing.M),
    ) {
        FoxWordmark(contentDescription = "FoxDrop", modifier = Modifier.fillMaxWidth().height(96.dp))
        Text("Delivering near you", style = MaterialTheme.typography.headlineMedium)
        Text("Body text in Poppins Regular. ₱1,234.00", style = MaterialTheme.typography.bodyLarge)
        PillTabSwitch(persistentListOf("Log In", "Sign Up"), selectedIndex = 1, onSelect = {})
        FoxButton(text = "Place Order", onClick = {}, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(FoxSpacing.M)) {
            FoxButton("Loading", {}, Modifier.width(120.dp), state = FoxButtonState.Loading)
            FoxButton("Done", {}, Modifier.width(120.dp), state = FoxButtonState.Success)
        }
        FoxOutlinedButton(text = "Log In", onClick = {}, modifier = Modifier.fillMaxWidth())
        FoxTextButton(text = "Forgot password?", onClick = {})
        FoxTextField(
            value = "917 123 4567",
            onValueChange = {},
            label = "Mobile number",
            prefix = "+63 ",
            leadingIcon = Icons.Rounded.Phone,
            modifier = Modifier.fillMaxWidth(),
        )
        FoxTextField(
            value = "",
            onValueChange = {},
            label = "Email",
            errorMessage = "Enter a valid email address",
            modifier = Modifier.fillMaxWidth(),
        )
        FoxOtpField(value = "1234", onValueChange = {}, contentDescription = "Code", modifier = Modifier.fillMaxWidth())
        SkeletonList(loadingDescription = "Loading stores", itemCount = 2)
        EmptyState(
            icon = Icons.Rounded.Storefront,
            title = "No stores yet",
            message = "Stores near you will show up here.",
        )
        ErrorState(
            title = "No connection",
            message = "Check your signal and try again.",
            retryLabel = "Retry",
            onRetry = {},
        )
    }
}

@FoxPreviews
@Composable
private fun ComponentGalleryPreview() {
    FoxDropTheme {
        Surface { ComponentGallery() }
    }
}
