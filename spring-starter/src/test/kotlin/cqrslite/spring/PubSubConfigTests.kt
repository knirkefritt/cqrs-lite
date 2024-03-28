package cqrslite.spring

import cqrslite.core.messaging.pubsub.EventBus
import cqrslite.core.messaging.pubsub.NoBusEventBus
import cqrslite.spring.harness.ContainerizedPostgresDatabase
import cqrslite.spring.harness.TestContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = [OverrideDefaultEventBusTests.OverrideDefaultBus::class, TestContext::class])
@ActiveProfiles("test")
@ExtendWith(ContainerizedPostgresDatabase::class)
class OverrideDefaultEventBusTests {

    @Configuration
    class OverrideDefaultBus {
        @Bean
        fun anotherBus(): EventBus = mock<EventBus>()
    }

    @Autowired
    var eventBus: EventBus? = null

    @Test
    fun validate_that_I_can_override_the_default_event_bus() {
        assert(eventBus != null)
        assert(eventBus !is NoBusEventBus)
    }
}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = [TestContext::class])
@ActiveProfiles("test")
@ExtendWith(ContainerizedPostgresDatabase::class)
class KeepDefaultEventBus {

    @Autowired
    var eventBus: EventBus? = null

    @Test
    fun validate_that_the_default_event_bus_is_provided() {
        assert(eventBus != null)
        assert(eventBus is NoBusEventBus)
    }
}
