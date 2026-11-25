package ph.foxdrop.core.designsystem.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/** Light, dark, and 200 % font previews for every screen and component (§11A.7, CLAUDE.md). */
@Preview(name = "Light", showBackground = true)
@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF05153C,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Preview(name = "Font 200%", showBackground = true, fontScale = 2f)
annotation class FoxPreviews

/** Phone and tablet sizes for screens with adaptive layouts. */
@Preview(name = "Phone", device = "spec:width=411dp,height=891dp", showBackground = true)
@Preview(name = "Tablet", device = "spec:width=1280dp,height=800dp,dpi=240", showBackground = true)
annotation class FoxDevicePreviews
