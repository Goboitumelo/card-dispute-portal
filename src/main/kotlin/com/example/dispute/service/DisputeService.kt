package com.example.dispute.service

import com.example.dispute.domain.Customer
import com.example.dispute.domain.Dispute
import com.example.dispute.domain.DisputeStatus
import com.example.dispute.domain.DisputeStatusHistory
import com.example.dispute.repository.CustomerRepository
import com.example.dispute.repository.DisputeRepository
import com.example.dispute.repository.DisputeStatusHistoryRepository
import com.example.dispute.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class DisputeService(
    private val customers: CustomerRepository,
    private val transactions: TransactionRepository,
    private val disputes: DisputeRepository,
    private val history: DisputeStatusHistoryRepository,
    private val clock: Clock,
) {

    @Transactional
    fun submit(customerId: UUID, command: SubmitDisputeCommand): Dispute {
        val customer = findCustomer(customerId)
        // Unknown and other customers' transactions both return 404 so ownership is not leaked.
        val transaction = transactions.findByIdAndCardCustomerId(command.transactionId, customerId)
            ?: throw NotFoundException("Transaction not found")
        val now = clock.instant()

        if (now.isAfter(transaction.occurredAt.plus(DISPUTE_WINDOW))) {
            throw UnprocessableException("Disputes must be raised within 120 days of the transaction")
        }
        if (command.disputedAmountMinor > transaction.amountMinor) {
            throw UnprocessableException("Disputed amount exceeds the transaction amount")
        }
        if (disputes.existsByTransactionIdAndStatusIn(transaction.id, DisputeStatus.OPEN)) {
            throw ConflictException("An open dispute already exists for this transaction")
        }

        val dispute = disputes.saveAndFlush(
            Dispute(
                id = UUID.randomUUID(),
                transaction = transaction,
                customer = customer,
                reason = command.reason,
                description = command.description,
                disputedAmountMinor = command.disputedAmountMinor,
                status = DisputeStatus.SUBMITTED,
                createdAt = now,
                updatedAt = now,
            ),
        )
        history.save(
            DisputeStatusHistory(dispute, null, DisputeStatus.SUBMITTED, "customer:$customerId", null, now),
        )
        return dispute
    }

    @Transactional(readOnly = true)
    fun listFor(customerId: UUID): List<Dispute> =
        disputes.findAllByCustomerIdOrderByCreatedAtDesc(customerId)

    @Transactional(readOnly = true)
    fun getFor(customerId: UUID, disputeId: UUID): Dispute = findOwned(customerId, disputeId)

    @Transactional(readOnly = true)
    fun historyFor(customerId: UUID, disputeId: UUID): List<DisputeStatusHistory> {
        findOwned(customerId, disputeId)
        return history.findAllByDisputeIdOrderByChangedAtAsc(disputeId)
    }

    @Transactional
    fun withdraw(customerId: UUID, disputeId: UUID, note: String?): Dispute {
        val dispute = findOwned(customerId, disputeId)
        changeStatus(dispute, DisputeStatus.WITHDRAWN, "customer:$customerId", note)
        return dispute
    }

    // Used by bank staff workflows. Customers withdraw through withdraw().
    @Transactional
    fun transition(disputeId: UUID, to: DisputeStatus, staffId: String, note: String?): Dispute {
        if (to == DisputeStatus.WITHDRAWN) {
            throw UnprocessableException("Only the customer can withdraw a dispute")
        }
        val dispute = disputes.findById(disputeId).orElseThrow { NotFoundException("Dispute not found") }
        changeStatus(dispute, to, "agent:$staffId", note)
        return dispute
    }

    private fun changeStatus(dispute: Dispute, to: DisputeStatus, changedBy: String, note: String?) {
        val from = dispute.status
        if (!from.canTransitionTo(to)) {
            throw ConflictException("Cannot move dispute from $from to $to")
        }
        val now = clock.instant()
        dispute.status = to
        dispute.updatedAt = now
        history.save(DisputeStatusHistory(dispute, from, to, changedBy, note, now))
    }

    private fun findOwned(customerId: UUID, disputeId: UUID): Dispute =
        disputes.findByIdAndCustomerId(disputeId, customerId)
            ?: throw NotFoundException("Dispute not found")

    private fun findCustomer(customerId: UUID): Customer =
        customers.findById(customerId).orElseThrow { NotFoundException("Customer not found") }

    private companion object {
        val DISPUTE_WINDOW: Duration = Duration.ofDays(120)
    }
}
