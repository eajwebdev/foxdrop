package ph.foxdrop.feature.onboarding

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ph.foxdrop.core.model.FoxDropApp

@Immutable
data class OnboardingPage(
    val icon: ImageVector,
    @param:StringRes val title: Int,
    @param:StringRes val body: Int,
)

/** Each app tells its own story in three pages. */
fun onboardingPagesFor(app: FoxDropApp): ImmutableList<OnboardingPage> =
    when (app) {
        FoxDropApp.Customer ->
            persistentListOf(
                OnboardingPage(
                    Icons.Rounded.Storefront,
                    R.string.onboarding_customer_1_title,
                    R.string.onboarding_customer_1_body,
                ),
                OnboardingPage(
                    Icons.Rounded.Payments,
                    R.string.onboarding_customer_2_title,
                    R.string.onboarding_customer_2_body,
                ),
                OnboardingPage(
                    Icons.Rounded.Map,
                    R.string.onboarding_customer_3_title,
                    R.string.onboarding_customer_3_body,
                ),
            )

        FoxDropApp.Rider ->
            persistentListOf(
                OnboardingPage(
                    Icons.Rounded.TwoWheeler,
                    R.string.onboarding_rider_1_title,
                    R.string.onboarding_rider_1_body,
                ),
                OnboardingPage(
                    Icons.Rounded.Navigation,
                    R.string.onboarding_rider_2_title,
                    R.string.onboarding_rider_2_body,
                ),
                OnboardingPage(
                    Icons.Rounded.Savings,
                    R.string.onboarding_rider_3_title,
                    R.string.onboarding_rider_3_body,
                ),
            )

        FoxDropApp.Business ->
            persistentListOf(
                OnboardingPage(
                    Icons.Rounded.Storefront,
                    R.string.onboarding_business_1_title,
                    R.string.onboarding_business_1_body,
                ),
                OnboardingPage(
                    Icons.Rounded.Groups,
                    R.string.onboarding_business_2_title,
                    R.string.onboarding_business_2_body,
                ),
                OnboardingPage(
                    Icons.Rounded.AccountBalanceWallet,
                    R.string.onboarding_business_3_title,
                    R.string.onboarding_business_3_body,
                ),
            )
    }

/** Customer and rider apps need location; the business app asks later, when a store sets its address. */
fun FoxDropApp.asksForLocationOnStart(): Boolean = this != FoxDropApp.Business
