package com.stoum.overlay.model.gomafia

import com.google.gson.annotations.SerializedName

data class StatsDto (
        @SerializedName("primary"         ) var primary        : Primary?        = Primary(),
        @SerializedName("win_rate"        ) var winRate        : WinRate?        = WinRate(),
        @SerializedName("games_stats"     ) var gamesStats     : GamesStats?     = GamesStats(),
        @SerializedName("advanced_points" ) var advancedPoints : AdvancedPoints? = AdvancedPoints()
) {
    data class Primary (

            @SerializedName("mafia"       ) var mafia      : Int? = null,
            @SerializedName("red"         ) var red        : Int? = null,
            @SerializedName("don"         ) var don        : Int? = null,
            @SerializedName("sheriff"     ) var sheriff    : Int? = null,
            @SerializedName("total_games" ) var totalGames : Int? = null

    )

    data class Win (

            @SerializedName("value"   ) var value   : Int? = null,
            @SerializedName("percent" ) var percent : Int? = null

    )

    data class Total (

            @SerializedName("value" ) var value : Int? = null

    )

    data class Mafia (

            @SerializedName("win"   ) var win   : Win?   = Win(),
            @SerializedName("total" ) var total : Total? = Total()

    )

    data class Red (

            @SerializedName("win"   ) var win   : Win?   = Win(),
            @SerializedName("total" ) var total : Total? = Total()

    )

    data class Don (

            @SerializedName("win"   ) var win   : Win?   = Win(),
            @SerializedName("total" ) var total : Total? = Total()

    )

    data class Sheriff (

            @SerializedName("win"   ) var win   : Win?   = Win(),
            @SerializedName("total" ) var total : Total? = Total()

    )

    data class TotalWins (

            @SerializedName("value"   ) var value   : Int? = null,
            @SerializedName("percent" ) var percent : Int? = null

    )

    data class WinRate (

            @SerializedName("mafia"       ) var mafia      : Mafia?     = Mafia(),
            @SerializedName("red"         ) var red        : Red?       = Red(),
            @SerializedName("don"         ) var don        : Don?       = Don(),
            @SerializedName("sheriff"     ) var sheriff    : Sheriff?   = Sheriff(),
            @SerializedName("total_games" ) var totalGames : Int?       = null,
            @SerializedName("total_wins"  ) var totalWins  : TotalWins? = TotalWins()

    )


    data class GamesStats (

            @SerializedName("average_points" ) var averagePoints : Double? = null,
            @SerializedName("prize_places"   ) var prizePlaces   : Int?    = null

    )

    data class AdvancedPoints (

            @SerializedName("red"             ) var red           : Map<String, Double>     = mutableMapOf(),
            @SerializedName("black"           ) var black         : Map<String, Double>     = mutableMapOf(),
            @SerializedName("sheriff"         ) var sheriff       : Map<String, Double>     = mutableMapOf(),
            @SerializedName("points_10_games" ) var points10Games : Double? = null

    )
}