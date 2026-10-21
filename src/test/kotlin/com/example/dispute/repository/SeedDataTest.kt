package com.example.dispute.repository

import com.example.dispute.TestcontainersConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals

@DataJpaTest(properties = ["spring.flyway.locations=classpath:db/migration,classpath:db/seed"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration::class)
class SeedDataTest @Autowired constructor(
    private val customers: CustomerRepository,
    private val cards: CardRepository,
    private val transactions: TransactionRepository,
    private val disputes: DisputeRepository,
) {

    @Test
    fun `dev seed loads customers, cards, transactions and disputes`() {
        assertEquals(2, customers.count())
        assertEquals(3, cards.count())
        assertEquals(6, transactions.count())
        assertEquals(4, disputes.count())
    }
}
