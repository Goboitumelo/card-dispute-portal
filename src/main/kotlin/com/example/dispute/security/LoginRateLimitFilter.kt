package com.example.dispute.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Rejects login attempts with 429 while a client is blocked, and counts each failed login (401) against that client.
 */
class LoginRateLimitFilter(private val limiter: LoginRateLimiter) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.method != "POST" || request.requestURI != LOGIN_PATH

    /**
     * Blocks a client that has used up its failures, otherwise runs the login and records a failure on 401.
     *
     * @param request the incoming request
     * @param response the outgoing response
     * @param filterChain the rest of the filter chain
     */
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val client = request.remoteAddr
        val retryAfter = limiter.retryAfter(client)
        if (retryAfter != null) {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.setHeader(HttpHeaders.RETRY_AFTER, retryAfter.seconds.coerceAtLeast(1).toString())
            return
        }
        filterChain.doFilter(request, response)
        if (response.status == HttpStatus.UNAUTHORIZED.value()) {
            limiter.recordFailure(client)
        }
    }

    private companion object {
        const val LOGIN_PATH = "/api/auth/login"
    }
}
