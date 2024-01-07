package com.stoum.overlay

import com.google.gson.Gson
import com.stoum.overlay.entity.Game
import com.stoum.overlay.entity.GameType
import com.stoum.overlay.entity.Player
import com.stoum.overlay.model.GameInfo
import com.stoum.overlay.model.GameInfo.PlayerInfo
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.repository.PlayerRepository
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
        val emitterService: EmitterService,
    val gameRepository: GameRepository,
    val playerRepository: PlayerRepository
) : CommandLineRunner {
    val log: Logger = LoggerFactory.getLogger(this::class.java)

    override fun run(vararg args: String?) {
/*        val gameInfo = GameInfo()
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
        }*/
/*        val gameInfo = GameInfo()
        val playerInfo = PlayerInfo(
            "Stoum",
            "https://s3.vk-admin.com/gomafia/user/avatar/1685/ava_1660844652.jpg",
            "black",
            "killed" to "1",
            mutableListOf(mapOf("red" to "1")),
            mutableMapOf("red" to ("1" to "1"))
        )
        gameInfo.players.add(playerInfo)

        println(Gson().toJson(gameInfo))*/

        val game = Game(type = GameType.CUSTOM)
        gameRepository.save(game)
        repeat(10) {
            val player = Player(
                nickname = "Stoum$it",
                photoUrl = "https://s3.vk-admin.com/gomafia/user/avatar/1685/ava_1660844652.jpg",
                role = "black",
                place = it,
                status = "killed" to "$it",
                checks = mutableListOf(mapOf("first" to "red", "second" to "$it")),
                stat = mutableMapOf(
                    "red" to mapOf("first" to "${it * 10}", "second" to "${it * 10 - 5}"),
                    "black" to mapOf("first" to "${it * 10}", "second" to "${it * 10 - 5}"),
                    "sher" to mapOf("first" to "${it * 10}", "second" to "${it * 10 - 5}"),
                    "don" to mapOf("first" to "${it * 10}", "second" to "${it * 10 - 5}"),
                )
                //gameId = game.id!!
            )
            playerRepository.save(player)

            game.players.add(player)
        }


        gameRepository.save(game)

        println(game.id)
    }
}