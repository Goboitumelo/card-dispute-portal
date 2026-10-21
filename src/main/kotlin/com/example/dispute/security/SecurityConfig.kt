package com.example.dispute.security

import com.example.dispute.service.AuthService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig {

    /**
     * Configures stateless bearer-token authentication. Login and the health endpoint are public.
     *
     * @param http the builder to configure
     * @param auth the service that resolves bearer tokens
     * @return the configured filter chain
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity, auth: AuthService): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(BearerTokenFilter(auth), UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling { it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) }
            .authorizeHttpRequests {
                it.requestMatchers("/actuator/health", "/api/auth/login").permitAll()
                    .anyRequest().authenticated()
            }
            .build()

    /**
     * Provides the encoder used to verify customer passwords.
     *
     * @return a BCrypt password encoder
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
