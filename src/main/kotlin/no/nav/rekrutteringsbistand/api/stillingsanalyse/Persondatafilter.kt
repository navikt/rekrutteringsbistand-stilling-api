package no.nav.rekrutteringsbistand.api.stillingsanalyse


object persondataFilter {

    private val emailRegex = "[a-zA-Z0-9_+.%-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}".toRegex()
    private val langtNummerRegex = "\\b\\d{3,}\\b".toRegex()

    fun filtrerUtPersonsensitiveData(tekst: String): String {
        return tekst
            .let { emailRegex.replace(it, "emailadresse") }
            .let { langtNummerRegex.replace(it, "") }
    }
}