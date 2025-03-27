package no.nav.rekrutteringsbistand.api.hendelser

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("test")
@Component
class RapidApplikasjonTest: RapidApplikasjon {
    override fun publish(key: Stillingsid, value: JsonMessage) = println("Test")
}
