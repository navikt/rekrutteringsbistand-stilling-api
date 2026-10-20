package no.nav.rekrutteringsbistand.api.support

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.introspect.DefaultAccessorNamingStrategy
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.kotlinModule
import java.util.TimeZone

object JacksonMappers {

    /**
     * Jackson 3 slår på StrictNullChecks som standard, slik at null i f.eks. List<String> eller Map<String, String>
     * gir feil. Vi slår det av for å beholde oppførselen fra Jackson 2. Brukes også av Spring sin JsonMapper.
     */
    val kotlinModule: KotlinModule = kotlinModule { disable(KotlinFeature.StrictNullChecks) }

    /** Felt som starter med æ, ø eller å forsvinner stille uten denne. Brukes også av Spring sin JsonMapper. */
    val accessorNaming: DefaultAccessorNamingStrategy.Provider =
        DefaultAccessorNamingStrategy.Provider().withFirstCharAcceptance(true, true)

    /**
     * Ignorerer ukjente felt, beholder tidssonen i innkommende datoer og bruker Europe/Oslo som standard tidssone.
     */
    val standard: JsonMapper = JsonMapper.builder()
        .addModule(kotlinModule)
        .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .defaultTimeZone(TimeZone.getTimeZone("Europe/Oslo"))
        .accessorNaming(accessorNaming)
        .build()
}
