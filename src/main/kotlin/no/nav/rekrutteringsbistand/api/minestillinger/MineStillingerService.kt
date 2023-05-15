package no.nav.rekrutteringsbistand.api.minestillinger

import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import org.springframework.stereotype.Service

@Service
class MineStillingerService(private val mineStillingerRepository: MineStillingerRepository) {

    fun lagre(rekrutteringsbistandStilling: RekrutteringsbistandStilling, eierNavIdent: String) {
        mineStillingerRepository.lagre(MinStilling.fromRekrutteringsbistandStilling(rekrutteringsbistandStilling, eierNavIdent))
    }
}