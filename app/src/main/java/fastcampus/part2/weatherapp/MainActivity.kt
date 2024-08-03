package fastcampus.part2.weatherapp

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val retrofit = Retrofit.Builder()
            .baseUrl("http://apis.data.go.kr/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(WeatherService::class.java)

        val baseDateTime = BaseDateTime.getBaseDateTime()
        val converter = GeoPointConverter()
        val point = converter.convert(lat = 37.5532, lon = 127.1906) // lat: 위도, lon: 경도

        service.getVillageForecast(
            serviceKey = "fbK2g297uMEM8V6tRh8OrEcJYGYvS2aK/hLSkVSySexCD0yEVarZgDG7Li6ZbrOy1Wa++Irb+dZHjwnpnSDHBA==",
            baseDate = baseDateTime.baseDate,
            baseTime = baseDateTime.baseTime,
            nx = point.nx,
            ny = point.ny
        ).enqueue(object : Callback<WeatherEntity> {
            override fun onResponse(call: Call<WeatherEntity>, response: Response<WeatherEntity>) {

                // todo if(response.isSuccessful) 예외처리

                val forecastDateTimeMap = mutableMapOf<String, Forecast>()
                val forecastList =
                    response.body()?.response?.body?.items?.forecastEntities.orEmpty()

                for (forecast in forecastList) {

                    if (forecastDateTimeMap["${forecast.forecastDate}/${forecast.forecastTime}"] == null) {
                        forecastDateTimeMap["${forecast.forecastDate}/${forecast.forecastTime}"] =
                            Forecast(
                                forecastDate = forecast.forecastDate,
                                forecastTime = forecast.forecastTime
                            )
                    }
                    forecastDateTimeMap["${forecast.forecastDate}/${forecast.forecastTime}"]?.apply {

                        when (forecast.category) {
                            Category.POP -> precipitation = forecast.forecastValue.toInt()
                            Category.PTY -> precipitationType = transformRainType(forecast)
                            Category.SKY -> sky = transformSky(forecast)
                            Category.TMP -> temperature = forecast.forecastValue.toDouble()
                            else -> {}

                        }
                    }

                }
                Log.e("Forecast", forecastDateTimeMap.toString())

            }

            override fun onFailure(call: Call<WeatherEntity>, t: Throwable) {
                t.printStackTrace()
            }

        })

    }

    private fun transformRainType(forecast: ForecastEntity): String {
        return when (forecast.forecastValue.toInt()) {
            0 -> "없음"
            1 -> "비"
            2 -> "비/눈"
            3 -> "눈"
            4 -> "소나기"
            else -> ""
        }
    }

    private fun transformSky(forecast: ForecastEntity): String {
        return when (forecast.forecastValue.toInt()) {
            1 -> "맑음"
            3 -> "구름많음"
            4 -> "흐림"
            else -> ""
        }
    }
}