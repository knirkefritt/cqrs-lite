package cqrslite.core.messaging

import cqrslite.core.ConcurrencyException
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.lang.reflect.ParameterizedType
import java.util.concurrent.ConcurrentHashMap

class HandlerHubImpl(
    private val registry: HandlerRegistry,
) : HandlerHub {

    data class CommandCacheEntry(val a: Class<*>, val b: Class<*>)
    data class EventCacheEntry(val handlingType: TypeOfMessageHandling, val eventType: Class<*>)

    private val commandHandlerCache = ConcurrentHashMap<CommandCacheEntry, HandlerRegistry.MessageHandlerTemplate>()
    private val eventHandlerCache = ConcurrentHashMap<EventCacheEntry, List<HandlerRegistry.MessageHandlerTemplate>>()

    override suspend fun <T, R> executeCommand(command: T, tClass: Class<T>, rClass: Class<R>): R {
        val key = CommandCacheEntry(tClass, rClass)

        var template = commandHandlerCache[key]

        if (template == null) {
            template = inspectBeansAndFindCommandHandler(tClass, rClass)
            commandHandlerCache[key] = template
        }

        var attempt = 0
        while (true) {
            attempt++
            val handler = template.create<CommandHandler<T, R>>()

            val result = newSuspendedTransaction {
                try {
                    handler.handle(command)
                } catch (ex: ConcurrencyException) {
                    if (attempt > 2) {
                        throw ex
                    } else {
                        rollback()
                        null
                    }
                }
            }
            if (result != null) {
                return result
            }
        }
    }

    override suspend fun <T> runEventHandlers(event: T, eClass: Class<T>, whichHandlers: TypeOfMessageHandling) {
        val key = EventCacheEntry(whichHandlers, eClass)
        var templates = eventHandlerCache[key]

        if (templates == null) {
            templates = inspectBeansAndFindEventHandlers(eClass, whichHandlers)
            eventHandlerCache[key] = templates
        }

        if (templates.isEmpty()) {
            return
        }

        runEventHandlers(templates, event)
    }

    private suspend fun <T> runEventHandlers(handlerBeans: List<HandlerRegistry.MessageHandlerTemplate>, event: T) {
        var attempt = 0
        while (true) {
            attempt++
            val completed = newSuspendedTransaction {
                try {
                    handlerBeans.forEach { bean ->
                        bean.create<EventHandler<T>>().handle(event)
                    }
                    true
                } catch (ex: ConcurrencyException) {
                    if (attempt > 2) {
                        throw ex
                    } else {
                        rollback()
                        false
                    }
                }
            }
            if (completed) {
                return
            }
        }
    }

    override suspend fun <T> runEventHandlers(events: Iterable<T>, whichHandlers: TypeOfMessageHandling) = events
        .filterNotNull()
        .groupBy { e -> e::class.java }
        .forEach { group ->
            group.run {
                val key = EventCacheEntry(whichHandlers, group.key)
                var handlerBeans = eventHandlerCache[key]

                if (handlerBeans == null) {
                    handlerBeans = inspectBeansAndFindEventHandlers(group.key, whichHandlers)
                    eventHandlerCache[key] = handlerBeans
                }

                if (handlerBeans.isNotEmpty()) {
                    group.value.forEach {
                        runEventHandlers(handlerBeans, it)
                    }
                }
            }
        }

    private fun <T> inspectBeansAndFindEventHandlers(eClass: Class<T>, whichHandlers: TypeOfMessageHandling) =
        registry.eventHandlers
            .filter { bean ->
                bean.clazz.genericInterfaces.firstOrNull { i ->
                    i is ParameterizedType &&
                        i.actualTypeArguments.isNotEmpty() &&
                        i.actualTypeArguments[0] == eClass
                } != null &&
                    resolveTypeOfMessageHandling(bean.clazz.kotlin) == whichHandlers
            }

    private fun <R, T> inspectBeansAndFindCommandHandler(
        tClass: Class<T>,
        rClass: Class<R>,
    ): HandlerRegistry.MessageHandlerTemplate = registry.commandHandlers
        .single { bean ->
            bean.clazz.genericInterfaces.firstOrNull { i ->
                i is ParameterizedType &&
                    i.actualTypeArguments.size > 1 &&
                    i.actualTypeArguments[0] == tClass &&
                    i.actualTypeArguments[1] == rClass
            } != null
        }
}
