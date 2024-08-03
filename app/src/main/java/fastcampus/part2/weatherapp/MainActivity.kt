package fastcampus.part2.weatherapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import com.google.android.gms.location.LocationServices
import fastcampus.part2.weatherapp.databinding.ActivityMainBinding
import fastcampus.part2.weatherapp.databinding.ItemForecastBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                updateLocation()
            }

            else -> {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { //설정창으로 이동
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION))

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

    private fun updateLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener {

            Thread {
                try {
                    val addressList = Geocoder(this, Locale.KOREA).getFromLocation(
                        it.latitude,
                        it.longitude,
                        1
                    )
                    runOnUiThread {
                        binding.tvLocation.text = addressList?.get(0)?.thoroughfare.orEmpty()
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }.start()


            val retrofit = Retrofit.Builder()
                .baseUrl("http://apis.data.go.kr/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(WeatherService::class.java)

            val baseDateTime = BaseDateTime.getBaseDateTime()
            val converter = GeoPointConverter()
            val point = converter.convert(lat = 37.5017, lon = 127.0178)
            Log.e("convert", "$point")
            //val point = converter.convert(lat = it.latitude, lon = it.longitude) // lat: 위도, lon: 경도

            service.getVillageForecast(
                serviceKey = "fbK2g297uMEM8V6tRh8OrEcJYGYvS2aK/hLSkVSySexCD0yEVarZgDG7Li6ZbrOy1Wa++Irb+dZHjwnpnSDHBA==",
                baseDate = baseDateTime.baseDate,
                baseTime = baseDateTime.baseTime,
                nx = point.nx,
                ny = point.ny
            ).enqueue(object : Callback<WeatherEntity> {
                override fun onResponse(
                    call: Call<WeatherEntity>,
                    response: Response<WeatherEntity>
                ) {

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

                    val list = forecastDateTimeMap.values.toMutableList()
                    list.sortWith { f1, f2 ->
                        val f1DateTime = "${f1.forecastDate}${f1.forecastTime}"
                        val f2DateTime = "${f2.forecastDate}${f2.forecastTime}"

                        return@sortWith f1DateTime.compareTo(f2DateTime)
                    }

                    val currentForecast = list.first()

                    binding.tvTemperature.text =
                        getString(R.string.temperature_text, currentForecast.temperature)
                    binding.tvSky.text = currentForecast.weather
                    binding.tvPrecipitation.text =
                        getString(R.string.precipitation_text, currentForecast.precipitation)

                    binding.childForecastLayout.apply {

                        list.forEachIndexed { index, forecast ->
                            if (index == 0) {
                                return@forEachIndexed
                            }

                            val itemView = ItemForecastBinding.inflate(layoutInflater)

                            itemView.tvTime.text = forecast.forecastTime
                            itemView.tvWeather.text = forecast.weather
                            itemView.tvTemperature.text =
                                getString(R.string.temperature_text, forecast.temperature)

                            addView(itemView.root)

                        }

                    }

                    Log.e("Forecast", forecastDateTimeMap.toString())
                }


                override fun onFailure(call: Call<WeatherEntity>, t: Throwable) {
                    Log.e("API Call Failure", t.message.toString())
                    t.printStackTrace()
                }

            })

        }
    }
}