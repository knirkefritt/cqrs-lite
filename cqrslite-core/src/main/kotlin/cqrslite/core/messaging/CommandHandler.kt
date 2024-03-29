package cqrslite.core.messaging

interface CommandHandler<T, R> {
    suspend fun handle(cmd: T): R
}
