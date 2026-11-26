package ph.foxdrop.core.network.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ph.foxdrop.core.domain.operator.OperatorLicenseRepository
import ph.foxdrop.core.domain.partner.PartnerReviewRepository
import ph.foxdrop.core.domain.partner.RiderOnboardingRepository
import ph.foxdrop.core.domain.partner.StoreOnboardingRepository
import ph.foxdrop.core.domain.province.ProvinceRepository
import ph.foxdrop.core.domain.territory.TerritoryRepository
import ph.foxdrop.core.network.operator.ApiOperatorLicenseRepository
import ph.foxdrop.core.network.partner.ApiPartnerReviewRepository
import ph.foxdrop.core.network.partner.ApiRiderOnboardingRepository
import ph.foxdrop.core.network.partner.ApiStoreOnboardingRepository
import ph.foxdrop.core.network.province.ApiProvinceRepository
import ph.foxdrop.core.network.territory.ApiTerritoryRepository

/**
 * Phase 2 repositories are thin mappings of API resources (no caching or merging yet), so the network classes
 * implement the domain interfaces directly instead of adding pass-through classes in core:data.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class OnboardingModule {
    @Binds
    abstract fun provinces(impl: ApiProvinceRepository): ProvinceRepository

    @Binds
    abstract fun operatorLicense(impl: ApiOperatorLicenseRepository): OperatorLicenseRepository

    @Binds
    abstract fun territory(impl: ApiTerritoryRepository): TerritoryRepository

    @Binds
    abstract fun storeOnboarding(impl: ApiStoreOnboardingRepository): StoreOnboardingRepository

    @Binds
    abstract fun riderOnboarding(impl: ApiRiderOnboardingRepository): RiderOnboardingRepository

    @Binds
    abstract fun partnerReviews(impl: ApiPartnerReviewRepository): PartnerReviewRepository
}
