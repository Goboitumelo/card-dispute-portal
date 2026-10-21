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
@Table(name = "cards")
class Card(
    @Id
    val id: UUID,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    val customer: Customer,

    // Only the last four digits are ever stored.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 4)
    val lastFour: String,

    @Column(nullable = false)
    val createdAt: Instant,
)
