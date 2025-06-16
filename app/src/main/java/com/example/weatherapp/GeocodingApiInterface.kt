package com.example.weatherapp

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApiInterface {
    @GET("geo/1.0/direct")
    fun getCitySuggestions(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        @Query("appid") apiKey: String
    ): Call<List<CitySuggestion>>
}

data class CitySuggestion(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String?
)