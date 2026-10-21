package com.example.dispute.api

import com.example.dispute.service.AuthService
import com.example.dispute.service.bearerTokenFrom
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(private val auth: AuthService) {

    /**
     * Exchanges an email and password for a bearer token.
     *
     * @param request the login credentials
     * @return the raw token and its expiry
     */
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse {
        val issued = auth.login(request.email, request.password)
        return LoginResponse(issued.token, issued.expiresAt)
    }

    /**
     * Revokes the bearer token that authenticated this request.
     *
     * @param authorization the Authorization header carrying the token
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String) {
        bearerTokenFrom(authorization)?.let { auth.revoke(it) }
    }
}
