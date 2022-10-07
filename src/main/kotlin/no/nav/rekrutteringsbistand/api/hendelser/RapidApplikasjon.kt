package no.nav.rekrutteringsbistand.api.hendelser

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoRepository
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class RapidApplikasjon(
    @Autowired private val context: ApplicationContext,
    @Autowired private val stillingsinfoRepository: StillingsinfoRepository,
    @Autowired private val rapidsConnection: RapidsConnection
) : InitializingBean {

    override fun afterPropertiesSet() {
        StillingsinfoPopulator(rapidsConnection, stillingsinfoRepository)
        Appkiller(rapidsConnection, context)
    }
}