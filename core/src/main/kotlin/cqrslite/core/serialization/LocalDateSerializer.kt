package cqrslite.core.serialization

import com.google.gson.*
import java.lang.reflect.Type
import java.time.LocalDate

class LocalDateSerializer :
    JsonSerializer<LocalDate>,
    JsonDeserializer<LocalDate> {
    override fun serialize(localDate: LocalDate, type: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(localDate.toString())
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext?): LocalDate {
        return LocalDate.parse(json.asString)
    }
}
