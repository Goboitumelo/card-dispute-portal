package com.example.dispute.security

import com.example.dispute.service.AuthService
import org.springframework.beans.factory.annotation.Value
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
import java.time.Clock
import java.time.Duration

@Configuration
@EnableWebSecurity
class SecurityConfig {

    /**
     * Provides the limiter that blocks clients after repeated failed logins.
     *
     * @param clock the clock used to time windows
     * @param maxFailures the failed logins allowed per window
     * @param window the length of each window
     * @return the login rate limiter
     */
    @Bean
    fun loginRateLimiter(
        clock: Clock,
        @Value("\${dispute.login-rate-limit.max-failures}") maxFailures: Int,
        @Value("\${dispute.login-rate-limit.window}") window: Duration,
    ): LoginRateLimiter = LoginRateLimiter(maxFailures, window, clock)

    /**
     * Configures stateless bearer-token authentication. Login and the health endpoint are public.
     * Login is rate limited per client; blocked clients receive 429.
     *
     * @param http the builder to configure
     * @param auth the service that resolves bearer tokens
     * @param loginRateLimiter the limiter applied to login attempts
     * @return the configured filter chain
     */
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        auth: AuthService,
        loginRateLimiter: LoginRateLimiter,
    ): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(LoginRateLimitFilter(loginRateLimiter), UsernamePasswordAuthenticationFilter::class.java)
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
