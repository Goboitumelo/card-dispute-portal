package com.example.dispute.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "dispute_status_history")
class DisputeStatusHistory(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dispute_id", nullable = false)
    val dispute: Dispute,

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    val fromStatus: DisputeStatus?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val toStatus: DisputeStatus,

    @Column(nullable = false, length = 100)
    val changedBy: String,

    @Column(length = 500)
    val note: String?,

    @Column(nullable = false)
    val changedAt: Instant,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
        protected set
}
