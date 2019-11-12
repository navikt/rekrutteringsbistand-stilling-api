package no.nav.rekrutteringsbistand.api.rekrutteringsbistand

import org.springframework.stereotype.Service

@Service
class RekrutteringsbistandService(val repository: RekrutteringsbistandRepository) {
    fun hentForStilling(uuid: String): Rekrutteringsbistand = repository.hentForStilling(uuid)
}
