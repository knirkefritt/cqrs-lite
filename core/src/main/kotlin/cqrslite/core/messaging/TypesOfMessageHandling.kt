package cqrslite.core.messaging

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

@Target(AnnotationTarget.CLASS)
annotation class InProcess

@Target(AnnotationTarget.CLASS)
annotation class Queued

enum class TypeOfMessageHandling(private val description: String) {
    Default("Asynchronous message processing, processed from the outbox"),
    InProcess("Synchronous message processing, directly after committing messages to the event store"),
    Queue("Asynchronous message processing, processed off a queue"),
}

fun <T : Any> resolveTypeOfMessageHandling(clazz: KClass<T>): TypeOfMessageHandling {
    val inProcess = clazz.findAnnotation<InProcess>()
    val queued = clazz.findAnnotation<Queued>()

    if (inProcess != null) {
        if (queued != null) {
            throw Exception("You have marked a handler as both @Queue and @InProcess, that does not make any sense")
        }
        return TypeOfMessageHandling.InProcess
    } else if (queued != null) {
        return TypeOfMessageHandling.Queue
    }
    return TypeOfMessageHandling.Default
}
