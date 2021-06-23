package no.nav.rekrutteringsbistand.api.stillingsinfo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.RapidsConnection
import org.springframework.stereotype.Service

@Service
class VeilederHendelseService(private val rapids: RapidsConnection) {
    private val mapper = ObjectMapper()
    fun publiserOppdaterVeilederHendelse(stillingsid: String, eierNavident: String, eierNavn: String) =
        rapids.publish(stillingsid, oppdatertVeilederHendelse(stillingsid, Veileder(eierNavident, eierNavn)))

    private fun oppdatertVeilederHendelse(stillingsid: String, veileder: Veileder) =
        mapper.createObjectNode().apply {
            put("@event_name", "Stilling.Veileder.Oppdatert")
            set<ObjectNode>("@veileder", mapper.readTree(mapper.writeValueAsString(veileder)))
            put("@stillingsid", stillingsid)
        }.toJson()

    private data class Veileder(val eierNavident: String, val eierNavn: String)

    private fun ObjectNode.toJson() = mapper.writeValueAsString(this)
}