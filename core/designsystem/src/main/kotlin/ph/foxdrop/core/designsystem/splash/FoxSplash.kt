package ph.foxdrop.core.designsystem.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.PathInterpolator
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import kotlin.math.hypot
import ph.foxdrop.core.designsystem.theme.FoxMotion

private const val ICON_EXIT_SCALE = 1.15f

/** Same curve as FoxMotion.EmphasizedAccelerate, for View animators. */
private val EmphasizedAccelerateInterpolator = PathInterpolator(0.3f, 0f, 0.8f, 0.15f)

/**
 * Splash exit (§11A.1): the fox icon grows 1.0 → 1.15 and fades while the navy splash closes as a circle,
 * revealing the app underneath (300 ms). With "Remove animations" on, the splash is simply removed.
 */
fun SplashScreen.installFoxExitAnimation(reduceMotion: () -> Boolean) {
    setOnExitAnimationListener { provider ->
        if (reduceMotion()) {
            provider.remove()
        } else {
            animateExit(provider)
        }
    }
}

// Lint's Recycle check expects start() on each ObjectAnimator; they are all started together by the AnimatorSet.
@SuppressLint("Recycle")
private fun animateExit(provider: SplashScreenViewProvider) {
    val splash = provider.view
    val icon: View? = runCatching { provider.iconView }.getOrNull()
    val duration = FoxMotion.MEDIUM.toLong()

    val animators = mutableListOf<Animator>()
    if (icon != null) {
        animators += ObjectAnimator.ofFloat(icon, View.SCALE_X, 1f, ICON_EXIT_SCALE)
        animators += ObjectAnimator.ofFloat(icon, View.SCALE_Y, 1f, ICON_EXIT_SCALE)
        animators += ObjectAnimator.ofFloat(icon, View.ALPHA, 1f, 0f)
    }
    if (splash.isAttachedToWindow && splash.width > 0) {
        val cx = splash.width / 2
        val cy = splash.height / 2
        val radius = hypot(cx.toFloat(), cy.toFloat())
        animators += ViewAnimationUtils.createCircularReveal(splash, cx, cy, radius, 0f)
    } else {
        animators += ObjectAnimator.ofFloat(splash, View.ALPHA, 1f, 0f)
    }
    AnimatorSet().apply {
        playTogether(animators)
        this.duration = duration
        interpolator = EmphasizedAccelerateInterpolator
        addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) = provider.remove()

                override fun onAnimationCancel(animation: Animator) = provider.remove()
            },
        )
        start()
    }
}
