package com.example.dispute.api

import com.example.dispute.TestcontainersConfiguration
import com.example.dispute.domain.AuthToken
import com.example.dispute.repository.AuthTokenRepository
import com.example.dispute.repository.CustomerRepository
import com.example.dispute.service.hashToken
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@Transactional
class AuthControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val customers: CustomerRepository,
    private val tokens: AuthTokenRepository,
) {

    @Test
    fun `login returns a token and its expiry`() {
        mockMvc.perform(login(THANDI_EMAIL, THANDI_PASSWORD))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").isNotEmpty)
            .andExpect(jsonPath("$.expiresAt").isNotEmpty)
    }

    @Test
    fun `login rejects a wrong password with 401`() {
        mockMvc.perform(login(THANDI_EMAIL, "wrong-password"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `login rejects an unknown email with 401`() {
        mockMvc.perform(login("nobody@example.com", THANDI_PASSWORD))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `login rejects a blank password with 400`() {
        mockMvc.perform(login(THANDI_EMAIL, " "))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `a bearer token authorizes protected endpoints`() {
        val token = issueToken()
        mockMvc.perform(get("/api/disputes").header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
            .andExpect(status().isOk)
    }

    @Test
    fun `basic credentials are no longer accepted`() {
        mockMvc.perform(get("/api/disputes").with(httpBasic(THANDI_EMAIL, THANDI_PASSWORD)))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `an unknown token is rejected with 401`() {
        mockMvc.perform(get("/api/disputes").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `an expired token is rejected with 401`() {
        val raw = "expired-token-for-test"
        val now = Instant.now()
        val customerId = customers.findByEmail(THANDI_EMAIL)!!.id
        tokens.save(AuthToken(hashToken(raw), customerId, now.minusSeconds(7200), now.minusSeconds(3600)))

        mockMvc.perform(get("/api/disputes").header(HttpHeaders.AUTHORIZATION, "Bearer $raw"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `logout revokes the token`() {
        val token = issueToken()

        mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
            .andExpect(status().isNoContent)
        mockMvc.perform(get("/api/disputes").header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `logout requires a token`() {
        mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isUnauthorized)
    }

    private fun login(email: String, password: String) =
        post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"email":"$email","password":"$password"}""")

    private fun issueToken(): String {
        val body = mockMvc.perform(login(THANDI_EMAIL, THANDI_PASSWORD))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString
        return JsonPath.read(body, "$.token")
    }

    private companion object {
        const val THANDI_EMAIL = "thandi@example.com"
        const val THANDI_PASSWORD = "password"
    }
}
