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

    /** Grants the customer role to every authenticated user. */
    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_CUSTOMER"))

    /** Returns the stored password hash. */
    override fun getPassword(): String = passwordHash

    /** Returns the customer's email, which is used as the username. */
    override fun getUsername(): String = email
}

@Service
class CustomerUserDetailsService(private val customers: CustomerRepository) : UserDetailsService {

    /**
     * Loads a customer by email for authentication.
     *
     * @param username the customer's email address
     * @return the principal for the customer
     * @throws UsernameNotFoundException if no customer has that email
     */
    override fun loadUserByUsername(username: String): UserDetails {
        val customer = customers.findByEmail(username)
            ?: throw UsernameNotFoundException("No customer with that email")
        return CustomerPrincipal(customer.id, customer.email, customer.passwordHash)
    }
}
