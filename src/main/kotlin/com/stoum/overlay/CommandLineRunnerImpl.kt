package com.stoum.overlay

import com.stoum.overlay.repository.GamePlayerRepository
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
import com.stoum.overlay.service.gomafia.GomafiaRestClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
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
        //     gomafiaRestClient.getTournaments()
        //         .filter {
        //             LocalDate.parse(it.dateEnd)
        //                 .isAfter(LocalDate.of(2024, Month.JANUARY, 1))
        //         }
        //         .flatMap { listOfNotNull(it.id?.let { id -> gomafiaRestClient.getTournament(id.toInt()) }) }
        //         .flatMap { it.games }
        //         .filter { it.table.any { players.contains(it.id) } }
        //         .forEach { game ->
        //             val blackWin = game.win == "mafia"
        //             for (player1 in game.table.filter { players.contains(it.id) }) {
        //                 for (player2 in game.table.filter { players.contains(it.id) }) {
        //                     // println("Found ${player1.login} ${player2.login}")
        //                     val pair = normalizePair(Pair(player1.login!!, player2.login!!))
        //                     if (isRed(player1.role!!) && isRed(player2.role!!)) {
        //                         valueRedRedTotal.merge(pair, 1, Int::plus)
        //                     }
        //                     if (isRed(player1.role!!) && isRed(player2.role!!) && !blackWin) {
        //                         valueRedRedWin.merge(pair, 1, Int::plus)
        //                     }
        //                     if (!isRed(player1.role!!) && !isRed(player2.role!!)) {
        //                         valueBlackBlackTotal.merge(pair, 1, Int::plus)
        //                     }
        //                     if (!isRed(player1.role!!) && !isRed(player2.role!!) && blackWin) {
        //                         valueBlackBlackWin.merge(pair, 1, Int::plus)
        //                     }
        //                 }
        //             }
        //             for (player in game.table.filter { players.contains(it.id) }) {
        //                 val role = toPolemicaRole(player.role!!)
        //                 valueWinRates.merge(
        //                     Pair(player.login!!, role),
        //                     Pair(if (role.isBlack() == blackWin) 1 else 0, 1)
        //                 ) { acc, m -> Pair(acc.first + m.first, acc.second + m.second) }
        //             }
        //         }
        //
        //     println("----- Red-Red total")
        //     printCsv(valueRedRedTotal)
        //     println("----- Red-Red win")
        //     printCsv(valueRedRedWin)
        //     println("----- Black-Black total")
        //     printCsv(valueBlackBlackTotal)
        //     println("----- Black-Black win")
        //     printCsv(valueBlackBlackWin)
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
    //
    // private fun printCsv(value: MutableMap<Pair<String, String>, Int>) {
    //     println(value.map { "${it.key.first},${it.key.second},${it.value}" }
    //         .joinToString("\n") { it })
    // }
    //
    // fun toRoleWinStat(r: Role, m: Map<Role, Pair<Int, Int>>): List<Int> {
    //     val element = m.getOrDefault(r, Pair(0, 0))
    //     return listOf(element.first, element.second)
    // }
    //
    // fun toPolemicaRole(s: String): Role {
    //     return when (s) {
    //         "red" -> Role.PEACE
    //         "mafia" -> Role.MAFIA
    //         "sheriff" -> Role.SHERIFF
    //         "don" -> Role.DON
    //         else -> throw IllegalArgumentException("Unknown role: $s")
    //     }
    // }
    //
    // fun isRed(s: String): Boolean {
    //     return s == "red" || s == "sheriff"
    // }
    //
    // fun normalizePair(pair: Pair<String, String>): Pair<String, String> {
    //     val sorted = pair.toList().sorted()
    //     return Pair(sorted.first(), sorted.last())
    // }
}
