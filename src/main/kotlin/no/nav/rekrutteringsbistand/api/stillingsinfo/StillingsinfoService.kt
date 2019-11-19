package no.nav.rekrutteringsbistand.api.stillingsinfo

import arrow.core.Option
import org.springframework.stereotype.Service

@Service
class StillingsinfoService(private val repository: StillingsinfoRepository) {
    fun hentForStilling(stillingId: Stillingsid): Option<Stillingsinfo> =
            repository.hentForStilling(stillingId)
}
