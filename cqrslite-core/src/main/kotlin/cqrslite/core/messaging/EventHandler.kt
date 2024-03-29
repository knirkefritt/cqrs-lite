package cqrslite.core.messaging

interface EventHandler<T> {
    suspend fun handle(event: T)
}
