package com.example.dispute.repository

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
    fun findByEmail(email: String): Customer?
}

interface CardRepository : JpaRepository<Card, UUID> {
    fun findAllByCustomerId(customerId: UUID): List<Card>
}

interface TransactionRepository : JpaRepository<Transaction, UUID> {
    fun findAllByCardIdOrderByOccurredAtDesc(cardId: UUID): List<Transaction>

    @EntityGraph(attributePaths = ["card"])
    fun findAllByCardCustomerIdOrderByOccurredAtDesc(customerId: UUID): List<Transaction>

    fun findByIdAndCardCustomerId(id: UUID, customerId: UUID): Transaction?
}

interface DisputeRepository : JpaRepository<Dispute, UUID> {
    fun findAllByCustomerIdOrderByCreatedAtDesc(customerId: UUID): List<Dispute>

    fun findByIdAndCustomerId(id: UUID, customerId: UUID): Dispute?

    fun existsByTransactionIdAndStatusIn(transactionId: UUID, statuses: Collection<DisputeStatus>): Boolean
}

interface DisputeStatusHistoryRepository : JpaRepository<DisputeStatusHistory, Long> {
    fun findAllByDisputeIdOrderByChangedAtAsc(disputeId: UUID): List<DisputeStatusHistory>
}
