package cqrslite.core.messaging

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

@Target(AnnotationTarget.CLASS)
annotation class InProcess

@Target(AnnotationTarget.CLASS)
annotation class Queued

enum class TypeOfMessageHandling {
    /**
     * Asynchronous message processing, processed from the outbox
     */
    Default,

    /**
     * Synchronous message processing, directly after committing messages to the event store
     */
    InProcess,

    /**
     * Asynchronous message processing, processed off a queue
     */
    Queue,
}

fun <T : Any> resolveTypeOfMessageHandling(clazz: KClass<T>): TypeOfMessageHandling {
    val inProcess = clazz.findAnnotation<InProcess>()
    val queued = clazz.findAnnotation<Queued>()

    if (inProcess != null) {
        if (queued != null) {
            throw InconsistentMappingException(
                """
                You have marked a handler as both @Queue and @InProcess, that does not make any sense
                """.trimIndent(),
            )
        }
        return TypeOfMessageHandling.InProcess
    } else if (queued != null) {
        return TypeOfMessageHandling.Queue
    }
    return TypeOfMessageHandling.Default
}

class InconsistentMappingException(
    override val message: String?,
) : Exception(message)
