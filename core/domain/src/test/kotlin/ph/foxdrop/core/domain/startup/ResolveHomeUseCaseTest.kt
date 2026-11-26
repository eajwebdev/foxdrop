package ph.foxdrop.core.domain.startup

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.domain.auth.RoleRepository
import ph.foxdrop.core.domain.startup.ResolveHomeUseCase.Companion.decideHome
import ph.foxdrop.core.model.AppRole
import ph.foxdrop.core.model.FoxDropApp
import ph.foxdrop.core.model.RoleGrant

/**
 * Phase 1 acceptance: each demo user lands on the correct home. Mirrors the demo accounts in
 * backend/database/seeders/DemoSeeder.php.
 */
class ResolveHomeUseCaseTest {
    private val customer = RoleGrant(AppRole.Customer)
    private val rider = listOf(customer, RoleGrant(AppRole.Rider, provinceId = "neg"))
    private val operator = listOf(customer, RoleGrant(AppRole.Operator, provinceId = "neg"))
    private val storeOwner = listOf(customer, RoleGrant(AppRole.StoreOwner, provinceId = "neg", storeId = "mamas"))
    private val admin = listOf(customer, RoleGrant(AppRole.SuperAdmin))

    @Test
    fun `customers land on the customer home`() {
        assertEquals(HomeDecision.CustomerHome, decideHome(FoxDropApp.Customer, listOf(customer)))
        assertEquals(HomeDecision.RiderNotRegistered, decideHome(FoxDropApp.Rider, listOf(customer)))
        assertEquals(HomeDecision.BusinessNotRegistered, decideHome(FoxDropApp.Business, listOf(customer)))
    }

    @Test
    fun `riders land on the rider home and can still order as customers`() {
        assertEquals(HomeDecision.RiderHome("neg"), decideHome(FoxDropApp.Rider, rider))
        assertEquals(HomeDecision.CustomerHome, decideHome(FoxDropApp.Customer, rider))
    }

    @Test
    fun `business app routes operators and store owners`() {
        assertEquals(HomeDecision.OperatorHome("neg"), decideHome(FoxDropApp.Business, operator))
        assertEquals(HomeDecision.StoreHome("mamas"), decideHome(FoxDropApp.Business, storeOwner))
        assertEquals(
            HomeDecision.OperatorHome("neg"),
            decideHome(FoxDropApp.Business, operator + storeOwner),
        )
        assertEquals(
            HomeDecision.StoreHome("s1"),
            decideHome(FoxDropApp.Business, listOf(RoleGrant(AppRole.StoreStaff, provinceId = "neg", storeId = "s1"))),
        )
        assertEquals(
            HomeDecision.OperatorHome("p2"),
            decideHome(FoxDropApp.Business, listOf(RoleGrant(AppRole.OperatorStaff, provinceId = "p2"))),
        )
    }

    @Test
    fun `admin accounts are kept out of every Android app`() {
        FoxDropApp.entries.forEach { assertEquals(HomeDecision.AdminUseWeb, decideHome(it, admin)) }
        assertEquals(
            HomeDecision.AdminUseWeb,
            decideHome(FoxDropApp.Business, listOf(RoleGrant(AppRole.AdminStaff)) + operator),
        )
    }

    @Test
    fun `use case passes repository errors through`() = runTest {
        val failing = ResolveHomeUseCase(roleRepository(FoxResult.Failure(DomainError.Network)))
        assertEquals(FoxResult.Failure(DomainError.Network), failing(FoxDropApp.Business))

        val ok = ResolveHomeUseCase(roleRepository(FoxResult.Success(operator)))
        assertEquals(FoxResult.Success(HomeDecision.OperatorHome("neg")), ok(FoxDropApp.Business))
    }

    private fun roleRepository(result: FoxResult<List<RoleGrant>>) =
        object : RoleRepository {
            override suspend fun myRoles() = result
        }
}
