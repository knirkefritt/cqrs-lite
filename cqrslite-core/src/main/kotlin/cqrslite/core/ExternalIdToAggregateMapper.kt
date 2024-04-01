package cqrslite.core

import java.util.*

interface ExternalIdToAggregateMapper {
    suspend fun <T : AggregateRoot> getOrCreateAggregate(externalId: String, session: Session, clazz: Class<T>): T

    suspend fun <T : AggregateRoot> mapToAggregate(externalId: String, aggregateId: UUID, clazz: Class<T>): Boolean

    suspend fun <T : AggregateRoot> findAggregate(existingId: String, session: Session, clazz: Class<T>): T?
}
