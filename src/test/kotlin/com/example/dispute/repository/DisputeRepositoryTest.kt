package com.example.dispute.repository

import com.example.dispute.TestcontainersConfiguration
import com.example.dispute.domain.Card
import com.example.dispute.domain.Customer
import com.example.dispute.domain.Dispute
import com.example.dispute.domain.DisputeReason
import com.example.dispute.domain.DisputeStatus
import com.example.dispute.domain.DisputeStatusHistory
import com.example.dispute.domain.Transaction
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration::class)
class DisputeRepositoryTest @Autowired constructor(
    private val customers: CustomerRepository,
    private val cards: CardRepository,
    private val transactions: TransactionRepository,
    private val disputes: DisputeRepository,
    private val history: DisputeStatusHistoryRepository,
    private val em: EntityManager,
) {

    @Test
    fun `finds a customer by email`() {
        val customer = newCustomer("lookup@example.com")
        em.flush()
        em.clear()

        assertEquals(customer.id, customers.findByEmail("lookup@example.com")?.id)
    }

    @Test
    fun `lists a card's transactions newest first`() {
        val card = newCard(newCustomer())
        val older = newTransaction(card, occurredAt = NOW)
        val newer = newTransaction(card, occurredAt = NOW.plusSeconds(86_400))
        em.flush()
        em.clear()

        assertEquals(
            listOf(newer.id, older.id),
            transactions.findAllByCardIdOrderByOccurredAtDesc(card.id).map { it.id },
        )
    }

    @Test
    fun `persists a dispute with its status history in order`() {
        val customer = newCustomer()
        val dispute = newDispute(newTransaction(newCard(customer)), customer)
        history.save(
            DisputeStatusHistory(dispute, null, DisputeStatus.SUBMITTED, "customer", null, NOW),
        )
        history.saveAndFlush(
            DisputeStatusHistory(
                dispute, DisputeStatus.SUBMITTED, DisputeStatus.UNDER_REVIEW,
                "agent:ops-01", "Merchant statement requested", NOW.plusSeconds(60),
            ),
        )
        em.clear()

        assertEquals(
            listOf(dispute.id),
            disputes.findAllByCustomerIdOrderByCreatedAtDesc(customer.id).map { it.id },
        )
        assertEquals(
            listOf(DisputeStatus.SUBMITTED, DisputeStatus.UNDER_REVIEW),
            history.findAllByDisputeIdOrderByChangedAtAsc(dispute.id).map { it.toStatus },
        )
    }

    @Test
    fun `rejects a second open dispute on the same transaction`() {
        val customer = newCustomer()
        val transaction = newTransaction(newCard(customer))
        newDispute(transaction, customer, DisputeStatus.SUBMITTED)

        assertFailsWith<DataIntegrityViolationException> {
            newDispute(transaction, customer, DisputeStatus.UNDER_REVIEW)
        }
    }

    @Test
    fun `allows a new dispute once the previous one is closed`() {
        val customer = newCustomer()
        val transaction = newTransaction(newCard(customer))
        newDispute(transaction, customer, DisputeStatus.RESOLVED_REJECTED)
        assertFalse(disputes.existsByTransactionIdAndStatusIn(transaction.id, DisputeStatus.OPEN))

        newDispute(transaction, customer, DisputeStatus.SUBMITTED)

        assertTrue(disputes.existsByTransactionIdAndStatusIn(transaction.id, DisputeStatus.OPEN))
    }

    @Test
    fun `rejects an update based on a stale version`() {
        val customer = newCustomer()
        val dispute = newDispute(newTransaction(newCard(customer)), customer)
        em.clear()

        val stale = disputes.findById(dispute.id).get()
        em.detach(stale)
        val fresh = disputes.findById(dispute.id).get()
        fresh.status = DisputeStatus.UNDER_REVIEW
        disputes.saveAndFlush(fresh)

        stale.status = DisputeStatus.WITHDRAWN
        assertFailsWith<ObjectOptimisticLockingFailureException> {
            disputes.saveAndFlush(stale)
        }
    }

    private fun newCustomer(email: String = "${UUID.randomUUID()}@example.com"): Customer =
        customers.save(Customer(UUID.randomUUID(), email, "Test Customer", "hash", NOW))

    private fun newCard(customer: Customer): Card =
        cards.save(Card(UUID.randomUUID(), customer, "4242", NOW))

    private fun newTransaction(card: Card, occurredAt: Instant = NOW): Transaction =
        transactions.save(
            Transaction(UUID.randomUUID(), card, "Test Merchant", 10_000, "ZAR", occurredAt, NOW),
        )

    private fun newDispute(
        transaction: Transaction,
        customer: Customer,
        status: DisputeStatus = DisputeStatus.SUBMITTED,
    ): Dispute =
        disputes.saveAndFlush(
            Dispute(
                UUID.randomUUID(), transaction, customer, DisputeReason.DUPLICATE_CHARGE,
                "Charged twice", 5_000, status, NOW, NOW,
            ),
        )

    private companion object {
        val NOW: Instant = Instant.parse("2026-09-01T10:00:00Z")
    }
}
