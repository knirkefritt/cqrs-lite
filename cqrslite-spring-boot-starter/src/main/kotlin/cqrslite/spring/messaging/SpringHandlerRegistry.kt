package cqrslite.spring.messaging

import cqrslite.core.messaging.CommandHandler
import cqrslite.core.messaging.EventHandler
import cqrslite.core.messaging.HandlerRegistry
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.ApplicationContext

/**
 * An event-/command handler registry which finds all handlers registered as beans
 */
class SpringHandlerRegistry(
    private var context: ApplicationContext,
) : BeanFactoryPostProcessor, HandlerRegistry {
    override lateinit var commandHandlers: List<HandlerRegistry.MessageHandlerTemplate>
    override lateinit var eventHandlers: List<HandlerRegistry.MessageHandlerTemplate>

    data class HandlerBean(override val clazz: Class<*>, val beanName: String, val context: ApplicationContext) :
        HandlerRegistry.MessageHandlerTemplate {
        override fun <T> create(): T {
            @Suppress("UNCHECKED_CAST")
            return context.getBean(beanName) as T
        }
    }

    @Throws(BeansException::class)
    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        commandHandlers = beanFactory.beanDefinitionNames.mapNotNull {
            val beanDefinition = beanFactory.getBeanDefinition(it)
            getClazzIfBeanIsCommandHandlerType(it, beanDefinition)
        }

        eventHandlers = beanFactory.beanDefinitionNames.mapNotNull {
            val beanDefinition = beanFactory.getBeanDefinition(it)
            getClazzIfBeanIsEventHandlerType(it, beanDefinition)
        }
    }

    private fun getClazzIfBeanIsCommandHandlerType(beanName: String, beanDefinition: BeanDefinition) =
        getClassIfBeanIsHandlerType(beanDefinition, beanName, CommandHandler::class.java)

    private fun getClazzIfBeanIsEventHandlerType(beanName: String, beanDefinition: BeanDefinition) =
        getClassIfBeanIsHandlerType(beanDefinition, beanName, EventHandler::class.java)

    private fun getClassIfBeanIsHandlerType(beanDefinition: BeanDefinition, beanName: String, handlerType: Class<*>) =
        beanDefinition.beanClassName
            ?.let { Class.forName(beanDefinition.beanClassName) }
            ?.takeIf {
                handlerType.isAssignableFrom(it)
            }?.let { HandlerBean(it, beanName, this.context) }
}
