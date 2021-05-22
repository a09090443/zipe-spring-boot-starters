package com.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ExampleKotlinApplication

fun main(args: Array<String>) {
    runApplication<ExampleKotlinApplication>(*args)
}
