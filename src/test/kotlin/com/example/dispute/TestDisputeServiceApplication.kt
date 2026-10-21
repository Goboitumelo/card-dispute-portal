package com.example.dispute

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<DisputeServiceApplication>().with(TestcontainersConfiguration::class).run(*args)
}
