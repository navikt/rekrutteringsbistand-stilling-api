package no.nav.rekrutteringsbistand.api

import no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettStillingAdministrationDto
import no.nav.rekrutteringsbistand.api.autorisasjon.InnloggetVeileder
import no.nav.rekrutteringsbistand.api.autorisasjon.Rolle
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
        created = LocalDateTime.now().withNano(0),
        createdBy = "nss-admin",
        updated = LocalDateTime.now().withNano(0),
        updatedBy = "nss-admin",
        title = "testnss",
        status = "ACTIVE",
        privacy = "SHOW_ALL",
        source = "ASS",
        medium = "ASS",
        reference = UUID.randomUUID().toString(),
        published = LocalDateTime.now().withNano(0),
        expires = LocalDateTime.now().withNano(0),
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
        kategori = kategori,
        eierNavn = "Clark Kent",
        eierNavident = "C12345",
        eierNavKontorEnhetId = "1234",
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
        stillingsid = Stillingsid(enStilling.uuid),
        eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName, navKontorEnhetId = "1234"),
        stillingskategori = null,
    )

    val enStillingsinfoInboundDto = StillingsinfoInboundDto(
        stillingsid = enStilling.uuid,
        eierNavident = enVeileder.navIdent,
        eierNavn = enVeileder.displayName,
        eierNavKontorEnhetId = "1234",
    )

    val enStillingsinfoUtenEier = Stillingsinfo(
        stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
        stillingsid = Stillingsid(enStilling.uuid),
        eier = null,
        stillingskategori = null,
    )

    val enAnnenStillingsinfo = Stillingsinfo(
        stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
        stillingsid = Stillingsid(enAnnenStilling.uuid),
        eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName, navKontorEnhetId = "1234"),
        stillingskategori = null,
    )

    val enStillingsinfoOppdatering = OppdaterEier(
        stillingsinfoid = enStillingsinfo.stillingsinfoid,
        eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName, navKontorEnhetId = "1234"),
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
        stillingsId = UUID.randomUUID(),
        innhold = DirektemeldtStillingInnhold(
            title = "Stilling 2",
            administration = DirektemeldtStillingAdministration(
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
            businessName = "Bedrift no. 2",
            firstPublished = true,
            deactivatedByExpiry = false,
            activationOnPublishingDate = null
        ),
        opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
        opprettetAv = enVeileder.navIdent,
        sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
        sistEndretAv = enVeileder.navIdent,
        status = "ACTIVE",
        annonsenr = "1",
        utløpsdato = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(1),
        publisert = publishedFor3DagerSiden,
        publisertAvAdmin = publishedFor3DagerSiden.toString(),
        adminStatus = "DONE",
    )

    val stillingerSomSkalDeaktiveres = listOf(
        DirektemeldtStilling(
            stillingsId = UUID.randomUUID(),
            innhold = DirektemeldtStillingInnhold(
                title = "Stilling 1",
                administration = DirektemeldtStillingAdministration(
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
                businessName = "Bedriften",
                firstPublished = true,
                deactivatedByExpiry = false,
                activationOnPublishingDate = null
            ),
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = enVeileder.navIdent,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            sistEndretAv = enVeileder.navIdent,
            status = "ACTIVE",
            annonsenr = "2",
            utløpsdato = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(1),
            publisert = publishedFor3DagerSiden,
            publisertAvAdmin = publishedFor3DagerSiden.toString(),
            adminStatus = "DONE"
        ),
        DirektemeldtStilling(
            stillingsId = UUID.randomUUID(),
            innhold = DirektemeldtStillingInnhold(
                title = "Stilling 2",
                administration = DirektemeldtStillingAdministration(
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
                businessName = "Bedrift no. 2",
                firstPublished = true,
                deactivatedByExpiry = false,
                activationOnPublishingDate = null
            ),
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = enVeileder.navIdent,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            sistEndretAv = enVeileder.navIdent,
            status = "ACTIVE",
            annonsenr = "3",
            utløpsdato = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(1),
            publisert = publishedFor3DagerSiden,
            publisertAvAdmin = publishedFor3DagerSiden.toString(),
            adminStatus = "DONE"
        ),
        DirektemeldtStilling(
            stillingsId = UUID.randomUUID(),
            innhold = DirektemeldtStillingInnhold(
                title = "Stilling 3",
                administration = DirektemeldtStillingAdministration(
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
                businessName = "Bedrift no. 4",
                firstPublished = true,
                deactivatedByExpiry = false,
                activationOnPublishingDate = null
            ),
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = enVeileder.navIdent,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            sistEndretAv = enVeileder.navIdent,
            status = "ACTIVE",
            annonsenr = "4",
            utløpsdato = publishedFor3DagerSiden,
            publisert = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(1),
            publisertAvAdmin = publishedFor3DagerSiden.toString(),
            adminStatus = "PENDING"
        )
    )

    val stillingSomIkkeSkalDeaktiveres = DirektemeldtStilling(
        stillingsId = UUID.randomUUID(),
        innhold = DirektemeldtStillingInnhold(
            title = "Stilling 6",
            administration = DirektemeldtStillingAdministration(
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
            businessName = "Bedrift no. 4",
            firstPublished = true,
            deactivatedByExpiry = false,
            activationOnPublishingDate = null
        ),
        opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
        opprettetAv = enVeileder.navIdent,
        sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
        sistEndretAv = enVeileder.navIdent,
        status = "ACTIVE",
        annonsenr = "5",
        utløpsdato = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).plusDays(3),
        publisert = publishedFor3DagerSiden,
        publisertAvAdmin = publishedFor3DagerSiden.toString(),
        adminStatus = "PENDING"
    )


    val stillingerSomSkalAktiveres = listOf(
        DirektemeldtStilling(
            stillingsId = UUID.randomUUID(),
            innhold = DirektemeldtStillingInnhold(
                title = "Stilling 4",
                administration = DirektemeldtStillingAdministration(
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
                businessName = "Bedriften",
                firstPublished = true,
                deactivatedByExpiry = false,
                activationOnPublishingDate = null
            ),
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = enVeileder.navIdent,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            sistEndretAv = enVeileder.navIdent,
            status = "INACTIVE",
            annonsenr = "6",
            utløpsdato = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).plusDays(10),
            publisert = publishedFor2TimerSiden,
            publisertAvAdmin = publishedFor2TimerSiden.toString(),
            adminStatus = "DONE"
        ),
        DirektemeldtStilling(
            stillingsId = UUID.randomUUID(),
            innhold = DirektemeldtStillingInnhold(
                title = "Stilling 12",
                administration = DirektemeldtStillingAdministration(
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
                businessName = "Bedrift no. 2",
                firstPublished = true,
                deactivatedByExpiry = false,
                activationOnPublishingDate = null
            ),
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = enVeileder.navIdent,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            sistEndretAv = enVeileder.navIdent,
            status = "INACTIVE",
            annonsenr = "7",
            utløpsdato = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).plusDays(10),
            publisert = publishedFor2TimerSiden,
            publisertAvAdmin = publishedFor2TimerSiden.toString(),
            adminStatus = "DONE"
        )
    )

    val stillingSomIkkeSkalAktiveres = DirektemeldtStilling(
        stillingsId = UUID.randomUUID(),
        innhold = DirektemeldtStillingInnhold(
            title = "Stilling 3",
            administration = DirektemeldtStillingAdministration(
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
            businessName = "Bedrift no. 2",
            firstPublished = true,
            deactivatedByExpiry = false,
            activationOnPublishingDate = null
        ),
        opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
        opprettetAv = enVeileder.navIdent,
        sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
        sistEndretAv = enVeileder.navIdent,
        status = "INACTIVE",
        annonsenr = "8",
        utløpsdato = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).plusDays(10),
        publisert = publishedFor2TimerSiden,
        publisertAvAdmin = publishedFor2TimerSiden.toString(),
        adminStatus = "PENDING"
    )

    val esResponse = """ 
        {
        	"_index": "stilling_5",
        	"_type": "_doc",
        	"_id": "4f7417d0-8678-4b75-9536-ec94cc4aa5bf",
        	"_version": 3,
        	"_seq_no": 294384,
        	"_primary_term": 1,
        	"found": true,
        	"_source": {
        		"stilling": {
        			"uuid": "4f7417d0-8678-4b75-9536-ec94cc4aa5bf",
        			"annonsenr": "958754",
        			"status": "INACTIVE",
        			"privacy": "INTERNAL_NOT_SHOWN",
        			"published": "2024-10-23T01:00:00",
        			"publishedByAdmin": "2024-09-29T22:33:19.397287",
        			"expires": "2024-10-23T01:00:00",
        			"created": "2024-09-29T12:53:26.419561",
        			"updated": "2024-10-24T00:00:00.554003",
        			"employer": {
        				"name": "ORDKNAPP BLOMSTRETE TIGER AS",
        				"publicName": "ORDKNAPP BLOMSTRETE TIGER AS",
        				"orgnr": "312113341",
        				"parentOrgnr": "311185268",
        				"orgform": "BEDR"
        			},
        			"categories": [],
        			"source": "DIR",
        			"medium": "DIR",
        			"businessName": "ORDKNAPP BLOMSTRETE TIGER AS",
        			"locations": [
        				{
        					"address": null,
        					"postalCode": null,
        					"city": null,
        					"county": "TRØNDELAG",
        					"countyCode": "50",
        					"municipal": "LEKA",
        					"municipalCode": "5052",
        					"latitue": null,
        					"longitude": null,
        					"country": "NORGE"
        				}
        			],
        			"reference": "4f7417d0-8678-4b75-9536-ec94cc4aa5bf",
        			"administration": {
        				"status": "DONE",
        				"remarks": [],
        				"comments": "",
        				"reportee": "F_Z993141 E_Z993141",
        				"navIdent": "Z993141"
        			},
        			"properties": {
        				"extent": "Heltid",
        				"employerhomepage": "https://nettsted",
        				"workhours": [
					        "Dagtid",
					        "Kveld"
				        ],
        				"applicationdue": "Snarest",
        				"workday": [
        					"Ukedager"
        				],
                        "keywords": null,
        				"jobtitle": "Fylkesbarnevernsjef",
        				"positioncount": 1,
        				"engagementtype": "Fast",
        				"classification_styrk08_score": 0.9506593453694958,
        				"employerdescription": "<p>om bedriften</p>",
        				"jobarrangement": "Skift",
        				"adtext": "<p>tekst</p>",
        				"classification_styrk08_code": 1341,
        				"searchtags": [
        					{
        						"label": "Fylkesbarnevernsjef",
        						"score": 1
        					}
        				],
        				"classification_esco_code": "http://data.europa.eu/esco/isco/c1341",
        				"classification_input_source": "jobtitle",
        				"sector": "Privat"
        			},
        			"contacts": [
        				{
        					"name": "Test",
        					"role": "",
        					"title": "test",
        					"email": "test@test.test",
        					"phone": "222222222"
        				}
        			],
        			"tittel": "Fylkesbarnevernsjef"
        		},
        		"stillingsinfo": {
        			"eierNavident": null,
        			"eierNavn": null,
        			"notat": null,
        			"stillingsid": "4f7417d0-8678-4b75-9536-ec94cc4aa5bf",
        			"stillingsinfoid": "4f877d02-c527-4630-9156-ba75ee8856db",
        			"stillingskategori": "STILLING"
        		}
        	}
        }
    """.trimIndent()
}
