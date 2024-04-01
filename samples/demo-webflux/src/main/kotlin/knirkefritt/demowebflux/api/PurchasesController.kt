package knirkefritt.demowebflux.api

import cqrslite.core.messaging.HandlerHub
import knirkefritt.demowebflux.domain.PurchaseSodaCommand
import knirkefritt.demowebflux.domain.PurchaseSodaCommandResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class PurchasesController(
    val handlerHub: HandlerHub,
) {
    @PutMapping("/purchase", consumes = ["application/json"])
    suspend fun purchase(@RequestBody body: PurchaseBody): ResponseEntity<Boolean> {
        val response = handlerHub.executeCommand(
            PurchaseSodaCommand(body.location),
            PurchaseSodaCommand::class.java,
            PurchaseSodaCommandResponse::class.java
        )

        return ResponseEntity.ok().body(true)
    }

    data class PurchaseBody(
        val location: String,
    )
}