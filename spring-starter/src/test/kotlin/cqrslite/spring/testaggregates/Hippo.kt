package cqrslite.spring.testaggregates

import cqrslite.core.AggregateRoot
import cqrslite.spring.testevents.SomethingSimpleJustHappened
import java.util.*

class Hippo() : AggregateRoot() {

    constructor(id: UUID) : this() {
        this.id = id
    }

    fun someUseCase() {
        applyChange(SomethingSimpleJustHappened())
    }
}
