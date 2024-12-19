package com.example.test_pracric

import android.Manifest
import android.app.Activity
import android.content.Intent
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
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.awaitResponse
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Модель для ответа от Geocoding API
data class GeocodingResponse(val results: List<Result>)
data class Result(val geometry: Geometry)
data class Geometry(val location: GeocodingLocation)
data class GeocodingLocation(val lat: Double, val lng: Double)

// Модель для ответа от Directions API
data class DirectionsResponse(val routes: List<Route>)
data class Route(val legs: List<Leg>)
data class Leg(val distance: Distance, val duration: Duration, val steps: List<Step>)
data class Distance(val text: String, val value: Int)
data class Duration(val text: String, val value: Int)
data class Step(val distance: Distance, val duration: Duration, val end_location: GeocodingLocation, val start_location: GeocodingLocation)

// Интерфейс для Geocoding API
interface GeocodingApi {
    @GET("maps/api/geocode/json")
    fun getCoordinates(@Query("address") address: String, @Query("key") apiKey: String): Call<GeocodingResponse>
}

// Интерфейс для Directions API
interface DirectionsApi {
    @GET("maps/api/directions/json")
    fun getRoute(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") apiKey: String
    ): Call<DirectionsResponse>
}

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var userLocation: LatLng

    private lateinit var fromTextView: EditText
    private lateinit var toTextView: EditText
    private lateinit var costTextView: TextView

    private val AUTOCOMPLETE_REQUEST_CODE = 1
    private val PLACE_AUTOCOMPLETE_REQUEST_CODE = 2

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

        // Инициализация Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.MAPS_API_KEY))
        }

        fromTextView = findViewById(R.id.fromTextView)
        toTextView = findViewById(R.id.toTextView)
        costTextView = findViewById(R.id.costTextView)

        val orderButton: Button = findViewById(R.id.orderButton)
        orderButton.setOnClickListener { orderTaxi() }

        val showRouteButton: Button = findViewById(R.id.showRouteButton)
        showRouteButton.setOnClickListener { showRoute() }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Обработчики кликов для автозаполнения
        fromTextView.setOnClickListener { showAutocomplete(true) }
        toTextView.setOnClickListener { showAutocomplete(false) }
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
            } else {
                Toast.makeText(this, "Не удалось получить текущее местоположение.", Toast.LENGTH_SHORT).show()
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

        getLatLngFromAddress(toAddress) { latLng ->
            if (latLng != null) {
                map.addMarker(MarkerOptions().position(latLng).title("Точка назначения"))
                calculateAndDisplayCost(userLocation, latLng) // Рассчитываем стоимость
            } else {
                Toast.makeText(this, "Не удалось получить координаты", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculateAndDisplayCost(start: LatLng, end: LatLng) {
        val distance = FloatArray(1)
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, distance)
        val distanceInKm = distance[0] / 1000
        val cost = distanceInKm * 20 // Пример расчета стоимости

        costTextView.text = "Стоимость: ${cost.toInt()}₽"
    }

    private fun getLatLngFromAddress(address: String, callback: (LatLng?) -> Unit) {
        val apiKey = getString(R.string.MAPS_API_KEY)
        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val geocodingApi = retrofit.create(GeocodingApi::class.java)
        val call = geocodingApi.getCoordinates(address, apiKey)

        // Используем Coroutine для сетевых запросов
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = call.awaitResponse()
                if (response.isSuccessful) {
                    val results = response.body()?.results
                    if (results != null && results.isNotEmpty()) {
                        val location = results[0].geometry.location
                        val latLng = LatLng(location.lat, location.lng)
                        withContext(Dispatchers.Main) {
                            callback(latLng)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MapsActivity, "Адрес не найден", Toast.LENGTH_SHORT).show()
                            callback(null)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MapsActivity, "Ошибка API: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                        callback(null)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MapsActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_SHORT).show()
                    callback(null)
                }
            }
        }
    }

    private fun showRoute() {
        val fromAddress = fromTextView.text.toString()
        val toAddress = toTextView.text.toString()

        if (fromAddress.isEmpty() || toAddress.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, введите оба адреса.", Toast.LENGTH_SHORT).show()
            return
        }

        getLatLngFromAddress(fromAddress) { fromLatLng ->
            getLatLngFromAddress(toAddress) { toLatLng ->
                if (fromLatLng != null && toLatLng != null) {
                    getDirections(fromLatLng, toLatLng)
                } else {
                    Toast.makeText(this, "Не удалось получить координаты.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getDirections(origin: LatLng, destination: LatLng) {
        val apiKey = getString(R.string.MAPS_API_KEY) // Замените на ваш ключ API
        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val directionsApi = retrofit.create(DirectionsApi::class.java)
        val call = directionsApi.getRoute("${origin.latitude},${origin.longitude}", "${destination.latitude},${destination.longitude}", apiKey)

        call.enqueue(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val routes = response.body()?.routes
                    if (routes != null && routes.isNotEmpty()) {
                        val leg = routes[0].legs[0]
                        val distance = leg.distance.text
                        val duration = leg.duration.text
                        Toast.makeText(this@MapsActivity, "Расстояние: $distance, Время в пути: $duration", Toast.LENGTH_SHORT).show()

                        // Рисуем маршрут
                        drawRoute(routes[0])
                    } else {
                        Toast.makeText(this@MapsActivity, "Маршрут не найден", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MapsActivity, "Ошибка получения маршрута", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                Toast.makeText(this@MapsActivity, "Ошибка сети: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun drawRoute(route: Route) {
        val polylineOptions = PolylineOptions()
        for (step in route.legs[0].steps) {
            val start = step.start_location
            polylineOptions.add(LatLng(start.lat, start.lng))
        }
        map.addPolyline(polylineOptions)
    }

    // Метод для показа автозаполнения адреса
    private fun showAutocomplete(isFrom: Boolean) {
        val fields = listOf(Place.Field.ID, Place.Field.NAME)

        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
            .setCountry("RU") // Устанавливаем страну на Россию
            .build(this)
        startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    // Получаем выбранное место
                    val place = Autocomplete.getPlaceFromIntent(data!!)
                    if (fromTextView.hasFocus()) {
                        fromTextView.setText(place.name) // Устанавливаем выбранный адрес в поле "Откуда"
                    } else if (toTextView.hasFocus()) {
                        toTextView.setText(place.name) // Устанавливаем выбранный адрес в поле "Куда"
                    }
                }
                Activity.RESULT_CANCELED -> {
                    // Обработка отмены действия
                    Toast.makeText(this, "Автозаполнение отменено", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Обработка ошибки
                    Toast.makeText(this, "Произошла ошибка при получении адреса.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}