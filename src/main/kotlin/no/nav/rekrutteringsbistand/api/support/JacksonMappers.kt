package no.nav.rekrutteringsbistand.api.support

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.introspect.DefaultAccessorNamingStrategy
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.util.TimeZone

object JacksonMappers {

    /**
     * Ignorerer ukjente felt, beholder tidssonen i innkommende datoer og bruker Europe/Oslo som standard tidssone.
     */
    val standard: JsonMapper = jacksonMapperBuilder()
        .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .defaultTimeZone(TimeZone.getTimeZone("Europe/Oslo"))
        // Felt som starter med æ, ø eller å forsvinner stille uten denne
        .accessorNaming(DefaultAccessorNamingStrategy.Provider().withFirstCharAcceptance(true, true))
        .build()
}
