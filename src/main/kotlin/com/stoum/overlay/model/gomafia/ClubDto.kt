package com.stoum.overlay.model.gomafia

import com.fasterxml.jackson.annotation.JsonProperty

data class ClubDto(
        @JsonProperty("avatar_link") val avatar_link: String?,
        @JsonProperty("city") val city: String?,
        @JsonProperty("club_score") val club_score: String?,
        @JsonProperty("country") val country: String?,
        @JsonProperty("elo_average") val elo_average: String?,
        @JsonProperty("id") val id: String?,
        @JsonProperty("rank") val rank: String?,
        @JsonProperty("title") val title: String?,
        @JsonProperty("total_rows") val total_rows: String?
)