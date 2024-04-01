package cqrslite.core

import java.util.*

class MapperThrowingOnFirstUse : ExternalIdToAggregateMapper {
    override suspend fun <T : AggregateRoot> getOrCreateAggregate(
        externalId: String,
        session: Session,
        clazz: Class<T>,
    ): T {
        throw MapperNotConfigured("getOrCreateAggregate")
    }

    override suspend fun <T : AggregateRoot> mapToAggregate(
        externalId: String,
        aggregateId: UUID,
        clazz: Class<T>,
    ): Boolean {
        throw MapperNotConfigured("mapToAggregate")
    }

    override suspend fun <T : AggregateRoot> findAggregate(existingId: String, session: Session, clazz: Class<T>): T? {
        throw MapperNotConfigured("findAggregate")
    }

    class MapperNotConfigured(operation: String) : Exception(
        """
        |Trying to $operation, but the mapper has not been configured. You need to add
        | cqrs.lookup schema, mappingTable and map configuration in order to enable the mapper        
        """.trimMargin(),
    )
}
