package com.jst.coronacounter.data

import com.squareup.moshi.Json


data class Features(
    @field:Json(name="features") var features: List<Attributes>
)

data class Attributes(
    @field:Json(name="attributes") var attributes: Statistics
)

data class Statistics(
    @field:Json(name="Deaths") var deaths: Int,
    @field:Json(name="Confirmed") var confirmed: Int,
    @field:Json(name="Recovered") var recovered: Int
)