package no.nav.rekrutteringsbistand.api.support

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("internal")
class HealtCheckController {

    @GetMapping("isAlive")
    fun isAlive(): ResponseEntity<String> = ResponseEntity.ok("{isAlive:true}")

    @GetMapping("isReady")
    fun isReady(): ResponseEntity<String> = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{isReady:false}")

}