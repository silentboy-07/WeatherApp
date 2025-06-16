package com.example.weatherapp

data class ForecastResponse(
    val list: List<ForecastItem>
)

data class ForecastItem(
    val dt: Long, // Unix timestamp
    val main: Main // Reuses the existing Main data class
)