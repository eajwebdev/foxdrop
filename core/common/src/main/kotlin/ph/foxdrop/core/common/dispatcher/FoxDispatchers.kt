package ph.foxdrop.core.common.dispatcher

import javax.inject.Qualifier

/** Inject dispatchers instead of hard-coding `Dispatchers.IO` so tests can replace them (§16). */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: FoxDispatchers)

enum class FoxDispatchers {
    Default,
    IO,
}
