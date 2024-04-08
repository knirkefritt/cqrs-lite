package cqrslite.spring.config

import cqrslite.core.messaging.HandlerHub
import cqrslite.core.messaging.HandlerHubImpl
import cqrslite.spring.messaging.SpringHandlerRegistry
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean

@AutoConfiguration
@ConditionalOnMissingBean(HandlerHub::class)
class MessagingConfig {
    @Bean
    fun springHandlerHub(registry: SpringHandlerRegistry): HandlerHub = HandlerHubImpl(registry)

    @Bean
    fun handlerRegistry(context: ApplicationContext) = SpringHandlerRegistry(context)
}
