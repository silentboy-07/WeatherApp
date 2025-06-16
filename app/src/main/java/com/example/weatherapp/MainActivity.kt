package com.example.weatherapp

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListPopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.weatherapp.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private var sunRiseTime: Long = 0
    private var sunSetTime: Long = 0
    private lateinit var listPopupWindow: ListPopupWindow
    private lateinit var suggestionAdapter: ArrayAdapter<String>
    private var citySuggestions: List<CitySuggestion> = emptyList()
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var shouldShowSuggestions = false // Control suggestion visibility

    companion object {
        private const val API_BASE_URL = "https://api.openweathermap.org/data/2.5/"
        private const val GEOCODING_API_BASE_URL = "https://api.openweathermap.org/"
        private const val API_KEY = "Enter Your API KEY"
        private const val TEMPERATURE_UNIT = "metric"
        private val DATE_FORMATTER = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        private val TIME_FORMATTER = SimpleDateFormat("HH:mm", Locale.getDefault())
        private val DAY_FORMATTER = SimpleDateFormat("EEEE", Locale.getDefault())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Initialize UI with loader
        binding.weatherContainer.visibility = View.GONE
        binding.loadingView.visibility = View.VISIBLE

        setupSearchView()
        fetchWeatherData("Rajkot")
    }

    private fun setupSearchView() {
        // Initialize ListPopupWindow for suggestions
        listPopupWindow = ListPopupWindow(this).apply {
            anchorView = binding.searchView
            width = ListPopupWindow.MATCH_PARENT
            isModal = true
        }
        suggestionAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf<String>())
        listPopupWindow.setAdapter(suggestionAdapter)

        binding.searchView.apply {
            // Enable suggestions when SearchView gains focus
            setOnQueryTextFocusChangeListener { _, hasFocus ->
                Log.d("SearchView", "Focus changed: $hasFocus")
                if (hasFocus) {
                    shouldShowSuggestions = true
                    val currentQuery = query.toString()
                    if (currentQuery.length >= 2) {
                        fetchCitySuggestions(currentQuery)
                    }
                } else {
                    listPopupWindow.dismiss()
                }
            }

            setOnQueryTextListener(object : android.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    Log.d("SearchView", "Query submitted: $query")
                    query?.takeIf { it.isNotBlank() }?.let {
                        shouldShowSuggestions = false // Disable suggestions after submission
                        handleSearch(it)
                        clearFocus()
                        listPopupWindow.dismiss()
                    } ?: Toast.makeText(this@MainActivity, "Please enter a city name", Toast.LENGTH_SHORT).show()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (shouldShowSuggestions) {
                        newText?.takeIf { it.length >= 2 }?.let {
                            // Debounce API calls
                            searchRunnable?.let { handler.removeCallbacks(it) }
                            searchRunnable = Runnable { fetchCitySuggestions(it) }
                            handler.postDelayed(searchRunnable!!, 300)
                        } ?: listPopupWindow.dismiss()
                    }
                    return true
                }
            })

            // Handle suggestion clicks
            listPopupWindow.setOnItemClickListener { _, _, position, _ ->
                val selectedCity = citySuggestions.getOrNull(position)?.name ?: return@setOnItemClickListener
                Log.d("SearchView", "Suggestion clicked: $selectedCity")
                shouldShowSuggestions = false // Disable suggestions after selection
                setQuery(selectedCity, false) // Update SearchView text
                handleSearch(selectedCity) // Fetch weather data
                listPopupWindow.dismiss()
                // Delay clearFocus to avoid UI issues
                handler.postDelayed({ clearFocus() }, 100)
            }

            // Handle search button click
            findViewById<View>(androidx.appcompat.R.id.search_button)?.setOnClickListener {
                val query = query.toString()
                Log.d("SearchView", "Search button clicked: $query")
                if (query.isNotBlank()) {
                    shouldShowSuggestions = false // Disable suggestions
                    handleSearch(query)
                    listPopupWindow.dismiss()
                    clearFocus()
                } else {
                    Toast.makeText(this@MainActivity, "Please enter a city name", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchCitySuggestions(query: String) {
        if (!shouldShowSuggestions) {
            listPopupWindow.dismiss()
            return
        }
        Retrofit.Builder()
            .baseUrl(GEOCODING_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeocodingApiInterface::class.java)
            .getCitySuggestions(query, 5, API_KEY)
                .enqueue(object : Callback<List<CitySuggestion>> {
                    override fun onResponse(call: Call<List<CitySuggestion>>, response: Response<List<CitySuggestion>>) {
                        if (response.isSuccessful && shouldShowSuggestions) {
                            citySuggestions = response.body() ?: emptyList()
                            val suggestions = citySuggestions.map { "${it.name}${it.state?.let { s -> ", $s" } ?: ""}, ${it.country}" }
                            suggestionAdapter.clear()
                            suggestionAdapter.addAll(suggestions)
                            suggestionAdapter.notifyDataSetChanged()
                            if (suggestions.isNotEmpty()) {
                                listPopupWindow.show()
                            } else {
                                listPopupWindow.dismiss()
                            }
                        } else {
                            listPopupWindow.dismiss()
                        }
                    }

                    override fun onFailure(call: Call<List<CitySuggestion>>, t: Throwable) {
                        Log.e("CityFetchError", "Failed to fetch city suggestions: ${t.message}, Cause: ${t.cause}", t)
                        listPopupWindow.dismiss()
                    }
                })
    }

    private fun handleSearch(query: String) {
        binding.weatherContainer.visibility = View.GONE
        binding.loadingView.visibility = View.VISIBLE
        Log.d("WeatherFetch", "Fetching weather for: $query")

        val retrofit = Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(ApiInterface::class.java)

        api.getWeatherData(query, API_KEY, TEMPERATURE_UNIT).enqueue(object : Callback<WeatherApp> {
            override fun onResponse(call: Call<WeatherApp>, response: Response<WeatherApp>) {
                Log.d("WeatherFetch", "Current weather response code: ${response.code()}")
                binding.loadingView.visibility = View.GONE
                shouldShowSuggestions = false
                if (response.isSuccessful && response.body() != null) {
                    val weatherData = response.body()!!

                    api.getForecast(query, API_KEY, TEMPERATURE_UNIT).enqueue(object : Callback<ForecastResponse> {
                        override fun onResponse(call: Call<ForecastResponse>, response: Response<ForecastResponse>) {
                            if (response.isSuccessful && response.body() != null) {
                                val forecastData = response.body()!!.list
                                val (maxTemp, minTemp) = calculateDailyTemps(forecastData, weatherData.timezone)
                                updateWeatherUI(weatherData, query, maxTemp, minTemp)
                                binding.weatherContainer.visibility = View.VISIBLE
                            } else {
                                Toast.makeText(this@MainActivity, "Failed to fetch forecast data", Toast.LENGTH_SHORT).show()
                                updateWeatherUI(weatherData, query, weatherData.main.temp_max, weatherData.main.temp_min)
                                binding.weatherContainer.visibility = View.VISIBLE
                            }
                        }

                        override fun onFailure(call: Call<ForecastResponse>, t: Throwable) {
                            Log.e("WeatherFetchError", "Forecast failed: ${t.message}", t)
                            binding.loadingView.visibility = View.GONE
                            binding.weatherContainer.visibility = View.GONE
                            shouldShowSuggestions = false
                            binding.noInternetMessage.visibility = View.VISIBLE
                            binding.noInternetMessage.text = "No internet connection. Please check your network."
                        }
                    })
                } else {
                    citySuggestions.firstOrNull()?.let { suggestion ->
                        fetchWeatherData(suggestion.name)
                        Toast.makeText(this@MainActivity, "Did you mean ${suggestion.name}?", Toast.LENGTH_SHORT).show()
                    } ?: run {
                        Toast.makeText(this@MainActivity, "City not found", Toast.LENGTH_SHORT).show()
                        binding.weatherContainer.visibility = View.GONE
                    }
                }
            }

            override fun onFailure(call: Call<WeatherApp>, t: Throwable) {
                Log.e("WeatherFetchError", "Failed: ${t.message}, Cause: ${t.cause}", t)
                binding.loadingView.visibility = View.GONE
                binding.weatherContainer.visibility = View.GONE
                shouldShowSuggestions = false
                binding.noInternetMessage.visibility = View.VISIBLE
                binding.noInternetMessage.text = "No internet connection. Please check your network."
            }
        })
    }

    private fun fetchWeatherData(cityName: String) {
        binding.weatherContainer.visibility = View.GONE
        binding.loadingView.visibility = View.VISIBLE
        Log.d("WeatherFetch", "Fetching weather for: $cityName")

        val retrofit = Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(ApiInterface::class.java)

        api.getWeatherData(cityName, API_KEY, TEMPERATURE_UNIT).enqueue(object : Callback<WeatherApp> {
            override fun onResponse(call: Call<WeatherApp>, response: Response<WeatherApp>) {
                Log.d("WeatherFetch", "Current weather response code: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val weatherData = response.body()!!

                    api.getForecast(cityName, API_KEY, TEMPERATURE_UNIT).enqueue(object : Callback<ForecastResponse> {
                        override fun onResponse(call: Call<ForecastResponse>, response: Response<ForecastResponse>) {
                            binding.loadingView.visibility = View.GONE
                            shouldShowSuggestions = false
                            if (response.isSuccessful && response.body() != null) {
                                val forecastData = response.body()!!.list
                                val (maxTemp, minTemp) = calculateDailyTemps(forecastData, weatherData.timezone)
                                updateWeatherUI(weatherData, cityName, maxTemp, minTemp)
                                binding.weatherContainer.visibility = View.VISIBLE
                            } else {
                                Toast.makeText(this@MainActivity, "Failed to fetch forecast data", Toast.LENGTH_SHORT).show()
                                updateWeatherUI(weatherData, cityName, weatherData.main.temp_max, weatherData.main.temp_min)
                                binding.weatherContainer.visibility = View.VISIBLE
                            }
                        }

                        override fun onFailure(call: Call<ForecastResponse>, t: Throwable) {
                            Log.e("WeatherFetchError", "Forecast failed: ${t.message}", t)
                            binding.loadingView.visibility = View.GONE
                            binding.weatherContainer.visibility = View.GONE
                            shouldShowSuggestions = false
                            binding.noInternetMessage.visibility = View.VISIBLE
                            binding.noInternetMessage.text = "No internet connection. Please check your network."
                        }
                    })
                } else {
                    binding.loadingView.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "City not found or invalid response", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<WeatherApp>, t: Throwable) {
                Log.e("WeatherFetchError", "Current weather failed: ${t.message}", t)
                binding.loadingView.visibility = View.GONE
                binding.weatherContainer.visibility = View.GONE
                shouldShowSuggestions = false
                binding.noInternetMessage.visibility = View.VISIBLE
                binding.noInternetMessage.text = "No internet connection. Please check your network."
            }
        })
    }

    private fun calculateDailyTemps(forecastItems: List<ForecastItem>, timezoneOffsetSeconds: Int): Pair<Double, Double> {
        // Get current time in the city's timezone
        val currentTime = System.currentTimeMillis() / 1000 + timezoneOffsetSeconds
        // Define the start and end of the current day in the city's timezone
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = currentTime * 1000
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis / 1000 - timezoneOffsetSeconds // Adjust back to UTC
        val todayEnd = todayStart + 24 * 60 * 60 // End of day

        // Filter forecast items for a wider range (±24 hours) to capture more data
        val dayForecasts = forecastItems.filter { it.dt in (todayStart - 24 * 3600)..(todayEnd + 24 * 3600) }

        // Log for debugging
        Log.d("WeatherFetch", "Day forecasts timestamps: ${dayForecasts.map { it.dt }}")
        Log.d("WeatherFetch", "Day forecasts temp_max: ${dayForecasts.map { it.main.temp_max }}")
        Log.d("WeatherFetch", "Day forecasts temp_min: ${dayForecasts.map { it.main.temp_min }}")

        // Collect all temp_max and temp_min values
        val maxTemps = dayForecasts.map { it.main.temp_max }
        val minTemps = dayForecasts.map { it.main.temp_min }

        // Compute overall max and min
        val maxTemp = maxTemps.maxOrNull() ?: 0.0
        val minTemp = minTemps.minOrNull() ?: 0.0

        // Fallback if insufficient data or minor difference
        return if (dayForecasts.size < 3 || maxTemp - minTemp < 2.0) {
            // If fewer than 3 points or difference is too small, use a heuristic
            val adjustedMax = maxTemp + 2.0 // Increase max for realistic range
            val adjustedMin = minTemp - 2.0 // Decrease min for realistic range
            Log.d("WeatherFetch", "Applying heuristic: max=$adjustedMax, min=$adjustedMin")
            Pair(adjustedMax, adjustedMin)
        } else {
            Pair(maxTemp, minTemp)
        }
    }

    private fun updateWeatherUI(weatherData: WeatherApp, cityName: String, maxTemp: Double, minTemp: Double) {
        with(binding) {
            temp.text = "${weatherData.main.temp} °C"
            weather.text = weatherData.weather.firstOrNull()?.main ?: "Unknown"
            binding.maxTemp.text = "Max Temp: %.1f °C".format(maxTemp)
            binding.minTemp.text = "Min Temp: %.1f °C".format(minTemp)
            humidity.text = "${weatherData.main.humidity} %"
            windSpeed.text = "${weatherData.wind.speed} m/s"
            sunRise.text = formatTime(weatherData.sys.sunrise.toLong())
            sunset.text = formatTime(weatherData.sys.sunset.toLong())
            sea.text = "${weatherData.main.pressure} hPa"
            condition.text = weatherData.weather.firstOrNull()?.main ?: "Unknown"
            day.text = formatDay(System.currentTimeMillis())
            date.text = formatDate()
            binding.cityName.text = cityName
        }

        sunRiseTime = weatherData.sys.sunrise.toLong()
        sunSetTime = weatherData.sys.sunset.toLong()
        updateUIForWeatherCondition(weatherData.weather.firstOrNull()?.main ?: "Unknown")
    }

    private sealed class WeatherCondition(val dayBackground: Int, val nightBackground: Int?, val animation: Int) {
        object Clear : WeatherCondition(R.drawable.sunny_background, R.drawable.clearmoon2, R.raw.sun) {
            override fun getAnimation(isDay: Boolean) = if (isDay) animation else R.raw.moon
        }
        object Cloudy : WeatherCondition(R.drawable.colud_background, R.drawable.cloudymoon4, R.raw.cloud) {
            override fun getAnimation(isDay: Boolean) = if (isDay) animation else R.raw.moon
        }
        object Rain : WeatherCondition(R.drawable.rain_background, null, R.raw.rain)
        object Snow : WeatherCondition(R.drawable.snow_background, null, R.raw.snow)
        open fun getAnimation(isDay: Boolean) = animation
    }

    private fun updateUIForWeatherCondition(condition: String) {
        val currentTime = System.currentTimeMillis() / 1000
        val isDay = currentTime in sunRiseTime..sunSetTime

        val weatherCondition = when (condition) {
            "Clear Sky", "Sunny", "Clear" -> WeatherCondition.Clear
            "Partly Clouds", "Clouds", "Overcast", "Mist", "Foggy" -> WeatherCondition.Cloudy
            "Rain", "Light Rain", "Drizzle", "Moderate Rain", "Showers", "Heavy Rain" -> WeatherCondition.Rain
            "Snow", "Light Snow", "Moderate Snow", "Heavy Snow", "Blizzard" -> WeatherCondition.Snow
            else -> WeatherCondition.Clear
        }

        val textColor = if (weatherCondition is WeatherCondition.Rain) {
            Color.BLACK
        } else {
            if (isDay) Color.BLACK else Color.WHITE
        }

        val backgroundDrawable = if (isDay) R.drawable.searchview_day else R.drawable.searchview_night

        with(binding) {
            root.setBackgroundResource(if (isDay) weatherCondition.dayBackground else weatherCondition.nightBackground ?: weatherCondition.dayBackground)
            lottieAnimationView.setAnimation(weatherCondition.getAnimation(isDay))
            lottieAnimationView.playAnimation()

            listOf(temp, weather, maxTemp, minTemp, day, date, cityName, textView2).forEach { it.setTextColor(textColor) }
            cityName.compoundDrawables[0]?.setTint(textColor)

            searchView.setBackgroundResource(backgroundDrawable)
            searchView.findViewById<android.widget.EditText>(androidx.appcompat.R.id.search_src_text)?.apply {
                setTextColor(textColor)
                setHintTextColor(textColor)
                setBackgroundColor(Color.TRANSPARENT)
            }
            searchView.findViewById<android.widget.ImageView>(androidx.appcompat.R.id.search_mag_icon)?.setColorFilter(textColor)
            searchView.findViewById<android.widget.ImageView>(androidx.appcompat.R.id.search_close_btn)?.setColorFilter(textColor)
        }
    }

    private fun formatDate(): String = DATE_FORMATTER.format(Date())
    private fun formatTime(timestamp: Long): String = TIME_FORMATTER.format(Date(timestamp * 1000))
    private fun formatDay(timestamp: Long): String = DAY_FORMATTER.format(Date(timestamp))
}
