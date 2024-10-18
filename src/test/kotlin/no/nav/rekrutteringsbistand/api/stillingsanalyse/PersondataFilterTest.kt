package no.nav.rekrutteringsbistand.api.stillingsanalyse

import org.junit.Assert.assertEquals
import org.junit.Test

class PersondataFilterTest {

    @Test
    fun `filtrerUtPersonsensitiveData skal erstatte emailadresse`() {
        val input = "Send en e-post til test@eksempel.no."
        val expected = "Send en e-post til emailadresse."
        val actual = persondataFilter.filtrerUtPersonsensitiveData(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `filtrerUtPersonsensitiveData skal fjerne lange tall`() {
        val input = "Referansenummer 12345 må oppgis."
        val expected = "Referansenummer  må oppgis."
        val actual = persondataFilter.filtrerUtPersonsensitiveData(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `filtrerUtPersonsensitiveData skal håndtere blandet sensitiv data`() {
        val input = "Kontakt oss på 12345678 eller e-post til test@eksempel.no. Nummer: 98765."
        val expected = "Kontakt oss på telefonnummer eller e-post til emailadresse. Nummer: ."
        val actual = persondataFilter.filtrerUtPersonsensitiveData(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `filtrerUtPersonsensitiveData skal ikke endre tekst uten sensitiv data`() {
        val input = "Vi ser etter en dyktig utvikler."
        val expected = "Vi ser etter en dyktig utvikler."
        val actual = persondataFilter.filtrerUtPersonsensitiveData(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `filtrerUtPersonsensitiveData skal beholde tall med mindre enn 3 sifre`() {
        val input = "Prosjekt 5 er viktig."
        val expected = "Prosjekt 5 er viktig."
        val actual = persondataFilter.filtrerUtPersonsensitiveData(input)
        assertEquals(expected, actual)
    }
}
