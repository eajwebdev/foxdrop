package ph.foxdrop.core.model

/** A WGS84 position (latitude, longitude in degrees). */
data class GeoPoint(val lat: Double, val lng: Double) {
    init {
        require(lat in -MAX_LAT..MAX_LAT) { "latitude out of range" }
        require(lng in -MAX_LNG..MAX_LNG) { "longitude out of range" }
    }

    private companion object {
        const val MAX_LAT = 90.0
        const val MAX_LNG = 180.0
    }
}
