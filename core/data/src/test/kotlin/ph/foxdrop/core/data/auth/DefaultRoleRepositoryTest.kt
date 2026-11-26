package ph.foxdrop.core.data.auth

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import ph.foxdrop.core.common.result.DomainError
import ph.foxdrop.core.common.result.FoxResult
import ph.foxdrop.core.model.AppRole
import ph.foxdrop.core.model.RoleGrant
import ph.foxdrop.core.network.auth.RolesRemoteDataSource

class DefaultRoleRepositoryTest {
    private val customer = listOf(RoleGrant(AppRole.Customer))
    private val operator = customer + RoleGrant(AppRole.Operator, "neg")

    private fun roles(saved: List<RoleGrant>?, fresh: FoxResult<List<RoleGrant>>) =
        DefaultRoleRepository(
            object : RolesRemoteDataSource {
                override suspend fun savedRoles() = saved

                override suspend fun fetchRoles() = fresh
            },
        ).roles()

    @Test
    fun `saved roles come first, then the server's when they changed`() = runTest {
        assertEquals(
            listOf(FoxResult.Success(customer), FoxResult.Success(operator)),
            roles(saved = customer, fresh = FoxResult.Success(operator)).toList(),
        )
    }

    @Test
    fun `unchanged roles are reported once`() = runTest {
        assertEquals(listOf(FoxResult.Success(customer)), roles(customer, FoxResult.Success(customer)).toList())
    }

    @Test
    fun `no signal keeps the saved roles`() = runTest {
        assertEquals(
            listOf(FoxResult.Success(customer)),
            roles(customer, FoxResult.Failure(DomainError.Network)).toList(),
        )
    }

    @Test
    fun `a rejected session is reported even after the saved roles`() = runTest {
        assertEquals(
            listOf(FoxResult.Success(customer), FoxResult.Failure(DomainError.Unauthorized)),
            roles(customer, FoxResult.Failure(DomainError.Unauthorized)).toList(),
        )
    }

    @Test
    fun `without saved roles a network failure is reported`() = runTest {
        assertEquals(
            listOf(FoxResult.Failure(DomainError.Network)),
            roles(null, FoxResult.Failure(DomainError.Network)).toList(),
        )
    }
}
