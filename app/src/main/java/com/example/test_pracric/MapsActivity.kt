package com.example.test_pracric

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Модель для ответа от Geocoding API
data class GeocodingResponse(val results: List<Result>)
data class Result(val geometry: Geometry)
data class Geometry(val location: GeocodingLocation)
data class GeocodingLocation(val lat: Double, val lng: Double)

// Интерфейс для Geocoding API
interface GeocodingApi {
    @GET("maps/api/geocode/json")
    fun getCoordinates(
        @Query("address") address: String,
        @Query("key") apiKey: String
    ): Call<GeocodingResponse>
}

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var userLocation: LatLng

    private lateinit var fromTextView: EditText
    private lateinit var toTextView: EditText
    private lateinit var costTextView: TextView

    // Callback for permission request
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            getLastLocation()
        } else {
            Toast.makeText(this, "Разрешение на доступ к местоположению не предоставлено.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        fromTextView = findViewById(R.id.fromTextView)
        toTextView = findViewById(R.id.toTextView)
        costTextView = findViewById(R.id.costTextView)

        val orderButton: Button = findViewById(R.id.orderButton)
        orderButton.setOnClickListener { orderTaxi() }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLastLocation()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                userLocation = LatLng(location.latitude, location.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                map.addMarker(MarkerOptions().position(userLocation).title("Вы здесь"))
            }
        }
    }

    private fun orderTaxi() {
        val fromAddress = fromTextView.text.toString()
        val toAddress = toTextView.text.toString()

        if (fromAddress.isEmpty() || toAddress.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, введите оба адреса.", Toast.LENGTH_SHORT).show()
            return
        }

        getLatLngFromAddress(toAddress) // Запускаем асинхронный запрос для получения координат
    }

    private fun calculateAndDisplayCost(start: LatLng, end: LatLng) {
        val distance = FloatArray(1)
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, distance)
        val distanceInKm = distance[0] / 1000
        val cost = distanceInKm * 20 // Пример расчета стоимости

        costTextView.text = "Стоимость: ${cost.toInt()}₽"
    }

    private fun getLatLngFromAddress(address: String) {
        val apiKey = "AIzaSyDWjji17-4YYxzrDNoh9oo60HKg0vNnBFw" // Замените на ваш ключ API
        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val geocodingApi = retrofit.create(GeocodingApi::class.java)
        val call = geocodingApi.getCoordinates(address, apiKey)

        call.enqueue(object : Callback<GeocodingResponse> {
            override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val results = response.body()?.results
                    if (results != null && results.isNotEmpty()) {
                        val location = results[0].geometry.location
                        val latLng = LatLng(location.lat, location.lng)
                        map.addMarker(MarkerOptions().position(latLng).title("Точка назначения"))
                        calculateAndDisplayCost(userLocation, latLng) // Рассчитываем стоимость
                    } else {
                        Toast.makeText(this@MapsActivity, "Адрес не найден", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MapsActivity, "Ошибка получения координат", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                Toast.makeText(this@MapsActivity, "Ошибка сети: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}