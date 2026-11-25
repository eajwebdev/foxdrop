package ph.foxdrop.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Spacing scale 4/8/12/16/24/32 (§11A.6). Screens use these, never raw dp numbers. */
object FoxSpacing {
    val XXS = 4.dp
    val XS = 8.dp
    val S = 12.dp
    val M = 16.dp
    val L = 24.dp
    val XL = 32.dp
    val XXL = 48.dp

    /** Minimum touch target (§10, §11A.7). */
    val MinTouchTarget = 48.dp
}

/** Shapes 8/16/24/pill (§11, §11A.6): 16 dp cards, pill buttons. */
object FoxShapes {
    val Small = RoundedCornerShape(8.dp)
    val Medium = RoundedCornerShape(16.dp)
    val Large = RoundedCornerShape(24.dp)
    val Pill = CircleShape
}

internal val FoxMaterialShapes =
    Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = FoxShapes.Small,
        medium = FoxShapes.Medium,
        large = FoxShapes.Large,
        extraLarge = RoundedCornerShape(32.dp),
    )

/** Maximum widths so content stays readable on tablets and landscape (§11A.6). */
object FoxLayout {
    val MaxFormWidth = 520.dp
    val MaxButtonWidth = 480.dp
    val MaxTextWidth = 440.dp
}
