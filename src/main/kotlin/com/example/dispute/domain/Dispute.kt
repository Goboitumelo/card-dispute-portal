package com.example.dispute.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "disputes")
class Dispute(
    @Id
    val id: UUID,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    val transaction: Transaction,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    val customer: Customer,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    val reason: DisputeReason,

    @Column(nullable = false, length = 1000)
    val description: String,

    @Column(nullable = false)
    val disputedAmountMinor: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: DisputeStatus,

    @Column(nullable = false)
    val createdAt: Instant,

    @Column(nullable = false)
    var updatedAt: Instant,
) {
    // A null version tells Spring Data that the entity is new, so save() calls persist().
    @Version
    var version: Long? = null
        protected set
}
