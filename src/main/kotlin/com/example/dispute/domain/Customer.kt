package com.example.dispute.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "customers")
class Customer(
    @Id
    val id: UUID,

    @Column(nullable = false, unique = true, length = 254)
    val email: String,

    @Column(nullable = false, length = 150)
    val fullName: String,

    @Column(nullable = false, length = 100)
    val passwordHash: String,

    @Column(nullable = false)
    val createdAt: Instant,
)
