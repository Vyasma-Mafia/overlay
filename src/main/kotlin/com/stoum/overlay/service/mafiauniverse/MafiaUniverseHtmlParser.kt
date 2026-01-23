package com.stoum.overlay.service.mafiauniverse

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.regex.Pattern

/**
 * Data classes for parsed game information
 */
data class ParsedTournament(
    val id: Int,
    val name: String
)

data class ParsedGameInfo(
    val gameId: Int,
    val tourNumber: Int,
    val tableNumber: Int
)

data class ParsedGameDetails(
    val tournamentId: Int,
    val tournamentName: String,
    val tourNumber: Int,
    val tableNumber: Int,
    val gameDate: LocalDateTime?,
    val winner: String?, // "Мафия" or "Мирный"
    val players: List<ParsedPlayer>
)

data class ParsedPlayer(
    val position: Int,
    val nickname: String,
    val role: String? // "Мафия", "Мирный", "Шериф", "Дон"
)

data class ParsedTournamentPlayer(
    val playerId: Int?,
    val nickname: String,
    val realName: String?,
    val teamNumber: Int?,
    val registrationDate: String?,
    val isConfirmed: Boolean,
    val isPaid: Boolean
)

@Component
class MafiaUniverseHtmlParser {

    /**
     * Parses the tournaments list page and extracts tournament information
     */
    fun parseTournamentsList(html: String): List<ParsedTournament> {
        val doc = Jsoup.parse(html)
        val tournaments = mutableListOf<ParsedTournament>()

        // Find all tournament links with pattern /Tournament/{id}
        doc.select("a[href^=/Tournament/]").forEach { link ->
            val href = link.attr("href")
            val tournamentIdMatch = Pattern.compile("/Tournament/(\\d+)").matcher(href)
            if (tournamentIdMatch.find()) {
                val tournamentId = tournamentIdMatch.group(1).toInt()
                val tournamentName = link.text().trim()
                if (tournamentName.isNotEmpty()) {
                    tournaments.add(ParsedTournament(tournamentId, tournamentName))
                }
            }
        }

        return tournaments.distinctBy { it.id }
    }

    /**
     * Parses the games list page and extracts game information
     */
    fun parseGamesList(html: String): List<ParsedGameInfo> {
        val doc = Jsoup.parse(html)
        val games = mutableListOf<ParsedGameInfo>()

        // Find all game links with pattern /Games/Details/{id}
        doc.select("a[href^=/Games/Details/]").forEach { link ->
            val href = link.attr("href")
            val gameIdMatch = Pattern.compile("/Games/Details/(\\d+)").matcher(href)
            if (gameIdMatch.find()) {
                val gameId = gameIdMatch.group(1).toInt()

                // Extract tour number and table number from link text
                // Format: "Тур {tour} стол {table}"
                val linkText = link.text().trim()
                val tourTableMatch = Pattern.compile("Тур\\s+(\\d+)\\s+стол\\s+(\\d+)", Pattern.CASE_INSENSITIVE)
                    .matcher(linkText)

                if (tourTableMatch.find()) {
                    val tourNumber = tourTableMatch.group(1).toInt()
                    val tableNumber = tourTableMatch.group(2).toInt()
                    games.add(ParsedGameInfo(gameId, tourNumber, tableNumber))
                } else {
                    // Try to find tour number from h5 headings
                    val tourHeading = link.parents().firstOrNull {
                        it.tagName() == "h5" && it.text().contains("Тур")
                    }
                    if (tourHeading != null) {
                        val tourMatch = Pattern.compile("Тур\\s+(\\d+)", Pattern.CASE_INSENSITIVE)
                            .matcher(tourHeading.text())
                        if (tourMatch.find()) {
                            val tourNumber = tourMatch.group(1).toInt()
                            // Try to extract table number from surrounding context
                            val tableNumber = extractTableNumberFromContext(link) ?: 1
                            games.add(ParsedGameInfo(gameId, tourNumber, tableNumber))
                        }
                    }
                }
            }
        }

        return games.distinctBy { it.gameId }
    }

    /**
     * Extracts table number from surrounding HTML context
     */
    private fun extractTableNumberFromContext(link: Element): Int? {
        // Look for "стол {number}" in parent elements
        var current: Element? = link.parent()
        var depth = 0
        while (current != null && depth < 5) {
            val text = current.text()
            val tableMatch = Pattern.compile("стол\\s+(\\d+)", Pattern.CASE_INSENSITIVE).matcher(text)
            if (tableMatch.find()) {
                return tableMatch.group(1).toInt()
            }
            current = current.parent()
            depth++
        }
        return null
    }

