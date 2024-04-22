package no.nav.rekrutteringsbistand

import no.nav.common.audit_log.cef.AuthorizationDecision
import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.log.AuditLogger
import no.nav.common.audit_log.log.AuditLoggerImpl
import org.slf4j.LoggerFactory


object AuditLogg {
    private val secureLog = LoggerFactory.getLogger("secureLog")!!
    private val auditLogger: AuditLogger = AuditLoggerImpl()

    fun loggOvertattStilling(navIdent: String, forrigeEier: String?, stillingsid: String) {
        logCefMessage(navIdent = navIdent, userid = "",
            msg = "NAV-ansatt har overtatt stilling og kandidatliste med stillingsid $stillingsid"
                    + if(forrigeEier!=null) " fra $forrigeEier" else "")
    }

    private fun logCefMessage(
        navIdent: String,
        userid: String,
        msg: String,
        authorizationDecision: AuthorizationDecision = AuthorizationDecision.PERMIT
    ) {
        val message = CefMessage.builder()
            .applicationName("Rekrutteringsbistand")
            .loggerName("rekrutteringsbistand-stilling-api")
            .event(CefMessageEvent.ACCESS)
            .name("Sporingslogg")
            .authorizationDecision(authorizationDecision)
            .sourceUserId(navIdent)
            .destinationUserId(userid)
            .timeEnded(System.currentTimeMillis())
            .extension("msg", msg)
            .build()
        auditLogger.log(message)
        secureLog.info("auditlogger: {}", message)
    }
}
