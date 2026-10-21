package com.example.dispute.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "transactions")
class Transaction(
    @Id
    val id: UUID,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    val card: Card,

    @Column(nullable = false, length = 150)
    val merchantName: String,

    // Minor units (e.g. cents) to avoid floating-point rounding.
    @Column(nullable = false)
    val amountMinor: Long,

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    val currency: String,

    @Column(nullable = false)
    val occurredAt: Instant,

    @Column(nullable = false)
    val createdAt: Instant,
)
