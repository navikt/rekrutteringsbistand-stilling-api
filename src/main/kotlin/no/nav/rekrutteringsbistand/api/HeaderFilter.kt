package no.nav.rekrutteringsbistand.api

import org.springframework.web.filter.OncePerRequestFilter

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.io.IOException

class HeaderFilter : OncePerRequestFilter() {

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse,
                                  filterChain: FilterChain) {
        response.setHeader("X-Frame-Options", "sameorigin")
        response.setHeader("X-Content-Type-Options", "nosniff")
        response.setHeader("X-XSS-Protection", "1; mode=block")
        filterChain.doFilter(request, response)
    }
}
