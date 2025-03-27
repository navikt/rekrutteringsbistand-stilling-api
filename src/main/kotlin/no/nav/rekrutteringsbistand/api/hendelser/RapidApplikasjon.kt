package no.nav.rekrutteringsbistand.api.hendelser

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid

interface RapidApplikasjon {

    fun publish(key: Stillingsid, value: JsonMessage)
}
