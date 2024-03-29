package cqrslite.core.messaging.pubsub

import cqrslite.core.Event
import cqrslite.core.messaging.HandlerHub
import cqrslite.core.messaging.TypeOfMessageHandling

class NoBusEventBus(
    private val handlerHub: HandlerHub,
) : EventBus {
    override suspend fun <T : Event> publish(events: List<T>) = handlerHub.runEventHandlers(
        events,
        whichHandlers = TypeOfMessageHandling.Queue,
    )
}
