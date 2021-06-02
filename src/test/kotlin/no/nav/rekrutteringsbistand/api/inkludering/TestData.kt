package no.nav.rekrutteringsbistand.api.inkludering

import no.nav.pam.stilling.ext.avro.*
import java.time.LocalDateTime
import java.util.*

val enAd = Ad(
    UUID.randomUUID().toString(),
    "annonsenr",
    "tittel",
    AdStatus.ACTIVE,
    PrivacyChannel.INTERNAL_NOT_SHOWN,
    Administration(
        AdministrationStatus.DONE,
        listOf(RemarkType.FOREIGN_JOB),
        "kommentar",
        "reportee",
        "navIdent"
    ),
    LocalDateTime.now().toString(),
    LocalDateTime.now().toString(),
    LocalDateTime.now().toString(),
    LocalDateTime.now().toString(),
    Company(
        "navn",
        "publicname",
        "orgnr",
        "parentOrgnr",
        "orgform"
    ),
    listOf(StyrkCategory("kode", "name")),
    "source",
    "medium",
    "reference",
    LocalDateTime.now().toString(),
    "businessName",
    listOf(
        Location(
            "address",
            "postalCode",
            "county",
            "municipal",
            "country",
            "latitue",
            "longitude",
            "municipal_code",
            "county_code"
        )
    ),
    listOf(
            Property("sector", "Offentlig"),
            Property("adtext", "<h1>Tittel</h2><p>Den beste stillingen <b>noen sinne</b></p>"),
            Property("searchtags", "[{\"label\":\"Sales Promotion Manager\",\"score\":1.0},{\"label\":\"Salgssjef\",\"score\":0.25137392},{\"label\":\"Sales Manager (Hotels)\",\"score\":0.21487874},{\"label\":\"Promotions Director\",\"score\":0.09032349},{\"label\":\"Salgsfremmer\",\"score\":0.09004237}]"),
            Property("tags", """["INKLUDERING", "INKLUDERING__ARBEIDSTID", "INKLUDERING__FYSISK", "INKLUDERING__ARBEIDSMILJØ", "INKLUDERING__GRUNNLEGGENDE", "TILTAK_ELLER_VIRKEMIDDEL", "TILTAK_ELLER_VIRKEMIDDEL__LØNNSTILSKUDD", "TILTAK_ELLER_VIRKEMIDDEL__MENTORTILSKUDD", "TILTAK_ELLER_VIRKEMIDDEL__LÆRLINGPLASS", "PRIORITERT_MÅLGRUPPE", "PRIORITERT_MÅLGRUPPE__UNGE_UNDER_30", "PRIORITERT_MÅLGRUPPE__SENIORER_OVER_45", "PRIORITERT_MÅLGRUPPE__KOMMER_FRA_LAND_UTENFOR_EØS", "PRIORITERT_MÅLGRUPPE__HULL_I_CV_EN", "PRIORITERT_MÅLGRUPPE__LITE_ELLER_INGEN_UTDANNING", "PRIORITERT_MÅLGRUPPE__LITE_ELLER_INGEN_ARBEIDSERFARING", "STATLIG_INKLUDERINGSDUGNAD"]""")
    )
)
