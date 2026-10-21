package com.example.dispute.service

import com.example.dispute.TestcontainersConfiguration
import com.example.dispute.domain.DisputeReason
import com.example.dispute.domain.DisputeStatus
import com.example.dispute.domain.Transaction
import com.example.dispute.repository.CardRepository
import com.example.dispute.repository.TransactionRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@SpringBootTest
@ActiveProfiles("dev")
@Import(TestcontainersConfiguration::class)
@Transactional
class DisputeServiceTest @Autowired constructor(
    private val service: DisputeService,
    private val cards: CardRepository,
    private val transactions: TransactionRepository,
    private val clock: Clock,
) {

    @Test
    fun `submits a dispute on the customer's own transaction`() {
        val dispute = service.submit(THANDI, command(DUPLICATE_CHARGE_TX, 129_900))

        assertEquals(DisputeStatus.SUBMITTED, dispute.status)
        assertEquals(
            listOf(DisputeStatus.SUBMITTED),
            service.historyFor(THANDI, dispute.id).map { it.toStatus },
        )
    }

    @Test
    fun `hides transactions owned by another customer`() {
        assertFailsWith<NotFoundException> {
            service.submit(THANDI, command(BEN_TX, 1_000))
        }
    }

    @Test
    fun `rejects a disputed amount above the transaction amount`() {
        assertFailsWith<UnprocessableException> {
            service.submit(THANDI, command(WOOLWORTHS_TX, 45_991))
        }
    }

    @Test
    fun `rejects disputes raised after the 120 day window`() {
        val card = cards.findAllByCustomerId(THANDI).first()
        val old = transactions.save(
            Transaction(
                UUID.randomUUID(), card, "Old Merchant", 10_000, "ZAR",
                clock.instant().minus(Duration.ofDays(121)), clock.instant(),
            ),
        )

        assertFailsWith<UnprocessableException> {
            service.submit(THANDI, command(old.id, 10_000))
        }
    }

    @Test
    fun `allows only one open dispute per transaction`() {
        service.submit(THANDI, command(DUPLICATE_CHARGE_TX, 129_900))

        assertFailsWith<ConflictException> {
            service.submit(THANDI, command(DUPLICATE_CHARGE_TX, 129_900))
        }
    }

    @Test
    fun `customer withdraws an open dispute`() {
        val dispute = service.withdraw(THANDI, SUBMITTED_DISPUTE, "No longer needed")

        assertEquals(DisputeStatus.WITHDRAWN, dispute.status)
        assertEquals(
            listOf(DisputeStatus.SUBMITTED, DisputeStatus.WITHDRAWN),
            service.historyFor(THANDI, SUBMITTED_DISPUTE).map { it.toStatus },
        )
    }

    @Test
    fun `customer cannot withdraw another customer's dispute`() {
        assertFailsWith<NotFoundException> {
            service.withdraw(BEN, SUBMITTED_DISPUTE, null)
        }
    }

    @Test
    fun `rejects withdrawing a closed dispute`() {
        assertFailsWith<ConflictException> {
            service.withdraw(THANDI, WITHDRAWN_DISPUTE, null)
        }
    }

    @Test
    fun `staff move a dispute through review to resolution`() {
        service.transition(SUBMITTED_DISPUTE, DisputeStatus.UNDER_REVIEW, "ops-01", "Picked up")
        val resolved = service.transition(
            SUBMITTED_DISPUTE, DisputeStatus.RESOLVED_ACCEPTED, "ops-01", "Refund approved",
        )

        assertEquals(DisputeStatus.RESOLVED_ACCEPTED, resolved.status)
    }

    @Test
    fun `staff cannot apply a transition the state machine does not allow`() {
        assertFailsWith<ConflictException> {
            service.transition(REJECTED_DISPUTE, DisputeStatus.UNDER_REVIEW, "ops-01", null)
        }
    }

    @Test
    fun `staff cannot withdraw a dispute on the customer's behalf`() {
        assertFailsWith<UnprocessableException> {
            service.transition(SUBMITTED_DISPUTE, DisputeStatus.WITHDRAWN, "ops-01", null)
        }
    }

    private fun command(transactionId: UUID, amountMinor: Long) = SubmitDisputeCommand(
        transactionId = transactionId,
        reason = DisputeReason.DUPLICATE_CHARGE,
        description = "Charged twice",
        disputedAmountMinor = amountMinor,
    )

    private companion object {
        val THANDI: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val BEN: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val WOOLWORTHS_TX: UUID = UUID.fromString("b0000000-0000-0000-0000-000000000001")
        val DUPLICATE_CHARGE_TX: UUID = UUID.fromString("b0000000-0000-0000-0000-000000000002")
        val BEN_TX: UUID = UUID.fromString("b0000000-0000-0000-0000-000000000006")
        val SUBMITTED_DISPUTE: UUID = UUID.fromString("d0000000-0000-0000-0000-000000000001")
        val WITHDRAWN_DISPUTE: UUID = UUID.fromString("d0000000-0000-0000-0000-000000000004")
        val REJECTED_DISPUTE: UUID = UUID.fromString("d0000000-0000-0000-0000-000000000003")
    }
}
