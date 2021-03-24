package no.nav.rekrutteringsbistand.api.standardsøk

import org.springframework.stereotype.Service

@Service
class StandardsøkService(val standardsøkRepository: StandardsøkRepository) {
    fun oppdaterStandardsøk(lagreStandardsøkDto: LagreStandardsøkDto, navIdent: String): LagretStandardsøk? {
        standardsøkRepository.oppdaterStandardsøk(lagreStandardsøkDto, navIdent)
        return standardsøkRepository.hentStandardsøk(navIdent)
    }

    fun hentStandardsøk(navIdent: String): LagretStandardsøk? =
            standardsøkRepository.hentStandardsøk(navIdent)
}