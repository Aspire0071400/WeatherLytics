package com.aspire.weatherlytics.Activites

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.aspire.weatherlytics.Activites.POJO.WeatherModel
import com.aspire.weatherlytics.Activites.Utils.ApiUtils
import com.aspire.weatherlytics.R
import com.aspire.weatherlytics.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationproviderClient : FusedLocationProviderClient
    private lateinit var activityMainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = DataBindingUtil.setContentView(this,R.layout.activity_main)
        supportActionBar?.hide()
        fusedLocationproviderClient = LocationServices.getFusedLocationProviderClient(this)
        activityMainBinding.mainLayout.visibility = View.GONE
        getCurrentLocation()

    }


    private fun getCurrentLocation() {
        if(checkPermissions()){
            if(isLocationEnabled()){

                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                   requestPermission()
                    return
                }
                fusedLocationproviderClient.lastLocation.addOnCompleteListener(this) {
                    task ->
                    val location : Location?=task.result
                    if(location == null){
                        Toast.makeText(this,"Internal Error",Toast.LENGTH_SHORT).show()
                    }else{
                        fetchCurrentLocationWeather(location.latitude.toString(),location.longitude.toString())
                    }
                }

            }else{
                Toast.makeText(this,"Turn on Location",Toast.LENGTH_SHORT).show()
                val i = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(i)


            }
        }else{
            requestPermission()
        }
    }

    private fun fetchCurrentLocationWeather(latitude: String, longitude: String) {

        ApiUtils.getApiInterface()?.getCurrentWeatherData(latitude,longitude,apiKey)?.enqueue(object :Callback<WeatherModel>{
            override fun onResponse(call: Call<WeatherModel>, response: Response<WeatherModel>) {
                if(response.isSuccessful){
                    setDataOnViews(response.body())
                }
            }

            override fun onFailure(call: Call<WeatherModel>, t: Throwable) {
                Toast.makeText(applicationContext,"Api Error",Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun KelvinToCelsius(temp: Double): Double {
        var intTemp = temp
        intTemp = intTemp.minus(273)
        return intTemp.toBigDecimal().setScale(1,RoundingMode.UP).toDouble()
    }

    private fun setDataOnViews(body: WeatherModel?) {
        val sdf = SimpleDateFormat("DD/MM/YYYY hh:mm")
        val currentDate = sdf.format(Date())

        activityMainBinding.dateText.text = currentDate
        activityMainBinding.maxTemp.text = "Day "+KelvinToCelsius(body!!.main.temp_max) +"°"
        activityMainBinding.minTemp.text = "Night "+KelvinToCelsius(body!!.main.temp_min) +"°"
        activityMainBinding.currentTemp.text = ""+KelvinToCelsius(body!!.main.temp) +"°"
        activityMainBinding.humidityTv.text = body.main.humidity.toString()
        activityMainBinding.groundTv.text = body.main.grnd_level.toString()
        activityMainBinding.sealevelTv.text = body.main.sea_level.toString()
        activityMainBinding.countryTv.text = body.sys.country.toString()
        activityMainBinding.sunriseTv.text = timeStampToLocalDate(body.sys.sunrise.toLong())
        activityMainBinding.sunsetTv.text = timeStampToLocalDate(body.sys.sunset.toLong())
        activityMainBinding.windspeedTv.text = body.wind.speed.toString() +"m/s"
        activityMainBinding.pressureTv.text = body.main.pressure.toString() +"%"
        activityMainBinding.conditionsTv.text = body.weather[0].main
        activityMainBinding.cityName.setText(body.name)
        activityMainBinding.weatherText.text = body.weather[0].main.toString()

        updateUI(body.weather[0].id)


    }

    private fun updateUI(id: kotlin.Int) {
        if(id in 200..232){//ThunderStorm

        }else if (id in 500..531){//Rain

        }else if (id in 600..622){//Snow

        }else if (id in 801..804){//Clouds

        }else if (id == 800){//Clear

        }else if(id == 721){//Haze

        }else if(id == 741){//Fog

        }
    }

    private fun timeStampToLocalDate(timeStamp: Long): String {
        val localTime = timeStamp.let{
            Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).toLocalTime()
        }
        return localTime.toString()
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,android.Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager : LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun checkPermissions() : Boolean{
        return (ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    companion object{
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
        const val apiKey = "c82ee33a8c37820d217230a1ac81cc6f"
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == PERMISSION_REQUEST_ACCESS_LOCATION){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(applicationContext,"Granted",Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            }
            else{
                Toast.makeText(applicationContext,"Denied",Toast.LENGTH_SHORT).show()
            }
        }
    }

}