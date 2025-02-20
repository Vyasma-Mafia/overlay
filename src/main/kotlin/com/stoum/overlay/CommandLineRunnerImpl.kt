package com.stoum.overlay

import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.stoum.overlay.repository.GamePlayerRepository
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
import com.stoum.overlay.service.gomafia.GomafiaRestClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    value = ["app.development", "app.crawlScheduler.enable"],
    havingValue = "true"
)
class CommandLineRunnerImpl(
        val emitterService: EmitterService,
        val gameRepository: GameRepository,
        val gamePlayerRepository: GamePlayerRepository,
        val gomafiaRestClient: GomafiaRestClient
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

        /*        val game = Game(type = GameType.CUSTOM)
        gameRepository.save(game)
        repeat(10) {
            val player = Player(
                nickname = "Stoum$it",
                photoUrl = "https://s3.vk-admin.com/gomafia/user/avatar/1685/ava_1660844652.jpg",
                role = "red",
                place = it,
                //status = "killed" to "$it",
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

        println(game.id)*/

        /* val userWithStats = gomafiaRestClient.getUserWithStats(575)

        println(userWithStats)*/
        // val counter: MutableMap<String, Pair<Int, Int>> = mutableMapOf()
        //
        // gomafiaRestClient.getTournament(1915).games.forEach { game ->
        //     val blackWin = game.win == "mafia"
        //     for (player in game.table) {
        //         counter.merge(
        //             player.login!!,
        //             Pair(
        //                 if (!blackWin && isRed(player.role!!)) 1 else 0,
        //                 if (isRed(player.role!!)) 1 else 0
        //             )
        //         ) { (acf, acs), (itf, its) ->
        //             Pair(acf + itf, acs + its)
        //         }
        //     }
        // }
        //
        // println(counter.map { it.key to it.value.first.toDouble() / it.value.second.toDouble() }
        //     .sortedByDescending { it.second })

        //     val players = listOf(91, 2304, 45, 1502, 892, 276, 319, 2075, 127, 768)
        //     val valueRedRedTotal: MutableMap<Pair<String, String>, Int> = hashMapOf()
        //     val valueRedRedWin: MutableMap<Pair<String, String>, Int> = hashMapOf()
        //     val valueBlackBlackTotal: MutableMap<Pair<String, String>, Int> = hashMapOf()
        //     val valueBlackBlackWin: MutableMap<Pair<String, String>, Int> = hashMapOf()
        //     val valueWinRates: MutableMap<Pair<String, Role>, Pair<Int, Int>> = hashMapOf()
        // val positionSimpleStats = mutableMapOf<Position, SimpleStat>()
        // Position.entries.forEach { position ->
        //     positionSimpleStats[position] = SimpleStat()
        // }
        // gomafiaRestClient.getTournaments()
        //     .flatMap { listOfNotNull(it.id?.let { id -> gomafiaRestClient.getTournament(id.toInt()) }) }
        //     .flatMap { it.games }
        //     .forEach { game ->
        //         val blackWin = game.win == "mafia"
        //
        //         for (player in game.table) {
        //             positionSimpleStats[Position.fromInt(player.place!!)]?.let { stat ->
        //                 val role = toPolemicaRole(player.role!!)
        //                 stat.red += if (role.isRed()) 1 else 0
        //                 stat.black += if (role.isBlack()) 1 else 0
        //                 stat.redWin += if (!blackWin && role.isRed()) 1 else 0
        //                 stat.blackWin += if (blackWin && role.isBlack()) 1 else 0
        //             }
        //         }
        //     }
        // println(positionSimpleStats)
        //
        // println("----- Red-Red total")
        // printCsv(valueRedRedTotal)
        // println("----- Red-Red win")
        // printCsv(valueRedRedWin)
        // println("----- Black-Black total")
        // printCsv(valueBlackBlackTotal)
        // println("----- Black-Black win")
        // printCsv(valueBlackBlackWin)
        //
        //     println("----- Win rates")
        //     val ans = valueWinRates.entries
        //         .groupBy { it.key.first }
        //         .mapValues { m ->
        //             m.value.groupBy { it.key.second }
        //                 .mapValues { it.value.sumOf { it.value.first } to it.value.sumOf { it.value.second } }
        //                 .map { it.key to it.value }
        //                 .toMap()
        //         }
        //         .map {
        //             listOf(
        //                 listOf(it.key),
        //                 toRoleWinStat(Role.PEACE, it.value),
        //                 toRoleWinStat(Role.SHERIFF, it.value),
        //                 toRoleWinStat(Role.MAFIA, it.value),
        //                 toRoleWinStat(Role.DON, it.value)
        //             ).flatten().joinToString(",") { it.toString() }
        //         }
        //         .joinToString("\n") { it }
        //     println(ans)
    }

    data class SimpleStat(
        var red: Long = 0,
        var black: Long = 0,
        var redWin: Long = 0,
        var blackWin: Long = 0
    ) {
        fun winRate(): Double {
            return (redWin.toDouble() + blackWin) / (red + black)
        }

        fun redWinRate(): Double {
            return redWin.toDouble() / red
        }

        fun blackWinRate(): Double {
            return blackWin.toDouble() / black
        }
    }

    private fun printCsv(value: MutableMap<Pair<String, String>, Int>) {
        println(value.map { "${it.key.first},${it.key.second},${it.value}" }
            .joinToString("\n") { it })
    }

    fun toRoleWinStat(r: Role, m: Map<Role, Pair<Int, Int>>): List<Int> {
        val element = m.getOrDefault(r, Pair(0, 0))
        return listOf(element.first, element.second)
    }

    fun toPolemicaRole(s: String): Role {
        return when (s) {
            "red" -> Role.PEACE
            "mafia" -> Role.MAFIA
            "sheriff" -> Role.SHERIFF
            "don" -> Role.DON
            else -> throw IllegalArgumentException("Unknown role: $s")
        }
    }

    fun isRed(s: String): Boolean {
        return s == "red" || s == "sheriff"
    }

    fun normalizePair(pair: Pair<String, String>): Pair<String, String> {
        val sorted = pair.toList().sorted()
        return Pair(sorted.first(), sorted.last())
    }
}
