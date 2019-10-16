package no.nav.rekrutteringsbistand.api.innloggetbruker


import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

class InnloggetBrukerDTO @JsonCreator
constructor(@param:JsonProperty("userName") var userName: String?,
            @param:JsonProperty("displayName") var displayName: String?,
            @param:JsonProperty("navIdent") var navIdent: String?)
