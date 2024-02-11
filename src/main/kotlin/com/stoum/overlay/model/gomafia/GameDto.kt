package com.stoum.overlay.model.gomafia

import com.fasterxml.jackson.annotation.JsonProperty
import com.stoum.overlay.model.gomafia.PlayerDto
import java.util.*

data class GameDto(
        @JsonProperty("tableNum"          ) var tableNum        : Int?             = null,
        @JsonProperty("table"             ) var table           : ArrayList<PlayerDto> = arrayListOf(),
        @JsonProperty("win"               ) var win             : String?          = null,
        @JsonProperty("referee"           ) var referee         : String?          = null,
        @JsonProperty("referee_is_paid"   ) var refereeIsPaid   : String?          = null,
        @JsonProperty("referee_icon_type" ) var refereeIconType : String?          = null,
        @JsonProperty("referee_icon"      ) var refereeIcon     : String?          = null,
        @JsonProperty("game_num") var gameNum: Int? = null
) {

/*
    constructor(gameNum: Int, gameDto: GameDto) : this(
            gameDto.tableNum       ,
            gameDto.table          ,
            gameDto.win            ,
            gameDto.referee        ,
            gameDto.refereeIsPaid  ,
            gameDto.refereeIconType,
            gameDto.refereeIcon    ,
            gameNum
    )
*/

    override fun toString(): String {
        return "GameDto(tableNum=$tableNum, table=$table, win=$win, referee=$referee, refereeIsPaid=$refereeIsPaid, refereeIconType=$refereeIconType, refereeIcon=$refereeIcon, gameNum=$gameNum)"
    }
}
