package com.example.dispute.api

import com.example.dispute.TestcontainersConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@Transactional
class DisputeControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @Test
    fun `rejects requests without credentials`() {
        mockMvc.perform(get("/api/disputes"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `lists only the caller's disputes`() {
        mockMvc.perform(get("/api/disputes").with(thandi()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
    }

    @Test
    fun `returns 404 for another customer's dispute`() {
        mockMvc.perform(get("/api/disputes/$BEN_DISPUTE").with(thandi()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `lists the customer's transactions across all cards`() {
        mockMvc.perform(get("/api/transactions").with(thandi()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(5))
    }

    @Test
    fun `submits a dispute and returns 201 with a location`() {
        mockMvc.perform(
            post("/api/disputes")
                .with(thandi())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"transactionId":"$DUPLICATE_CHARGE_TX","reason":"DUPLICATE_CHARGE",
                     "description":"Charged twice","disputedAmountMinor":129900}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.status").value("SUBMITTED"))
    }

    @Test
    fun `rejects a blank description with 400`() {
        mockMvc.perform(
            post("/api/disputes")
                .with(thandi())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"transactionId":"$DUPLICATE_CHARGE_TX","reason":"DUPLICATE_CHARGE",
                     "description":" ","disputedAmountMinor":129900}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `withdraws an open dispute`() {
        mockMvc.perform(post("/api/disputes/$SUBMITTED_DISPUTE/withdraw").with(thandi()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("WITHDRAWN"))
    }

    @Test
    fun `returns 409 when withdrawing a closed dispute`() {
        mockMvc.perform(post("/api/disputes/$WITHDRAWN_DISPUTE/withdraw").with(thandi()))
            .andExpect(status().isConflict)
    }

    private fun thandi() = httpBasic("thandi@example.com", "password")

    private companion object {
        const val DUPLICATE_CHARGE_TX = "b0000000-0000-0000-0000-000000000002"
        const val SUBMITTED_DISPUTE = "d0000000-0000-0000-0000-000000000001"
        const val WITHDRAWN_DISPUTE = "d0000000-0000-0000-0000-000000000004"
        const val BEN_DISPUTE = "d0000000-0000-0000-0000-000000000003"
    }
}
