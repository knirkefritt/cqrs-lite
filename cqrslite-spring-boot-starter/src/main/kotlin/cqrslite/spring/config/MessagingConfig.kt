package cqrslite.spring.config

import cqrslite.core.messaging.CommandHandler
import cqrslite.core.messaging.EventHandler
import cqrslite.core.messaging.HandlerHub
import cqrslite.spring.messaging.HandlerHubImpl
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Configuration
class MessagingConfig {
    @Bean
    fun instance(context: ApplicationContext, registry: HandlerRegistry): HandlerHub = HandlerHubImpl(context, registry)
}

@Component
class HandlerRegistry : BeanFactoryPostProcessor {
    lateinit var commandHandlers: List<HandlerBean>
    lateinit var eventHandlers: List<HandlerBean>

    data class HandlerBean(val clazz: Class<*>, val beanName: String)

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
            }?.let { HandlerBean(it, beanName) }
}
