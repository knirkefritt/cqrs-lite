package cqrslite.core

class ShouldBeAssignedByAggregateRoot : Exception(
    """
    The aggregate root "flushUncommittedChanges" should assign id, version and timestamp.
    This is a programming error
    """.trimIndent(),
)
