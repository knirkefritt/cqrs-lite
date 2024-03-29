package cqrslite.core

import cqrslite.core.messaging.HandlerHub
import cqrslite.core.messaging.TypeOfMessageHandling
import cqrslite.core.messaging.pubsub.EventBus
import cqrslite.core.serialization.EventSerializer
import org.jetbrains.exposed.sql.IntegerColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.sql.ResultSet

/**
 * Picks messages from the outbox and passes them on to default event handlers.
 * All messages are published on the event bus after processing, this will make the available
 * for @Queue event handling
 */
class PostgresOutbox(
    private val outboxRecord: OutboxRecord,
    private val handlerHub: HandlerHub,
    private val eventBus: EventBus,
) : Outbox {
    companion object {
        private val deleteFromOutbox = """
            DELETE FROM write.outbox
            WHERE Id IN (
                SELECT Id FROM write.outbox
                ORDER BY Id
                FOR UPDATE SKIP LOCKED
                LIMIT ?
            )
            RETURNING payload;
        """.trimIndent()
    }

    override suspend fun save(events: Iterable<Event>) {
        outboxRecord.batchInsert(events) { e ->
            this[outboxRecord.key] = e.id
                ?: throw ShouldBeAssignedByAggregateRoot()
            this[outboxRecord.payload] = e
        }
    }

    override suspend fun publish() {
        var done = false

        fun collectResultSet(rs: ResultSet): List<Event> {
            val messagesToPublish = mutableListOf<Event>()
            while (rs.next()) {
                messagesToPublish.add(outboxRecord.deserialize(rs.getString(1)))
            }
            return messagesToPublish
        }

        while (!done) {
            newSuspendedTransaction {
                val payload = exec(
                    deleteFromOutbox,
                    args = listOf(
                        Pair(IntegerColumnType(), 1),
                    ),
                    transform = ::collectResultSet,
                    explicitStatementType = StatementType.SELECT,
                )

                payload?.run {
                    handlerHub.runEventHandlers(
                        this,
                        whichHandlers = TypeOfMessageHandling.Default,
                    )

                    eventBus.publish(this)
                }

                done = (payload?.isEmpty()) ?: true
            }
        }
    }
}

class OutboxRecord(private val serializer: EventSerializer) : Table("write.outbox") {
    private val id = long("id").autoIncrement()
    val key = uuid("key")
    val payload = jsonb(
        name = "payload",
        serialize = ::serialize,
        deserialize = ::deserialize,
    )
    override val primaryKey = PrimaryKey(id, name = "outbox_pkey")

    private fun serialize(event: Event): String = serializer.serialize(event)
    fun deserialize(json: String): Event = serializer.deserialize(json, Event::class.java)
}
