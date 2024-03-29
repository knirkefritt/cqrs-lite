package cqrslite.core

import java.util.*

interface Session {
    /**
     * Adds an aggregate for tracking, so that it will be persisted upon the next Commit to the database
     */
    fun <T : AggregateRoot> add(aggregate: T)

    suspend fun <T : AggregateRoot> get(id: UUID, expectedVersion: Int? = null, clazz: Class<T>): T

    /**
     * Commits any tracked aggregate changes to the db and clears the list of tracked aggregates
     */
    suspend fun commit()
}
