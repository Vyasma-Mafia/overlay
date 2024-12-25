package com.github.mafia.vyasma.polemicaachivementservice.utils

import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGameResult
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGuess
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaPlayer
import com.github.mafia.vyasma.polemicaachivementservice.model.game.Position
import com.github.mafia.vyasma.polemicaachivementservice.model.game.Role

fun PolemicaGame.getRealComKiller(): Position? {
    if (getFirstKilled()?.let { this.getRole(it) } != Role.SHERIFF) {
        return null
    }
    return this.comKiller ?: this.getDon()
}

fun PolemicaGame.getFirstKilled() = this.getKilled().find { it.night == 1 }?.position

fun PolemicaGame.getKilled(): List<KilledPlayer> {
    // all shots victims in nights equal
    return this.shots.groupBy { it.night }.map { (night, shots) ->
        val candidates = shots.map { it.victim }.toSet()
        if (candidates.size == 1) {
            KilledPlayer(candidates.first(), night)
        } else {
            KilledPlayer(null, night)
        }
    }
}

fun PolemicaGame.getDon(): Position {
    return this.players.find { it.role == Role.DON }!!.position
}

fun PolemicaGame.getSheriff(): Position {
    return this.players.find { it.role == Role.SHERIFF }!!.position
}

fun PolemicaGame.getRole(position: Position): Role {
    return this.players.find { it.position == position }!!.role
}

fun PolemicaGame.getFinalVotes(): List<FinalVote> {
    return this.votes.groupBy { it.day }.map { (day, votes) ->
        val votesNumMax = votes.map { it.num }.max()
        if (votesNumMax == 0) {
            return@map emptyList()
        }
        val lastVotes = votes.dropWhile { it.num < votesNumMax }
        val realVotes = lastVotes.filter { it.num == votesNumMax }
        val votingResult = realVotes.groupBy { it.candidate } // candidate -> [voters]
        if (votingResult.isEmpty()) {
            emptyList()
        } else if (votingResult.values.map { it.size }.toSet().size == 1 && votingResult.size > 1) { // попил
            val convicted = votingResult.keys.toList()
            val expelVoters = lastVotes.filter { it.num == 0 }
            val expelled = expelVoters.size > (realVotes.size - expelVoters.size)
            expelVoters.map { FinalVote(day, it.voter, convicted, expelled) }
        } else {
            val convicted = votingResult.maxBy { it.value.size }.key
            realVotes
                .map { FinalVote(day, it.voter, listOf(it.candidate), it.candidate == convicted) }
        }
    }.flatten()
}

fun PolemicaGame.playersOnTable(): List<Position> {
    return this.players.map { it.position }.minus(this.getKickedFromTable().map { it.position }.toSet())
}

fun PolemicaGame.playersWithRoles(roles: List<Role>): List<Position> {
    return this.players.filter { it.role in roles }.map { it.position }
}

fun PolemicaGame.isRedWin() = result == PolemicaGameResult.RED_WIN
fun PolemicaGame.isBlackWin() = result == PolemicaGameResult.BLACK_WIN

fun PolemicaGame.getBlacksOnTable(): Set<Position> {
    val playersOnTable = playersOnTable().toSet()
    val blacks = playersWithRoles(listOf(Role.MAFIA, Role.DON)).toSet()
    val blackOnTable = playersOnTable.intersect(blacks)
    return blackOnTable
}

fun PolemicaGame.getKickedFromTable(): List<KickedPlayer> {
    val killedPlayers = getKilled()
        .filter { it.position != null }
        .map { KickedPlayer(it.position!!, GamePhase(it.night, Phase.NIGHT), KickReason.KILL) }
    val votedPlayers = getFinalVotes()
        .filter { it.expelled }
        .flatMap { finalVote ->
            finalVote.convicted.map {
                KickedPlayer(
                    it,
                    GamePhase(finalVote.day, Phase.DAY),
                    KickReason.VOTING
                )
            }
        }
    val disqualed = players.filter { it.disqual != null }
        .map { KickedPlayer(it.position, GamePhase(it.disqual!!.day, Phase.NIGHT), KickReason.DISQUAL) }
    return (killedPlayers + votedPlayers + disqualed).toSet().sortedBy { it.gamePhase }
}

fun PolemicaGame.getCriticDay(): Int? {
    val lastDay = this.stage.day + 1
    var red = 7
    var black = 3
    val kickedPlayer = getKickedFromTable().groupBy { it.gamePhase.num }
    for (day in 2..lastDay) {
        for (kick in kickedPlayer[day - 1] ?: listOf()) {
            if (getRole(kick.position).isRed()) {
                red -= 1
            } else {
                black -= 1
            }
        }
        if (red <= black + 2) {
            return day
        }
    }
    return null
}

inline fun PolemicaGame.check(f: InPolemicaGameContext.() -> Int): Int {
    val context = InPolemicaGameContext(this)
    return try {
        f(context)
    } catch (e: CheckException) {
        0
    }
}

data class InPolemicaGameContext(val game: PolemicaGame) {
    fun Position.role(): Role {
        return game.getRole(this@role)
    }

    fun Position.guess(): PolemicaGuess? {
        return game.players.find { it.position == this@guess }?.guess
    }

    fun Position.player(): PolemicaPlayer {
        return game.players.find { it.position == this@player }!!
    }

    fun assert(boolean: Boolean) {
        if (boolean) {
            return
        }
        throw CheckException
    }

    fun assert(block: () -> Boolean) {
        assert(block())
    }
}

internal object CheckException : Exception() {
    private fun readResolve(): Any = CheckException
    override fun fillInStackTrace(): Throwable {
        return this
    }
}

data class KilledPlayer(val position: Position?, val night: Int)
data class FinalVote(val day: Int, val position: Position, val convicted: List<Position>, val expelled: Boolean)
data class GamePhase(val num: Int, val phase: Phase) : Comparable<GamePhase> {
    override fun compareTo(other: GamePhase): Int {
        if (num < other.num) return -1
        if (num > other.num) return 1
        return phase.compareTo(other.phase)
    }
}

data class KickedPlayer(val position: Position, val gamePhase: GamePhase, val reason: KickReason)

enum class Phase : Comparable<Phase> {
    DAY, NIGHT
}

enum class KickReason {
    VOTING, KILL, DISQUAL
}
