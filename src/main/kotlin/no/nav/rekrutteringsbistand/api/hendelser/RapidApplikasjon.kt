package no.nav.rekrutteringsbistand.api.hendelser

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoRepository
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Profile
import org.springframework.core.env.AbstractEnvironment
import org.springframework.core.env.Environment
import org.springframework.core.env.MapPropertySource
import org.springframework.stereotype.Component

@Profile("!test")
@Component
class RapidApplikasjon(
    @Autowired private val context: ApplicationContext,
    @Autowired private val stillingsinfoRepository: StillingsinfoRepository,
    @Autowired private val environment: Environment
) : InitializingBean {

    companion object {
        fun <T: RapidsConnection> T.registrerLyttere(
            stillingsinfoRepository: StillingsinfoRepository,
            context: ApplicationContext
        ) = apply {
            StillingsinfoPopulator(this, stillingsinfoRepository)
            Appkiller(this, context)
        }
    }

    override fun afterPropertiesSet() {
        RapidApplication.create(environment.toMap()).registrerLyttere(stillingsinfoRepository, context).start()
    }
}

private fun Environment.toMap() = if (this is AbstractEnvironment)
    this.propertySources.filterIsInstance<MapPropertySource>()
        .flatMap { it.source.map { (key, value) -> key to value.toString() } }.toMap()
else emptyMap<String, String>()
