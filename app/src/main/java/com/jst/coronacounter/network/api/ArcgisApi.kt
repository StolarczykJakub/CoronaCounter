package com.jst.coronacounter.network.api

import com.jst.coronacounter.data.Features
import retrofit2.http.GET

interface ArcgisApi {
    @GET("query?f=json&where=(Country_Region%3D%27Poland%27)&outFields=Deaths,Confirmed,recovered")
    suspend fun getStatistics(): Features
}