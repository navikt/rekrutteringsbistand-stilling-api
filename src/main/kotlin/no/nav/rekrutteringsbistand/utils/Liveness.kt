package rekrutteringsbistand.stilling.indekser.utils

import no.nav.rekrutteringsbistand.api.support.LOG
import java.lang.Exception

object Liveness {
    var isAlive = true
        private set

    fun kill(årsak: String, exception: Exception) {
        LOG.error(årsak, exception)
        isAlive = false
    }
}
