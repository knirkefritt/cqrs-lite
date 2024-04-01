package knirkefritt.demowebflux.readmodels.listOfPurchases

import knirkefritt.demowebflux.domain.SodaWasPurchased
import cqrslite.core.messaging.EventHandler
import cqrslite.core.messaging.InProcess
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.javatime.timestamp
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("update-list-of-purchases")
@Scope("prototype")
@InProcess
class HandleSodaWasPurchased: EventHandler<SodaWasPurchased> {
    override suspend fun handle(event: SodaWasPurchased) {
        ListOfPurchases.insertIgnore {
            it[aggregateId] = event.id!!
            it[version] = event.version!!
            it[timeOfPurchase] = event.timestamp!!
            it[sodaBrand] = event.sodaBrand
        }
    }
}

object ListOfPurchases : Table("read.list_of_purchases") {
    val aggregateId = uuid("aggregate_id")
    val version = integer("version")
    val timeOfPurchase = timestamp("time_of_purchase")
    val sodaBrand = text("soda_brand")
}