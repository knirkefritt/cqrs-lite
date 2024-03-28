package cqrslite.core.serialization

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.internal.bind.JsonTreeReader
import com.google.gson.internal.bind.JsonTreeWriter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Adds a $type property to the json-document for classes derived from the specified class [clazz]
 * @param clazz the base class for all classes which should be serialized with $type information
 * @param namingConvention a map of serialized names for each class that should be decorated with $type information, note: if the serialized class is not in the map, $type property will not be added
 */
class TypeAddingFactory(
    private val clazz: Class<*>,
    private val namingConvention: Map<Class<*>, String>,
) : TypeAdapterFactory {

    private val reverseMap: Map<String, Class<*>> = namingConvention.entries.associateBy({ it.value }, { it.key })

    override fun <T> create(gson: Gson, type: TypeToken<T>?): TypeAdapter<T>? {
        if (type == null || !clazz.isAssignableFrom(type.rawType)) {
            return null
        }

        val thisFactory = this

        return object : TypeAdapter<T>() {
            override fun write(writer: JsonWriter, value: T?) {
                val treeWriter = JsonTreeWriter()
                gson.getDelegateAdapter(thisFactory, type)?.write(treeWriter, value)
                val jsonObject = treeWriter.get().asJsonObject
                (
                    namingConvention[type.rawType] ?: throw MissingMappingException(
                        """
                        Could not find any mapping for ${type.rawType}, check your application config
                        """.trimIndent(),
                    )
                    ).let { jsonObject.addProperty("\$type", it) }
                gson.toJson(jsonObject, writer)
            }

            override fun read(reader: JsonReader): T {
                val jsonObject = JsonParser.parseReader(reader).asJsonObject
                val classType = reverseMap[jsonObject["\$type"].asString]
                @Suppress("UNCHECKED_CAST")
                return gson.getDelegateAdapter(
                    thisFactory,
                    TypeToken.get(classType),
                )?.read(JsonTreeReader(jsonObject)) as T
            }
        }
    }
}
