package cqrslite.core

import java.util.*

interface Repository {
    suspend fun <T : AggregateRoot> save(aggregate: T)

    suspend fun <T : AggregateRoot> get(aggregateId: UUID, clazz: Class<T>): T
}
