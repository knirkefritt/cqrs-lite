package cqrslite.core

import java.time.Instant
import java.util.UUID

abstract class AggregateRoot(var id: UUID = DefaultUUID) {
    companion object {
        val DefaultUUID = UUID(0, 0)
    }

    private val changes: MutableList<Event> = mutableListOf()

    var version: Int = 0
        private set

    /**
     * Inspect currently tracked changes
     */
    fun getUncommittedChanges() = changes.toList()

    /**
     * Prepare for commit: returns changes that should be persisted and clears
     * the tracked changes
     */
    internal fun flushUncommittedChanges(): Iterable<Event> {
        val snapshot = changes.toTypedArray()
        var i = 0
        for (e in snapshot) {
            if (e.id == null && id == DefaultUUID) {
                throw AggregateOrEventMissingIdException(this::class.java, e::class.java)
            }
            if (e.id == null) {
                e.id = id
            }
            if (e.id != id) {
                throw EventIdIncorrectException(e.id ?: DefaultUUID, id)
            }
            i++
            e.version = version + i
            e.timestamp = Instant.now()
        }
        version += snapshot.size
        changes.clear()
        return snapshot.asIterable()
    }

    /**
     * Loads the aggregate by applying all previous events in sequence
     */
    internal fun loadFromHistory(history: Iterable<Event>) {
        for (e in history) {
            val eventVersion = e.version
            val eventId = e.id ?: throw MissingEventIdException(e.javaClass)

            if (eventVersion != version + 1) {
                throw EventsOutOfOrderException(e.id ?: DefaultUUID)
            }
            if (eventId != id && id != DefaultUUID) {
                throw EventIdIncorrectException(eventId, id)
            }
            applyEvent(e)
            id = eventId
            version++
        }
    }

    /**
     * Applies a state change to the aggregate and tracks the change for persistence
     */
    protected fun applyChange(event: Event) {
        applyEvent(event)
        changes.add(event)
    }
    private fun applyEvent(event: Event) {
        this.invokeMethod("apply", event)
    }
}

class AggregateOrEventMissingIdException(aggregateType: Class<*>, eventType: Class<*>) : Exception(
    "The aggregate $aggregateType or event $eventType has no id (value equals default id)",
)

class EventIdIncorrectException(eventId: UUID, aggregateId: UUID) : Exception(
    "Incorrect event id: $eventId, aggregate id: $aggregateId",
)

class EventsOutOfOrderException(eventId: UUID) : Exception("Events out of order for event with id $eventId")

class MissingEventIdException(eventType: Class<*>) : Exception("Missing event id on $eventType")
