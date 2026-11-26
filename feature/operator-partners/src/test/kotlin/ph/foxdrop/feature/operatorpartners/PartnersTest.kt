@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package ph.foxdrop.feature.operatorpartners

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.designsystem.component.Loadable
import ph.foxdrop.core.domain.onboarding.Problem
import ph.foxdrop.core.domain.partner.RiderStatus
import ph.foxdrop.core.domain.partner.StoreStatus
import ph.foxdrop.core.testing.MainDispatcherRule
import ph.foxdrop.core.testing.fake.FakePartnerReviewRepository
import ph.foxdrop.core.testing.screenshot.FoxScreenshotVariant
import ph.foxdrop.core.testing.screenshot.captureFoxScreenshots

@RunWith(AndroidJUnit4::class)
class PartnerListViewModelTest {
    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    private val reviews = FakePartnerReviewRepository()

    @Test
    fun `stores load per filter and keep the list while refreshing`() = runTest(main.dispatcher) {
        val vm = StoreListViewModel(reviews)
        assertEquals(Loadable.Loading, vm.state.value.items)
        vm.refresh()
        runCurrent()
        assertEquals(listOf("s1"), (vm.state.value.items as Loadable.Ready).value.map { it.id })

        vm.refresh()
        assertTrue(vm.state.value.items is Loadable.Ready)

        vm.select(PartnerFilter.Rejected)
        runCurrent()
        assertEquals(PartnerFilter.Rejected, vm.state.value.filter)
        assertEquals(listOf("stores:Submitted", "stores:Submitted", "stores:Rejected"), reviews.calls)
    }

    @Test
    fun `riders map filters to statuses and show failures`() = runTest(main.dispatcher) {
        reviews.error = DomainError.Network
        val vm = RiderListViewModel(reviews)
        vm.refresh()
        runCurrent()
        assertEquals(Loadable.Failed(DomainError.Network), vm.state.value.items)

        reviews.error = null
        vm.select(PartnerFilter.Approved)
        assertEquals(Loadable.Loading, vm.state.value.items)
        runCurrent()
        assertTrue(vm.state.value.items is Loadable.Ready)
        assertEquals("riders:${RiderStatus.Approved}", reviews.calls.last())
    }

    @Test
    fun `filters map to the server statuses`() {
        assertEquals(StoreStatus.Submitted, PartnerFilter.Pending.storeStatus())
        assertEquals(StoreStatus.Active, PartnerFilter.Approved.storeStatus())
        assertEquals(RiderStatus.DocsReview, PartnerFilter.Pending.riderStatus())
        assertEquals(RiderStatus.Rejected, PartnerFilter.Rejected.riderStatus())
    }
}

@RunWith(AndroidJUnit4::class)
class ReviewViewModelTest {
    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    private val reviews = FakePartnerReviewRepository()

    private fun store() = StoreReviewViewModel(SavedStateHandle(mapOf("storeId" to "s1")), reviews)

    private fun rider() = RiderReviewViewModel(SavedStateHandle(mapOf("riderId" to "r1")), reviews)

    @Test
    fun `approving a store marks it active`() = runTest(main.dispatcher) {
        val vm = store()
        runCurrent()
        assertTrue(vm.state.value.review is Loadable.Ready)

        vm.approve()
        assertTrue(vm.state.value.deciding)
        runCurrent()

        val state = vm.state.value
        assertFalse(state.deciding)
        assertTrue(state.decided)
        assertEquals(StoreStatus.Active, (state.review as Loadable.Ready).value.summary.status)
        assertEquals("approveStore:s1", reviews.calls.last())
    }

    @Test
    fun `rejecting needs a reason the applicant can read`() = runTest(main.dispatcher) {
        val vm = rider()
        runCurrent()
        vm.confirmReject()
        assertNull(vm.state.value.rejectDialog)

        vm.openRejectDialog()
        vm.onReasonChanged("bad")
        vm.confirmReject()
        assertEquals(Problem.TooShort, vm.state.value.rejectDialog?.problem)

        vm.onReasonChanged("  Selfie is blurry  ")
        assertNull(vm.state.value.rejectDialog?.problem)
        vm.confirmReject()
        assertNull(vm.state.value.rejectDialog)
        runCurrent()

        val review = (vm.state.value.review as Loadable.Ready).value
        assertEquals(RiderStatus.Rejected, review.summary.status)
        assertEquals("Selfie is blurry", review.rejectionReason)
    }

