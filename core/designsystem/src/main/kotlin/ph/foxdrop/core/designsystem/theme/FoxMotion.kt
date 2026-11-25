package ph.foxdrop.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * Motion tokens (§11A). Every animation in the apps takes its spec from here — no magic numbers in screens.
 * When the user turned on "Remove animations" ([reduceMotion]), every spec becomes an instant [snap] and
 * looping animations (shimmer, pulses) must stop; components check [reduceMotion] for that.
 */
@Immutable
class FoxMotion(val reduceMotion: Boolean) {
    /** Enter: emphasized decelerate. */
    fun <T> enter(durationMillis: Int = MEDIUM, delayMillis: Int = 0): FiniteAnimationSpec<T> =
        if (reduceMotion) snap() else tween(durationMillis, delayMillis, EmphasizedDecelerate)

    /** Exit: emphasized accelerate. */
    fun <T> exit(durationMillis: Int = SHORT, delayMillis: Int = 0): FiniteAnimationSpec<T> =
        if (reduceMotion) snap() else tween(durationMillis, delayMillis, EmphasizedAccelerate)

    /** On-screen change of an element (size, colour, position). */
    fun <T> standard(durationMillis: Int = MEDIUM, delayMillis: Int = 0): FiniteAnimationSpec<T> =
        if (reduceMotion) snap() else tween(durationMillis, delayMillis, Standard)

    /** Playful bounce: cart badge, heart, press release. */
    fun <T> bouncy(): FiniteAnimationSpec<T> =
        if (reduceMotion) snap() else spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)

    /** Quick spring for press feedback. */
    fun <T> press(): FiniteAnimationSpec<T> =
        if (reduceMotion) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh,
            )
        }

    /** Hero element dashing in with an overshoot (first-run intro fox). */
    fun <T> dash(): FiniteAnimationSpec<T> =
        if (reduceMotion) snap() else spring(dampingRatio = DASH_DAMPING, stiffness = Spring.StiffnessLow)

    /** Horizontal shake for a wrong code, in dp offsets (§11A.2). Settles at 0. */
    fun shake(): FiniteAnimationSpec<Float> =
        if (reduceMotion) {
            snap()
        } else {
            keyframes {
                durationMillis = SHAKE_MILLIS
                -18f at 50
                16f at 100
                -12f at 150
                10f at 200
                -6f at 250
                4f at 300
            }
        }

    /** Delay between items that enter one after another (sign-up fields, OTP boxes). */
    fun staggerDelay(index: Int): Int = if (reduceMotion) 0 else index * STAGGER

    companion object {
        const val SHORT = 150
        const val MEDIUM = 300
        const val LONG = 450
        const val STAGGER = 40
        private const val SHAKE_MILLIS = 400
        private const val DASH_DAMPING = 0.62f

        val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
        val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
        val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

        /** Press feedback scale for buttons and cards (§11A.4). */
        const val PRESSED_SCALE = 0.96f
    }
}
