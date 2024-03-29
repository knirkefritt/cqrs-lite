package cqrslite.core

interface Outbox {

    suspend fun save(events: Iterable<Event>)

    suspend fun publish()
}
