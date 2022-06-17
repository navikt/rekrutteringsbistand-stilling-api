package no.nav.rekrutteringsbistand.api.stillingsinfo

import no.nav.rekrutteringsbistand.api.Testdata.enStillingsinfo
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.option.optionOf
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.*
import java.util.*

class StillingsinfoServiceTest {
    private val repository = mock(StillingsinfoRepository::class.java)
    private val kandidatlisteKlient = mock(KandidatlisteKlient::class.java)
    private val arbeidsplassenKlient = mock(ArbeidsplassenKlient::class.java)
    private val stillingsinfoService = StillingsinfoService(repository, kandidatlisteKlient, arbeidsplassenKlient)

    @Test
    fun `Oppretting av kandidatliste på ekstern stilling skal lagre stillingsinfo for så å reversere lagring når kall mot kandidat-api feiler`() {
        `when`(repository.hentForStilling(anyObject(Stillingsid::class.java))).thenReturn(optionOf(null))
        `when`(kandidatlisteKlient.sendStillingOppdatert(anyObject(Stillingsid::class.java))).thenThrow(RuntimeException::class.java)

        assertThrows<RuntimeException> {
            stillingsinfoService.overtaEierskapForEksternStillingOgKandidatliste(Stillingsid(UUID.randomUUID()), Eier("DummyIdent", "DummyNavn"))
        }

        verify(repository, times(1)).upsert(anyObject(Stillingsinfo::class.java))
        verify(repository, times(1)).slett(anyObject(Stillingsid::class.java))
    }

    @Test
    fun `Oppretting av kandidatliste på ekstern stilling skal lagre endret stillingsinfo for så å reversere endringa når kall mot kandidat-api feiler`() {
        val eksisterendeStillingsinfo = enStillingsinfo
        `when`(repository.hentForStilling(eksisterendeStillingsinfo.stillingsid)).thenReturn(optionOf(enStillingsinfo))
        `when`(kandidatlisteKlient.sendStillingOppdatert(anyObject(Stillingsid::class.java))).thenThrow(RuntimeException::class.java)
        val nyEier = Eier("DummyIdent", "DummyNavn")

        assertThrows<RuntimeException> {
            stillingsinfoService.overtaEierskapForEksternStillingOgKandidatliste(eksisterendeStillingsinfo.stillingsid, nyEier)
        }

        verify(repository, times(1)).upsert(eksisterendeStillingsinfo)
        val stillingsnifoMedNyEier = enStillingsinfo.copy(eier = nyEier)
        verify(repository, times(1)).upsert(stillingsnifoMedNyEier)
    }

    private fun <T> anyObject(type: Class<T>): T = Mockito.any<T>(type)
}
