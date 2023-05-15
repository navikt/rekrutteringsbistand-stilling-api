package no.nav.rekrutteringsbistand.api.minestillinger

import no.nav.rekrutteringsbistand.api.stilling.Stilling
import org.springframework.stereotype.Service

@Service
class MineStillingerService(private val mineStillingerRepository: MineStillingerRepository) {

    fun opprett(stilling: Stilling, eierNavIdent: String) {
        mineStillingerRepository.opprett(MinStilling.fromStilling(stilling, eierNavIdent))
    }

    fun oppdater(stilling: Stilling, eierNavIdent: String) {
        mineStillingerRepository.oppdater(MinStilling.fromStilling(stilling, eierNavIdent))
    }

}
