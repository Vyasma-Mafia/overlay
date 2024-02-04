package com.stoum.overlay.model.gomafia

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName


data class TournamentDto(
    @JsonProperty("id"                              ) var id                            : String? = null,
    @JsonProperty("title"                           ) var title                         : String? = null,
    @JsonProperty("date_start"                      ) var dateStart                     : String? = null,
    @JsonProperty("date_end"                        ) var dateEnd                       : String? = null,
    @JsonProperty("country"                         ) var country                       : String? = null,
    @JsonProperty("city"                            ) var city                          : String? = null,
    @JsonProperty("type"                            ) var type                          : String? = null,
    @JsonProperty("status"                          ) var status                        : String? = null,
    @JsonProperty("registration_discussion_link"    ) var registrationDiscussionLink    : String? = null,
    @JsonProperty("chat_link"                       ) var chatLink                      : String? = null,
    @JsonProperty("vk_group_link"                   ) var vkGroupLink                   : String? = null,
    @JsonProperty("main_referee_id"                 ) var mainRefereeId                 : String? = null,
    @JsonProperty("user_organizer_id"               ) var userOrganizerId               : String? = null,
    @JsonProperty("contribution_currency"           ) var contributionCurrency          : String? = null,
    @JsonProperty("contribution_amount"             ) var contributionAmount            : String? = null,
    @JsonProperty("projected_count_of_participant"  ) var projectedCountOfParticipant   : String? = null,
    @JsonProperty("is_hidden_result"                ) var isHiddenResult                : String? = null,
    @JsonProperty("is_fsm_rating"                   ) var isFsmRating                   : String? = null,
    @JsonProperty("star"                            ) var star                          : String? = null,
    @JsonProperty("type_translate"                  ) var typeTranslate                 : String? = null,
    @JsonProperty("status_translate"                ) var statusTranslate               : String? = null,
    @JsonProperty("country_translate"               ) var countryTranslate              : String? = null,
    @JsonProperty("city_translate"                  ) var cityTranslate                 : String? = null,
    @JsonProperty("contribution_currency_translate" ) var contributionCurrencyTranslate : String? = null,
    @JsonProperty("elo_average"                     ) var eloAverage : String? = null,
    @JsonProperty("total_rows"                     ) var totalRows : String? = null,
)
