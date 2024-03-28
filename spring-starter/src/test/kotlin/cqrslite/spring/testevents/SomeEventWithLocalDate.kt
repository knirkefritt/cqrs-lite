package cqrslite.spring.testevents

import cqrslite.core.Event
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Suppress("unused")
data class SomeEventWithLocalDate(
    override var id: UUID? = null,
    override var version: Int? = null,
    override var timestamp: Instant? = null,
    val localDate: LocalDate,
) : Event
