package no.nav.rekrutteringsbistand.api.kandidatliste

import no.nav.rekrutteringsbistand.api.stilling.DirektemeldtStilling
import java.time.LocalDateTime
import java.util.HashMap

data class KandidatlisteStillingDto(
    val uuid: String?,
    val created: LocalDateTime?,
    val updated: LocalDateTime?,
    val title: String?,
    val status: String?,
    val source: String?,
    val employer: Arbeidsgiver?,
    val administration: Administration?,
    val properties: Map<String, String> = HashMap(),
    val publishedByAdmin: String?,
) {
    constructor(stilling: DirektemeldtStilling) : this(
        uuid = stilling.stillingsId.toString(),
        created = stilling.opprettet.toLocalDateTime(),
        updated = stilling.sistEndret.toLocalDateTime(),
        title = stilling.innhold.title,
        status = stilling.status,
        source = stilling.innhold.source,
        employer = Arbeidsgiver(
            name = stilling.innhold.employer?.name,
            orgnr = stilling.innhold.employer?.orgnr
        ),
        administration = Administration(
            status = stilling.adminStatus,
            reportee = stilling.innhold.administration?.reportee,
            navIdent = stilling.innhold.administration?.navIdent
        ),
        properties = stilling.innhold.properties,
        publishedByAdmin = stilling.publisertAvAdmin
    )
}

data class Administration(
    val status: String?,
    val reportee: String?,
    val navIdent: String?,
)

data class Arbeidsgiver(
    val name: String?,
    val orgnr: String?,
)