    @Test
    fun `a failed decision keeps the application and reports why`() = runTest(main.dispatcher) {
        reviews.writeError = DomainError.Conflict("Already reviewed")
        val vm = rider()
        runCurrent()
        vm.approve()
        runCurrent()
        assertEquals(DomainError.Conflict("Already reviewed"), vm.state.value.message)
        assertFalse(vm.state.value.decided)
        vm.dismissMessage()
        assertNull(vm.state.value.message)

        vm.openRejectDialog()
        vm.dismissRejectDialog()
        assertNull(vm.state.value.rejectDialog)
    }

    @Test
    fun `loading errors can be retried`() = runTest(main.dispatcher) {
        reviews.error = DomainError.Network
        val vm = store()
        runCurrent()
        assertEquals(Loadable.Failed(DomainError.Network), vm.state.value.review)
        reviews.error = null
        vm.load()
        runCurrent()
        assertTrue(vm.state.value.review is Loadable.Ready)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PartnersScreenshotTest {
    private val themes = listOf(FoxScreenshotVariant.PhoneLight, FoxScreenshotVariant.PhoneDark)
    private val light = listOf(FoxScreenshotVariant.PhoneLight)

    @Test
    fun stores() =
        captureFoxScreenshots("partners_stores") {
            StoreApprovalsScreen(PartnerListState(items = Loadable.Ready(PartnerPreviewData.stores)), {}, {}, {})
        }

    @Test
    fun storesLoading() =
        captureFoxScreenshots("partners_stores_loading", light) {
            StoreApprovalsScreen(PartnerListState(), {}, {}, {})
        }

    @Test
    fun ridersEmpty() =
        captureFoxScreenshots("partners_riders_empty", themes) {
            RiderApprovalsScreen(PartnerListState(PartnerFilter.Rejected, Loadable.Ready(persistentListOf())), {
            }, {}, {})
        }

    @Test
    fun riders() =
        captureFoxScreenshots("partners_riders", light) {
            RiderApprovalsScreen(PartnerListState(items = Loadable.Ready(PartnerPreviewData.riders)), {}, {}, {})
        }

    @Test
    fun ridersError() =
        captureFoxScreenshots("partners_riders_error", light) {
            RiderApprovalsScreen(PartnerListState(items = Loadable.Failed(DomainError.Network)), {}, {}, {})
        }

    @Test
    fun storeReview() =
        captureFoxScreenshots("partners_store_review") {
            StoreReviewScreen(ReviewState(Loadable.Ready(PartnerPreviewData.storeReview)), ReviewActions())
        }

    @Test
    fun storeReviewRejected() =
        captureFoxScreenshots("partners_store_review_rejected", light) {
            val rejected = PartnerPreviewData.storeReview
            StoreReviewScreen(
                ReviewState(
                    Loadable.Ready(
                        rejected.copy(
                            summary = rejected.summary.copy(status = StoreStatus.Rejected),
                            rejectionReason = "Permit is expired",
                        ),
                    ),
                ),
                ReviewActions(),
            )
        }

    @Test
    fun riderReviewRejectDialog() =
        captureFoxScreenshots("partners_rider_reject_dialog", themes) {
            RiderReviewScreen(
                ReviewState(
                    Loadable.Ready(PartnerPreviewData.riderReview),
                    rejectDialog = RejectDialog("bad", Problem.TooShort),
                ),
                ReviewActions(),
            )
        }

    @Test
    fun riderReviewDeciding() =
        captureFoxScreenshots("partners_rider_review", light) {
            RiderReviewScreen(
                ReviewState(Loadable.Ready(PartnerPreviewData.riderReview), deciding = true),
                ReviewActions(),
            )
        }

    @Test
    fun reviewLoadingAndError() {
        captureFoxScreenshots("partners_review_loading", light) { StoreReviewScreen(ReviewState(), ReviewActions()) }
        captureFoxScreenshots("partners_review_error", light) {
            RiderReviewScreen(ReviewState(Loadable.Failed(DomainError.Network)), ReviewActions())
        }
    }
}
