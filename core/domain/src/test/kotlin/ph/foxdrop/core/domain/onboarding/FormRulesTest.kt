package ph.foxdrop.core.domain.onboarding

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.foxdrop.core.domain.media.PhotoUpload
import ph.foxdrop.core.domain.onboarding.RiderApplicationRules.Field as RiderField
import ph.foxdrop.core.domain.partner.VehicleType
import ph.foxdrop.core.domain.territory.RateCard

class FormRulesTest {
    private val today = LocalDate.of(2026, 11, 26)

    @Test
    fun `operator application needs a province, name, mobile number and valid ID`() {
        val empty = OperatorApplicationRules.validate(null, "", "", "", hasValidId = false)
        assertEquals(
            mapOf(
                OperatorApplicationRules.Field.Province to Problem.Required,
                OperatorApplicationRules.Field.BusinessName to Problem.Required,
                OperatorApplicationRules.Field.ContactPhone to Problem.Required,
                OperatorApplicationRules.Field.ValidId to Problem.Required,
            ),
            empty,
        )
        val bad = OperatorApplicationRules.validate("p", "G", "12345", "not-an-email", hasValidId = true)
        assertEquals(Problem.TooShort, bad[OperatorApplicationRules.Field.BusinessName])
        assertEquals(Problem.Invalid, bad[OperatorApplicationRules.Field.ContactPhone])
        assertEquals(Problem.Invalid, bad[OperatorApplicationRules.Field.ContactEmail])
        assertTrue(OperatorApplicationRules.validate("p", "Gonzaga Logistics", "0917 123 4567", "", true).isEmpty())
    }

    @Test
    fun `store application needs details, a location and both photos`() {
        val problems = StoreApplicationRules.validate(null, "", "", "", "12", false, false, false)
        assertEquals(StoreApplicationRules.Field.entries.toSet(), problems.keys)
        assertTrue(
            StoreApplicationRules.validate("p", "Mama's Kitchen", "Lacson St.", "Bacolod City", "", true, true, true)
                .isEmpty(),
        )
        assertEquals(
            Problem.TooLong,
            StoreApplicationRules.validate("p", "x".repeat(121), "Lacson", "Bacolod", "", true, true, true)[
                StoreApplicationRules.Field.Name,
            ],
        )
    }

    @Test
    fun `riders on a bicycle need no plate or vehicle papers`() {
        val photos = setOf(RiderField.ValidId, RiderField.DriversLicense, RiderField.Selfie)
        val bicycle =
            RiderApplicationRules.validate(
                "p",
                "Ana Villanueva",
                VehicleType.Bicycle,
                "",
                "N01-23-456",
                today.plusYears(1),
                today,
                photos,
            )
        assertTrue(bicycle.isEmpty())
        assertFalse(RiderApplicationRules.needsVehiclePapers(VehicleType.Bicycle))

        val motorcycle =
            RiderApplicationRules.validate(
                "p",
                "Paolo Reyes",
                VehicleType.Motorcycle,
                "",
                "N01-23-456",
                today.plusYears(1),
                today,
                photos,
            )
        assertEquals(
            mapOf(
                RiderField.PlateNo to Problem.Required,
                RiderField.OrCr to Problem.Required,
                RiderField.VehiclePhoto to Problem.Required,
            ),
            motorcycle,
        )
    }

    @Test
    fun `rider license must not be expired and the plate must look like a plate`() {
        val all = RiderApplicationRules.requiredPhotos(VehicleType.Motorcycle).toSet()
        val problems =
            RiderApplicationRules.validate("p", "Paolo", VehicleType.Motorcycle, "neg@1234!", "N01", today, today, all)
        assertEquals(Problem.Invalid, problems[RiderField.PlateNo])
        assertEquals(Problem.TooShort, problems[RiderField.LicenseNo])
        assertEquals(Problem.NotInFuture, problems[RiderField.LicenseExpiry])
        assertEquals(
            Problem.Required,
            RiderApplicationRules.validate(
                null,
                "",
                VehicleType.Car,
                "NEG 1234",
                "N01-23-456",
                null,
                today,
                all,
            )[RiderField.LicenseExpiry],
        )
    }

    @Test
    fun `rate card values must be amounts and sensible distances`() {
        assertTrue(RateCardRules.validate(4_000, 2.0, 1_000, 4_000, 15.0, 8_000).isEmpty())
        val problems = RateCardRules.validate(null, -1.0, null, null, 0.0, 12_000)
        assertEquals(RateCardRules.Field.entries.toSet(), problems.keys)
        assertEquals(RateCard.Default, RateCardRules.toCard(4_000, 2.0, 1_000, 4_000, 15.0, 8_000))
    }

    @Test
    fun `a rejection reason says enough without an essay`() {
        assertEquals(Problem.Required, RejectionReason.validate("  "))
        assertEquals(Problem.TooShort, RejectionReason.validate("bad"))
        assertEquals(Problem.TooLong, RejectionReason.validate("x".repeat(301)))
        assertNull(RejectionReason.validate("The business permit photo is blurry."))
    }

    @Test
    fun `photos compare by content`() {
        assertEquals(PhotoUpload(byteArrayOf(1, 2), "a.jpg"), PhotoUpload(byteArrayOf(1, 2), "a.jpg"))
        assertFalse(PhotoUpload(byteArrayOf(1), "a.jpg") == PhotoUpload(byteArrayOf(2), "a.jpg"))
        assertEquals(
            PhotoUpload(byteArrayOf(1, 2), "a.jpg").hashCode(),
            PhotoUpload(byteArrayOf(1, 2), "a.jpg").hashCode(),
        )
        assertEquals(2, PhotoUpload(byteArrayOf(1, 2)).sizeBytes)
        assertTrue(PhotoUpload(byteArrayOf(1)).toString().contains("1 bytes"))
    }
}
