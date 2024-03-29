package cqrslite.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.Instant
import java.util.*

@Suppress("unused", "UNUSED_PARAMETER")
class DynamicInvokerTests {
    @Test
    fun call_correct_method_on_aggregate() {
        val aggregate = Aggregate()

        aggregate.exposeApplyChange(SomeEvent(UUID.randomUUID(), 1, Instant.now()))
        assert(aggregate.calledSome) { "correct method was called and not the one with the overridden parameter type" }

        aggregate.exposeApplyChange(AnotherEvent(UUID.randomUUID(), 1, Instant.now()))
        assert(
            aggregate.calledAnother,
        ) { "overridden method was called, not the one with the class as an input argument" }
    }

    @Test
    fun cache_key_handles_varargs() {
        DynamicInvoker.cachedMembers.clear()

        val aggregate = Aggregate()

        aggregate.exposeApplyChange(SomeEvent(UUID.randomUUID(), 1, Instant.now()))
        aggregate.exposeApplyChange(SomeEvent(UUID.randomUUID(), 1, Instant.now()))
        assert(DynamicInvoker.cachedMembers.count() == 1)
    }

    @Test
    fun make_sure_method_cache_separates_between_two_aggregates() {
        val aggregate = Aggregate()

        aggregate.exposeApplyChange(SomeEvent(UUID.randomUUID(), 1, Instant.now()))

        assertDoesNotThrow {
            val aggregate2 = AnotherAggregate()
            aggregate2.exposeApplyChange(SomeEvent(UUID.randomUUID(), 1, Instant.now()))
        }
    }

    @Test
    fun hydrating_with_wrong_aggregate_type() {
        val aggregate = Aggregate()

        aggregate.exposeApplyChange(SomeEvent(UUID.randomUUID(), 1, Instant.now()))

        assertDoesNotThrow {
            val aggregate2 = ThirdAggregateMissingApply()
            aggregate2.exposeApplyChange(SomeEvent(UUID.randomUUID(), 1, Instant.now()))
        }
    }

    open class SomeEvent(override var id: UUID?, override var version: Int?, override var timestamp: Instant?) : Event

    class AnotherEvent(id: UUID, version: Int, timeStamp: Instant) : SomeEvent(id, version, timeStamp)

    class Aggregate : AggregateRoot() {
        var calledSome = false
        var calledAnother = false

        fun exposeApplyChange(event: Event) {
            this.applyChange(event)
        }

        fun apply(event: SomeEvent) {
            calledSome = true
        }

        fun apply(event: AnotherEvent) {
            calledAnother = true
        }
    }

    class AnotherAggregate : AggregateRoot() {
        fun exposeApplyChange(event: Event) {
            this.applyChange(event)
        }
        fun apply(event: SomeEvent) {
        }

        fun apply(event: AnotherEvent) {
        }
    }

    class ThirdAggregateMissingApply : AggregateRoot() {
        fun exposeApplyChange(event: Event) {
            this.applyChange(event)
        }
    }
}