    /**
     * Parses the game details page and extracts complete game information
     */
    fun parseGameDetails(html: String): ParsedGameDetails {
        val doc = Jsoup.parse(html)

        // Extract tournament ID and name
        val tournamentLink = doc.select("a[href^=/Tournament/]").firstOrNull()
        val tournamentId = tournamentLink?.let {
            val href = it.attr("href")
            val match = Pattern.compile("/Tournament/(\\d+)").matcher(href)
            if (match.find()) match.group(1).toInt() else null
        } ?: throw IllegalArgumentException("Tournament ID not found in game details")

        val tournamentName = tournamentLink?.select("b")?.text()?.trim()
            ?: tournamentLink?.text()?.trim()
            ?: throw IllegalArgumentException("Tournament name not found")

        // Extract tour number
        val tourNumber = extractTourNumber(doc)
            ?: throw IllegalArgumentException("Tour number not found")

        // Extract table number
        val tableNumber = extractTableNumber(doc)
            ?: throw IllegalArgumentException("Table number not found")

        // Extract game date
        val gameDate = extractGameDate(doc)

        // Extract winner
        val winner = extractWinner(doc)

        // Extract players
        val players = extractPlayers(doc)

        return ParsedGameDetails(
            tournamentId = tournamentId,
            tournamentName = tournamentName,
            tourNumber = tourNumber,
            tableNumber = tableNumber,
            gameDate = gameDate,
            winner = winner,
            players = players
        )
    }

    /**
     * Extracts tour number from game details page
     */
    private fun extractTourNumber(doc: Document): Int? {
        // Look for "Тур" in dt/dd pairs
        doc.select("dt").forEach { dt ->
            if (dt.text() == "Тур") {
                val dd = dt.nextElementSibling()
                val text = dd?.text()?.trim()
                if (text != null) {
                    return text.toIntOrNull()
                }
            }
        }
        return null
    }

    /**
     * Extracts table number from game details page
     */
    private fun extractTableNumber(doc: Document): Int? {
        // Look for "Стол" in dt/dd pairs
        doc.select("dt").forEach { dt ->
            if (dt.text().contains("Стол", ignoreCase = true)) {
                val dd = dt.nextElementSibling()
                val text = dd?.text()?.trim()
                if (text != null) {
                    return text.toIntOrNull()
                }
            }
        }
        return null
    }

    /**
     * Extracts game date from game details page
     */
    private fun extractGameDate(doc: Document): LocalDateTime? {
        // Look for date in h4 heading: "Игра Тур 1 стол 1 от 06-10-2025 09:54"
        val h4 = doc.select("h4.text-center").firstOrNull()
        if (h4 != null) {
            val text = h4.text()
            // Try pattern "от DD-MM-YYYY HH:mm"
            val dateMatch = Pattern.compile("от\\s+(\\d{2})-(\\d{2})-(\\d{4})\\s+(\\d{2}):(\\d{2})")
                .matcher(text)
            if (dateMatch.find()) {
                try {
                    val day = dateMatch.group(1).toInt()
                    val month = dateMatch.group(2).toInt()
                    val year = dateMatch.group(3).toInt()
                    val hour = dateMatch.group(4).toInt()
                    val minute = dateMatch.group(5).toInt()
                    return LocalDateTime.of(year, month, day, hour, minute)
                } catch (e: Exception) {
                    // Ignore parsing errors
                }
            }
        }
        return null
    }

    /**
     * Extracts winner from game details page
     */
    private fun extractWinner(doc: Document): String? {
        // Look for "Победитель" in dt/dd pairs
        doc.select("dt").forEach { dt ->
            if (dt.text().contains("Победитель", ignoreCase = true)) {
                val dd = dt.nextElementSibling()
                val text = dd?.text()?.trim()
                if (text != null) {
                    // Map to internal format: "Мафия" -> "black", "Мирный" -> "red"
                    return when {
                        text.contains("Мафия", ignoreCase = true) -> "black"
                        text.contains("Мирный", ignoreCase = true) -> "red"
                        else -> text
                    }
                }
            }
        }
        return null
    }

