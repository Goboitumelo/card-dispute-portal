package com.example.dispute.domain

enum class DisputeStatus {
    SUBMITTED,
    UNDER_REVIEW,
    INFO_REQUESTED,
    RESOLVED_ACCEPTED,
    RESOLVED_REJECTED,
    WITHDRAWN;

    val allowedNext: Set<DisputeStatus>
        get() = when (this) {
            SUBMITTED -> setOf(UNDER_REVIEW, WITHDRAWN)
            UNDER_REVIEW -> setOf(INFO_REQUESTED, RESOLVED_ACCEPTED, RESOLVED_REJECTED, WITHDRAWN)
            INFO_REQUESTED -> setOf(UNDER_REVIEW, WITHDRAWN)
            RESOLVED_ACCEPTED, RESOLVED_REJECTED, WITHDRAWN -> emptySet()
        }

    fun canTransitionTo(next: DisputeStatus): Boolean = next in allowedNext

    companion object {
        // Must match the predicate of the uq_disputes_one_open_per_transaction index.
        val OPEN: Set<DisputeStatus> = setOf(SUBMITTED, UNDER_REVIEW, INFO_REQUESTED)
    }
}
