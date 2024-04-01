package cqrslite.core

class OutboxDisabled : Outbox {
    override suspend fun save(events: Iterable<Event>) {
    }

    override suspend fun publish() {
    }
}
