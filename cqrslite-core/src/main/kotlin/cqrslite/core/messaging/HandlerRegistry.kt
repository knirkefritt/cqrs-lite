package cqrslite.core.messaging

/**
 * Represents a registry of all the event and command handlers which will be used when executing commands
 * or handling events
 */
interface HandlerRegistry {
    var commandHandlers: List<MessageHandlerTemplate>
    var eventHandlers: List<MessageHandlerTemplate>

    interface MessageHandlerTemplate {
        val clazz: Class<*>

        fun <T> create(): T
    }
}
