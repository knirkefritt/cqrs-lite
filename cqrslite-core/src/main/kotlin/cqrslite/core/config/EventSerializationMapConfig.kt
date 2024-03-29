package cqrslite.core.config

class EventSerializationMapConfig() {
    lateinit var map: Map<Class<*>, String>

    constructor(map: Map<Class<*>, String>) : this() {
        this.map = map
    }
}