    /**
     * Extracts players from game details page
     */
    private fun extractPlayers(doc: Document): List<ParsedPlayer> {
        val players = mutableListOf<ParsedPlayer>()

        // Find the players table - look for table with "Игрок" and "Роль" headers
        val table = doc.select("table").firstOrNull { table ->
            val headers = table.select("th")
            headers.any { it.text().contains("Игрок", ignoreCase = true) } &&
                headers.any { it.text().contains("Роль", ignoreCase = true) }
        } ?: return emptyList()

        // Extract players from table rows
        table.select("tbody tr").forEach { row ->
            val cells = row.select("td")
            if (cells.size >= 2) {
                // First cell should be position number
                val positionText = cells[0].text().trim()
                val position = positionText.toIntOrNull()

                // Second cell should be player nickname
                val nickname = cells[1].text().trim()

                // Third cell (if exists) should be role
                val roleText = if (cells.size >= 3) {
                    cells[2].text().trim()
                } else null

                if (position != null && nickname.isNotEmpty()) {
                    // Map role to internal format
                    val role = roleText?.let { mapRoleToInternal(it) }
                    players.add(ParsedPlayer(position, nickname, role))
                }
            }
        }

        return players.sortedBy { it.position }
    }

    /**
     * Maps MafiaUniverse role names to internal role format
     */
    private fun mapRoleToInternal(roleText: String): String? {
        return when {
            roleText.contains("Мафия", ignoreCase = true) -> "black"
            roleText.contains("Дон", ignoreCase = true) -> "don"
            roleText.contains("Шериф", ignoreCase = true) -> "sher"
            roleText.contains("Мирный", ignoreCase = true) -> "red"
            else -> null
        }
    }

    /**
     * Extracts unique player nicknames from games list page
     * Used for getting tournament participants
     */
    fun extractPlayerNicknamesFromGamesList(html: String): Set<String> {
        val doc = Jsoup.parse(html)
        val nicknames = mutableSetOf<String>()

        // Find all player names in game tables
        doc.select("table.ended_true tbody tr").forEach { row ->
            val cells = row.select("td")
            if (cells.size >= 2) {
                // Second cell typically contains player nickname
                val nickname = cells[1].text().trim()
                if (nickname.isNotEmpty() && !nickname.matches(Regex("\\d+"))) {
                    nicknames.add(nickname)
                }
            }
        }

        return nicknames
    }

    /**
     * Parses the tournament players page and extracts all participants
     */
    fun parseTournamentPlayers(html: String): List<ParsedTournamentPlayer> {
        val doc = Jsoup.parse(html)
        val players = mutableListOf<ParsedTournamentPlayer>()

        // Find the players table - look for table with "Игрок" header
        val table = doc.select("table.table tbody").firstOrNull()
            ?: return emptyList()

        // Extract players from table rows
        table.select("tr").forEach { row ->
            val cells = row.select("td")
            if (cells.size >= 2) {
                // First cell: row number (we can skip it)

                // Second cell: player info
                val playerCell = cells[1]
                val playerLink = playerCell.select("a[href^=/Player/]").firstOrNull()
                val playerId = playerLink?.let {
                    val href = it.attr("href")
                    val match = Pattern.compile("/Player/(\\d+)").matcher(href)
                    if (match.find()) match.group(1).toIntOrNull() else null
                }

                val nickname = playerLink?.select("b")?.text()?.trim()
                    ?: playerLink?.text()?.trim()
                    ?: ""

                // Real name is in a div.small below the link
                val realName = playerCell.select("div.small").firstOrNull()?.text()?.trim()
                    ?.takeIf { it.isNotEmpty() }

                // Third cell: team number (if present)
                val teamNumber = if (cells.size >= 3) {
                    cells[2].text().trim().toIntOrNull()
                } else null

                // Fourth cell: registration date
                val registrationDate = if (cells.size >= 4) {
                    cells[3].text().trim().takeIf { it.isNotEmpty() }
                } else null

                // Fifth cell: confirmed checkbox
                val isConfirmed = if (cells.size >= 5) {
                    cells[4].select("input[type=checkbox]").firstOrNull()?.hasAttr("checked") == true
                } else false

                // Sixth cell: paid checkbox
                val isPaid = if (cells.size >= 6) {
                    cells[5].select("input[type=checkbox]").firstOrNull()?.hasAttr("checked") == true
                } else false

                if (nickname.isNotEmpty()) {
                    players.add(
                        ParsedTournamentPlayer(
                            playerId = playerId,
                            nickname = nickname,
                            realName = realName,
                            teamNumber = teamNumber,
                            registrationDate = registrationDate,
                            isConfirmed = isConfirmed,
                            isPaid = isPaid
                        )
                    )
                }
            }
        }

        return players
    }
}
