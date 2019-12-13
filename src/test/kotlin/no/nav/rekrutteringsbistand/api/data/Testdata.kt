package no.nav.rekrutteringsbistand.api.data

import no.nav.rekrutteringsbistand.api.autorisasjon.InnloggetVeileder
import no.nav.rekrutteringsbistand.api.stilling.StillingMedStillingsinfo
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import java.time.LocalDateTime
import java.util.*

object Testdata {
    val enVeileder = InnloggetVeileder("Clark.Kent@nav.no", "Clark Kent", "C12345")
    val enAnnenVeileder = InnloggetVeileder("Lex.Luthor@nav.no", "Lex Luthor", "Y123123")

    val enStilling = StillingMedStillingsinfo(
            rekruttering = null,
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

    val enAnnenStilling = enStilling.copy(
            id = 1001,
            uuid = UUID.randomUUID().toString(),
            reference = UUID.randomUUID().toString()
    )

    val enStillingsinfo = Stillingsinfo(
            stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
            eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName),
            stillingsid = Stillingsid(enStilling.uuid!!)
    )

    val enAnnenStillingsinfo = Stillingsinfo(
            stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
            eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName),
            stillingsid = Stillingsid(enAnnenStilling.uuid!!)
    )

    val enStillingsinfoOppdatering = OppdaterStillingsinfo(
            stillingsinfoid = enStillingsinfo.stillingsinfoid,
            eier = Eier(navident = enAnnenVeileder.navIdent, navn = enAnnenVeileder.displayName)
    )

    val etElasticSearchResultat = ElasticSearchResult(
            took = 52,
            timed_out = false,
            _shards = ElasticSearchResultShards(
                    total = 3,
                    successful = 3,
                    skipped = 0,
                    failed = 0
            ),
            hits = ElasticSearchHits(
                    total = ElasticSearchHitsTotal(
                            value = 2182,
                            relation = "eq"
                    ),
                    max_score = 10.240799F,
                    hits = listOf(
                            ElasticSearchHit(
                                _index = "underenhet20191204",
                                _type = "_doc",
                                _id = "914163854",
                                _score = 10.240799F,
                                _source = ElasticSearchHitSource(
                                        organisasjonsnummer = "914163854",
                                        navn = "NÆS & NÅS AS",
                                        organisasjonsform = "BEDR",
                                        antallAnsatte = 6,
                                        overordnetEnhet = "914134390",
                                        adresse = ElasticSearchHitSourceAddresse(
                                                adresse = "Klasatjønnveien 30",
                                                postnummer = "5172",
                                                poststed = "LODDEFJORD",
                                                kommunenummer = "1201",
                                                kommune = "BERGEN",
                                                landkode = "NO",
                                                land = "Norge"
                                        ),
                                        naringskoder = listOf(ElasticSearchHitSourceNæringskode(
                                                kode = "41.200",
                                                beskrivelse = "Oppføring av bygninger"
                                        ))
                                )
                        )
                    )
            )
    );
}
