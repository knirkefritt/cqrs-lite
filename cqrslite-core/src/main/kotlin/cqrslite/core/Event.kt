package cqrslite.core

import java.time.Instant
import java.util.UUID

interface Event {
    /**
     * The id of the aggregate which this event belongs to .
     * id + version number = event uniqueness
     */
    var id: UUID?

    /**
     * The logical sequence of events are given by the version number.
     * id + version number = event uniqueness
     */
    var version: Int?

    /**
     * When the event was stored in the database
     */
    var timestamp: Instant?
}
