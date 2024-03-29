@file:Suppress("UNUSED_PARAMETER", "unused")

package cqrslite.core

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.*

class SessionTests {

    @Test
    fun get_from_session_aggregate_does_not_exist_in_event_store_throws_correct_exception() {
        val eventStore = createInMemoryEventStore()
        val session = SessionImpl(Repository(eventStore))

        runBlocking {
            assertThrows<Repository.AggregateNotFoundException> {
                session.get(
                    id = UUID(1, 1),
                    clazz = Order::class.java,
                )
            }
        }
    }

    @Test
    fun get_from_session_aggregate_events_exist_in_event_store_returns_aggregate() {
        val id = UUID.randomUUID()
        val eventStore = createInMemoryEventStore(listOf(OrderHasBeenPlaced(id, version = 1, Instant.now())))
        val session = SessionImpl(Repository(eventStore))

        runBlocking {
            val aggregate = session.get(id = id, clazz = Order::class.java)
            assert(aggregate.orderHasBeenPlaced)
        }
    }

    @Test
    fun commit_session_tracks_new_aggregate_events_are_inserted_into_event_store() {
        val eventStore = createInMemoryEventStore()
        val session = SessionImpl(Repository(eventStore))

        runBlocking {
            val aggregate = Order(UUID.randomUUID())

            session.add(aggregate)

            aggregate.placeOrder()

            session.commit()

            assert(eventStore.events.count() == 1)
        }
    }

    object Users : Table() {
        private val id: Column<String> = varchar("id", 10)
        val name: Column<String> = varchar("name", length = 50)

        override val primaryKey = PrimaryKey(id, name = "PK_User_ID") // name is optional here
    }

    class Order() : AggregateRoot() {

        var orderHasBeenPlaced = false

        constructor(id: UUID) : this() {
            this.id = id
        }

        fun placeOrder() {
            this.applyChange(OrderHasBeenPlaced())
        }

        @Suppress("unused")
        fun apply(event: OrderHasBeenPlaced) {
            orderHasBeenPlaced = true
        }
    }

    class OrderHasBeenPlaced(override var id: UUID?, override var version: Int?, override var timestamp: Instant?) :
        Event {
        constructor() : this(null, null, null)
    }

    private fun createInMemoryEventStore(initialEvents: Iterable<Event>? = null) = InMemoryEventStore(initialEvents)

    class InMemoryEventStore(initialEvents: Iterable<Event>? = null) : EventStore {

        var events: MutableList<Event> = initialEvents?.toMutableList() ?: mutableListOf()

        override suspend fun save(events: Iterable<Event>) {
            this.events.addAll(events)
        }

        override suspend fun get(aggregateId: UUID, version: Int): Iterable<Event> = this.events
    }
}
