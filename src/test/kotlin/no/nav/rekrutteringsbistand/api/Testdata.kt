package no.nav.rekrutteringsbistand.api

import no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettStillingAdministrationDto
import no.nav.rekrutteringsbistand.api.autorisasjon.InnloggetVeileder
import no.nav.rekrutteringsbistand.api.autorisasjon.Rolle
import no.nav.rekrutteringsbistand.api.autorisasjon.TestRolle
import no.nav.rekrutteringsbistand.api.stilling.*
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

object Testdata {

    val enVeileder = InnloggetVeileder("Clark Kent", "C12345", listOf(Rolle.ARBEIDSGIVERRETTET))

    const val etNotat = "notatet"

    private  val styrkCode = "3112.12"
    private val styrkTittel = "Byggeleder"
    val styrk = Kategori(2148934, styrkCode, "STYRK08NAV", styrkTittel, null, null)

    val enStilling = Stilling(
        id = 1000,
        uuid = UUID.randomUUID().toString(),
        created = LocalDateTime.now(),
        createdBy = "nss-admin",
        updated = LocalDateTime.now(),
        updatedBy = "nss-admin",
        title = "testnss",
        status = "ACTIVE",
        privacy = "SHOW_ALL",
        source = "ASS",
        medium = "ASS",
        reference = UUID.randomUUID().toString(),
        published = LocalDateTime.now(),
        expires = LocalDateTime.now(),
        employer = null,
        administration = null,
        location = null,
        publishedByAdmin = null,
        businessName = null,
        firstPublished = null,
        deactivatedByExpiry = null,
        activationOnPublishingDate = null
    )

    val enOpprettStillingDto = OpprettStillingDto(
        createdBy = "pam-rekrutteringsbistand",
        updatedBy = "pam-rekrutteringsbistand",
        source = "DIR",
        privacy = "INTERNAL_NOT_SHOWN",
        administration = OpprettStillingAdministrationDto(
            status = "PENDING",
            reportee = enVeileder.displayName,
            navIdent = enVeileder.navIdent,
        ),
    )

    val enOpprettRekrutteringsbistandstillingDto =
        enOpprettRekrutteringsbistandstillingDtoMedKategori(Stillingskategori.ARBEIDSTRENING)

    fun enOpprettRekrutteringsbistandstillingDtoMedKategori(kategori: Stillingskategori) = OpprettRekrutteringsbistandstillingDto(
        stilling = enOpprettStillingDto,
        kategori = kategori
    )

    val enOpprettetStilling = Stilling(
        title = "Ny stilling",
        createdBy = "pam-rekrutteringsbistand",
        updatedBy = "pam-rekrutteringsbistand",
        source = "DIR",
        privacy = "INTERNAL_NOT_SHOWN",
        administration = Administration(
            status = "PENDING",
            reportee = enVeileder.displayName,
            navIdent = enVeileder.navIdent,
            id = 0,
            comments = "",
            remarks = emptyList()
        ),
        id = 1000,
        uuid = UUID.randomUUID().toString(),
        created = LocalDateTime.now(),
        updated = LocalDateTime.now(),
        reference = UUID.randomUUID().toString(),
        published = LocalDateTime.now(),
        expires = LocalDateTime.now(),
        activationOnPublishingDate = null,
        categoryList = emptyList(),
        mediaList = emptyList(),
        locationList = emptyList(),
        contactList = emptyList(),
        businessName = null,
        deactivatedByExpiry = null,
        firstPublished = null,
        publishedByAdmin = null,
        properties = emptyMap(),
        location = null,
        status = "ACTIVE",
        employer = null,
        medium = null,
    )

    val enAnnenStilling = enStilling.copy(
        id = 1001,
        uuid = UUID.randomUUID().toString(),
        title = "En annen stilling"
    )

    val enPageMedStilling = Page(
        content = listOf(enStilling),
        totalElements = 1,
        totalPages = 1
    )

