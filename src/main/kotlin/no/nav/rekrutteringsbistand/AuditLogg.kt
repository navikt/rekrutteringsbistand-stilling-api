package no.nav.rekrutteringsbistand

import no.nav.common.audit_log.cef.AuthorizationDecision
import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.log.AuditLogger
import no.nav.common.audit_log.log.AuditLoggerImpl
import no.nav.rekrutteringsbistand.api.support.log

object AuditLogg {
    private val secureLog = SecureLog(log)
    private val auditLogger: AuditLogger = AuditLoggerImpl()

    fun loggOvertattStilling(navIdent: String, forrigeEier: String?, stillingsid: String) {
        logCefMessage(navIdent = navIdent, userid = "",
            msg = "Nav-ansatt har overtatt stilling og kandidatliste med stillingsid $stillingsid"
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
        val ekstraSpaceSidenAuditloggerInnimellomKutterSisteTegn = " "
        auditLogger.log("$message" + ekstraSpaceSidenAuditloggerInnimellomKutterSisteTegn)
        secureLog.info("auditlogger: {}", "$message" + ekstraSpaceSidenAuditloggerInnimellomKutterSisteTegn)
    }
}
