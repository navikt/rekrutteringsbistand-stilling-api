package no.nav.rekrutteringsbistand.api.minestillinger

import no.nav.rekrutteringsbistand.api.stilling.Stilling
import org.springframework.stereotype.Service

@Service
class MineStillingerService(private val mineStillingerRepository: MineStillingerRepository) {

    fun lagre(stilling: Stilling, eierNavIdent: String) {
        mineStillingerRepository.lagre(MinStilling.fromStilling(stilling, eierNavIdent))
    }
}
