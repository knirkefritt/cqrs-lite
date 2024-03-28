package cqrslite.spring.testevents

import cqrslite.core.Event
import java.time.Instant
import java.util.*

data class SomethingSimpleJustHappened(
    override var id: UUID? = null,
    override var version: Int? = null,
    override var timestamp: Instant? = null,
) : Event
