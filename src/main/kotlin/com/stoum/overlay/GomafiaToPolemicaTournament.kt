package com.stoum.overlay

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaPlayer
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaUser
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.stoum.overlay.service.gomafia.GomafiaRestClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
@Profile(value = ["dev"])
@ConditionalOnProperty(value = ["app.gomafiaToPolemicaTournament.enabled"], havingValue = "true")
class GomafiaToPolemicaTournament(
    val polemicaClient: PolemicaClient,
    val gomafiaRestClient: GomafiaRestClient,
    val objectMapper: ObjectMapper
) : CommandLineRunner {
    val log: Logger = LoggerFactory.getLogger(this::class.java)

    override fun run(vararg args: String?) {
        val gomafiaPlayers = hashSetOf<String>()
        val gomafiaToPolemicaNicknameMap = createNicknameMap()
        val polemicaMembers: Map<String, PolemicaClient.PolemicaCompetitionMember> =
            polemicaClient.getCompetitionMembers(3359).associateBy { it.player.username }
        val gomafiaToPolemicaPlayer: Map<String, PolemicaUser> = gomafiaToPolemicaNicknameMap
            .mapValues {
                polemicaMembers[it.value]?.player ?: throw IllegalStateException("Player ${it.value} not found")
            }

        val gameNumToStartedDate = mutableMapOf<Int, LocalDateTime>()
        for (i in 1..4) {
            gameNumToStartedDate[i] = LocalDateTime.of(2025, 4, 12, 10 + i, 0, 0)
        }
        for (i in 5..8) {
            gameNumToStartedDate[i] = LocalDateTime.of(2025, 4, 12, 10 + i, 30, 0)
        }
        for (i in 9..12) {
            gameNumToStartedDate[i] = LocalDateTime.of(2025, 4, 13, 10 + i - 8, 0, 0)
        }
        for (i in 13..15) {
            gameNumToStartedDate[i] = LocalDateTime.of(2025, 4, 13, 10 + i - 8, 30, 0)
        }

        val gameNumTableNumToMaster = mutableMapOf<Pair<Int, Int>, Int>()
        for (i in 1..15) {
            gameNumTableNumToMaster[Pair(i, 1)] = 86129
            gameNumTableNumToMaster[Pair(i, 2)] = 89123
            gameNumTableNumToMaster[Pair(i, 3)] = 57556
            gameNumTableNumToMaster[Pair(i, 4)] = 98224
            gameNumTableNumToMaster[Pair(i, 5)] = 63218
            gameNumTableNumToMaster[Pair(i, 6)] = 60904
            gameNumTableNumToMaster[Pair(i, 7)] = 70516
            gameNumTableNumToMaster[Pair(i, 8)] = 59762
            gameNumTableNumToMaster[Pair(i, 9)] = 65678
            gameNumTableNumToMaster[Pair(i, 10)] = 60758
        }

        for (i in 9..15) {
            gameNumTableNumToMaster[Pair(i, 6)] = 86667
            gameNumTableNumToMaster[Pair(i, 7)] = 70516
        }

        for (i in 1..2) {
            gameNumTableNumToMaster[Pair(i, 1)] = 63218
            gameNumTableNumToMaster[Pair(i, 5)] = 86129
        }

        val admins: Map<Long, PolemicaUser> =
            polemicaClient.getCompetitionAdmins(3359).associateBy { it.player.id }.mapValues { it.value.player }

        gomafiaRestClient.getTournament(1918)
            .games
            .sortedBy { it.tableNum }
            .sortedBy { it.gameNum }
            .forEach {
                // gomafiaPlayers.addAll(it.table.mapNotNull { it.login })
                val players = it.table.map {
                    println(it.login)
                    PolemicaPlayer(
                        position = Position.fromInt(it.place!!)!!,
                        username = gomafiaToPolemicaPlayer[it.login]!!.username,
                        role = Role.PEACE,
                        techs = listOf(),
                        fouls = listOf(),
                        guess = null,
                        player = gomafiaToPolemicaPlayer[it.login],
                        disqual = null,
                        award = null
                    )
                }
                val master = admins[gameNumTableNumToMaster[Pair(it.gameNum, it.tableNum)]!!.toLong()]!!
                val polemicaGame = PolemicaGame(
                    id = null,
                    master = master.id,
                    referee = master,
                    scoringVersion = "3.0",
                    scoringType = 1,
                    version = 4,
                    zeroVoting = null,
                    tags = null,
                    players = players,
                    checks = listOf(),
                    shots = listOf(),
                    stage = null,
                    votes = listOf(),
                    comKiller = null,
                    bonuses = listOf(),
                    started = gameNumToStartedDate[it.gameNum] ?: LocalDateTime.now(),
                    stop = null,
                    isLive = true,
                    result = null,
                    num = it.gameNum,
                    table = it.tableNum,
                    phase = 0,
                    factor = 1.0
                )
                println(objectMapper.writeValueAsString(polemicaGame))
                polemicaClient.postGameToCompetition(3359, polemicaGame)
            }

        // gomafiaPlayers.forEach { println(it) }
        //
        // println("-----")
        //
        // members.forEach { println(it.player.username) }
        //
        // println("-----")
        //
        //
        // gomafiaPlayers.filterNot { gomafiaToPolemicaNicknameMap.containsKey(it) }.forEach { println(it) }
        //
        // println("-----")
        //
        // members.filterNot { gomafiaToPolemicaNicknameMap.containsValue(it.player.username) }
        //     .forEach { println(it.player.username) }
    }

    fun createNicknameMap(): Map<String, String> {
        return mapOf(
            "ZEVS" to "Zevs",
            "Самурай" to "Samurai4444",
            "Фурия." to "Фурия",
            "BELLEVIL" to "BELLEVIL",
            "Romanoff" to "Romanoff",
            "Орочимару" to "Орочимару",
            "Juls" to "Juls",
            "Маринетка" to "Маринетка",
            "Jeveksy" to "Jeveksy",
            "Karl" to "Karll",
            "Ленивец" to "Ленивец",
            "tms" to "tms",
            "Леброн" to "LEBRONSPB",
            "Кэт" to "Кэт_театрум",
            "crsms" to "Крисмас",
            "Талисман" to "eternal_poet",
            "pevoon" to "pevoon",
            "Cowboy" to "cowboy",
            "Кара" to "Kara4444",
            "Stereo" to "Stereo",
            "Shavuha" to "Shavuha",
            "Айра" to "Айра.",
            "Сметана" to "Smetanishe666",
            "Юпитер" to "Юпитeр",
            "Doc." to "Doc.",
            "ЧехонтЭ" to "ЧехонтЭ",
            "Фырчик" to "Фырчик",
            "JTH" to "JTH",
            "Urusta" to "Urusta",
            "Феодал" to "Феодал",
            "Malkav" to "Malkav",
            "Macarena" to "Macarena",
            "Текила" to "Текила",
            "Секрет" to "Секрет-",
            "Доктор Ливси" to "Доктор_Ливси",
            "KA" to "KAA",
            "Шахист" to "Шахист",
            "МуМу" to "Муму",
            "Saint-P" to "Saint-P",
            "Эндван" to "And1",
            "MadCat" to "MadCat",
            "Accord" to "Accord",
            "Велосипедостроитель" to "Велосипедостроитель",
            "NotZorro" to "NotZorro",
            "Том" to "Том",
            "Julie" to "Госпожа Julie",
            "SheyWost" to "SheyWost",
            "Заяц" to "Заяц",
            "Vibe" to "Vibe",
            "Axeon" to "Axeon",
            "Chaffy" to "chaffy",
            "Плюшка" to "Плюшка)",
            "Домино" to "Domino",
            "V." to "V.",
            "fey" to "fey",
            "Lemona" to "Lemona",
            "Sema" to "г-н Sema",
            "Аргентум" to "Аргентум111",
            "avomarika" to "avomarika",
            "Сильвана" to "ДонСильвана",
            "Алёна" to "Alena78",
            "Размен" to "Размен",
            "Мэд" to "Мэд",
            "Palda666" to "Padla666",
            "Пожарный" to "Пожарный",
            "ФДМ" to "ФДМ",
            "Бaлдёж" +
                "" to "Балдёж",
            "Паныч" to "Паныч",
            "ЯБ" to "ЯБ",
            "Фред" to "Фред",
            "Жуля" to "Жуля",
            "Комиссар Жибер" to "Комиссар Жибер",
            "Короля" to "Короля",
            "Оксюморон" to "Оксюморон",
            "Рэндом" to "Рэндом",
            "Воробушек" to "Воробушек",
            "Самбука" to "самбука_",
            "WIMP" to "WIMP",
            "For_eLL" to "For_eLL_",
            "Милкис" to "Milkiss",
            "Сеньор Помидор" to "С. Помидор",
            "Кольт vdk" to "Koltvl",
            "Егор Крид" to "ЕгорКрид",
            "Rikso" to "Rikso",
            "механика" to "механика",
            "Вега" to "Вега",
            "Gavrr" to "Gavrr",
            "Солез" to "Солез",
            "Оладуш" to "Оладуш",
            "Ментик" to "Ментик",
            "Рокса" to "Рокса",
            "Сложный" to "Сложный",
            "WELSH" to "WELSH",
            "Хичкок" to "Xu4kok",
            "Антимаг" to "Antimuge",
            "Ластхиро" to "Ластхиро",
            "Enigma_" to "Enigma_IO",
            "Никто." to "GWBKSSamara",
            "Enjoy" to "Johnny Utah",
            "Плохой человек" to "alex_babich",
            "Bellissima" to "Bellissima",
            "Молодой_22" to "sadameisme",
            "Любезный" to "Любезный"
        )
    }
}
