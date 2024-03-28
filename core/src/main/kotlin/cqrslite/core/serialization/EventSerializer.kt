package cqrslite.core.serialization

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import cqrslite.core.Event
import cqrslite.core.config.EventSerializationMapConfig
import java.lang.reflect.Type
import java.time.Instant
import java.time.LocalDate

class EventSerializer(private val cfg: EventSerializationMapConfig) {

    private var gson: Gson = GsonBuilder()
        .registerTypeAdapterFactory(
            TypeAddingFactory(
                Event::class.java,
                cfg.map,
            ),
        )
        .registerTypeAdapter(Instant::class.java, InstantSerializer())
        .registerTypeAdapter(LocalDate::class.java, LocalDateSerializer())
        .create()

    fun <T> serialize(event: T): String = gson.toJson(event)

    fun <T> deserialize(json: String, clazz: Class<T>): T = gson.fromJson(json, clazz)

    /**
     * TypeToken is primarily designed for capturing parameterized types with type parameters. You can use this
     * override if you want to deserialize parameterized types (something like Wrapper<T>).
     * The type token for the class T can be obtained as:
     * val typeToken = object : TypeToken<T>() {}.type
     */
    fun <T> deserialize(json: String, type: Type): T = gson.fromJson(json, type)

    fun <T> eventName(clazz: Class<T>) = cfg.map[clazz] ?: throw MissingMappingException(
        """
        Could not find any mapping for ${clazz.name}, check your application config
        """.trimIndent(),
    )
}
