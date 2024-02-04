package com.stoum.overlay.model.gomafia

import com.fasterxml.jackson.annotation.JsonProperty

data class TournamentResultDto(
        @JsonProperty("compensation_point") val compensation_point: String?,
        @JsonProperty("extra_point") val extra_point: String?,
        @JsonProperty("fine") val fine: String?,
        @JsonProperty("first_kill") val first_kill: String?,
        @JsonProperty("first_kill_point") val first_kill_point: String?,
        @JsonProperty("global_game") val global_game: String?,
        @JsonProperty("icon") val icon: String?,
        @JsonProperty("icon_type") val icon_type: String?,
        @JsonProperty("is_paid") val is_paid: String?,
        @JsonProperty("login") val login: String?,
        @JsonProperty("place") val place: String?,
        @JsonProperty("sum") val sum: String?,
        @JsonProperty("sum_extra") val sum_extra: String?,
        @JsonProperty("win") val win: String?,
        @JsonProperty("win_as_don") val win_as_don: String?,
        @JsonProperty("win_as_sheriff") val win_as_sheriff: String?
)