package cqrslite.spring

import cqrslite.core.ConcurrencyException
import cqrslite.core.messaging.CommandHandler
import cqrslite.core.messaging.EventHandler
import cqrslite.core.messaging.HandlerHubImpl
import cqrslite.core.messaging.InProcess
import cqrslite.core.messaging.TypeOfMessageHandling
import cqrslite.spring.harness.ContainerizedPostgresDatabase
import cqrslite.spring.harness.TestContext
import cqrslite.spring.messaging.SpringHandlerRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = [TestContext::class])
@ActiveProfiles("test")
@ExtendWith(ContainerizedPostgresDatabase::class)
class HandlerHubImplTests {

    @Test
    fun `execute a command that resolves to a command handler, returns the command handler return value`() {
        val handler = MyTestCommandHandler()

        val context = mock<ApplicationContext> {
            on { getBean("silly-bean") } doReturn handler
        }

        val registry = SpringHandlerRegistry(context)
        registry.eventHandlers = listOf()
        registry.commandHandlers = listOf(SpringHandlerRegistry.HandlerBean(MyTestCommandHandler::class.java, "silly-bean", context))

        val handlerHub = HandlerHubImpl(registry)

        runBlocking {
            val response = handlerHub.executeCommand(TestCommand(), TestCommand::class.java, TestCommandResponse::class.java)
            assert(response.text == "hello")
        }
    }

    @Suppress("RedundantUnitExpression")
    @Test
    fun `handle an event that resolves to a event handler, event processed by event handler`() {
        runBlocking {
            val handler = mock<MyTestEventHandler> {}

            var handled = false
            whenever(handler.handle(any<TestEvent>())).then {
                handled = true
                Unit
            }

            val context = mock<ApplicationContext> {
                on { getBean("silly-bean") } doReturn handler
            }

            val registry = SpringHandlerRegistry(context)
            registry.eventHandlers = listOf(SpringHandlerRegistry.HandlerBean(MyTestEventHandler::class.java, "silly-bean", context))
            registry.commandHandlers = listOf()

            val handlerHub = HandlerHubImpl(registry)

            handlerHub.runEventHandlers(TestEvent(), TestEvent::class.java)
            assert(handled)
        }
    }

    @Suppress("RedundantUnitExpression")
    @Test
    fun `handle an event that do not resolve to a event handler, event processed without failures`() {
        runBlocking {
            val handler = mock<InProcessTestHandler> {}

            var handled = false
            whenever(handler.handle(any<TestEvent>())).then {
                handled = true
                Unit
            }

            val context = mock<ApplicationContext> {
                on { getBean("silly-bean") } doReturn handler
            }

            val registry = SpringHandlerRegistry(context)
            registry.eventHandlers = listOf(SpringHandlerRegistry.HandlerBean(InProcessTestHandler::class.java, "silly-bean", context))
            registry.commandHandlers = listOf()

            val handlerHub = HandlerHubImpl(registry)

            handlerHub.runEventHandlers(TestEvent(), TestEvent::class.java, whichHandlers = TypeOfMessageHandling.Default)
            assert(!handled)
        }
    }

    @Test
    fun `execute a command twice, utilizes cache by not looking up the registry twice`() {
        val handler = MyTestCommandHandler()

        val context = mock<ApplicationContext> {
            on { getBean("silly-bean") } doReturn handler
        }

        val registry = mock<SpringHandlerRegistry> {
            on { eventHandlers } doReturn listOf()
            on { commandHandlers } doReturn
                listOf(SpringHandlerRegistry.HandlerBean(MyTestCommandHandler::class.java, "silly-bean", context))
        }
        val handlerHub = HandlerHubImpl(registry)

        runBlocking {
            handlerHub.executeCommand(TestCommand(), TestCommand::class.java, TestCommandResponse::class.java)
            val result = handlerHub.executeCommand(TestCommand(), TestCommand::class.java, TestCommandResponse::class.java)

            verify(registry, times(1)).commandHandlers
            assert(result.text == "hello")
        }
    }

    @Test
    fun `handle event multiple times, utilizes cache by not looking up the registry twice`() {
        val handler = MyTestEventHandler()

        val context = mock<ApplicationContext> {
            on { getBean("silly-bean") } doReturn handler
        }

        val registry = mock<SpringHandlerRegistry> {
            on { eventHandlers } doReturn listOf(SpringHandlerRegistry.HandlerBean(MyTestEventHandler::class.java, "silly-bean", context))
            on { commandHandlers } doReturn listOf()
        }
        val handlerHub = HandlerHubImpl(registry)

        runBlocking {
            handlerHub.runEventHandlers(TestEvent(), TestEvent::class.java)
            handlerHub.runEventHandlers(TestEvent(), TestEvent::class.java)

            verify(registry, times(1)).eventHandlers
        }
    }

    @Test
    fun `execute a command that fails with concurrency on first attempt, returns the command handler return value`() {
        runBlocking {
            val handler = mock<MyTestCommandHandler> {}

            var first = true
            whenever(handler.handle(any())).doAnswer { _ ->
                if (first) {
                    first = false
                    throw ConcurrencyException(UUID.randomUUID())
                } else {
                    TestCommandResponse("world")
                }
            }

            val context = mock<ApplicationContext> {
                on { getBean("silly-bean") } doReturn handler
            }

            val registry = SpringHandlerRegistry(context)
            registry.eventHandlers = listOf()
            registry.commandHandlers = listOf(SpringHandlerRegistry.HandlerBean(MyTestCommandHandler::class.java, "silly-bean", context))

            val handlerHub = HandlerHubImpl(registry)

            val response = handlerHub.executeCommand(TestCommand(), TestCommand::class.java, TestCommandResponse::class.java)
            assert(response.text == "world")
        }
    }

    @Test
    fun `handle an event that fails with concurrency on first attempt, handled by event handler on second attempt`() {
        runBlocking {
            val handler = mock<MyTestEventHandler> {}

            var first = true
            var handled = false
            whenever(handler.handle(any())).doAnswer { _ ->
                if (first) {
                    first = false
                    throw ConcurrencyException(UUID.randomUUID())
                } else {
                    handled = true
                }
            }

            val context = mock<ApplicationContext> {
                on { getBean("silly-bean") } doReturn handler
            }

            val registry = SpringHandlerRegistry(context)
            registry.eventHandlers = listOf(SpringHandlerRegistry.HandlerBean(MyTestEventHandler::class.java, "silly-bean", context))
            registry.commandHandlers = listOf()

            val handlerHub = HandlerHubImpl(registry)

            handlerHub.runEventHandlers(TestEvent(), TestEvent::class.java)
            assert(handled)
        }
    }

    private class TestCommand
    private class TestCommandResponse(val text: String)
    private class MyTestCommandHandler : CommandHandler<TestCommand, TestCommandResponse> {
        override suspend fun handle(cmd: TestCommand): TestCommandResponse = TestCommandResponse("hello")
    }

    private class TestEvent
    private class MyTestEventHandler : EventHandler<TestEvent> {
        override suspend fun handle(event: TestEvent) {
        }
    }

    @InProcess
    private class InProcessTestHandler : EventHandler<TestEvent> {
        override suspend fun handle(event: TestEvent) {
        }
    }
}
