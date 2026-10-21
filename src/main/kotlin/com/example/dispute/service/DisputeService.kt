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

    /**
     * Submits a dispute for one of the customer's transactions.
     *
     * @param customerId the customer making the request
     * @param command the dispute details
     * @return the saved dispute
     * @throws NotFoundException if the customer does not exist, or the transaction does not belong to them
     * @throws UnprocessableException if the dispute window has closed or the amount is too high
     * @throws ConflictException if an open dispute already exists for the transaction
     */
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

    /**
     * Lists the customer's disputes, newest first.
     *
     * @param customerId the customer whose disputes to list
     * @return the disputes, newest first
     */
    @Transactional(readOnly = true)
    fun listFor(customerId: UUID): List<Dispute> =
        disputes.findAllByCustomerIdOrderByCreatedAtDesc(customerId)

    /**
     * Returns one of the customer's disputes.
     *
     * @param customerId the customer making the request
     * @param disputeId the dispute to return
     * @return the dispute
     * @throws NotFoundException if the dispute does not exist or belongs to another customer
     */
    @Transactional(readOnly = true)
    fun getFor(customerId: UUID, disputeId: UUID): Dispute = findOwned(customerId, disputeId)

    /**
     * Returns the status history of one of the customer's disputes, oldest first.
     *
     * @param customerId the customer making the request
     * @param disputeId the dispute whose history to return
     * @return the status changes, oldest first
     * @throws NotFoundException if the dispute is not owned by the customer
     */
    @Transactional(readOnly = true)
    fun historyFor(customerId: UUID, disputeId: UUID): List<DisputeStatusHistory> {
        findOwned(customerId, disputeId)
        return history.findAllByDisputeIdOrderByChangedAtAsc(disputeId)
    }

    /**
     * Withdraws one of the customer's disputes.
     *
     * @param customerId the customer making the request
     * @param disputeId the dispute to withdraw
     * @param note an optional note stored with the status change
     * @return the withdrawn dispute
     * @throws NotFoundException if the dispute is not owned by the customer
     * @throws ConflictException if the dispute cannot be withdrawn from its current status
     */
    @Transactional
    fun withdraw(customerId: UUID, disputeId: UUID, note: String?): Dispute {
        val dispute = findOwned(customerId, disputeId)
        changeStatus(dispute, DisputeStatus.WITHDRAWN, "customer:$customerId", note)
        return dispute
    }

    /**
     * Moves a dispute to a new status on behalf of bank staff.
     *
     * Customers withdraw through [withdraw] instead.
     *
     * @param disputeId the dispute to update
     * @param to the target status
     * @param staffId the staff member making the change
     * @param note an optional note stored with the status change
     * @return the updated dispute
     * @throws UnprocessableException if [to] is WITHDRAWN
     * @throws NotFoundException if the dispute does not exist
     * @throws ConflictException if the status change is not allowed
     */
    @Transactional
    fun transition(disputeId: UUID, to: DisputeStatus, staffId: String, note: String?): Dispute {
        if (to == DisputeStatus.WITHDRAWN) {
            throw UnprocessableException("Only the customer can withdraw a dispute")
        }
        val dispute = disputes.findById(disputeId).orElseThrow { NotFoundException("Dispute not found") }
        changeStatus(dispute, to, "agent:$staffId", note)
        return dispute
    }

    /**
     * Applies a status change and records it in the status history.
     *
     * @param dispute the dispute to update
     * @param to the target status
     * @param changedBy the actor, such as `customer:<id>` or `agent:<id>`
     * @param note an optional note stored with the change
     * @throws ConflictException if the transition is not allowed
     */
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

    /**
     * Finds a dispute owned by the customer.
     *
     * @param customerId the owning customer
     * @param disputeId the dispute to find
     * @return the dispute
     * @throws NotFoundException if no matching dispute exists
     */
    private fun findOwned(customerId: UUID, disputeId: UUID): Dispute =
        disputes.findByIdAndCustomerId(disputeId, customerId)
            ?: throw NotFoundException("Dispute not found")

    /**
     * Finds a customer by ID.
     *
     * @param customerId the customer to find
     * @return the customer
     * @throws NotFoundException if the customer does not exist
     */
    private fun findCustomer(customerId: UUID): Customer =
        customers.findById(customerId).orElseThrow { NotFoundException("Customer not found") }

    private companion object {
        val DISPUTE_WINDOW: Duration = Duration.ofDays(120)
    }
}
