package com.example.dispute.service

import com.example.dispute.domain.Transaction
import com.example.dispute.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TransactionService(private val transactions: TransactionRepository) {

    @Transactional(readOnly = true)
    fun listFor(customerId: UUID): List<Transaction> =
        transactions.findAllByCardCustomerIdOrderByOccurredAtDesc(customerId)
}
