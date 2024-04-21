package cqrslite.spring

import cqrslite.core.*
import cqrslite.core.config.EventSerializationMapConfig
import cqrslite.core.messaging.HandlerHub
import cqrslite.core.messaging.TypeOfMessageHandling
import cqrslite.core.messaging.pubsub.EventBus
import cqrslite.core.serialization.EventSerializerImpl
import cqrslite.spring.harness.ContainerizedPostgresDatabase
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import java.time.Instant
import java.util.*
import kotlin.coroutines.coroutineContext

@ExtendWith(ContainerizedPostgresDatabase::class)
class PostgresOutboxTests {

    class TestEvent(
        override var id: UUID?,
        override var version: Int?,
        override var timestamp: Instant?,
    ) : Event

    private class FakeHandlerHub(
        val runEventHandlersImpl: suspend (event: Any) -> Unit = {},
    ) : HandlerHub {
        override suspend fun <T, R> executeCommand(command: T, tClass: Class<T>, rClass: Class<R>): R {
            TODO("Not yet implemented")
        }

        override suspend fun <T> runEventHandlers(event: T, eClass: Class<T>, whichHandlers: TypeOfMessageHandling) {
            runEventHandlersImpl(event as Any)
        }

        override suspend fun <T> runEventHandlers(events: Iterable<T>, whichHandlers: TypeOfMessageHandling) {
            runEventHandlersImpl(events as Any)
        }
    }

    @Test
    fun run_default_handlers_verify_that_a_session_coroutine_context_has_been_initialized() {
        ContainerizedPostgresDatabase.runFlyway()

        val handler = FakeHandlerHub(
            runEventHandlersImpl = {
                val session = coroutineContext[SessionContextElement.Key]
                assert(session != null)
            },
        )

        val outboxRecord = OutboxRecord(EventSerializerImpl(EventSerializationMapConfig(map = mapOf(TestEvent::class.java to "test_event"))))

        val postgresOutbox = PostgresOutbox(outboxRecord, handler, eventBus = mock<EventBus> { })

        runBlocking {
            newSuspendedTransaction {
                outboxRecord.deleteAll()
                outboxRecord.insert {
                    it[key] = UUID.randomUUID()
                    it[payload] = TestEvent(UUID.randomUUID(), 1, Instant.now())
                }

                postgresOutbox.publish {
                    SessionImpl(mock<Repository> { }).asCoroutineContext()
                }

                rollback()
            }
        }
    }
}
