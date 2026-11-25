package ph.foxdrop.core.common.time

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** All business dates are shown in Asia/Manila, whatever the phone's time zone (§10). */
object ManilaTime {
    val ZONE: ZoneId = ZoneId.of("Asia/Manila")

    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)

    fun now(clock: Clock = Clock.systemUTC()): ZonedDateTime = ZonedDateTime.now(clock.withZone(ZONE))

    fun formatTime(instant: Instant): String = timeFormatter.format(instant.atZone(ZONE))

    fun formatDate(instant: Instant): String = dateFormatter.format(instant.atZone(ZONE))
}
