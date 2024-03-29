package cqrslite.spring

import cqrslite.core.ConcurrencyException
import cqrslite.core.EventStreamRecord
import cqrslite.core.PostgresEventStore
import cqrslite.spring.harness.ContainerizedPostgresDatabase
import cqrslite.spring.harness.TestContext
import cqrslite.spring.testevents.SomeEventWithAMap
import cqrslite.spring.testevents.SomeEventWithLocalDate
import cqrslite.spring.testevents.SomethingSimpleJustHappened
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.Instant
import java.time.LocalDate
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = [TestContext::class])
@ActiveProfiles("test")
@ExtendWith(ContainerizedPostgresDatabase::class)
class PostgresEventStoreTests {

    @Autowired
    private lateinit var store: PostgresEventStore

    @Autowired
    private lateinit var eventStream: EventStreamRecord

    @Test
    fun insert_events_into_event_stream_then_load_them() {
        runBlocking {
            newSuspendedTransaction {
                addLogger(StdOutSqlLogger)

                val id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")

                store.save(
                    listOf(
                        SomethingSimpleJustHappened(id, 1, Instant.now()),
                        SomeEventWithAMap(id, 2, Instant.now(), mapOf("key" to "bar")),
                        SomeEventWithLocalDate(id, 3, Instant.now(), LocalDate.now()),
                    ),
                )

                val events = store.get(id, 0)

                assert(events.count() == 3)
                assert(events.first()::class.java == SomethingSimpleJustHappened::class.java)
                assert(events.drop(1).first()::class.java == SomeEventWithAMap::class.java)
                assert(events.drop(2).first()::class.java == SomeEventWithLocalDate::class.java)

                rollback()
            }
        }
    }

    @Test
    fun simulate_concurrency_exception_by_trying_to_store_two_events_with_same_id_and_version_throws_concurrency_exception() {
        runBlocking {
            newSuspendedTransaction {
                SchemaUtils.create(eventStream)

                val id = UUID.fromString("8aaea865-3863-46a9-8544-082d4f183a3f")

                assertThrows<ConcurrencyException> {
                    store.save(
                        listOf(
                            SomethingSimpleJustHappened(id, 1, Instant.now()),
                            SomeEventWithAMap(id, 1, Instant.now(), mapOf("key" to "bar")),
                        ),
                    )
                }

                rollback()
            }
        }
    }
}
