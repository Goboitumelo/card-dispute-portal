package com.example.dispute.service

import com.example.dispute.domain.DisputeReason
import java.util.UUID

class NotFoundException(message: String) : RuntimeException(message)

class ConflictException(message: String) : RuntimeException(message)

class UnprocessableException(message: String) : RuntimeException(message)

data class SubmitDisputeCommand(
    val transactionId: UUID,
    val reason: DisputeReason,
    val description: String,
    val disputedAmountMinor: Long,
)
