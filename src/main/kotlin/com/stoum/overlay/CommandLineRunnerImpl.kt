package com.stoum.overlay

import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.stoum.overlay.repository.GamePlayerRepository
import com.stoum.overlay.repository.GameRepository
import com.stoum.overlay.service.EmitterService
import com.stoum.overlay.service.gomafia.GomafiaRestClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("dev")
class CommandLineRunnerImpl(
        val emitterService: EmitterService,
        val gameRepository: GameRepository,
        val gamePlayerRepository: GamePlayerRepository,
    val gomafiaRestClient: GomafiaRestClient
) : CommandLineRunner {
    val log: Logger = LoggerFactory.getLogger(this::class.java)

    override fun run(vararg args: String?) {
        // Player list: ID -> Nickname
        val playerMap = mapOf(
            91 to "Воробушек",
            319 to "Милкис",
            972 to "Lemona",
            1401 to "Неон",
            2422 to "Мэд",
            2038 to "Хокаге",
            4970 to "Баунти",
            1776 to "Грозный",
            1420 to "Кудрявый",
            6861 to "Ave 17"
        )
        val playerIds = playerMap.keys.toSet()

        // Statistics maps
        val redRedTotal: MutableMap<Pair<String, String>, Int> = hashMapOf()
        val redRedWin: MutableMap<Pair<String, String>, Int> = hashMapOf()
        val blackBlackTotal: MutableMap<Pair<String, String>, Int> = hashMapOf()
        val blackBlackWin: MutableMap<Pair<String, String>, Int> = hashMapOf()

        log.info("Fetching all tournaments...")
        val tournaments = gomafiaRestClient.getTournaments()
        log.info("Found ${tournaments.size} tournaments")

        tournaments.forEachIndexed { index, tournament ->
            val tournamentId = tournament.id ?: return@forEachIndexed
            log.info("Processing tournament ${index + 1}/${tournaments.size}: ID=$tournamentId")

            try {
                val tournamentData = gomafiaRestClient.getTournament(tournamentId.toInt())

                tournamentData.games.forEach { game ->
                    val blackWin = game.win == "mafia"
                    val redWin = !blackWin

                    // Find players from our list in this game
                    val playersInGame = game.table
                        .filter { it.id != null && it.id in playerIds && it.role != null }
                        .map { it.id!! to it.role!! }
                        .toMap()

                    // Process all pairs of players from our list that are in this game
                    val playerIdsInGame = playersInGame.keys.toList()
                    for (i in playerIdsInGame.indices) {
                        for (j in (i + 1) until playerIdsInGame.size) {
                            val player1Id = playerIdsInGame[i]
                            val player2Id = playerIdsInGame[j]
                            val player1Role = playersInGame[player1Id]!!
                            val player2Role = playersInGame[player2Id]!!
                            val player1Nickname = playerMap[player1Id]!!
                            val player2Nickname = playerMap[player2Id]!!
                            val pair = normalizePair(Pair(player1Nickname, player2Nickname))

                            // Check if both are red/sheriff
                            if (isRed(player1Role) && isRed(player2Role)) {
                                redRedTotal.merge(pair, 1) { old, new -> old + new }
                                if (redWin) {
                                    redRedWin.merge(pair, 1) { old, new -> old + new }
                                }
                            }

                            // Check if both are mafia/don
                            if ((player1Role == "mafia" || player1Role == "don") &&
                                (player2Role == "mafia" || player2Role == "don")
                            ) {
                                blackBlackTotal.merge(pair, 1) { old, new -> old + new }
                                if (blackWin) {
                                    blackBlackWin.merge(pair, 1) { old, new -> old + new }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                log.warn("Error processing tournament $tournamentId: ${e.message}")
            }
        }

        // Print results
        println("----- Red-Red total")
        printCsv(redRedTotal)
        println("----- Red-Red win")
        printCsv(redRedWin)
        println("----- Black-Black total")
        printCsv(blackBlackTotal)
        println("----- Black-Black win")
        printCsv(blackBlackWin)
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

    private fun printTsv(value: MutableMap<Pair<String, String>, Int>) {
        println(value.map { "${it.key.first}\t${it.key.second}\t${it.value}" }
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
