package com.example.dispute.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class ClockConfig {

    /**
     * Provides the clock used for all timestamps.
     *
     * @return the system clock in UTC
     */
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
