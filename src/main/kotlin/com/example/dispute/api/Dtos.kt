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
    /**
     * Converts the request into a service command.
     *
     * @return the command for the dispute service
     */
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

/** Maps a transaction to its API response. The card is exposed by its last four digits only. */
fun Transaction.toResponse() = TransactionResponse(
    id = id,
    cardLastFour = card.lastFour,
    merchantName = merchantName,
    amountMinor = amountMinor,
    currency = currency,
    occurredAt = occurredAt,
)

/** Maps a dispute to its API response. */
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

/** Maps a status change to its API response. */
fun DisputeStatusHistory.toResponse() = HistoryResponse(
    fromStatus = fromStatus,
    toStatus = toStatus,
    note = note,
    changedAt = changedAt,
)
