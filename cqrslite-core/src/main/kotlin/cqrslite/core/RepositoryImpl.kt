package cqrslite.core

import java.lang.reflect.Constructor
import java.util.*

class RepositoryImpl(private val eventStore: EventStore) : Repository {

    override suspend fun <T : AggregateRoot> save(aggregate: T) {
        val changes = aggregate.flushUncommittedChanges()
        if (!changes.any()) {
            return
        }

        eventStore.save(changes)
    }

    override suspend fun <T : AggregateRoot> get(aggregateId: UUID, clazz: Class<T>): T {
        return loadAggregate(aggregateId, clazz)
    }

    private suspend fun <T : AggregateRoot> loadAggregate(id: UUID, clazz: Class<T>): T {
        val events = eventStore.get(id, -1)
        if (!events.any()) {
            throw AggregateNotFoundException(clazz, id)
        }

        val aggregate = AggregateFactory.createAggregate(clazz)
        AggregateRoot.loadFromHistory(aggregate, events)
        return aggregate
    }

    class AggregateFactory {
        companion object {
            private val cache = mutableMapOf<Class<*>, Constructor<*>>()
            fun <T : AggregateRoot> createAggregate(clazz: Class<T>): T {
                try {
                    @Suppress("UNCHECKED_CAST")
                    cache[clazz]?.let { return it.newInstance() as T }
                    val ctor = clazz.getDeclaredConstructor()
                    cache[clazz] = ctor
                    return ctor.newInstance()
                } catch (e: Exception) {
                    throw MissingParameterLessConstructorException(clazz)
                }
            }
        }
    }

    class MissingParameterLessConstructorException(clazz: Class<*>) : Exception(
        "Class $clazz is missing a parameterless constructor",
    )
}
