package cqrslite.spring.messaging

import cqrslite.core.ConcurrencyException
import cqrslite.core.messaging.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.springframework.context.ApplicationContext
import java.lang.reflect.ParameterizedType
import java.util.concurrent.ConcurrentHashMap

class SpringBeanHandlerHub(
    private val context: ApplicationContext,
    private val registry: HandlerRegistry,
) : HandlerHub {

    data class CommandCacheEntry(val a: Class<*>, val b: Class<*>)
    data class EventCacheEntry(val handlingType: TypeOfMessageHandling, val eventType: Class<*>)

    private val commandHandlerCache = ConcurrentHashMap<CommandCacheEntry, String>()
    private val eventHandlerCache = ConcurrentHashMap<EventCacheEntry, List<HandlerRegistry.HandlerBean>>()

    override suspend fun <T, R> executeCommand(command: T, tClass: Class<T>, rClass: Class<R>): R {
        val key = CommandCacheEntry(tClass, rClass)

        var beanName = commandHandlerCache[key]

        if (beanName == null) {
            beanName = inspectBeansAndFindCommandHandler(tClass, rClass)
            commandHandlerCache[key] = beanName
        }

        var attempt = 0
        while (true) {
            attempt++
            val handler = context.getBean(beanName)
                .let { h ->
                    @Suppress("UNCHECKED_CAST")
                    h as CommandHandler<T, R>
                }

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
        var handlerBeans = eventHandlerCache[key]

        if (handlerBeans == null) {
            handlerBeans = inspectBeansAndFindEventHandlers(eClass, whichHandlers)
            eventHandlerCache[key] = handlerBeans
        }

        if (handlerBeans.isEmpty()) {
            return
        }

        runEventHandlers(handlerBeans, event)
    }

    private suspend fun <T> runEventHandlers(handlerBeans: List<HandlerRegistry.HandlerBean>, event: T) {
        var attempt = 0
        while (true) {
            attempt++
            val completed = newSuspendedTransaction {
                try {
                    handlerBeans.forEach { bean ->
                        @Suppress("UNCHECKED_CAST")
                        (context.getBean(bean.beanName) as EventHandler<T>).handle(event)
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

    private fun <R, T> inspectBeansAndFindCommandHandler(tClass: Class<T>, rClass: Class<R>): String {
        val handlerBean = registry.commandHandlers
            .single { bean ->
                bean.clazz.genericInterfaces.firstOrNull { i ->
                    i is ParameterizedType &&
                        i.actualTypeArguments.size > 1 &&
                        i.actualTypeArguments[0] == tClass &&
                        i.actualTypeArguments[1] == rClass
                } != null
            }

        return handlerBean.beanName
    }
}
