package com.stoum.overlay.model.gomafia

import com.fasterxml.jackson.annotation.JsonProperty

data class UserDto(
        @JsonProperty("avatar_link") val avatar_link: String?,
        @JsonProperty("elo") val elo: String?,
        @JsonProperty("icon") val icon: String?,
        @JsonProperty("icon_type") val icon_type: String?,
        @JsonProperty("id") val id: String?,
        @JsonProperty("is_paid") val is_paid: String?,
        @JsonProperty("login") val login: String?,
        @JsonProperty("paid_account") val paid_account: String?,
        @JsonProperty("rank") val rank: String?,
        @JsonProperty("title") val title: String?,
        @JsonProperty("total_rows") val total_rows: String?,
        @JsonProperty("tournaments_gg") val tournaments_gg: List<Int>?,
        @JsonProperty("tournaments_played") val tournaments_played: String?,
        @JsonProperty("tournaments_score") val tournaments_score: String?,
        @JsonProperty("country") val country: String?,
        @JsonProperty("city") val city: String?,
)