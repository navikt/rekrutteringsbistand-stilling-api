package no.nav.rekrutteringsbistand.api.stilling

import java.util.UUID

class PubliserteArbeidsplassenStillinger {
    companion object {
        /**
         * Nye stillinger skal publiseres via import-apiet, så dette er stillinger som allerede er publisert via Rest API'et
         */
        fun erPublisertPåArbeidsplassenViaRestApi(uuid: UUID): Boolean {
            return PUBLISERTE_STILLINGER.contains(uuid.toString())
        }

        val PUBLISERTE_STILLINGER: List<String> = listOf(
            // Fra produksjon
            "2570a875-8e40-4d5f-92c2-310f51332757",
            "5e1ca6d0-f915-4086-85bf-95e2bbee7f05",

            // Fra dev
            "94c0fd72-307f-4243-977e-db95de2cbb1c",
        )
    }

}
