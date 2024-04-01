package knirkefritt.demowebflux.domain

import cqrslite.core.ExternalIdToAggregateMapper
import cqrslite.core.SessionManager
import cqrslite.core.messaging.CommandHandler
import org.springframework.stereotype.Component

@Component
class HandlePurchaseSoda(
    private val mapper: ExternalIdToAggregateMapper,
) : CommandHandler<PurchaseSodaCommand, PurchaseSodaCommandResponse> {
    override suspend fun handle(cmd: PurchaseSodaCommand): PurchaseSodaCommandResponse {
        val session = SessionManager.session()

        val aggregate = mapper.getOrCreateAggregate(cmd.location, session, VendingMachine::class.java)
        aggregate.purchaseSoda(cmd.sodaBrand)

        session.commit()
        return PurchaseSodaCommandResponse(success = true)
    }
}