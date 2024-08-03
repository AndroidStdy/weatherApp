package fastcampus.part2.weatherapp

data class Forecast(
    val forecastDate: String,
    val forecastTime: String,

    var temperature: Double = 0.0,
    var sky: String = "",
    var precipitation: Int = 0, // 강수량
    var precipitationType: String = "" // 강수형태
){

    val weather: String
        get(){
            return if(precipitationType == "" || precipitationType == "없음"){
                sky
            }else{
                precipitationType
            }
        }
}
