package no.nav.rekrutteringsbistand.api.stillingsanalyse

import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingskategori
import no.nav.rekrutteringsbistand.api.support.log
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpClientErrorException

@RestController
@Protected
@RequestMapping("/rekrutteringsbistand/stillingsanalyse")
class StillingsanalyseController(
    private val openAiClient: OpenAiClient,
    private val tokenUtils: TokenUtils,
) {

    @PostMapping
    fun analyserStilling(
        @RequestBody stillingsanalyseDto: StillingsanalyseDto
    ): ResponseEntity<StillingsanalyseResponsDto> {
        tokenUtils.hentInnloggetVeileder().validerMinstEnAvRollene()

        if (stillingsanalyseDto.source != "DIR") {
            log.error("Mottatt stilling for analyse som ikke er source=DIR, source=${stillingsanalyseDto.source}")
            throw HttpClientErrorException.create(
                "Kan kun analysere stillinger med source DIR",
                HttpStatus.BAD_REQUEST,
                "Bad request",
                HttpHeaders(),
                byteArrayOf(),
                null
            )
        }

        val filtrertDto = stillingsanalyseDto.copy(
            stillingstittel = PersondataFilter.filtrerUtPersonsensitiveData(stillingsanalyseDto.stillingstittel),
            stillingstekst = PersondataFilter.filtrerUtPersonsensitiveData(stillingsanalyseDto.stillingstekst)
        )

        val response = openAiClient.analyserStilling(filtrertDto)
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
        val source: String
    )
}
