package knirkefritt.demowebflux.domain

import cqrslite.core.Event
import java.time.Instant
import java.util.*

class SodaWasPurchased(
    override var id: UUID? = null,
    override var timestamp: Instant? = null,
    override var version: Int? = null,
) : Event
