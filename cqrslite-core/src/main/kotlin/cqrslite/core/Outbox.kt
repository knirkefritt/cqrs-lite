package cqrslite.core

import kotlin.coroutines.CoroutineContext

interface Outbox {

    suspend fun save(events: Iterable<Event>)

    suspend fun publish(context: suspend() -> CoroutineContext)
}
