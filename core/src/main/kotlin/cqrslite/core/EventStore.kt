package cqrslite.core

import java.util.*

/**
 * The event store is a persistence abstraction, it's responsible for saving and loading events,
 * it's also responsible for handling concurrency when saving events
 */
interface EventStore {
    /**
     * Stores events in the db, throws concurrency exception if existing events are found in the database
     * with the same
     */
    suspend fun save(events: Iterable<Event>)
    suspend fun get(aggregateId: UUID, version: Int): Iterable<Event>
}
