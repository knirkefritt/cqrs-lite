package cqrslite.core

import com.google.gson.reflect.TypeToken
import cqrslite.core.config.EventSerializationMapConfig
import cqrslite.core.serialization.EventSerializerImpl
import cqrslite.core.serialization.MissingMappingException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.*

class EventSerializerImplTests {

    @Test
    fun `serialize then deserialize parameterized type, expects deserialized type to match serialized type`() {
        val serializer = EventSerializerImpl(
            cfg = EventSerializationMapConfig(
                map = mapOf(SomethingSimpleJustHappened::class.java to "foo"),
            ),
        )

        val envelope = Envelope(SomethingSimpleJustHappened(version = 1, timestamp = Instant.now()))
        val json = serializer.serialize(envelope)

        val typeToken = object : TypeToken<Envelope<Event>>() {}.type
        val deserializedMessage = serializer.deserialize<Envelope<Event>>(json, type = typeToken)
        assert(envelope == deserializedMessage)
    }

    @Test
    fun `registered event, get name of event`() {
        val serializer = EventSerializerImpl(
            cfg = EventSerializationMapConfig(
                map = mapOf(SomethingSimpleJustHappened::class.java to "foo"),
            ),
        )

        assert(serializer.eventName(clazz = SomethingSimpleJustHappened::class.java) == "foo")
    }

    @Test
    fun `not registered event, throws error`() {
        val serializer = EventSerializerImpl(
            cfg = EventSerializationMapConfig(map = mapOf()),
        )

        assertThrows<MissingMappingException> { serializer.eventName(clazz = SomethingSimpleJustHappened::class.java) }
    }

    data class Envelope<T>(
        val enveloped: T,
    )

    data class SomethingSimpleJustHappened(
        override var id: UUID? = null,
        override var version: Int? = null,
        override var timestamp: Instant? = null,
    ) : Event
}
