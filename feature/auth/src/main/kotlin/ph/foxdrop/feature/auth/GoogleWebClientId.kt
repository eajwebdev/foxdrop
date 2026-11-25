package ph.foxdrop.feature.auth

import javax.inject.Qualifier

/** Google OAuth "Web application" client ID used as the server client ID for Credential Manager. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GoogleWebClientId
