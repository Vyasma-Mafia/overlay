package com.stoum.overlay

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OverlayApplication

fun main(args: Array<String>) {
	runApplication<OverlayApplication>(*args)
}
