# 🌤️ WeatherApp – Real-Time Weather with Smart UI

WeatherApp is a dynamic and visually engaging Android application built using **Kotlin**, integrating **OpenWeatherMap API**, **Lottie animations**, and **Retrofit** to provide accurate real-time weather updates. The app allows users to search for cities and view essential weather information with beautiful UI transitions based on the current conditions.

---

## 🚀 Features

- 🔍 **City Search Support** (via keyboard or search icon)
- 📊 **Real-time Weather Data**
  - Temperature (°C)
  - Humidity (%)
  - Wind Speed (m/s)
  - Sunrise & Sunset Time
  - Sea Level Pressure (hPa)
- 🔄 **Daily Highs and Lows**  
  - Max/min temperature precision via **5-day/3-hour forecast API**
- 🎨 **Weather-based Dynamic UI**  
  - Backgrounds and Lottie animations change based on weather condition
- 🧭 **Timezone-aware Weather Logic**
- 🚫 **Fallback UI for Network Failures or Invalid Cities**
- 🔧 **Built with Clean Architecture** using ViewBinding and Retrofit

---

## 🛠️ Tech Stack

- **Language**: Kotlin  
- **IDE**: Android Studio  
- **API**: [OpenWeatherMap API](https://openweathermap.org/)  
- **Network**: Retrofit  
- **UI/UX**: Lottie Animations + ViewBinding  
- **Date/Time Handling**: Java SimpleDateFormat, Timezone-aware calculations

---

## 📸 Screenshots

<table>
  <tr>
    <td><strong>🔍 Search View</strong></td>
    <td><strong>🌤️ Weather UI</strong></td>
  </tr>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/4ccfb949-ef09-4619-98f4-f546d7d2ab2e" width="300"/></td>
    <td><img src="https://github.com/user-attachments/assets/7f7dd5da-ba8c-46ab-b423-e03a6317e648" width="300"/></td>
  </tr>
</table>



> Add your actual screenshots in a `/screenshots` folder and link here.

---

## 🌐 API Setup

1. Get your free API key from [OpenWeatherMap](https://openweathermap.org/api)
2. Add your key in the Retrofit call inside `MainActivity.kt`:
   ```kotlin
   val response = retrofit.getWeatherData(cityName, "YOUR_API_KEY", "metric")

💡 Learnings
 - Handling real-time API data and UI sync
 - Debugging timezone mismatches and performance tuning
 - Implementing clean error handling and fallback UI
 - Improving user experience with animation and responsiveness   

🔗 Connect with Me
👨‍💻 Developed by [Vikas Singh](www.linkedin.com/in/vikas-singh-android)
📂 GitHub: @silentboy-07