    val enStillingsinfo = Stillingsinfo(
        stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
        eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName),
        stillingsid = Stillingsid(enStilling.uuid),
        stillingskategori = null
    )

    val enStillingsinfoInboundDto = StillingsinfoInboundDto(
        stillingsid = enStilling.uuid,
        eierNavident = enVeileder.navIdent,
        eierNavn = enVeileder.displayName
    )

    val enStillingsinfoUtenEier = Stillingsinfo(
        stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
        eier = null,
        stillingsid = Stillingsid(enStilling.uuid),
        stillingskategori = null
    )

    val enAnnenStillingsinfo = Stillingsinfo(
        stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
        eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName),
        stillingsid = Stillingsid(enAnnenStilling.uuid),
        stillingskategori = null
    )

    val enStillingsinfoOppdatering = OppdaterEier(
        stillingsinfoid = enStillingsinfo.stillingsinfoid,
        eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName)
    )

    val enRekrutteringsbistandStilling = RekrutteringsbistandStilling(
        stillingsinfo = enStillingsinfo.asStillingsinfoDto(),
        stilling = enStilling
    )

    val enRekrutteringsbistandStillingUtenEier = RekrutteringsbistandStilling(
        stillingsinfo = enStillingsinfoUtenEier.asStillingsinfoDto(),
        stilling = enStilling
    )

    val publishedFor3DagerSiden = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(3)
    val publishedFor2TimerSiden = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusHours(2)


    val enDirektemeldtStilling = DirektemeldtStilling(
        stillingsid = UUID.randomUUID(),
        innhold = DirektemeldtStillingBlob(
            title = "Stilling 2",
            administration = DirektemeldtStillingAdministration(
                status = "DONE",
                comments = "",
                reportee = enVeileder.displayName,
                remarks = listOf(),
                navIdent = enVeileder.navIdent
            ),
            mediaList = listOf(),
            contactList = listOf(),
            privacy = "INTERNAL_NOT_SHOWN",
            source = "DIR",
            medium = "DIR",
            reference = UUID.randomUUID().toString(),
            published = publishedFor3DagerSiden,
            expires = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(1),
            employer = DirektemeldtStillingArbeidsgiver(
                name = "Arbeidsgiver 2",
                mediaList = listOf(),
                contactList = listOf(),
                location = null,
                locationList = listOf(),
                properties = mapOf(),
                orgnr = "123432789",
                parentOrgnr = "234567891",
                publicName = null,
                orgform = null,
                employees = null
            ),
            location = null,
            locationList = listOf(),
            categoryList = listOf(),
            properties = mapOf(),
            publishedByAdmin = publishedFor3DagerSiden.toString(),
            businessName = "Bedrift no. 2",
            firstPublished = true,
            deactivatedByExpiry = false,
            activationOnPublishingDate = null
        ),
        opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
        opprettetAv = enVeileder.navIdent,
        sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
        sistEndretAv = enVeileder.navIdent,
        status = "ACTIVE"
    )

    val stillingerSomSkalDeaktiveres = listOf(
        DirektemeldtStilling(
            stillingsid = UUID.randomUUID(),
            innhold = DirektemeldtStillingBlob(
                title = "Stilling 1",
                administration = DirektemeldtStillingAdministration(
                    status = "DONE",
                    comments = "",
                    reportee = enVeileder.displayName,
                    remarks = listOf(),
                    navIdent = enVeileder.navIdent
                ),
                mediaList = listOf(),
                contactList = listOf(),
                privacy = "INTERNAL_NOT_SHOWN",
                source = "DIR",
                medium = "DIR",
                reference =  UUID.randomUUID().toString(),
                published = publishedFor3DagerSiden,
                expires = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(1),
                employer = DirektemeldtStillingArbeidsgiver(
                    name = "Arbeidsgiver 1",
                    mediaList = listOf(),
                    contactList = listOf(),
                    location = null,
                    locationList = listOf(),
                    properties = mapOf(),
                    orgnr = "123456789",
                    parentOrgnr = "234567891",
                    publicName = null,
                    orgform = null,
                    employees = null
                ),
                location = null,
                locationList = listOf(),
                categoryList = listOf(),
                properties = mapOf(),
                publishedByAdmin = publishedFor3DagerSiden.toString(),
                businessName = "Bedriften",
                firstPublished = true,
                deactivatedByExpiry = false,
                activationOnPublishingDate = null
            ),
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = enVeileder.navIdent,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            sistEndretAv = enVeileder.navIdent,
            status = "ACTIVE"
        ),
        DirektemeldtStilling(
            stillingsid = UUID.randomUUID(),
            innhold = DirektemeldtStillingBlob(
                title = "Stilling 2",
                administration = DirektemeldtStillingAdministration(
                    status = "DONE",
                    comments = "",
                    reportee = enVeileder.displayName,
                    remarks = listOf(),
                    navIdent = enVeileder.navIdent
                ),
                mediaList = listOf(),
                contactList = listOf(),
                privacy = "INTERNAL_NOT_SHOWN",
                source = "DIR",
                medium = "DIR",
                reference = UUID.randomUUID().toString(),
                published = publishedFor3DagerSiden,
                expires = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(1),
                employer = DirektemeldtStillingArbeidsgiver(
                    name = "Arbeidsgiver 2",
                    mediaList = listOf(),
                    contactList = listOf(),
                    location = null,
                    locationList = listOf(),
                    properties = mapOf(),
                    orgnr = "123432789",
                    parentOrgnr = "234567891",
                    publicName = null,
                    orgform = null,
                    employees = null
                ),
                location = null,
                locationList = listOf(),
                categoryList = listOf(),
                properties = mapOf(),
                publishedByAdmin = publishedFor3DagerSiden.toString(),
                businessName = "Bedrift no. 2",
                firstPublished = true,
                deactivatedByExpiry = false,
                activationOnPublishingDate = null
            ),
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = enVeileder.navIdent,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            sistEndretAv = enVeileder.navIdent,
            status = "ACTIVE"
        ),
        DirektemeldtStilling(
            stillingsid = UUID.randomUUID(),
            innhold = DirektemeldtStillingBlob(
                title = "Stilling 3",
                administration = DirektemeldtStillingAdministration(
                    status = "PENDING",
                    comments = "",
                    reportee = enVeileder.displayName,
                    remarks = listOf(),
                    navIdent = enVeileder.navIdent
                ),
                mediaList = listOf(),
                contactList = listOf(),
                privacy = "INTERNAL_NOT_SHOWN",
                source = "DIR",
                medium = "DIR",
                reference = UUID.randomUUID().toString(),
                published = publishedFor3DagerSiden,
                expires = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(1),
                employer = DirektemeldtStillingArbeidsgiver(
                    name = "Arbeidsgiver 4",
                    mediaList = listOf(),
                    contactList = listOf(),
                    location = null,
                    locationList = listOf(),
                    properties = mapOf(),
                    orgnr = "1234567743",
                    parentOrgnr = "234567891",
                    publicName = null,
                    orgform = null,
                    employees = null
                ),
                location = null,
                locationList = listOf(),
                categoryList = listOf(),
                properties = mapOf(),
                publishedByAdmin = publishedFor3DagerSiden.toString(),
                businessName = "Bedrift no. 4",
                firstPublished = true,
                deactivatedByExpiry = false,
                activationOnPublishingDate = null
            ),
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = enVeileder.navIdent,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            sistEndretAv = enVeileder.navIdent,
            status = "ACTIVE"
        )
    )

    val stillingSomIkkeSkalDeaktiveres = DirektemeldtStilling(
        stillingsid = UUID.randomUUID(),
        innhold = DirektemeldtStillingBlob(
            title = "Stilling 6",
            administration = DirektemeldtStillingAdministration(
                status = "PENDING",
                comments = "",
                reportee = enVeileder.displayName,
                remarks = listOf(),
                navIdent = enVeileder.navIdent
            ),
            mediaList = listOf(),
            contactList = listOf(),
            privacy = "INTERNAL_NOT_SHOWN",
            source = "DIR",
            medium = "DIR",
            reference = UUID.randomUUID().toString(),
            published = publishedFor3DagerSiden,
            expires = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).plusDays(3),
            employer = DirektemeldtStillingArbeidsgiver(
                name = "Arbeidsgiver 4",
                mediaList = listOf(),
                contactList = listOf(),
                location = null,
                locationList = listOf(),
                properties = mapOf(),
                orgnr = "1234567743",
                parentOrgnr = "234567891",
                publicName = null,
                orgform = null,
                employees = null
            ),
            location = null,
            locationList = listOf(),
            categoryList = listOf(),
            properties = mapOf(),
            publishedByAdmin = publishedFor3DagerSiden.toString(),
            businessName = "Bedrift no. 4",
            firstPublished = true,
            deactivatedByExpiry = false,
            activationOnPublishingDate = null
        ),
        opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
        opprettetAv = enVeileder.navIdent,
        sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
        sistEndretAv = enVeileder.navIdent,
        status = "ACTIVE"
    )


    val stillingerSomSkalAktiveres = listOf(
        DirektemeldtStilling(
            stillingsid = UUID.randomUUID(),
            innhold = DirektemeldtStillingBlob(
                title = "Stilling 4",
                administration = DirektemeldtStillingAdministration(
                    status = "DONE",
                    comments = "",
                    reportee = enVeileder.displayName,
                    remarks = listOf(),
                    navIdent = enVeileder.navIdent
                ),
                mediaList = listOf(),
                contactList = listOf(),
                privacy = "INTERNAL_NOT_SHOWN",
                source = "DIR",
                medium = "DIR",
                reference =  UUID.randomUUID().toString(),
                published = publishedFor2TimerSiden,
                expires = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).plusDays(10),
                employer = DirektemeldtStillingArbeidsgiver(
                    name = "Arbeidsgiver 1",
                    mediaList = listOf(),
                    contactList = listOf(),
                    location = null,
                    locationList = listOf(),
                    properties = mapOf(),
                    orgnr = "123456789",
                    parentOrgnr = "234567891",
                    publicName = null,
                    orgform = null,
                    employees = null
                ),
                location = null,
                locationList = listOf(),
                categoryList = listOf(),
                properties = mapOf(),
                publishedByAdmin = publishedFor2TimerSiden.toString(),
                businessName = "Bedriften",
                firstPublished = true,
                deactivatedByExpiry = false,
                activationOnPublishingDate = null
            ),
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = enVeileder.navIdent,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            sistEndretAv = enVeileder.navIdent,
            status = "INACTIVE"
        ),
        DirektemeldtStilling(
            stillingsid = UUID.randomUUID(),
            innhold = DirektemeldtStillingBlob(
                title = "Stilling 12",
                administration = DirektemeldtStillingAdministration(
                    status = "DONE",
                    comments = "",
                    reportee = enVeileder.displayName,
                    remarks = listOf(),
                    navIdent = enVeileder.navIdent
                ),
                mediaList = listOf(),
                contactList = listOf(),
                privacy = "INTERNAL_NOT_SHOWN",
                source = "DIR",
                medium = "DIR",
                reference = UUID.randomUUID().toString(),
                published = publishedFor2TimerSiden,
                expires = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).plusDays(10),
                employer = DirektemeldtStillingArbeidsgiver(
                    name = "Arbeidsgiver 2",
                    mediaList = listOf(),
                    contactList = listOf(),
                    location = null,
                    locationList = listOf(),
                    properties = mapOf(),
                    orgnr = "123432789",
                    parentOrgnr = "234567891",
                    publicName = null,
                    orgform = null,
                    employees = null
                ),
                location = null,
                locationList = listOf(),
                categoryList = listOf(),
                properties = mapOf(),
                publishedByAdmin = publishedFor2TimerSiden.toString(),
                businessName = "Bedrift no. 2",
                firstPublished = true,
                deactivatedByExpiry = false,
                activationOnPublishingDate = null
            ),
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = enVeileder.navIdent,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            sistEndretAv = enVeileder.navIdent,
            status = "INACTIVE"
        )
    )

    val stillingSomIkkeSkalAktiveres = DirektemeldtStilling(
        stillingsid = UUID.randomUUID(),
        innhold = DirektemeldtStillingBlob(
            title = "Stilling 3",
            administration = DirektemeldtStillingAdministration(
                status = "PENDING",
                comments = "",
                reportee = enVeileder.displayName,
                remarks = listOf(),
                navIdent = enVeileder.navIdent
            ),
            mediaList = listOf(),
            contactList = listOf(),
            privacy = "INTERNAL_NOT_SHOWN",
            source = "DIR",
            medium = "DIR",
            reference = UUID.randomUUID().toString(),
            published = publishedFor2TimerSiden,
            expires = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).plusDays(10),
            employer = DirektemeldtStillingArbeidsgiver(
                name = "Arbeidsgiver 2",
                mediaList = listOf(),
                contactList = listOf(),
                location = null,
                locationList = listOf(),
                properties = mapOf(),
                orgnr = "123432789",
                parentOrgnr = "234567891",
                publicName = null,
                orgform = null,
                employees = null
            ),
            location = null,
            locationList = listOf(),
            categoryList = listOf(),
            properties = mapOf(),
            publishedByAdmin = publishedFor2TimerSiden.toString(),
            businessName = "Bedrift no. 2",
            firstPublished = true,
            deactivatedByExpiry = false,
            activationOnPublishingDate = null
        ),
        opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
        opprettetAv = enVeileder.navIdent,
        sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
        sistEndretAv = enVeileder.navIdent,
        status = "INACTIVE"
    )
}
