package cqrslite.spring.config

import cqrslite.core.messaging.HandlerHub
import cqrslite.spring.messaging.HandlerRegistry
import cqrslite.spring.messaging.SpringBeanHandlerHub
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean

@AutoConfiguration
@ConditionalOnMissingBean(HandlerHub::class)
class MessagingConfig {
    @Bean
    fun springHandlerHub(context: ApplicationContext, registry: HandlerRegistry): HandlerHub =
        SpringBeanHandlerHub(context, registry)

    @Bean
    fun handlerRegistry(): HandlerRegistry = HandlerRegistry()
}
