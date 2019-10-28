package no.nav.rekrutteringsbistand.api.requester.support

import no.nav.rekrutteringsbistand.api.withAddedHeaders
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class HeaderFilter : OncePerRequestFilter() {

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(request: HttpServletRequest,
                                  response: HttpServletResponse,
                                  filterChain: FilterChain) =
            filterChain.doFilter(
                    request,
                    response.withAddedHeaders(
                            mapOf(
                                    "X-Frame-Options" to "sameorigin",
                                    "X-Content-Type-Options" to "nosniff",
                                    "X-XSS-Protection" to "1; mode=block")))
}

