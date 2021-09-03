package no.nav.rekrutteringsbistand.api.stillingsinfo

import no.nav.rekrutteringsbistand.api.option.Option
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.stilling.StillingMedStillingsinfo
import org.springframework.stereotype.Service

@Service
class StillingsinfoService(private val repository: StillingsinfoRepository) {

    fun hentStillingsinfo(stillingMedStillingsinfo: StillingMedStillingsinfo): Option<Stillingsinfo> =
        hentForStilling(Stillingsid(stillingMedStillingsinfo.uuid!!))

    fun hentStillingsinfo(stilling: Stilling): Option<Stillingsinfo> =
        hentForStilling(Stillingsid(stilling.uuid!!))

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
