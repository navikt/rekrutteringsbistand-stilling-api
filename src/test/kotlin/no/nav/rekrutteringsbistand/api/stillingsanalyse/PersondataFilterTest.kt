package no.nav.rekrutteringsbistand.api.stillingsanalyse

import org.junit.Assert.assertEquals
import org.junit.Test

class PersondataFilterTest {

    @Test
    fun `filtrerUtPersonsensitiveData skal erstatte e-postadresse`() {
        val input = "Send en e-post til test@eksempel.no."
        val expected = "Send en e-post til emailadresse."
        val actual = PersondataFilter.filtrerUtPersonsensitiveData(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `filtrerUtPersonsensitiveData skal fjerne lange tall uten separator`() {
        val input = "Referansenummer 12345 må oppgis."
        val expected = "Referansenummer  må oppgis."
        val actual = PersondataFilter.filtrerUtPersonsensitiveData(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `filtrerUtPersonsensitiveData skal fjerne tall med 3 eller flere sifre uten separator`() {
        val input = "Fødselsnummer: 8888888."
        val expected = "Fødselsnummer: ."
        val actual = PersondataFilter.filtrerUtPersonsensitiveData(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `filtrerUtPersonsensitiveData skal fjerne tall med 3 eller flere sifre med bindestrek`() {
        val input = "Kontakt oss på 8-8-8-8-8-8-8-8."
        val expected = "Kontakt oss på ."
        val actual = PersondataFilter.filtrerUtPersonsensitiveData(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `filtrerUtPersonsensitiveData skal fjerne tall med 3 eller flere sifre med mellomrom`() {
        val input = "Prosjektkode: 8 8 8 8 8 8 8 8."
        val expected = "Prosjektkode: ."
        val actual = PersondataFilter.filtrerUtPersonsensitiveData(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `filtrerUtPersonsensitiveData skal fjerne kombinasjoner av tall med 3 eller flere sifre og bindestrek`() {
        val input = "Referansenummer: 888-88888."
        val expected = "Referansenummer: ."
        val actual = PersondataFilter.filtrerUtPersonsensitiveData(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `filtrerUtPersonsensitiveData skal fjerne tall med 3 eller flere sifre hvis separert med ett tegn`() {
        val input = "Nummeret er 12-345 og 6789."
        val expected = "Nummeret er  og ."
        val actual = PersondataFilter.filtrerUtPersonsensitiveData(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `filtrerUtPersonsensitiveData skal fjerne tall med 3 eller flere sifre ved flere separatorer`() {
        val input = "Telefon: 8-8-8-8-8-8-8-8"
        val expected = "Telefon: "
        val actual = PersondataFilter.filtrerUtPersonsensitiveData(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `filtrerUtPersonsensitiveData skal ikke fjerne tall med mindre enn 3 sifre`() {
        val input = "Prosjekt 5 er viktig."
        val expected = "Prosjekt 5 er viktig."
        val actual = PersondataFilter.filtrerUtPersonsensitiveData(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `filtrerUtPersonsensitiveData skal fjerne tall uten spesifikk pattern`() {
        val input = "ID: 1234567."
        val expected = "ID: ."
        val actual = PersondataFilter.filtrerUtPersonsensitiveData(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `filtrerUtPersonsensitiveData skal fjerne tall i kombinasjon med bokstav`() {
        val input = "ID: 1234567A"
        val expected = "ID: A"
        val actual = PersondataFilter.filtrerUtPersonsensitiveData(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `filtrerUtPersonsensitiveData skal fjerne tall i parentes`() {
        val input = "Kontakt oss på (+37)88888888."
        val expected = "Kontakt oss på (+37)."
        val actual = PersondataFilter.filtrerUtPersonsensitiveData(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `filtrerUtPersonsensitiveData skal fjerne vanlig norsk telefonnummer`() {
        val input = "Telefonnummeret er 99999999."
        val expected = "Telefonnummeret er ."
        val actual = PersondataFilter.filtrerUtPersonsensitiveData(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `filtrerUtPersonsensitiveData skal fjerne tenkt fødselsnummer`() {
        val input = "Fødselsnummer: 01010111111."
        val expected = "Fødselsnummer: ."
        val actual = PersondataFilter.filtrerUtPersonsensitiveData(input)
        assertEquals(expected, actual)
    }
}
