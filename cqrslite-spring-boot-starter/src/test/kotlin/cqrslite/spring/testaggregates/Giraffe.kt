package cqrslite.spring.testaggregates

import cqrslite.core.AggregateRoot
import java.util.*

class Giraffe() : AggregateRoot() {
    constructor(id: UUID) : this() {
        this.id = id
    }
}
