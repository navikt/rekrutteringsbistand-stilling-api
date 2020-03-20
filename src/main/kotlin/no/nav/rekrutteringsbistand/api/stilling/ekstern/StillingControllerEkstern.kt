package no.nav.rekrutteringsbistand.api.stilling.ekstern

import no.nav.rekrutteringsbistand.api.stilling.StillingService
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@Unprotected
class StillingControllerEkstern(
        val stillingService: StillingService
) {
    @GetMapping("/rekrutteringsbistand/ekstern/api/v1/stilling/{uuid}")
    fun hentStilling(@PathVariable uuid: String, request: HttpServletRequest): ResponseEntity<Stilling> {
        val s = stillingService.hentStilling(uuid)

        fun copyProps(vararg keys: String): Map<String, String> =
                hashMapOf(*keys.filter { s.properties.get(it) != null }.map {
                    it to (s.properties.get(it) ?: "")
                }.toTypedArray())

        return ResponseEntity.ok().body(
                Stilling(
                        title = s.title,
                        properties = copyProps(
                                "adtext", "applicationdue", "applicationemail", "engagementtype", "jobarrangement", "extent", "workday",
                                "workhours", "positioncount", "sector", "starttime", "employerhomepage", "employerdescription", "medium"
                        ),
                        contactList = s.contactList.map {
                            Contact(
                                    name = it.name,
                                    email = it.email,
                                    phone = it.phone,
                                    title = it.title
                            )
                        },
                        location = s.location,
                        employer = s.employer?.let {
                            Arbeidsgiver(
                                    name = it.name,
                                    location = it.location
                            )
                        },
                        updated = s.updated,
                        medium = s.medium,
                        id = s.id
                )
        )
    }
}
