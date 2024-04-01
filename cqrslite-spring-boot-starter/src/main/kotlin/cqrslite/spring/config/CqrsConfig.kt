package cqrslite.spring.config

import cqrslite.core.*
import cqrslite.core.config.EventSerializationMapConfig
import cqrslite.core.config.ExternalIdToAggregateMapperConfig
import cqrslite.core.messaging.HandlerHub
import cqrslite.core.serialization.EventSerializer
import cqrslite.core.serialization.EventSerializerImpl
import cqrslite.core.serialization.SerializerThrowingOnFirstUse
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.*
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.annotation.*
import org.springframework.core.type.AnnotatedTypeMetadata

@AutoConfiguration
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
    fun serializerNotConfigured(): EventSerializer = SerializerThrowingOnFirstUse()

    @Bean
    @Conditional(AggregateMapperEnabledCondition::class)
    @ConfigurationProperties(prefix = "cqrs.lookup")
    fun lookupMapConfig(): ExternalIdToAggregateMapperConfig = ExternalIdToAggregateMapperConfig()

    @Bean
    @Conditional(AggregateMapperEnabledCondition::class)
    fun lookup(cfg: ExternalIdToAggregateMapperConfig): ExternalIdToAggregateMapper =
        ExternalIdToAggregateMapperImpl(cfg)

    @Bean
    @ConditionalOnMissingBean
    fun lookupNotConfigured(): ExternalIdToAggregateMapper = MapperThrowingOnFirstUse()

    @Bean
    @ConditionalOnMissingBean
    fun eventStreamRecord(serializer: EventSerializer): EventStreamRecord = EventStreamRecord(serializer)

    @Bean
    @ConditionalOnMissingBean
    fun outboxRecord(serializer: EventSerializer): OutboxRecord = OutboxRecord(serializer)

    @Bean
    @ConditionalOnMissingBean
    fun eventStore(record: EventStreamRecord, outbox: Outbox, handlerHub: HandlerHub): PostgresEventStore =
        PostgresEventStore(record, outbox, handlerHub)

    @Bean
    @ConditionalOnMissingBean
    fun repository(eventStore: EventStore): Repository = RepositoryImpl(eventStore)

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
