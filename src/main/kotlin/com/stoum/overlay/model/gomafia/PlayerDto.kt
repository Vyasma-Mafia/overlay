package com.stoum.overlay.model.gomafia

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class PlayerDto(
        @JsonProperty("place"    ) var place    : Int?    = null,
        @JsonProperty("id"    ) var id    : Int?    = null,
        @JsonProperty("login"    ) var login    : String? = null,
        @JsonProperty("role"     ) var role     : String? = null,
        @JsonProperty("points"   ) var points   : BigDecimal?    = null,
        @JsonProperty("type"     ) var type     : String? = null,
        @JsonProperty("eloDelta" ) var eloDelta : String? = null,
        @JsonProperty("elo" ) var elo : String? = null,
        @JsonProperty("isPaid"   ) var isPaid   : String? = null,
        @JsonProperty("icon"     ) var icon     : String? = null,
        @JsonProperty("iconType" ) var iconType : String? = null
)
