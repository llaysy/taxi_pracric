package com.example.test_pracric

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.test_pracric.user.DriverData
import com.example.test_pracric.user.OrderData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fromTextView: EditText
    private lateinit var toTextView: EditText
    private lateinit var priceTextView: EditText
    private lateinit var statusTextView: TextView
    private lateinit var driverInfoTextView: TextView
    private lateinit var paymentReminderTextView: TextView
    private lateinit var addressContainer: View
    private lateinit var ordersRef: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var mMap: GoogleMap
    private var currentOrderId: String? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)

        fromTextView = findViewById(R.id.fromTextView)
        toTextView = findViewById(R.id.toTextView)
        priceTextView = findViewById(R.id.priceTextView)
        statusTextView = findViewById(R.id.statusTextView)
        driverInfoTextView = findViewById(R.id.driverInfoTextView)
        paymentReminderTextView = findViewById(R.id.paymentReminderTextView)
        addressContainer = findViewById(R.id.addressContainer)

        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Инициализация клиента для определения местоположения
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        loadSavedData()

        ordersRef = FirebaseDatabase.getInstance().getReference("orders")

        val orderButton: Button = findViewById(R.id.orderButton)
        orderButton.setOnClickListener { orderTaxi() }
    }

    private fun saveData() {
        val fromAddress = fromTextView.text.toString()
        val toAddress = toTextView.text.toString()
        val price = priceTextView.text.toString()

        if (fromAddress.isEmpty() || toAddress.isEmpty() || price.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, введите все данные.", Toast.LENGTH_SHORT).show()
            return
        }

        with(sharedPreferences.edit()) {
            putString("fromAddress", fromAddress)
            putString("toAddress", toAddress)
            putString("price", price)
            apply()
        }

        Toast.makeText(this, "Данные сохранены.", Toast.LENGTH_SHORT).show()
    }

    private fun loadSavedData() {
        val fromAddress = sharedPreferences.getString("fromAddress", "")
        val toAddress = sharedPreferences.getString("toAddress", "")
        val price = sharedPreferences.getString("price", "")

        fromTextView.setText(fromAddress)
        toTextView.setText(toAddress)
        priceTextView.setText(price)
    }

    private fun orderTaxi() {
        saveData()
        statusTextView.text = "Статус: ищем водителя..."

        val fromAddress = fromTextView.text.toString()
        val toAddress = toTextView.text.toString()
        val price = priceTextView.text.toString()

        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userRating = snapshot.child("averageRating").getValue(Float::class.java) ?: 5.0f
                val paymentMethod = snapshot.child("paymentMethod").getValue(String::class.java) ?: "Неизвестно"

                val newOrderId = ordersRef.push().key ?: return

                val orderData = OrderData(
                    fromAddress = fromAddress,
                    toAddress = toAddress,
                    price = price,
                    status = "pending",
                    userId = userId,
                    userRating = userRating,
                    paymentMethod = paymentMethod
                )

                ordersRef.child(newOrderId).setValue(orderData)
                currentOrderId = newOrderId

                ordersRef.child(newOrderId).child("status").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        when (snapshot.getValue(String::class.java)) {
                            "accepted" -> {
                                statusTextView.visibility = View.GONE
                                addressContainer.visibility = View.GONE
                                driverInfoTextView.visibility = View.VISIBLE
                                paymentReminderTextView.visibility = View.VISIBLE

                                supportFragmentManager.beginTransaction().hide(mapFragment).commit()

                                loadDriverInfo(newOrderId)
                            }
                            "rejected" -> {
                                statusTextView.text = "Ваш заказ отклонен, ищем другого водителя"
                            }
                            "arrived" -> {
                                Toast.makeText(this@MapsActivity, "Водитель прибыл", Toast.LENGTH_SHORT).show()
                            }
                            "finished" -> {
                                showRatingDialog()
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@MapsActivity, "Ошибка обновления статуса заказа.", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MapsActivity, "Ошибка получения данных пользователя.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadDriverInfo(orderId: String) {
        ordersRef.child(orderId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val driverId = snapshot.child("driverId").getValue(String::class.java)
                driverId?.let {
                    val driversRef = FirebaseDatabase.getInstance().getReference("drivers").child(it)
                    driversRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        @SuppressLint("SetTextI18n")
                        override fun onDataChange(driverSnapshot: DataSnapshot) {
                            val driverName = driverSnapshot.child("fullName").getValue(String::class.java) ?: "Неизвестно"
                            val carNumber = driverSnapshot.child("carNumber").getValue(String::class.java) ?: "Неизвестно"
                            val rating = driverSnapshot.child("averageRating").getValue(Float::class.java) ?: 0.0f
                            val phoneNumber = driverSnapshot.child("phoneNumber").getValue(String::class.java) ?: "Неизвестно"
                            driverInfoTextView.text = "Водитель: $driverName\nНомер машины: $carNumber\nРейтинг: $rating\nТелефон: $phoneNumber"

                            val driverLocation = LatLng(driverSnapshot.child("latitude").getValue(Double::class.java) ?: 0.0,
                                driverSnapshot.child("longitude").getValue(Double::class.java) ?: 0.0)
                            mMap.addMarker(MarkerOptions().position(driverLocation).title("Водитель"))
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(driverLocation, 15f))
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@MapsActivity, "Ошибка загрузки данных водителя.", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MapsActivity, "Ошибка получения информации о заказе.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showRatingDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Оцените водителя")

        val ratingBar = RatingBar(this)
        ratingBar.numStars = 5
        ratingBar.stepSize = 1f
        builder.setView(ratingBar)

        builder.setPositiveButton("Оценить") { dialog, _ ->
            val rating = ratingBar.rating
            updateDriverRating(rating)
            dialog.dismiss()
            Toast.makeText(this, "Спасибо за вашу оценку!", Toast.LENGTH_SHORT).show()
            navigateToHomeActivity() // Переход на HomeActivity
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.dismiss()
            navigateToHomeActivity() // Переход на HomeActivity
        }

        builder.create().show()
    }

    private fun updateDriverRating(rating: Float) {
        currentOrderId?.let { orderId ->
            val orderRef = ordersRef.child(orderId)
            orderRef.child("driverId").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val driverId = snapshot.getValue(String::class.java) ?: return
                    val driverRef = FirebaseDatabase.getInstance().getReference("drivers").child(driverId)
                    driverRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(driverSnapshot: DataSnapshot) {
                            val driverData = driverSnapshot.getValue(DriverData::class.java) ?: return
                            val currentRating = driverData.rating
                            val newRating = (currentRating + rating) / 2 // Среднее арифметическое

                            driverRef.child("rating").setValue(newRating)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@MapsActivity, "Ошибка обновления рейтинга.", Toast.LENGTH_SHORT).show()
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MapsActivity, "Ошибка получения данных водителя.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun navigateToHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish() // Закрываем текущую активность
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Проверка разрешений
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Запрос разрешений
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        // Получение последнего известного местоположения
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val currentLocation = LatLng(location.latitude, location.longitude)
                mMap.addMarker(MarkerOptions().position(currentLocation).title("Вы здесь"))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
            } else {
                // Обработка случая, если местоположение недоступно
                Toast.makeText(this, "Не удалось определить местоположение", Toast.LENGTH_SHORT).show()
            }
        }
    }
}