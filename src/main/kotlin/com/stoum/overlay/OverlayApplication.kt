package com.stoum.overlay

import com.stoum.overlay.config.ApplicationConfig
import com.stoum.overlay.config.MafiaUniverseConfig
import com.stoum.overlay.config.ObjectStorageConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(ApplicationConfig::class, ObjectStorageConfig::class, MafiaUniverseConfig::class)
class OverlayApplication

fun main(args: Array<String>) {
	runApplication<OverlayApplication>(*args)
}
