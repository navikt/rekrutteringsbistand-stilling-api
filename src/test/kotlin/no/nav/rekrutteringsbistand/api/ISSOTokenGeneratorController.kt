package no.nav.rekrutteringsbistand.api

import com.nimbusds.jwt.JWTClaimsSet.Builder
import net.minidev.json.JSONArray
import no.nav.security.oidc.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriUtils

import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.io.IOException
import java.nio.charset.StandardCharsets

import no.nav.security.oidc.test.support.JwtTokenGenerator.*

@RestController
@RequestMapping("/local")
class ISSOTokenGeneratorController {


    @Unprotected
    @GetMapping("/cookie-isso")
    @Throws(IOException::class)
    fun addCookie(@RequestParam(value = "subject", defaultValue = "utvikling@test.local") subject: String,
                  @RequestParam(value = "cookiename", defaultValue = "isso-localhost-idtoken") cookieName: String,
                  @RequestParam(value = "redirect", required = false) redirect: String?,
                  @RequestParam(value = "role", defaultValue = "" + NAVGruppeRoller.NSS_ADMIN, required = false) role: String,
                  request: HttpServletRequest, response: HttpServletResponse): Cookie? {

        val claimsSet = Builder(buildClaimSet(subject, ISS, AUD, ACR, EXPIRY))
                .claim("unique_name", Testbruker.CLARK.userName)
                .claim("NAVident", Testbruker.CLARK.navIdent)
                .claim("name", Testbruker.CLARK.displayName)
                .claim("groups", JSONArray().appendElement(role)).build()

        val cookie = Cookie(cookieName, createSignedJWT(claimsSet).serialize())
        cookie.domain = "localhost"
        cookie.path = "/"
        response.addCookie(cookie)
        if (redirect != null) {
            val decodedRedirectValue = UriUtils.decode(redirect, StandardCharsets.UTF_8)
            response.sendRedirect(decodedRedirectValue)
            return null
        }
        return cookie
    }

}
