package ph.foxdrop.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ph.foxdrop.core.designsystem.theme.FoxLayout
import ph.foxdrop.core.designsystem.theme.FoxSpacing
import ph.foxdrop.core.designsystem.theme.FoxTheme

/** A scrolling form, centred and capped at [FoxLayout.MaxFormWidth] so it stays readable on tablets. */
@Composable
fun FoxFormColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = FoxLayout.MaxFormWidth).fillMaxWidth().padding(FoxSpacing.L),
            verticalArrangement = Arrangement.spacedBy(FoxSpacing.M),
            content = content,
        )
    }
}

/** A one-line message in a card: a rejection reason, a payment that is still pending, a server error. */
@Composable
fun FoxNotice(text: String, tone: StatusTone, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val color =
        when (tone) {
            StatusTone.Error -> colors.error
            StatusTone.Warning, StatusTone.Neutral -> colors.onSurface
            StatusTone.Info -> colors.secondary
            StatusTone.Success -> FoxTheme.colors.success
        }
    FoxCard(modifier.fillMaxWidth()) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            modifier = Modifier.padding(FoxSpacing.M),
        )
    }
}
