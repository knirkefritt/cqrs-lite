package cqrslite.spring

import cqrslite.core.Outbox
import cqrslite.core.Repository
import cqrslite.core.SessionImpl
import cqrslite.core.asCoroutineContext
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Scheduled

/*
 * Scheduled outbox polling
 */
class OutboxScheduledPoller(
    private val publisher: Outbox,
    private val repository: Repository,
) {
    @Scheduled(fixedDelay = 1000)
    fun singlePass() {
        runBlocking {
            publisher.publish {
                SessionImpl(repository).asCoroutineContext()
            }
        }
    }
}
