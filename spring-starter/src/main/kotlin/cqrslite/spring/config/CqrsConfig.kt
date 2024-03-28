package cqrslite.spring.config

import cqrslite.core.*
import cqrslite.core.config.EventSerializationMapConfig
import cqrslite.core.config.ExternalIdToAggregateMapperConfig
import cqrslite.core.messaging.HandlerHub
import cqrslite.core.serialization.EventSerializer
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.*

@Configuration
class CqrsConfig {

    @Bean
    @ConfigurationProperties(prefix = "cqrs.serialization")
    fun serializationMapConfig(): EventSerializationMapConfig = EventSerializationMapConfig()

    @Bean
    fun serializer(cfg: EventSerializationMapConfig) = EventSerializer(cfg)

    @Bean
    @ConfigurationProperties(prefix = "cqrs.lookup")
    fun lookupMapConfig(): ExternalIdToAggregateMapperConfig = ExternalIdToAggregateMapperConfig()

    @Bean
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
}
