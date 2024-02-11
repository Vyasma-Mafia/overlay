package com.stoum.overlay.service.gomafia

import com.stoum.overlay.GomafiaRestClient
import com.stoum.overlay.entity.gomafia.*
import com.stoum.overlay.model.gomafia.GomafiaRegion
import com.stoum.overlay.model.gomafia.UserDto
import com.stoum.overlay.repository.gomafia.*
import org.springframework.stereotype.Service
import java.sql.Date
import java.util.logging.Logger
import kotlin.jvm.optionals.getOrElse

@Service
class GomafiaGrabber(
        val client: GomafiaRestClient,
        val userRepository: GomafiaUserRepository,
        val clubRepository: GomafiaClubRepository,
        val tournamentRepository: GomafiaTournamentRepository,
        val gameRepository: GomafiaGameRepository,
        val gamePlayerRepository: GomafiaGamePlayerRepository,
        val tournamentResultRepository: GomafiaTournamentResultRepository
) {
    val log = Logger.getLogger("grabber")
    fun grab() {
        log.info("Grabbing users")
        //grabUsers()
        log.info("Grabbing clubs")
        //grabClubs()

        log.info("Grabbing tournaments")
        val tours = client.getTournaments()
        log.info("Found ${tours.size} tournaments to grab...")
        tours.forEach { tour ->
            try {
                val tournament = client.getTournament(tour.id!!.toInt())
                val t = tournament.tournamentDto
                val tEntity = GomafiaTournament(
                        t.id!!.toInt(),
                        t.title!!,
                        Date.valueOf(tour.dateStart),
                        t.type!!,
                        t.city!!,
                        t.country!!,
                        t.status!!,
                        t.userOrganizerId!!.toInt(),
                        t.isFsmRating!!,
                        t.star!!.toInt(),
                        t.eloAverage!!.toBigDecimal()
                )
                tournamentRepository.save(tEntity)

                log.info("Saving ${tournament.games.size} games for tournament ${tournament.tournamentDto.title}")
                tournament.games.forEach { g ->
                    val game = GomafiaGame(
                            tEntity.id,
                            g.gameNum!!,
                            g.tableNum!!,
                            g.referee,
                            g.win!!
                    )

                    gameRepository.save(game)

                    g.table.forEach { p ->
                        val gamePlayer = GomafiaGamePlayer(
                                p.id!!,
                                game.id!!,
                                p.login!!,
                                p.place!!,
                                p.role,
                                p.points!!,
                                p.type,
                                p.eloDelta.toIntOrZero()
                        )

                        gamePlayerRepository.save(gamePlayer)
                    }
                }

                log.info("Saving tournament results for tournament ${tournament.tournamentDto.title}")
                tournament.tournamentResults?.forEach { tr ->
                    val result = GomafiaTournamentResult(
                            tour.id!!.toInt(),
                            tr.login!!,
                            tr.place!!.toInt(),
                            tr.sum!!.toBigDecimal(),
                            tr.sum_extra!!.toBigDecimal(),
                            tr.win!!.toInt(),
                            tr.win_as_don!!.toInt(),
                            tr.win_as_sheriff!!.toInt(),
                            tr.compensation_point!!.toBigDecimal(),
                            tr.extra_point!!.toBigDecimal(),
                            tr.fine!!.toBigDecimal(),
                            tr.first_kill!!.toInt(),
                            tr.first_kill_point!!.toBigDecimal(),
                            tr.global_game!!.toBigDecimal()
                    )

                    tournamentResultRepository.save(result)
                }
            } catch (e: Exception) {
                log.severe("Encountered exception on tournament ${tour.id} ${tour.title}")
                e.printStackTrace()
            }
        }

    }

    private fun grabUsers() {
        GomafiaRegion.values().forEach { region ->
            log.info("Grabbing $region")
            val users = client.getUsers(2024, region)
            log.info("Found ${users.size} users to grab...")
            users.forEach {
                val userEntity = GomafiaUser(
                        it.id!!.toInt(),
                        it.elo!!.toBigDecimal(),
                        it.login!!,
                        it.avatar_link,
                        it.country,
                        it.city,
                        it.tournaments_played.toIntOrZero(),
                        it.tournaments_score.toIntOrZero(),
                        null,
                        region.region
                )

                userRepository.save(userEntity)
            }
        }
    }

    private fun grabClubs() {
        val clubs = client.getClubs()
        log.info("Found ${clubs.size} clubs to grab...")

        clubs.forEach {
            val club = GomafiaClub(
                    it.id!!.toInt(),
                    it.avatar_link,
                    it.club_score!!.toInt(),
                    it.elo_average!!.toBigDecimal(),
                    it.title!!,
                    it.city!!,
                    it.country!!
            )

            clubRepository.save(club)
            log.info("Updating users with club $club")
            client.getClubResidents(it.id.toInt())
                    //.map { r -> r.id!!.toInt() }
                    .forEach { r ->
                        val user = userRepository.findById(r.id.toIntOrZero()).getOrElse {
                            userRepository.save(userToEntity(r, null))
                        }
                        user.clubId = it.id.toInt()
                        userRepository.save(user)
                    }

        }
    }

    fun String?.toIntOrZero(): Int {
        return if ( this == null || this.isBlank() ) 0 else this.toInt()
    }

    fun userToEntity(userDto: UserDto, region: GomafiaRegion?): GomafiaUser {
        return GomafiaUser(
                userDto.id!!.toInt(),
                userDto.elo!!.toBigDecimal(),
                userDto.login!!,
                userDto.avatar_link,
                userDto.country,
                userDto.city,
                userDto.tournaments_played.toIntOrZero(),
                userDto.tournaments_score.toIntOrZero(),
                null,
                region?.region
        )
    }
}