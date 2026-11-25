package ph.foxdrop.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ph.foxdrop.core.designsystem.R

/**
 * Poppins, bundled (SIL OFL, licence in core/designsystem/licenses). Bundled rather than downloadable so text
 * renders the same offline (weak signal in the provinces) and in screenshot tests.
 */
val Poppins =
    FontFamily(
        Font(R.font.poppins_regular, FontWeight.Normal),
        Font(R.font.poppins_medium, FontWeight.Medium),
        Font(R.font.poppins_semibold, FontWeight.SemiBold),
        Font(R.font.poppins_bold, FontWeight.Bold),
        Font(R.font.poppins_bold_italic, FontWeight.Bold, FontStyle.Italic),
        Font(R.font.poppins_extrabold_italic, FontWeight.ExtraBold, FontStyle.Italic),
    )

private fun style(
    size: Int,
    line: Int,
    weight: FontWeight,
    italic: Boolean = false,
    tracking: Double = 0.0,
) = TextStyle(
    fontFamily = Poppins,
    fontWeight = weight,
    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

/** Headlines use ExtraBold Italic like the logo wordmark; body text stays Regular for readability. */
internal val FoxTypography =
    Typography(
        displayLarge = style(52, 60, FontWeight.ExtraBold, italic = true, tracking = -0.01),
        displayMedium = style(42, 50, FontWeight.ExtraBold, italic = true),
        displaySmall = style(34, 42, FontWeight.ExtraBold, italic = true),
        headlineLarge = style(30, 38, FontWeight.ExtraBold, italic = true),
        headlineMedium = style(26, 34, FontWeight.ExtraBold, italic = true),
        headlineSmall = style(22, 30, FontWeight.Bold, italic = true),
        titleLarge = style(20, 28, FontWeight.Bold),
        titleMedium = style(16, 24, FontWeight.SemiBold, tracking = 0.005),
        titleSmall = style(14, 20, FontWeight.SemiBold, tracking = 0.005),
        bodyLarge = style(16, 24, FontWeight.Normal),
        bodyMedium = style(14, 20, FontWeight.Normal),
        bodySmall = style(12, 16, FontWeight.Normal, tracking = 0.01),
        labelLarge = style(14, 20, FontWeight.SemiBold, tracking = 0.01),
        labelMedium = style(12, 16, FontWeight.SemiBold, tracking = 0.02),
        labelSmall = style(11, 16, FontWeight.Medium, tracking = 0.03),
    )

/** Label for filled pink buttons: 18 sp bold counts as WCAG large text, so 4.3:1 on pink passes AA. */
internal val FoxButtonLabel = style(18, 24, FontWeight.Bold, tracking = 0.01)
