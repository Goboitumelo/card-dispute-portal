package com.example.dispute.repository

import com.example.dispute.domain.AuthToken
import com.example.dispute.domain.Card
import com.example.dispute.domain.Customer
import com.example.dispute.domain.Dispute
import com.example.dispute.domain.DisputeStatus
import com.example.dispute.domain.DisputeStatusHistory
import com.example.dispute.domain.Transaction
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CustomerRepository : JpaRepository<Customer, UUID> {
    /** Finds a customer by email, or null if none exists. */
    fun findByEmail(email: String): Customer?
}

interface AuthTokenRepository : JpaRepository<AuthToken, String>

interface CardRepository : JpaRepository<Card, UUID> {
    /** Lists the cards owned by a customer. */
    fun findAllByCustomerId(customerId: UUID): List<Card>
}

interface TransactionRepository : JpaRepository<Transaction, UUID> {
    /** Lists a card's transactions, newest first. */
    fun findAllByCardIdOrderByOccurredAtDesc(cardId: UUID): List<Transaction>

    /** Lists a customer's transactions, newest first, loading each card in the same query. */
    @EntityGraph(attributePaths = ["card"])
    fun findAllByCardCustomerIdOrderByOccurredAtDesc(customerId: UUID): List<Transaction>

    /** Finds a transaction owned by the customer, or null if it belongs to someone else. */
    fun findByIdAndCardCustomerId(id: UUID, customerId: UUID): Transaction?
}

interface DisputeRepository : JpaRepository<Dispute, UUID> {
    /** Lists the customer's disputes, newest first. */
    fun findAllByCustomerIdOrderByCreatedAtDesc(customerId: UUID): List<Dispute>

    /** Finds a dispute owned by the customer, or null if it belongs to someone else. */
    fun findByIdAndCustomerId(id: UUID, customerId: UUID): Dispute?

    /** Checks whether the transaction has a dispute in any of the given statuses. */
    fun existsByTransactionIdAndStatusIn(transactionId: UUID, statuses: Collection<DisputeStatus>): Boolean
}

interface DisputeStatusHistoryRepository : JpaRepository<DisputeStatusHistory, Long> {
    /** Lists a dispute's status changes, oldest first. */
    fun findAllByDisputeIdOrderByChangedAtAsc(disputeId: UUID): List<DisputeStatusHistory>
}
