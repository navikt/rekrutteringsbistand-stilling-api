package no.nav.rekrutteringsbistand.api.rekrutteringsbistand

import arrow.core.Option
import org.springframework.stereotype.Service

@Service
class RekrutteringsbistandService(private val repository: RekrutteringsbistandRepository) {
    fun hentForStilling(stillingId: StillingId): Option<Rekrutteringsbistand> =
            repository.hentForStilling(stillingId)
}
