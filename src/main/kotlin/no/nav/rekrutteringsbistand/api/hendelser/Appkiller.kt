package no.nav.rekrutteringsbistand.api.hendelser

import no.nav.helse.rapids_rivers.RapidsConnection
import org.springframework.boot.availability.AvailabilityChangeEvent
import org.springframework.boot.availability.LivenessState
import org.springframework.context.ApplicationContext

class Appkiller(rapidsConnection: RapidsConnection, private val context: ApplicationContext): RapidsConnection.StatusListener {
    init {
        rapidsConnection.register(this)
    }
    override fun onShutdown(rapidsConnection: RapidsConnection) =
        AvailabilityChangeEvent.publish(context, LivenessState.BROKEN)
}