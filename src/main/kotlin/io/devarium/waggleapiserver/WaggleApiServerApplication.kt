package io.devarium.waggleapiserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WaggleApiServerApplication

fun main(args: Array<String>) {
    runApplication<WaggleApiServerApplication>(*args)
}
