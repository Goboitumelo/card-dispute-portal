package com.example.dispute

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DisputeServiceApplication

/**
 * Starts the dispute service.
 *
 * @param args command-line arguments, passed through to Spring Boot
 */
fun main(args: Array<String>) {
	runApplication<DisputeServiceApplication>(*args)
}
