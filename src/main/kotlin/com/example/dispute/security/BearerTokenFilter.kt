package com.example.dispute.security

import com.example.dispute.service.AuthService
import com.example.dispute.service.bearerTokenFrom
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Authenticates requests that carry a valid bearer token. Unknown or expired tokens leave the
 * request anonymous, so protected endpoints still reject it with 401.
 */
class BearerTokenFilter(private val auth: AuthService) : OncePerRequestFilter() {

    /**
     * Sets the security context when the request presents a valid token.
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
        val principal = bearerTokenFrom(request.getHeader(HttpHeaders.AUTHORIZATION))
            ?.let { auth.authenticate(it) }
        if (principal != null) {
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities)
        }
        filterChain.doFilter(request, response)
    }
}
