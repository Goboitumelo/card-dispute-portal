package com.example.dispute.api

import com.example.dispute.domain.Dispute
import com.example.dispute.domain.DisputeReason
import com.example.dispute.domain.DisputeStatus
import com.example.dispute.domain.DisputeStatusHistory
import com.example.dispute.domain.Transaction
import com.example.dispute.service.SubmitDisputeCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class SubmitDisputeRequest(
    val transactionId: UUID,
    val reason: DisputeReason,
    @field:NotBlank
    @field:Size(max = 1000)
    val description: String,
    @field:Positive
    val disputedAmountMinor: Long,
) {
    fun toCommand() = SubmitDisputeCommand(transactionId, reason, description, disputedAmountMinor)
}

data class WithdrawRequest(
    @field:Size(max = 500)
    val note: String?,
)

data class TransactionResponse(
    val id: UUID,
    val cardLastFour: String,
    val merchantName: String,
    val amountMinor: Long,
    val currency: String,
    val occurredAt: Instant,
)

data class DisputeResponse(
    val id: UUID,
    val transactionId: UUID,
    val reason: DisputeReason,
    val description: String,
    val disputedAmountMinor: Long,
    val status: DisputeStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)

// changedBy is omitted so internal staff identifiers are not exposed to customers.
data class HistoryResponse(
    val fromStatus: DisputeStatus?,
    val toStatus: DisputeStatus,
    val note: String?,
    val changedAt: Instant,
)

fun Transaction.toResponse() = TransactionResponse(
    id = id,
    cardLastFour = card.lastFour,
    merchantName = merchantName,
    amountMinor = amountMinor,
    currency = currency,
    occurredAt = occurredAt,
)

fun Dispute.toResponse() = DisputeResponse(
    id = id,
    transactionId = transaction.id,
    reason = reason,
    description = description,
    disputedAmountMinor = disputedAmountMinor,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun DisputeStatusHistory.toResponse() = HistoryResponse(
    fromStatus = fromStatus,
    toStatus = toStatus,
    note = note,
    changedAt = changedAt,
)
