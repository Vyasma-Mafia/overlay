package com.stoum.overlay

import com.stoum.overlay.service.EmitterService
import org.apache.logging.log4j.LogBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class CommandLineRunnerImpl(
        val emitterService: EmitterService
) : CommandLineRunner {
    val log: Logger = LoggerFactory.getLogger(this::class.java)

    override fun run(vararg args: String?) {
        do {
            if(emitterService.emitters != null) {
                emitterService.emitters.forEach { (id, emitter) ->
                    log.info("sending to $id")
                    emitter.send("Hello $id")
                }
            }
        } while (emitterService.emitters.isEmpty())
    }
}