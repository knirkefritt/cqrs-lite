package cqrslite.core.messaging

interface HandlerHub {
    /**
     * Finds the correct command handler based on the specified [tClass] and return type [rClass]
     * @param command the command to execute
     * @param tClass command type
     * @param rClass return type
     */
    suspend fun <T, R> executeCommand(command: T, tClass: Class<T>, rClass: Class<R>): R

    /**
     * Finds the correct event handlers based on the specified [eClass]
     * @param event the event to handle
     * @param eClass event type
     * @param whichHandlers the type of message handlers we are looking for
     */
    suspend fun <T> runEventHandlers(
        event: T,
        eClass: Class<T>,
        whichHandlers: TypeOfMessageHandling = TypeOfMessageHandling.Default,
    )

    /**
     * Processes a batch of events.
     * @param events the list of events to handle
     * @param whichHandlers the type of message handlers we are looking for
     */
    suspend fun <T> runEventHandlers(
        events: Iterable<T>,
        whichHandlers: TypeOfMessageHandling = TypeOfMessageHandling.Default,
    )
}
