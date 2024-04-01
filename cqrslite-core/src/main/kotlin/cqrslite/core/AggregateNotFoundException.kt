package cqrslite.core

import java.util.*

class AggregateNotFoundException(aggregateType: Class<*>, aggregateId: UUID) : Exception(
    "Could not find $aggregateType with id $aggregateId",
)
