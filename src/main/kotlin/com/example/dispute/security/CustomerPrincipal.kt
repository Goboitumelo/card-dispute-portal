package com.example.dispute.security

import com.example.dispute.repository.CustomerRepository
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.util.UUID

class CustomerPrincipal(
    val customerId: UUID,
    private val email: String,
    private val passwordHash: String,
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_CUSTOMER"))

    override fun getPassword(): String = passwordHash

    override fun getUsername(): String = email
}

@Service
class CustomerUserDetailsService(private val customers: CustomerRepository) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val customer = customers.findByEmail(username)
            ?: throw UsernameNotFoundException("No customer with that email")
        return CustomerPrincipal(customer.id, customer.email, customer.passwordHash)
    }
}
