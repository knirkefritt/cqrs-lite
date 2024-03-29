package cqrslite.core.messaging.pubsub

import cqrslite.core.Event

interface EventBus {
    suspend fun <T> publish(events: List<T>) where T : Event
}
