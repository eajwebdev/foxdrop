package ph.foxdrop.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.foxdrop.core.domain.media.PhotoUpload
import ph.foxdrop.core.domain.operator.ApplicationStatus
import ph.foxdrop.core.domain.operator.License
import ph.foxdrop.core.domain.operator.LicenseStatus
import ph.foxdrop.core.domain.operator.OperatorApplication
import ph.foxdrop.core.domain.operator.OperatorApplicationForm
import ph.foxdrop.core.domain.operator.Payment
import ph.foxdrop.core.domain.operator.PaymentStatus
import ph.foxdrop.core.domain.partner.MyRider
import ph.foxdrop.core.domain.partner.MyStore
import ph.foxdrop.core.domain.partner.PartnerDocument
import ph.foxdrop.core.domain.partner.RiderApplicationForm
import ph.foxdrop.core.domain.partner.RiderReview
import ph.foxdrop.core.domain.partner.RiderStatus
import ph.foxdrop.core.domain.partner.RiderSummary
import ph.foxdrop.core.domain.partner.StoreApplicationForm
import ph.foxdrop.core.domain.partner.StoreCategory
import ph.foxdrop.core.domain.partner.StoreReview
import ph.foxdrop.core.domain.partner.StoreStatus
import ph.foxdrop.core.domain.partner.StoreSummary
import ph.foxdrop.core.domain.partner.VehicleType
import ph.foxdrop.core.domain.province.ProvinceOption
import ph.foxdrop.core.domain.territory.FeePreview
import ph.foxdrop.core.domain.territory.OperatorProvince
import ph.foxdrop.core.domain.territory.Zone
import ph.foxdrop.core.model.GeoPoint

/** The Phase 2 models carry server values unchanged; screens rely on these shapes. */
class DomainModelsTest {
    private val photo = PhotoUpload(byteArrayOf(1, 2), "valid_id.jpg")
    private val bacolod = GeoPoint(10.674, 122.953)

    @Test
    fun `photo uploads compare by content`() {
        assertEquals(PhotoUpload(byteArrayOf(1, 2), "valid_id.jpg"), photo)
        assertNotEquals(PhotoUpload(byteArrayOf(1, 3), "valid_id.jpg"), photo)
        assertEquals(2, photo.sizeBytes)
    }

    @Test
    fun `an operator application leads to a license and its payment`() {
        val license =
            License(
                "l1", "ilo", "Iloilo", LicenseStatus.Grace, "2025-11-26", "2026-11-23", -3, 1_500_000, canPay = true,
            )
        val application =
            OperatorApplication("a1", ApplicationStatus.Approved, "Iloilo", "Gonzaga Logistics", null, license)
        val form = OperatorApplicationForm("ilo", "Gonzaga Logistics", null, "+639170000006", null, photo, null)
        val payment = Payment("p1", PaymentStatus.Pending, license.feeCentavos, "https://pay.example/p1")

        assertEquals(-3, application.license?.daysLeft)
        assertNull(form.businessPermit)
        assertEquals(1_500_000L, payment.amountCentavos)
        assertTrue(LicenseStatus.Rejected in LicenseStatus.entries)
        assertTrue(PaymentStatus.Expired in PaymentStatus.entries)
    }

    @Test
    fun `store models keep the applicant's details and documents`() {
        val form =
            StoreApplicationForm(
                "neg", "Mama's Kitchen", StoreCategory.Food, null, "Lacson St.", null, "Bacolod City", null,
                bacolod, photo, photo,
            )
        val summary =
            StoreSummary(
                "s1", form.name, form.category, StoreStatus.Submitted, form.addressLine, form.city, "Rosa", null,
                "2026-11-26",
            )
        val documents = listOf(PartnerDocument("VALID_ID", "https://x/id"))
        val review = StoreReview(summary, null, null, null, form.location, null, documents)
        val mine =
            MyStore("s1", form.name, form.category, StoreStatus.Rejected, "Blurry permit", "Negros Occidental", "neg")

        assertEquals(bacolod, review.location)
        assertEquals("VALID_ID", review.documents.single().type)
        assertEquals("Blurry permit", mine.rejectionReason)
        assertTrue(StoreStatus.Rejected in StoreStatus.entries)
    }

    @Test
    fun `rider models keep the vehicle and license details`() {
        val form =
            RiderApplicationForm(
                "neg", "Lito Cruz", VehicleType.Bicycle, null, "N01", "2028-01-31", photo, photo, photo, null, null,
            )
        val summary =
            RiderSummary(
                "r1",
                RiderStatus.DocsReview,
                form.fullName,
                null,
                form.vehicleType,
                form.plateNo,
                "2026-11-26",
            )
        val review = RiderReview(summary, "4567", form.licenseExpiry, null, emptyList())
        val mine =
            MyRider(
                "r1",
                RiderStatus.Approved,
                null,
                "Negros Occidental",
                VehicleType.Bicycle,
                null,
                "4567",
                "2028-01-31",
                "neg",
            )

        assertNull(form.orCr)
        assertEquals("2028-01-31", review.licenseExpiry)
        assertEquals(VehicleType.Bicycle, mine.vehicleType)
        assertTrue(RiderStatus.Rejected in RiderStatus.entries)
    }

    @Test
    fun `territory models keep the operator's province settings`() {
        val zone = Zone("z1", "Metro Bacolod", true, bacolod, 5.0, 78.5)
        val province = OperatorProvince("neg", "Negros Occidental", bacolod, 1_500, 7)
        val preview = FeePreview(20.0, null, null, null)
        val option = ProvinceOption("neg", "Negros Occidental", "Region VI", isServiceable = true, isAvailable = false)

        assertEquals(5.0, zone.radiusKm)
        assertEquals(7, province.storeFeeGraceDays)
        assertNull(preview.deliveryFeeCentavos)
        assertTrue(option.isServiceable)
    }
}
