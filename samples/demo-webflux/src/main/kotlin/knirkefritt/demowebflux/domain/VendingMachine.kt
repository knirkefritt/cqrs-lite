package knirkefritt.demowebflux.domain

import cqrslite.core.AggregateRoot
import java.util.*

class VendingMachine() : AggregateRoot() {

    constructor(id: UUID) : this() {
        this.id = id
    }

    fun purchaseSoda() {
        applyChange(SodaWasPurchased())
    }
}