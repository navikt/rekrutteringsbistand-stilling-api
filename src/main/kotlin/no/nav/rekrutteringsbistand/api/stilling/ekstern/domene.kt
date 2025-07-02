package no.nav.rekrutteringsbistand.api.stilling.ekstern

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.rekrutteringsbistand.api.stilling.Geografi
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingskategori
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList

@JsonIgnoreProperties(ignoreUnknown = true)
data class StillingForPersonbruker(
        val id: Long?,
        val annonsennr: String?,
        val uuid: String?,
        val updated: LocalDateTime?,
        val contactList: List<Contact> = ArrayList(),
        val title: String?,
        val medium: String?,
        val employer: Arbeidsgiver?,
        val businessName: String?,
        val status: String?,
        val location: Geografi?,
        val properties: Map<String, String> = HashMap(),
        val source: String?,
        val stillingskategori: Stillingskategori?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Arbeidsgiver(
        val contactList: List<Contact> = ArrayList(),
        val location: Geografi?,
        val properties: Map<String, String> = HashMap(),
        val name: String?,
        val publicName: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Contact(
        val name: String?,
        val email: String?,
        val phone: String?,
        val title: String?
)

