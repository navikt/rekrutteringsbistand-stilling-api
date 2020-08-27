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
class StillingControllerEkstern(val stillingService: StillingService) {

    @GetMapping("/rekrutteringsbistand/ekstern/api/v1/stilling/{uuid}")
    fun henkStilling(@PathVariable uuid: String, request: HttpServletRequest): ResponseEntity<Stilling> {
        val stilling = stillingService.hentStilling(uuid)

        fun copyProps(vararg keys: String): Map<String, String> =
                hashMapOf(*(keys.filter { stilling.properties.get(it) != null }.map {
                    it to (stilling.properties.get(it) ?: "")
                }.toTypedArray()))

        return ResponseEntity.ok().body(
                Stilling(
                        title = stilling.title,
                        properties = copyProps(
                                "adtext", "applicationdue", "applicationemail", "engagementtype", "jobarrangement", "extent", "workday",
                                "workhours", "positioncount", "sector", "starttime", "employerhomepage", "employerdescription",
                                "applicationurl", "jobtitle", "twitteraddress", "facebookpage", "linkedinpage"
                        ),
                        contactList = stilling.contactList.map {
                            Contact(
                                    name = it.name,
                                    email = it.email,
                                    phone = it.phone,
                                    title = it.title
                            )
                        },
                        location = stilling.location,
                        employer = stilling.employer?.let {
                            Arbeidsgiver(
                                    name = it.name,
                                    location = it.location,
                                    publicName = it.publicName
                            )
                        },
                        updated = stilling.updated,
                        medium = stilling.medium,
                        businessName = stilling.businessName,
                        status = stilling.status,
                        id = stilling.id,
                        uuid = stilling.uuid,
                        source = stilling.source
                )
        )
    }
}
