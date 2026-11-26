package ph.foxdrop.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import ph.foxdrop.core.model.GeoPoint

/** The result of asking for the phone's position once. */
sealed interface LocationResult {
    data class Found(val point: GeoPoint) : LocationResult

    /** The user has not allowed location access. */
    data object PermissionDenied : LocationResult

    /** Location is switched off in the phone's settings. */
    data object Disabled : LocationResult

    /** No fix in time (indoors, no signal). */
    data object Unavailable : LocationResult
}

/** One-shot current position (a store's address, an operator's zone centre). No tracking. */
interface LocationClient {
    fun hasPermission(): Boolean

    suspend fun currentLocation(): LocationResult
}

@Singleton
internal class FusedLocationClient @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : LocationClient {
    override fun hasPermission(): Boolean =
        LOCATION_PERMISSIONS.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    @SuppressLint("MissingPermission") // checked by hasPermission()
    override suspend fun currentLocation(): LocationResult {
        if (!hasPermission()) return LocationResult.PermissionDenied
        val manager = context.getSystemService(LocationManager::class.java)
        if (manager == null || !LocationManagerCompat.isLocationEnabled(manager)) return LocationResult.Disabled
        val cancellation = CancellationTokenSource()
        val location =
            withTimeoutOrNull(TIMEOUT_MS) {
                LocationServices.getFusedLocationProviderClient(context)
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
                    .await()
            }
        if (location == null) cancellation.cancel()
        return location?.let { LocationResult.Found(GeoPoint(it.latitude, it.longitude)) } ?: LocationResult.Unavailable
    }

    private companion object {
        const val TIMEOUT_MS = 15_000L
    }
}

/** The permissions to ask for; precise is preferred, approximate is accepted. */
val LOCATION_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
