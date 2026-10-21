package com.example.dispute.service

import com.example.dispute.domain.AuthToken
import com.example.dispute.repository.AuthTokenRepository
import com.example.dispute.repository.CustomerRepository
import com.example.dispute.security.CustomerPrincipal
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID

class InvalidCredentialsException : RuntimeException("Email or password is incorrect.")

/**
 * A newly issued bearer token.
 *
 * @property token the raw token, returned to the client once
 * @property expiresAt when the token stops being accepted
 */
data class IssuedToken(val token: String, val expiresAt: Instant)

@Service
class AuthService(
    private val customers: CustomerRepository,
    private val tokens: AuthTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock,
) {
    private val random = SecureRandom()

    // Hashed against when the email is unknown, so response time does not reveal which emails exist.
    private val unknownCustomerHash: String =
        requireNotNull(passwordEncoder.encode(UUID.randomUUID().toString()))

    /**
     * Verifies the credentials and issues a new bearer token.
     *
     * @param email the customer's email address
     * @param password the customer's password
     * @return the raw token and its expiry
     * @throws InvalidCredentialsException if the email or password is wrong
     */
    @Transactional
    fun login(email: String, password: String): IssuedToken {
        val customer = customers.findByEmail(email)
        val passwordMatches = passwordEncoder.matches(password, customer?.passwordHash ?: unknownCustomerHash)
        if (customer == null || !passwordMatches) throw InvalidCredentialsException()

        val raw = newRawToken()
        val now = clock.instant()
        val expiresAt = now.plus(TOKEN_LIFETIME)
        tokens.save(AuthToken(hashToken(raw), customer.id, now, expiresAt))
        return IssuedToken(raw, expiresAt)
    }

    /**
     * Resolves a raw bearer token to the customer it was issued to.
     *
     * @param rawToken the token presented by the client
     * @return the principal, or null if the token is unknown, expired, or its customer is gone
     */
    @Transactional(readOnly = true)
    fun authenticate(rawToken: String): CustomerPrincipal? {
        val token = tokens.findById(hashToken(rawToken)).orElse(null) ?: return null
        if (!token.expiresAt.isAfter(clock.instant())) return null
        val customer = customers.findById(token.customerId).orElse(null) ?: return null
        return CustomerPrincipal(customer.id, customer.email, customer.passwordHash)
    }

    /**
     * Revokes a raw bearer token. Unknown tokens are ignored.
     *
     * @param rawToken the token to revoke
     */
    @Transactional
    fun revoke(rawToken: String) {
        tokens.findById(hashToken(rawToken)).ifPresent { tokens.delete(it) }
    }

    private fun newRawToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private companion object {
        const val TOKEN_BYTES = 32
        val TOKEN_LIFETIME: Duration = Duration.ofHours(12)
    }
}

const val BEARER_PREFIX = "Bearer "

/**
 * Hashes a raw token with SHA-256 so that only the digest is stored.
 *
 * @param rawToken the raw token
 * @return the lowercase hex digest, 64 characters
 */
fun hashToken(rawToken: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(rawToken.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

/**
 * Extracts the token from an Authorization header.
 *
 * @param header the Authorization header value, or null
 * @return the token, or null if the header is missing or is not a non-empty Bearer header
 */
fun bearerTokenFrom(header: String?): String? =
    header
        ?.takeIf { it.startsWith(BEARER_PREFIX) }
        ?.removePrefix(BEARER_PREFIX)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
