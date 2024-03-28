package cqrslite.core

import cqrslite.core.messaging.HandlerHub
import cqrslite.core.messaging.TypeOfMessageHandling
import cqrslite.core.serialization.EventSerializer
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.json.jsonb
import java.util.*

class PostgresEventStore(
    private val record: EventStreamRecord,
    private val outbox: Outbox,
    private val handlerHub: HandlerHub,
) : EventStore {

    companion object {
        const val UNIQUE_VIOLATION = "23505"
    }

    override suspend fun save(events: Iterable<Event>) {
        try {
            record.batchInsert(events) { e ->
                this[record.id] =
                    e.id ?: throw Error("You should never get to the event store without having an id, this is a bug")
                this[record.version] = e.version
                    ?: throw Error("You should never get to the event store without having an version, this is a bug")
                this[record.payload] = e
                this[record.timestamp] = e.timestamp
                    ?: throw Error("You should never get to the event store without having an timestamp, this is a bug")
            }
            outbox.save(events)
            handlerHub.runEventHandlers(
                events,
                whichHandlers = TypeOfMessageHandling.InProcess,
            )
        } catch (e: ExposedSQLException) {
            if (e.sqlState == UNIQUE_VIOLATION) {
                throw ConcurrencyException(events.first().id ?: UUID(0, 0))
            } else {
                throw e
            }
        }
    }

    override suspend fun get(aggregateId: UUID, version: Int): Iterable<Event> {
        val where = Op.build { (record.id eq aggregateId) and (record.version greater version) }
        return record
            .select(where)
            .orderBy(record.version, SortOrder.ASC)
            .map { it[record.payload] }
    }
}

class EventStreamRecord(private val serializer: EventSerializer) : Table("write.event_stream") {
    val id = uuid("id")
    val version = integer("version")
    val payload = jsonb(
        name = "payload",
        serialize = ::serialize,
        deserialize = ::deserialize,
    )
    val timestamp = timestamp("timestamp")
    override val primaryKey = PrimaryKey(id, version, name = "event_stream_pkey")

    private fun serialize(event: Event): String = serializer.serialize(event)
    private fun deserialize(json: String): Event = serializer.deserialize(json, Event::class.java)
}
