package cqrslite.core

import kotlin.coroutines.CoroutineContext

class OutboxDisabled : Outbox {
    override suspend fun save(events: Iterable<Event>) {
    }

    override suspend fun publish(context: suspend() -> CoroutineContext) {
    }
}
