package cqrslite.core

import org.junit.jupiter.api.Test

class RepositoryImplTests {
    @Test
    fun create_aggregates_using_factory() {
        class Test : AggregateRoot()
        val instance = RepositoryImpl.AggregateFactory.createAggregate(clazz = Test::class.java)
        val instance2 = RepositoryImpl.AggregateFactory.createAggregate(clazz = Test::class.java)
        assert(instance != instance2)
    }
}
