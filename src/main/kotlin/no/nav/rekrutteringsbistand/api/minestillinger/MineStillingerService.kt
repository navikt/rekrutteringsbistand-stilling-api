package no.nav.rekrutteringsbistand.api.minestillinger

import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class MineStillingerService(private val mineStillingerRepository: MineStillingerRepository) {

    fun opprett(stilling: Stilling, eierNavIdent: String) {
        mineStillingerRepository.opprett(MinStilling.fromStilling(stilling, eierNavIdent))
    }

    fun oppdater(stilling: Stilling, eierNavIdent: String) {
        mineStillingerRepository.oppdater(MinStilling.fromStilling(stilling, eierNavIdent))
    }

    fun overtaEierskap(stilling: Stilling, navident: String) {
        val minStilling = MinStilling.fromStilling(stilling, navident)
        val finnesFraFør = mineStillingerRepository.hentForStillingsId(Stillingsid(stilling.uuid)) != null

        if (finnesFraFør) {
            mineStillingerRepository.oppdater(minStilling)
        } else {
            mineStillingerRepository.opprett(minStilling)
        }
    }

    fun behandleMeldingForEksternStilling(
        stillingsId: Stillingsid,
        tittel: String,
        status: String,
        utløpsdato: ZonedDateTime,
        sistEndret: ZonedDateTime,
        arbeidsgiverNavn: String
    ) {
        val minStilling = mineStillingerRepository.hentForStillingsId(stillingsId)

        if (minStilling != null) {
            mineStillingerRepository.oppdater(
                minStilling.copy(
                    tittel = tittel,
                    status = status,
                    utløpsdato = utløpsdato,
                    sistEndret = sistEndret,
                    arbeidsgiverNavn = arbeidsgiverNavn
                )
            )
        }
    }

    fun slett(stillingsId: Stillingsid) {
        mineStillingerRepository.slett(stillingsId)
    }
}
