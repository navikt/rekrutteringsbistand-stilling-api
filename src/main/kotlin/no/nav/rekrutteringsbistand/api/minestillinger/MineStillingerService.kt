package no.nav.rekrutteringsbistand.api.minestillinger

import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import org.springframework.stereotype.Service

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
}
