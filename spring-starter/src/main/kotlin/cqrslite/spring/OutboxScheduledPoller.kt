package cqrslite.spring

import cqrslite.core.Outbox
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Scheduled

/*
 * Scheduled outbox polling
 */
class OutboxScheduledPoller(
    private val publisher: Outbox,
) {
    @Scheduled(fixedDelay = 1000)
    fun singlePass() {
        runBlocking {
            publisher.publish()
        }
    }
}
