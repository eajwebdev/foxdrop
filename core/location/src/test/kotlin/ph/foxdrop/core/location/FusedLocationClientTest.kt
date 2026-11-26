package ph.foxdrop.core.location

import android.Manifest
import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class FusedLocationClientTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val client = FusedLocationClient(context)

    @Test
    fun `without permission the position is never read`() = runTest {
        assertFalse(client.hasPermission())
        assertEquals(LocationResult.PermissionDenied, client.currentLocation())
    }

    @Test
    fun `approximate location is enough, and switched-off location is reported`() = runTest {
        shadowOf(context).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        shadowOf(manager).setLocationEnabled(false)

        assertTrue(client.hasPermission())
        assertEquals(LocationResult.Disabled, client.currentLocation())
    }
}
