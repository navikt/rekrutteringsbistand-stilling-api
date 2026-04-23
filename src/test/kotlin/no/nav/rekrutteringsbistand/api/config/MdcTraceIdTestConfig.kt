package no.nav.rekrutteringsbistand.api.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.web.filter.OncePerRequestFilter

@TestConfiguration
class MdcTraceIdTestConfig {
    @Bean
    fun mdcTraceIdFilter(): FilterRegistrationBean<out OncePerRequestFilter?> {
        val filter = object : OncePerRequestFilter() {
            override fun doFilterInternal(
                req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain
            ) {
                MDC.put("trace_id", "test-trace-id")
                try { chain.doFilter(req, res) } finally { MDC.remove("trace_id") }
            }
        }
        return FilterRegistrationBean(filter).apply { order = Ordered.HIGHEST_PRECEDENCE }
    }
}