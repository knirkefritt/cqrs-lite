package cqrslite.core.serialization

import com.google.gson.*
import java.lang.reflect.Type
import java.time.Instant

class InstantSerializer :
    JsonSerializer<Instant>,
    JsonDeserializer<Instant> {
    override fun serialize(instant: Instant, type: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(instant.toString())
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext?): Instant {
        return Instant.parse(json.asString)
    }
}
