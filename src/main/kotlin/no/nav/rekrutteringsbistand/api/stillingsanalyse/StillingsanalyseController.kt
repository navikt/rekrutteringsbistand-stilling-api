package no.nav.rekrutteringsbistand.api.stillingsanalyse

import no.nav.rekrutteringsbistand.api.autorisasjon.Rolle
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingskategori
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Protected
@RequestMapping("/rekrutteringsbistand/stillingsanalyse")
class StillingsanalyseController(
    private val openAiClient: OpenAiClient,
    private val tokenUtils: TokenUtils
) {

    @PostMapping
    fun analyserStilling(
        @RequestBody stillingsanalyseDto: StillingsanalyseDto
    ): ResponseEntity<StillingsanalyseResponsDto> {
        tokenUtils.hentInnloggetVeileder().validerMinstEnAvRollene()

        val prompt = StillingsanalyseTemplate.lagPrompt(stillingsanalyseDto)
        val response = openAiClient.analyserStilling(prompt, stillingsanalyseDto.stillingsId)
        return ResponseEntity.ok(response)
    }

    data class StillingsanalyseResponsDto(
        val sensitiv: Boolean,
        val sensitivBegrunnelse: String,
        val samsvarMedTittel: Boolean,
        val tittelBegrunnelse: String,
        val samsvarMedType: Boolean,
        val typeBegrunnelse: String

    )

    data class StillingsanalyseDto(
        val stillingsId: String,
        val stillingstype: Stillingskategori,
        val stillingstittel: String,
        val stillingstekst: String,
    )
}
