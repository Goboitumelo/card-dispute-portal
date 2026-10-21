package com.example.dispute.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** A hashed bearer token. The raw token is never stored. */
@Entity
@Table(name = "auth_tokens")
class AuthToken(
    @Id
    @Column(name = "token_hash", length = 64)
    val tokenHash: String,

    @Column(name = "customer_id", nullable = false)
    val customerId: UUID,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,
)
