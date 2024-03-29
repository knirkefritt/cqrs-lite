package cqrslite.spring.config

import cqrslite.core.Outbox
import cqrslite.core.OutboxRecord
import cqrslite.core.PostgresOutbox
import cqrslite.core.messaging.HandlerHub
import cqrslite.core.messaging.pubsub.EventBus
import cqrslite.spring.OutboxScheduledPoller
import org.springframework.context.annotation.*
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
class OutboxConfig {

    @Bean
    @Conditional(SchedulingEnabledCondition::class)
    fun scheduler(outbox: Outbox) = OutboxScheduledPoller(outbox)

    @Bean
    fun outbox(outboxRecord: OutboxRecord, handlerHub: HandlerHub, eventBus: EventBus): Outbox =
        PostgresOutbox(outboxRecord, handlerHub, eventBus)

    class SchedulingEnabledCondition : Condition {
        override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
            return context.environment.getProperty("scheduling.enabled", Boolean::class.java, true)
        }
    }
}
