package com.example.dispute.api

import com.example.dispute.TestcontainersConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(properties = ["dispute.login-rate-limit.max-failures=3"])
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@Transactional
class LoginRateLimitTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @Test
    fun `login is blocked with 429 after too many failed attempts`() {
        repeat(3) {
            mockMvc.perform(login(FAILING_IP, THANDI_EMAIL, "wrong-password"))
                .andExpect(status().isUnauthorized)
        }

        mockMvc.perform(login(FAILING_IP, THANDI_EMAIL, THANDI_PASSWORD))
            .andExpect(status().isTooManyRequests)
            .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
    }

    @Test
    fun `successful logins do not count toward the limit`() {
        repeat(5) {
            mockMvc.perform(login(SUCCEEDING_IP, THANDI_EMAIL, THANDI_PASSWORD))
                .andExpect(status().isOk)
        }
    }

    @Test
    fun `a blocked client does not block other clients`() {
        repeat(3) {
            mockMvc.perform(login(ISOLATED_BLOCKED_IP, THANDI_EMAIL, "wrong-password"))
                .andExpect(status().isUnauthorized)
        }

        mockMvc.perform(login(ISOLATED_OTHER_IP, THANDI_EMAIL, THANDI_PASSWORD))
            .andExpect(status().isOk)
    }

    private fun login(ip: String, email: String, password: String) =
        post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"email":"$email","password":"$password"}""")
            .with { request ->
                request.remoteAddr = ip
                request
            }

    private companion object {
        const val THANDI_EMAIL = "thandi@example.com"
        const val THANDI_PASSWORD = "password"
        const val FAILING_IP = "203.0.113.10"
        const val SUCCEEDING_IP = "203.0.113.11"
        const val ISOLATED_BLOCKED_IP = "203.0.113.12"
        const val ISOLATED_OTHER_IP = "203.0.113.13"
    }
}
