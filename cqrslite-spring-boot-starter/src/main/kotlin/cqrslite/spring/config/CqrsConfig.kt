package cqrslite.spring.config

import cqrslite.core.*
import cqrslite.core.config.EventSerializationMapConfig
import cqrslite.core.config.ExternalIdToAggregateMapperConfig
import cqrslite.core.messaging.HandlerHub
import cqrslite.core.serialization.EventSerializer
import cqrslite.core.serialization.EventSerializerImpl
import cqrslite.core.serialization.NoOpEventSerializer
import org.springframework.boot.autoconfigure.condition.*
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.annotation.*
import org.springframework.core.type.AnnotatedTypeMetadata

@Configuration
class CqrsConfig {

    @Bean
    @Conditional(EventMapConfiguredCondition::class)
    @ConfigurationProperties(prefix = "cqrs.serialization")
    fun serializationMapConfig(): EventSerializationMapConfig = EventSerializationMapConfig()

    @Bean
    @ConditionalOnBean(EventSerializationMapConfig::class)
    fun serializer(cfg: EventSerializationMapConfig): EventSerializer = EventSerializerImpl(cfg)

    @Bean
    @ConditionalOnMissingBean(EventSerializationMapConfig::class)
    fun noOpSerializer(): EventSerializer = NoOpEventSerializer()

    @Bean
    @Conditional(AggregateMapperEnabledCondition::class)
    @ConfigurationProperties(prefix = "cqrs.lookup")
    fun lookupMapConfig(): ExternalIdToAggregateMapperConfig = ExternalIdToAggregateMapperConfig()

    @Bean
    @Conditional(AggregateMapperEnabledCondition::class)
    fun lookup(cfg: ExternalIdToAggregateMapperConfig): ExternalIdToAggregateMapper = ExternalIdToAggregateMapper(cfg)

    @Bean
    fun eventStream(serializer: EventSerializer): EventStreamRecord = EventStreamRecord(serializer)

    @Bean
    fun outboxRecord(serializer: EventSerializer): OutboxRecord = OutboxRecord(serializer)

    @Bean
    fun eventStore(record: EventStreamRecord, outbox: Outbox, handlerHub: HandlerHub): PostgresEventStore =
        PostgresEventStore(record, outbox, handlerHub)

    @Bean
    fun repository(eventStore: EventStore): Repository = Repository(eventStore)

    class AggregateMapperEnabledCondition : Condition {
        override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
            val schema = context.environment.getProperty("cqrs.lookup.schema", String::class.java, "")
            val table = context.environment.getProperty("cqrs.lookup.mappingTable", String::class.java, "")
            return schema.isNotEmpty() && table.isNotEmpty()
        }
    }

    class EventMapConfiguredCondition : Condition {
        override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
            val result = Binder.get(context.environment).bind("cqrs.serialization.map", Map::class.java)
            return if (result.isBound) result.get().isNotEmpty() else false
        }
    }
}
