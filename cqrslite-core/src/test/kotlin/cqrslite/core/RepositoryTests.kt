package cqrslite.core

import org.junit.jupiter.api.Test

class RepositoryTests {
    @Test
    fun create_aggregates_using_factory() {
        class Test : AggregateRoot()
        val instance = Repository.AggregateFactory.createAggregate(clazz = Test::class.java)
        val instance2 = Repository.AggregateFactory.createAggregate(clazz = Test::class.java)
        assert(instance != instance2)
    }
}
