package ph.foxdrop.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Brand colours sampled from the logos (§11). */
object FoxBrand {
    val Pink = Color(0xFFF2014F)
    val Navy = Color(0xFF05153C)
    val Pink50 = Color(0xFFFFE8EF)
    val PinkLight = Color(0xFFFF5C8A) // pink that keeps AA contrast as text on navy
    val Success = Color(0xFF1DB954)
    val Warning = Color(0xFFFFB020)
    val Error = Color(0xFFD32F2F)
    val White = Color(0xFFFFFFFF)
    val Background = Color(0xFFF7F7FA)
}

/**
 * Light scheme built around the brand seed #F2014F (dynamic colour is off: brand first).
 * Pink with white text is 4.3:1, so filled pink buttons use large bold labels (WCAG "large text" ≥ 3:1)
 * and small links use navy (see docs/DECISIONS.md).
 */
internal val FoxLightColors =
    lightColorScheme(
        primary = FoxBrand.Pink,
        onPrimary = FoxBrand.White,
        primaryContainer = Color(0xFFFFD9E0),
        onPrimaryContainer = Color(0xFF3F0012),
        inversePrimary = FoxBrand.PinkLight,
        secondary = FoxBrand.Navy,
        onSecondary = FoxBrand.White,
        secondaryContainer = Color(0xFFDDE2F5),
        onSecondaryContainer = FoxBrand.Navy,
        tertiary = Color(0xFF8A5100),
        onTertiary = FoxBrand.White,
        tertiaryContainer = Color(0xFFFFDDB8),
        onTertiaryContainer = Color(0xFF2C1600),
        background = FoxBrand.Background,
        onBackground = Color(0xFF111529),
        surface = FoxBrand.White,
        onSurface = Color(0xFF111529),
        surfaceVariant = Color(0xFFEDEEF4),
        onSurfaceVariant = Color(0xFF474C60),
        surfaceTint = FoxBrand.Pink,
        surfaceContainerLowest = FoxBrand.White,
        surfaceContainerLow = Color(0xFFFBFBFD),
        surfaceContainer = Color(0xFFF4F5F9),
        surfaceContainerHigh = Color(0xFFEEEFF4),
        surfaceContainerHighest = Color(0xFFE8E9F0),
        surfaceBright = FoxBrand.White,
        surfaceDim = Color(0xFFDCDDE5),
        inverseSurface = FoxBrand.Navy,
        inverseOnSurface = FoxBrand.White,
        outline = Color(0xFF767B8F),
        outlineVariant = Color(0xFFD4D6E0),
        error = FoxBrand.Error,
        onError = FoxBrand.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        scrim = Color(0xFF000000),
    )

/** Dark scheme: navy surfaces with pink accents (§11). */
internal val FoxDarkColors =
    darkColorScheme(
        primary = FoxBrand.PinkLight,
        onPrimary = Color(0xFF40000F),
        primaryContainer = FoxBrand.Pink,
        onPrimaryContainer = FoxBrand.White,
        inversePrimary = FoxBrand.Pink,
        secondary = Color(0xFFBCC6EA),
        onSecondary = FoxBrand.Navy,
        secondaryContainer = Color(0xFF1C2E63),
        onSecondaryContainer = Color(0xFFDDE2F5),
        tertiary = Color(0xFFFFB960),
        onTertiary = Color(0xFF482900),
        tertiaryContainer = Color(0xFF693C00),
        onTertiaryContainer = Color(0xFFFFDDB8),
        background = FoxBrand.Navy,
        onBackground = Color(0xFFE6E8F2),
        surface = FoxBrand.Navy,
        onSurface = Color(0xFFE6E8F2),
        surfaceVariant = Color(0xFF24335E),
        onSurfaceVariant = Color(0xFFC3C8DB),
        surfaceTint = FoxBrand.PinkLight,
        surfaceContainerLowest = Color(0xFF020C26),
        surfaceContainerLow = Color(0xFF0A1B45),
        surfaceContainer = Color(0xFF0E214F),
        surfaceContainerHigh = Color(0xFF15295A),
        surfaceContainerHighest = Color(0xFF1C3166),
        surfaceBright = Color(0xFF213670),
        surfaceDim = FoxBrand.Navy,
        inverseSurface = Color(0xFFE6E8F2),
        inverseOnSurface = FoxBrand.Navy,
        outline = Color(0xFF8D93AB),
        outlineVariant = Color(0xFF2E3D69),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        scrim = Color(0xFF000000),
    )

/** Extra semantic colours not in Material's scheme. Status chips always pair colour with icon + text (§11A.7). */
data class FoxExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val shimmerBase: Color,
    val shimmerHighlight: Color,
)

internal val FoxLightExtended =
    FoxExtendedColors(
        success = Color(0xFF14833B),
        onSuccess = FoxBrand.White,
        warning = Color(0xFF9A5B00),
        onWarning = FoxBrand.White,
        shimmerBase = Color(0xFFE9EAF0),
        shimmerHighlight = Color(0xFFF7F7FA),
    )

internal val FoxDarkExtended =
    FoxExtendedColors(
        success = Color(0xFF5BE38A),
        onSuccess = Color(0xFF003916),
        warning = FoxBrand.Warning,
        onWarning = Color(0xFF462A00),
        shimmerBase = Color(0xFF15295A),
        shimmerHighlight = Color(0xFF213670),
    )
