package no.nav.rekrutteringsbistand.api.stilling.ekstern

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.rekrutteringsbistand.api.stilling.Geografi
import no.nav.rekrutteringsbistand.api.stilling.Media
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList


@JsonIgnoreProperties(ignoreUnknown = true)
data class Stilling(
        val id: Long?,
        val updated: LocalDateTime?,
        val contactList: List<Contact> = ArrayList(),
        val title: String?,
        val medium: String?,
        val employer: Arbeidsgiver?,
        val location: Geografi?,
        val properties: Map<String, String> = HashMap()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Arbeidsgiver(
        val mediaList: List<Media> = ArrayList(),
        val contactList: List<Contact> = ArrayList(),
        val location: Geografi?,
        val properties: Map<String, String> = HashMap(),
        val name: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Contact(
        val name: String?,
        val email: String?,
        val phone: String?,
        val title: String?
)

