package no.nav.rekrutteringsbistand.api.hendelser

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.hendelser.RapidApplikasjon.Companion.registrerMineStillingerLytter
import no.nav.rekrutteringsbistand.api.minestillinger.MineStillingerLytter
import no.nav.rekrutteringsbistand.api.minestillinger.MineStillingerRepository
import no.nav.rekrutteringsbistand.api.minestillinger.MineStillingerService
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
    @Autowired private val arbeidsplassenKlient: ArbeidsplassenKlient,
    @Autowired private val mineStillingerService: MineStillingerService
    ): Runnable {

    companion object {
        fun <T: RapidsConnection> T.registrerBerikingslyttere(
            stillingsinfoRepository: StillingsinfoRepository,
            context: ApplicationContext,
            arbeidsplassenKlient: ArbeidsplassenKlient
        ) = apply {
            StillingsinfoPopulator(this, stillingsinfoRepository, arbeidsplassenKlient)
            StillingsinfoPopulatorGammel(this, stillingsinfoRepository, arbeidsplassenKlient)
            Appkiller(this, context)
        }

        fun <T: RapidsConnection> T.registrerMineStillingerLytter(mineStillingerService: MineStillingerService) =
            apply {
                MineStillingerLytter(this, mineStillingerService)
            }
    }

    @EventListener(ApplicationReadyEvent::class)
    fun afterPropertiesSet() {
        Thread(this).start()
    }

    override fun run() {
        RapidApplication.create(environment.toMap())
            .registrerBerikingslyttere(stillingsinfoRepository, context, arbeidsplassenKlient)
            .registrerMineStillingerLytter(mineStillingerService)
            .start()
    }
}

private fun Environment.toMap() = if (this is AbstractEnvironment)
    this.propertySources.filterIsInstance<MapPropertySource>()
        .flatMap { it.source.map { (key, value) -> key to value.toString() } }.toMap()
else emptyMap<String, String>()
