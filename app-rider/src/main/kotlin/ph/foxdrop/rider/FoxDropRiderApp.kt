package ph.foxdrop.rider

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class FoxDropRiderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Debug only; release builds log nothing (§16: never log tokens, phones, addresses, payments).
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}
