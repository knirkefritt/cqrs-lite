package cqrslite.spring.testevents

import cqrslite.core.Event
import java.time.Instant
import java.util.*

@Suppress("unused")
data class SomeEventWithAMap(
    override var id: UUID? = null,
    override var version: Int? = null,
    override var timestamp: Instant? = null,
    val map: Map<String, String>,
) : Event
