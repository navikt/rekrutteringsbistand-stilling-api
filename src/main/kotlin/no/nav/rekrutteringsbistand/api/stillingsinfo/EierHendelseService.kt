package no.nav.rekrutteringsbistand.api.stillingsinfo

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import org.springframework.stereotype.Service

@Service
class EierHendelseService(private val rapids: RapidsConnection) {

    fun publiser(stillingsid: String, eierNavident: String, eierNavn: String) =
        rapids.publish(stillingsid, oppdatertEierHendelse(stillingsid, Veileder(eierNavident, eierNavn)))

    private fun oppdatertEierHendelse(stillingsid: String, eier: Veileder) =
        JsonMessage.newMessage(
            mapOf(
                "@event_name" to "stilling_eier_oppdatert",
                "stillingsid" to stillingsid,
                "eier" to eier
            )
        ).toJson()

    private data class Veileder(val eierNavident: String, val eierNavn: String)
}
