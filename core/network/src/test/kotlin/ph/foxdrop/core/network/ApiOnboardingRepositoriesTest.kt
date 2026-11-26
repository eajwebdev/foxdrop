package ph.foxdrop.core.network

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.domain.media.PhotoUpload
import ph.foxdrop.core.domain.operator.ApplicationStatus
import ph.foxdrop.core.domain.operator.LicenseStatus
import ph.foxdrop.core.domain.operator.OperatorApplicationForm
import ph.foxdrop.core.domain.operator.PaymentStatus
import ph.foxdrop.core.domain.partner.RiderApplicationForm
import ph.foxdrop.core.domain.partner.RiderStatus
import ph.foxdrop.core.domain.partner.StoreApplicationForm
import ph.foxdrop.core.domain.partner.StoreCategory
import ph.foxdrop.core.domain.partner.StoreStatus
import ph.foxdrop.core.domain.partner.VehicleType
import ph.foxdrop.core.domain.territory.RateCard
import ph.foxdrop.core.model.GeoPoint
import ph.foxdrop.core.network.operator.ApiOperatorLicenseRepository
import ph.foxdrop.core.network.partner.ApiPartnerReviewRepository
import ph.foxdrop.core.network.partner.ApiRiderOnboardingRepository
import ph.foxdrop.core.network.partner.ApiStoreOnboardingRepository
import ph.foxdrop.core.network.province.ApiProvinceRepository
import ph.foxdrop.core.network.territory.ApiTerritoryRepository

private const val LICENSE_JSON =
    """{"id":"l1","province":{"id":"neg","name":"Negros Occidental"},"status":"PENDING_PAYMENT",""" +
        """"start_at":null,"end_at":null,"days_left":null,"fee_centavos":1500000,"can_pay":true}"""

class ApiOnboardingRepositoriesTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val dispatcher = StandardTestDispatcher()
    private val photo = PhotoUpload(byteArrayOf(1, 2, 3), "valid_id.jpg")

    private fun api(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        mockClient(dispatcher, requests, handler = handler)

    @Test
    fun `the province directory says where operators may apply`() = runTest(dispatcher) {
        val json =
            """{"data":[{"id":"neg","psgc_code":"0604500000","name":"Negros Occidental",""" +
                """"region_name":"Region VI","is_serviceable":true,"is_available":false},""" +
                """{"id":"ilo","name":"Iloilo","is_available":true}]}"""
        val repo = ApiProvinceRepository(api { jsonResponse(json) })

        val provinces = (repo.directory() as FoxResult.Success).value

        assertEquals(listOf(true, false), provinces.map { it.isServiceable })
        assertEquals(listOf(false, true), provinces.map { it.isAvailable })
        assertEquals("/api/v1/provinces", requests.single().url.encodedPath)
    }

    @Test
    fun `an operator application is a multipart upload with the ID photo`() = runTest(dispatcher) {
        val json =
            """{"data":{"id":"a1","status":"SUBMITTED","province":{"id":"neg","name":"Negros Occidental"},""" +
                """"business_name":"Gonzaga Logistics","rejection_reason":null,"license":$LICENSE_JSON}}"""
        val repo = ApiOperatorLicenseRepository(api { jsonResponse(json, HttpStatusCode.Created) })

        val result =
            repo.submitApplication(
                OperatorApplicationForm("neg", " Gonzaga Logistics ", null, "+639170000006", null, photo, null),
            )

        val application = (result as FoxResult.Success).value
        assertEquals(ApplicationStatus.Submitted, application.status)
        assertEquals(LicenseStatus.PendingPayment, application.license?.status)
        assertEquals(1_500_000L, application.license?.feeCentavos)
        val request = requests.single()
        assertEquals("/api/v1/operator/applications", request.url.encodedPath)
        assertTrue(request.body.contentType.toString().startsWith("multipart/form-data"))
        val body = request.bodyString()
        assertTrue(body.contains("name=\"province_id\""))
        assertTrue(body.contains("Gonzaga Logistics"))
        assertTrue(body.contains("filename=\"valid_id.jpg\""))
        assertTrue(body.contains("image/jpeg"))
        assertFalse(body.contains("business_permit"))
        assertFalse(body.contains("registration_number"))
    }

    @Test
    fun `a taken province comes back as a conflict with the server's message`() = runTest(dispatcher) {
        val json = """{"message":"This province already has an operator."}"""
        val repo = ApiOperatorLicenseRepository(api { jsonResponse(json, HttpStatusCode.Conflict) })

        assertEquals(
            FoxResult.Failure(DomainError.Conflict("This province already has an operator.")),
            repo.submitApplication(
                OperatorApplicationForm("neg", "Gonzaga", null, "+639170000006", null, photo, photo),
            ),
        )
    }

    @Test
    fun `no license is an empty answer, not an error`() = runTest(dispatcher) {
        val notFound = """{"message":"Not found."}"""
        val none = ApiOperatorLicenseRepository(api { jsonResponse(notFound, HttpStatusCode.NotFound) })
        assertEquals(FoxResult.Success(null), none.myLicense())

        val noApplication = ApiOperatorLicenseRepository(api { jsonResponse("""{"data":null}""") })
        assertEquals(FoxResult.Success(null), noApplication.myApplication())

        val some = ApiOperatorLicenseRepository(api { jsonResponse("""{"data":$LICENSE_JSON}""") })
        assertEquals(true, (some.myLicense() as FoxResult.Success).value?.canPay)
    }

    @Test
    fun `payments start on the license and are read back by id`() = runTest(dispatcher) {
        fun paymentJson(status: String) =
            """{"data":{"id":"p1","purpose":"LICENSE","status":"$status","amount_centavos":1500000,""" +
                """"checkout_url":"http://10.0.2.2:8000/dev/checkout/p1","expires_at":null}}"""
        val repo =
            ApiOperatorLicenseRepository(
                api { request ->
                    jsonResponse(
                        paymentJson(
                            if (request.method ==
                                HttpMethod.Post
                            ) {
                                "PENDING"
                            } else {
                                "PAID"
                            },
                        ),
                    )
                },
            )

        val started = (repo.startPayment("l1") as FoxResult.Success).value
        val paid = (repo.payment("p1") as FoxResult.Success).value

        assertEquals(PaymentStatus.Pending, started.status)
        assertEquals("http://10.0.2.2:8000/dev/checkout/p1", started.checkoutUrl)
        assertEquals(PaymentStatus.Paid, paid.status)
        assertEquals(listOf("/api/v1/licenses/l1/payments", "/api/v1/payments/p1"), requests.map { it.url.encodedPath })
    }

    @Test
    fun `territory calls send JSON and map zones, previews and settings`() = runTest(dispatcher) {
        val zoneJson =
            """{"data":{"id":"z1","name":"Metro Bacolod","is_active":true,"center":{"lat":10.67,"lng":122.95},""" +
                """"radius_km":5.0,"area_km2":78.5}}"""
        val previewJson =
            """{"data":[{"distance_km":3,"delivery_fee_centavos":5000,"rider_earning_centavos":4000,""" +
                """"operator_cut_centavos":1000},{"distance_km":20,"delivery_fee_centavos":null}]}"""
        val provinceJson =
            """{"data":{"id":"neg","name":"Negros Occidental","center":null,"store_fee_centavos":2000,""" +
                """"store_fee_grace_days":7}}"""
        val repo =
            ApiTerritoryRepository(
                api { request ->
                    when (request.url.encodedPath) {
                        "/api/v1/operator/zones" -> jsonResponse(zoneJson, HttpStatusCode.Created)
                        "/api/v1/operator/rate-cards/preview" -> jsonResponse(previewJson)
                        else -> jsonResponse(provinceJson)
                    }
                },
            )

        val zone = (repo.createCircleZone("Metro Bacolod", GeoPoint(10.67, 122.95), 5.0) as FoxResult.Success).value
        val preview = (repo.previewFees(RateCard.Default, listOf(3.0, 20.0)) as FoxResult.Success).value
        val province = (repo.updateStoreFee(2_000) as FoxResult.Success).value

        assertEquals(5.0, zone.radiusKm)
        assertEquals(GeoPoint(10.67, 122.95), zone.center)
        assertEquals(5_000L, preview.first().deliveryFeeCentavos)
        assertNull(preview.last().deliveryFeeCentavos)
        assertEquals(2_000L, province.storeFeeCentavos)
        assertTrue(requests[0].body.contentType.toString().startsWith("application/json"))
        assertTrue(requests[0].bodyString().contains("\"radius_km\":5.0"))
        assertTrue(requests[1].bodyString().contains("\"distances_km\":[3.0,20.0]"))
        assertEquals(HttpMethod.Patch, requests[2].method)
    }

    @Test
    fun `territory reads zones, rate card and province and deletes zones`() = runTest(dispatcher) {
        val cardJson =
            """{"data":{"id":"r1","base_fare_centavos":4000,"base_km":2.0,"rate_per_km_centavos":1000,""" +
                """"min_fee_centavos":4000,"max_km":15.0,"rider_share_bps":8000,""" +
                """"effective_from":"2026-11-26T00:00:00Z"}}"""
        val provinceJson =
            """{"data":{"id":"neg","name":"Negros Occidental","center":{"lat":10.6,"lng":122.9},""" +
                """"store_fee_centavos":1500}}"""
        val repo =
            ApiTerritoryRepository(
                api { request ->
                    when (request.url.encodedPath) {
                        "/api/v1/operator/zones" -> jsonResponse("""{"data":[]}""")
                        "/api/v1/operator/rate-card" -> jsonResponse(cardJson)
                        "/api/v1/operator/zones/z1" -> jsonResponse("", HttpStatusCode.NoContent)
                        else -> jsonResponse(provinceJson)
                    }
                },
            )

        assertEquals(FoxResult.Success(emptyList<Any>()), repo.zones())
        assertEquals(8_000, (repo.rateCard() as FoxResult.Success).value.riderShareBps)
        assertEquals(FoxResult.Success(Unit), repo.deleteZone("z1"))
        assertEquals(GeoPoint(10.6, 122.9), (repo.province() as FoxResult.Success).value.center)
        assertEquals(HttpMethod.Delete, requests[2].method)
    }

    @Test
    fun `publishing a rate card posts every field`() = runTest(dispatcher) {
        val repo =
            ApiTerritoryRepository(
                api { request -> jsonResponse("""{"data":${request.bodyString()}}""", HttpStatusCode.Created) },
            )

        val published =
            (repo.publishRateCard(RateCard.Default.copy(baseFareCentavos = 5_000)) as FoxResult.Success).value

        assertEquals(5_000L, published.baseFareCentavos)
        assertTrue(requests.single().bodyString().contains("\"rider_share_bps\":8000"))
    }

    @Test
    fun `a store application uploads the details, location and both photos`() = runTest(dispatcher) {
        val mineJson =
            """{"data":[{"id":"s1","name":"Mama's Kitchen","category":"FOOD","status":"REJECTED",""" +
                """"rejection_reason":"Blurry permit","province":{"id":"neg","name":"Negros Occidental"},""" +
                """"created_at":"x"}]}"""
        val createdJson =
            """{"data":{"id":"s1","name":"Mama's Kitchen","category":"FOOD","status":"SUBMITTED",""" +
                """"province":{"id":"neg","name":"Negros Occidental"}}}"""
        val repo =
            ApiStoreOnboardingRepository(
                api { request ->
                    if (request.method == HttpMethod.Get) {
                        jsonResponse(mineJson)
                    } else {
                        jsonResponse(createdJson, HttpStatusCode.Created)
                    }
                },
            )
        val form =
            StoreApplicationForm(
                "neg", "Mama's Kitchen", StoreCategory.Food, null, "Lacson St.", null, "Bacolod City", null,
                GeoPoint(10.674, 122.953), photo, photo,
            )

        val submitted = (repo.submit(form) as FoxResult.Success).value
        val mine = (repo.myStores() as FoxResult.Success).value

        assertEquals(StoreStatus.Submitted, submitted.status)
        assertEquals(StoreStatus.Rejected, mine.single().status)
        assertEquals("Blurry permit", mine.single().rejectionReason)
        val body = requests.first().bodyString()
        val fields = listOf("category", "latitude", "business_permit", "valid_id").map { "name=\"$it\"" }
        (fields + listOf("FOOD", "10.674")).forEach {
            assertTrue(it, body.contains(it))
        }
        assertFalse(body.contains("name=\"barangay\""))
    }

    @Test
    fun `a bicycle rider uploads no vehicle papers`() = runTest(dispatcher) {
        val riderJson =
            """{"data":{"id":"r1","status":"DOCS_REVIEW","province":{"id":"neg","name":"Negros Occidental"},""" +
                """"vehicle_type":"BICYCLE","plate_no":null,"license_no_last4":"4567",""" +
                """"license_expiry":"2028-01-31"}}"""
        val repo =
            ApiRiderOnboardingRepository(
                api { request ->
                    if (request.method == HttpMethod.Get) {
                        jsonResponse("""{"data":null}""")
                    } else {
                        jsonResponse(riderJson, HttpStatusCode.Created)
                    }
                },
            )
        val form =
            RiderApplicationForm(
                "neg", "Ana Villanueva", VehicleType.Bicycle, null, "N01-23-4567", "2028-01-31",
                photo, photo, photo, null, null,
            )

        assertEquals(FoxResult.Success(null), repo.me())
        val rider = (repo.submit(form) as FoxResult.Success).value

        assertEquals(RiderStatus.DocsReview, rider.status)
        assertEquals(VehicleType.Bicycle, rider.vehicleType)
        val body = requests.last().bodyString()
        assertTrue(body.contains("BICYCLE"))
        assertTrue(body.contains("N01-23-4567"))
        assertTrue(body.contains("name=\"selfie\""))
        assertFalse(body.contains("or_cr"))
        assertFalse(body.contains("plate_no"))
    }

    @Test
    fun `reviewers list by status and send rejection reasons`() = runTest(dispatcher) {
        val storeJson =
            """{"id":"s1","name":"Mama's Kitchen","category":"FOOD","status":"SUBMITTED",""" +
                """"address_line":"Lacson St.","city":"Bacolod City",""" +
                """"owner":{"full_name":"Rosa Lacson","phone":"+639170000007"},"created_at":"2026-11-26",""" +
                """"location":{"lat":10.674,"lng":122.953},""" +
                """"documents":[{"type":"BUSINESS_PERMIT","url":"https://x/s","status":"PENDING"}]}"""
        val riderJson =
            """{"id":"r1","status":"DOCS_REVIEW","full_name":"Paolo Reyes","phone":"+639170000003",""" +
                """"vehicle_type":"MOTORCYCLE","plate_no":"NEG 1234","created_at":"2026-11-26",""" +
                """"documents":[{"type":"SELFIE","url":"https://x/r"}]}"""
        val repo =
            ApiPartnerReviewRepository(
                api { request ->
                    val path = request.url.encodedPath
                    when {
                        path == "/api/v1/operator/stores" -> jsonResponse("""{"data":[$storeJson]}""")
                        path.startsWith("/api/v1/operator/stores/") -> jsonResponse("""{"data":$storeJson}""")
                        path == "/api/v1/operator/riders" -> jsonResponse("""{"data":[$riderJson]}""")
                        else -> jsonResponse("""{"data":$riderJson}""")
                    }
                },
            )

        val stores = (repo.stores(StoreStatus.Submitted) as FoxResult.Success).value
        val store = (repo.store("s1") as FoxResult.Success).value
        repo.approveStore("s1")
        repo.rejectStore("s1", "Blurry permit photo")
        val riders = (repo.riders(RiderStatus.DocsReview) as FoxResult.Success).value
        val rider = (repo.rider("r1") as FoxResult.Success).value
        repo.approveRider("r1")
        repo.rejectRider("r1", "Expired license")

        assertEquals("Rosa Lacson", stores.single().ownerName)
        assertEquals("SUBMITTED", requests[0].url.parameters["status"])
        assertEquals(GeoPoint(10.674, 122.953), store.location)
        assertEquals("https://x/s", store.documents.single().url)
        assertEquals("/api/v1/operator/stores/s1/approve", requests[2].url.encodedPath)
        assertEquals("{\"reason\":\"Blurry permit photo\"}", requests[3].bodyString())
        assertEquals("DOCS_REVIEW", requests[4].url.parameters["status"])
        assertEquals("NEG 1234", riders.single().plateNo)
        assertEquals("SELFIE", rider.documents.single().type)
        assertEquals("/api/v1/operator/riders/r1/reject", requests[7].url.encodedPath)
    }

    @Test
    fun `enum names follow the API and unknown values do not crash`() {
        assertEquals(LicenseStatus.PendingPayment, apiEnum("PENDING_PAYMENT", LicenseStatus.Unknown))
        assertEquals(LicenseStatus.Unknown, apiEnum("SOMETHING_NEW", LicenseStatus.Unknown))
        assertEquals(LicenseStatus.Unknown, apiEnum(null, LicenseStatus.Unknown))
        assertEquals("DOCS_REVIEW", RiderStatus.DocsReview.apiName())
        assertEquals("FOOD", StoreCategory.Food.apiName())
    }
}
