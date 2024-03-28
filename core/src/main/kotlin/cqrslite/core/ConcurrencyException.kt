package cqrslite.core

import java.util.*

class ConcurrencyException(aggregateId: UUID) : Exception(
    "Concurrency exception for aggregate $aggregateId",
)
