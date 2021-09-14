package no.nav.rekrutteringsbistand.api.stillingsinfo

import no.nav.rekrutteringsbistand.api.option.Option
import no.nav.rekrutteringsbistand.api.option.Some
import no.nav.rekrutteringsbistand.api.option.get
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import org.springframework.stereotype.Service

@Service
class StillingsinfoService(private val repository: StillingsinfoRepository) {
    fun endreEier(dto: StillingsinfoInboundDto): Stillingsinfo {
        val eksisterendeStillingsinfo = repository.hentForStilling(Stillingsid(dto.stillingsid))

        return if (eksisterendeStillingsinfo is Some) {
            val stillingsinfo = eksisterendeStillingsinfo.get()

            dto.tilOppdatertStillingsinfo(
                stillingsinfo.stillingsinfoid.asString(),
                stillingsinfo.notat
            ).apply {
                repository.oppdaterEierIdentOgEierNavn(
                    OppdaterEier(this.stillingsinfoid, Eier(dto.eierNavident, dto.eierNavn)),
                )
            }
        } else {
            dto.tilOpprettetStillingsinfo().apply {
                repository.opprett(this)
            }
        }
    }

    fun hentStillingsinfo(stilling: Stilling): Option<Stillingsinfo> =
        hentForStilling(Stillingsid(stilling.uuid))

    fun hentForStilling(stillingId: Stillingsid): Option<Stillingsinfo> =
        repository.hentForStilling(stillingId)

    fun hentForStillinger(stillingIder: List<Stillingsid>): List<Stillingsinfo> =
        repository.hentForStillinger(stillingIder)

    fun oppdaterNotat(stillingId: Stillingsid, oppdaterNotat: OppdaterNotat) {
        repository.oppdaterNotat(oppdaterNotat)
    }

    fun lagre(stillingsinfo: Stillingsinfo) {
        repository.opprett(stillingsinfo)
    }

}
