package cqrslite.core.serialization

interface EventSerializer {
    fun <T> serialize(event: T): String

    fun <T> deserialize(json: String, clazz: Class<T>): T
}

/**
 * Throws NoEventsConfiguredException when used, this is just to postpone
 * any missing configuration errors until actual usage
 */
class SerializerThrowingOnFirstUse : EventSerializer {
    override fun <T> serialize(event: T): String {
        throw NoEventsConfiguredException("serialize")
    }

    override fun <T> deserialize(json: String, clazz: Class<T>): T {
        throw NoEventsConfiguredException("deserialize")
    }

    class NoEventsConfiguredException(action: String) : Exception(
        """
        Trying to $action, but there exists no cqrs.serialization.map in application config
        """.trimIndent(),
    )
}
