package no.nav.rekrutteringsbistand.api.support.config

import org.apache.catalina.Context
import org.apache.tomcat.util.http.LegacyCookieProcessor
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory

import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// Tomcat logger feilformaterte cookies by default. Dette er et sikkerhetshull.
// Her fikser vi dette ved å bruke LegacyCookieProcessor, som ikke logger slike.
// Se https://www.jvt.me/posts/2020/04/07/tomcat-cookie-disclosure/ for mer informasjon.
@Configuration
class TomcatConfig {
    @Bean
    fun cookieProcessorCustomizer(): WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
        return WebServerFactoryCustomizer { tomcatServletWebServerFactory ->
            tomcatServletWebServerFactory
                    .addContextCustomizers(TomcatContextCustomizer { context: Context -> context.cookieProcessor = LegacyCookieProcessor() })
        }
    }
}
