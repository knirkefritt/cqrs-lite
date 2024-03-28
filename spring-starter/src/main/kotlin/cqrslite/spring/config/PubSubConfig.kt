package cqrslite.spring.config

import cqrslite.core.messaging.HandlerHub
import cqrslite.core.messaging.pubsub.EventBus
import cqrslite.core.messaging.pubsub.NoBusEventBus
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

/**
 * note: AutoConfiguration classes are not automatically picked up even if you use
 * SpringBootApplication (which automatically turns on autoconfiguration). You also have
 * to add META-INF so that it will be picked up as an import candidate. We use AutoConfiguration
 * instead of Configuration to make sure that the ConditionalOnMissingBean is evaluated after loading
 * user configuration
 */
@AutoConfiguration
@ConditionalOnMissingBean(EventBus::class)
class PubSubConfig {

    /**
     * This will register a default event bus which passes all queued events directly
     * to the event handler, processing them async, but without a queue (same as the default).
     * You can override this behavior by registering a new bean which implements the
     * event bus interface
     */
    @Bean
    @ConditionalOnMissingBean
    fun noBusEventBus(handlerHub: HandlerHub): EventBus = NoBusEventBus(handlerHub)
}
