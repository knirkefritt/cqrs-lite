package cqrslite.core

import java.util.*

class SessionImpl(private val repository: Repository) : Session {
    private val trackedAggregates: MutableMap<UUID, AggregateDescriptor> = HashMap()

    /**
     * Adds an aggregate for tracking, so that it will be persisted upon the next Commit to the database
     */
    override fun <T : AggregateRoot> add(aggregate: T) {
        if (!isTracked(aggregate.id)) {
            trackedAggregates[aggregate.id] = AggregateDescriptor(aggregate, aggregate.version)
        } else if (trackedAggregates[aggregate.id]?.aggregate != aggregate) {
            throw ConcurrencyException(aggregate.id)
        }
    }

    override suspend fun <T : AggregateRoot> get(id: UUID, expectedVersion: Int?, clazz: Class<T>): T {
        val trackedAggregate = clazz.cast(trackedAggregates[id]?.aggregate)
        if (trackedAggregate != null) {
            if (expectedVersion != null && trackedAggregate.version != expectedVersion) {
                throw ConcurrencyException(trackedAggregate.id)
            }
            return trackedAggregate
        }

        val aggregate = repository.get(id, clazz)
        if (expectedVersion != null && aggregate.version != expectedVersion) {
            throw ConcurrencyException(id)
        }
        add(aggregate)

        return aggregate
    }

    /**
     * Commits any tracked aggregate changes to the db and clears the list of tracked aggregates
     */
    override suspend fun commit() {
        val aggregates = ArrayList(trackedAggregates.values)
        trackedAggregates.clear()

        for (descriptor in aggregates) {
            repository.save(descriptor.aggregate)
        }
    }

    private fun isTracked(id: UUID): Boolean {
        return trackedAggregates.containsKey(id)
    }

    private data class AggregateDescriptor(val aggregate: AggregateRoot, val version: Int)
}
