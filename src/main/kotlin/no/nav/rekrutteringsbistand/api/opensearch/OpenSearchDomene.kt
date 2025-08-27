package no.nav.rekrutteringsbistand.api.opensearch

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.rekrutteringsbistand.api.stilling.*
import java.time.LocalDateTime
import java.time.ZonedDateTime

data class OpensSearchResponse(
    val _source: Source
) {
    fun toStilling(objectMapper: ObjectMapper): FrontendStilling = FrontendStilling(
        title = _source.stilling.tittel ?: "Stilling uten valgt jobbtittel",
        properties = _source.stilling.properties.mapValues {
            when(val value = it.value) {
                null -> "null"
                is List<*> -> objectMapper.writeValueAsString(value)
                else -> value.toString()
            }
        },
        businessName = _source.stilling.businessName,
        id = _source.stilling.annonsenr.toLong(), // Ok siden det kun er eksterne stillinger som har id med bare tall
        annonsenr = _source.stilling.annonsenr,
        uuid = _source.stilling.uuid,
        created = konverterDato(_source.stilling.created),
        createdBy = "",
        updated = konverterDato(_source.stilling.updated),
        updatedBy = "",
        status = _source.stilling.status,
        administration = _source.stilling.administration.toAdministration(),
        mediaList = emptyList(),
        contactList = _source.stilling.contacts.map(OpenSearchContact::toContact),
        privacy = _source.stilling.privacy,
        source = _source.stilling.source,
        medium = _source.stilling.medium,
        reference = _source.stilling.reference,
        published = if(_source.stilling.published.isNullOrBlank()) null else konverterDato(_source.stilling.published),
        expires =  if(_source.stilling.expires.isNullOrBlank()) null else konverterDato(_source.stilling.expires),
        employer = _source.stilling.employer?.toArbeidsgiver(),
        locationList = _source.stilling.locations.map(OpenSearchArbeidssted::toGeografi),
        categoryList = emptyList(), // Får aldri noen verdier her fra ekstern-topicet
        publishedByAdmin = _source.stilling.publishedByAdmin,
        firstPublished = null,
        deactivatedByExpiry = _source.stilling.deactivatedByExpiry,
        activationOnPublishingDate = null,
        location = null,
        versjon = null
    )

    fun konverterDato(dato: String): LocalDateTime {
        try {
            return LocalDateTime.parse(dato)
        } catch (e: Exception) {
            // ignorer for å støtte flere datoformater
        }

        try {
            return ZonedDateTime.parse(dato).toLocalDateTime()
        } catch (e: Exception) {
            throw RuntimeException("Greide ikke konvertere dato til zonedDateTime: $dato", e)
        }
    }

     data class Source(
         val stilling: OpenSearchStilling,
         val stillingsinfo: OpenSearchStillingsinfo?
    )

    data class OpenSearchStilling(
        val tittel: String?,
        val properties: Map<String, Any?>,
        val employer: Employer?,
        val locations: List<OpenSearchArbeidssted>,
        val contacts: List<OpenSearchContact>,
        val uuid: String,
        val annonsenr: String,
        val created: String, // Dato
        val updated: String, // Dato
        val status: String,
        val privacy: String?,
        val published: String?, //dato
        val expires: String?, //dato
        val source: String?,
        val medium: String?,
        val reference: String?,
        val administration: OpenSearchAdministration,
        val categories: List<OpenSearchCategories?>?,
        val businessName: String?,
        val publishedByAdmin: String?,
        val deactivatedByExpiry: Boolean?,
    )

    data class OpenSearchStillingsinfo(
        val eierNavident: String?,
        val eierNavn: String?,
        val notat: String?,
        val stillingsid: String,
        val stillingsinfoid: String,
        val stillingskategori: String?,
    )

    data class OpenSearchCategories(
        val styrkCode: String?,
        val name: String?
    )

    data class OpenSearchContact(
        val name: String,
        val title: String,
        val email: String,
        val phone: String,
        val role: String?
    ) {
        fun toContact() = Contact(
            name = name,
            email = email,
            phone = phone,
            role = role,
            title = title
        )
    }

    data class OpenSearchAdministration(
        val status: String?,
        val comments: String?,
        val reportee: String?,
        val remarks: List<String> = ArrayList(),
        val navIdent: String?
    ) {
        fun toAdministration() = Administration(
            status = status,
            comments = comments,
            reportee = reportee,
            remarks = remarks,
            navIdent = navIdent,
            id = null
        )
    }

    data class Employer(
        val name: String,
        val orgnr: String,
        val publicName: String
    ) {
        fun toArbeidsgiver() = Arbeidsgiver(
            name = name,
            orgnr = orgnr,
            publicName = publicName,
            id = null,
            uuid = null,
            created = null,
            createdBy = null,
            updated = null,
            updatedBy = null,
            mediaList = emptyList(),
            contactList = emptyList(),
            location = null,
            locationList = emptyList(),
            properties = emptyMap(),
            status = null,
            parentOrgnr = null,
            deactivated = null,
            orgform = null,
            employees = null
        )
    }

    data class OpenSearchArbeidssted(
        val address: String?,
        val postalCode: String?,
        val county: String?,
        val municipal: String?,
        val municipalCode: String?,
        val city: String?,
        val country: String?,
    ) {
        fun toGeografi(): Geografi = Geografi(
            address = address,
            postalCode = postalCode,
            county = county,
            municipal = municipal,
            municipalCode = municipalCode,
            city = city,
            country = country,
            latitude = null,
            longitude = null
        )
    }
}
