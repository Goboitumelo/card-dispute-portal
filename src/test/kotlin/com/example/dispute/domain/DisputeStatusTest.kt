package com.example.dispute.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DisputeStatusTest {

    @Test
    fun `submitted disputes can go to review or be withdrawn`() {
        assertEquals(setOf(DisputeStatus.UNDER_REVIEW, DisputeStatus.WITHDRAWN), DisputeStatus.SUBMITTED.allowedNext)
    }

    @Test
    fun `disputes under review can request info, be resolved or be withdrawn`() {
        assertEquals(
            setOf(
                DisputeStatus.INFO_REQUESTED,
                DisputeStatus.RESOLVED_ACCEPTED,
                DisputeStatus.RESOLVED_REJECTED,
                DisputeStatus.WITHDRAWN,
            ),
            DisputeStatus.UNDER_REVIEW.allowedNext,
        )
    }

    @Test
    fun `info requests return to review or are withdrawn`() {
        assertEquals(
            setOf(DisputeStatus.UNDER_REVIEW, DisputeStatus.WITHDRAWN),
            DisputeStatus.INFO_REQUESTED.allowedNext,
        )
    }

    @Test
    fun `closed disputes allow no further transitions`() {
        listOf(DisputeStatus.RESOLVED_ACCEPTED, DisputeStatus.RESOLVED_REJECTED, DisputeStatus.WITHDRAWN)
            .forEach { assertTrue(it.allowedNext.isEmpty(), "$it should be terminal") }
    }

    @Test
    fun `submitted disputes cannot be resolved without review`() {
        assertTrue(!DisputeStatus.SUBMITTED.canTransitionTo(DisputeStatus.RESOLVED_ACCEPTED))
    }
}
