package no.nav.rekrutteringsbistand.api.hendelser

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.core.env.AbstractEnvironment
import org.springframework.core.env.Environment
import org.springframework.core.env.MapPropertySource
import org.springframework.stereotype.Component

@Profile("!test")
@Component
class RapidApplikasjon(
    @Autowired private val context: ApplicationContext,
    @Autowired private val stillingsinfoRepository: StillingsinfoRepository,
    @Autowired private val environment: Environment,
    @Autowired private val arbeidsplassenKlient: ArbeidsplassenKlient
    ): Runnable {

    companion object {
        fun <T: RapidsConnection> T.registrerLyttere(
            stillingsinfoRepository: StillingsinfoRepository,
            context: ApplicationContext,
            arbeidsplassenKlient: ArbeidsplassenKlient
        ) = apply {
            StillingPopulator(this, stillingsinfoRepository, arbeidsplassenKlient)
            StillingsinfoPopulatorGammel(this, stillingsinfoRepository, arbeidsplassenKlient)
            Appkiller(this, context)
        }
    }

    @EventListener(ApplicationReadyEvent::class)
    fun afterPropertiesSet() {
        Thread(this).start()
    }

    override fun run() {
        RapidApplication.create(environment.toMap())
            .registrerLyttere(stillingsinfoRepository, context, arbeidsplassenKlient).start()
    }
}

private fun Environment.toMap() = if (this is AbstractEnvironment)
    this.propertySources.filterIsInstance<MapPropertySource>()
        .flatMap { it.source.map { (key, value) -> key to value.toString() } }.toMap()
else emptyMap<String, String>()
