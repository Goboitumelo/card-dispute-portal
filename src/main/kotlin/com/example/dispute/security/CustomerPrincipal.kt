package com.example.dispute.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

class CustomerPrincipal(
    val customerId: UUID,
    private val email: String,
    private val passwordHash: String,
) : UserDetails {

    /** Grants the customer role to every authenticated user. */
    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_CUSTOMER"))

    /** Returns the stored password hash. */
    override fun getPassword(): String = passwordHash

    /** Returns the customer's email, which is used as the username. */
    override fun getUsername(): String = email
}
