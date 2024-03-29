package cqrslite.core.config

class ExternalIdToAggregateMapperConfig {
    lateinit var schema: String
    lateinit var mappingTable: String
    lateinit var map: Map<Class<*>, String>
}
