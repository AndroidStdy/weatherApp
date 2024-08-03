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

        service.getVillageForecast(
            serviceKey = "fbK2g297uMEM8V6tRh8OrEcJYGYvS2aK/hLSkVSySexCD0yEVarZgDG7Li6ZbrOy1Wa++Irb+dZHjwnpnSDHBA==",
            baseDate = "20240803",
            baseTime = "1400",
            nx = 55,
            ny = 127
        ).enqueue(object: Callback<WeatherEntity>{
            override fun onResponse(call: Call<WeatherEntity>, response: Response<WeatherEntity>) {

                // todo if(response.isSuccessful) 예외처리

                val forecastList = response.body()?.response?.body?.items?.forecastEntities.orEmpty()

                for(forecast in forecastList){
                    Log.e("Forecast", forecast.toString())
                }

            }

            override fun onFailure(call: Call<WeatherEntity>, t: Throwable) {
                t.printStackTrace()
            }

        })

    }
}