package com.stoum.overlay

import com.google.gson.Gson
import com.stoum.overlay.model.GameInfo
import com.stoum.overlay.model.GameInfo.PlayerInfo
import com.stoum.overlay.service.EmitterService
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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
        val gameInfo = GameInfo()
        repeat((1..10).count()) {
            gameInfo.players.add(
                PlayerInfo(
                    "Stoum$it", "https://s3.vk-admin.com/gomafia/user/avatar/1685/ava_1660844652.jpg"
                )
            )
        }
        runBlocking {
            while (true)
                if (emitterService.hasEmitters()) {
                    delay(5000L)
                    emitterService.sendToAll("!gameinfo " + Gson().toJson(gameInfo))
                }
        }
    }
}