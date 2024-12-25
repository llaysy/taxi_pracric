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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var ordersRef: DatabaseReference
    private lateinit var mMap: GoogleMap

    // UI Components
    private lateinit var fromTextView: EditText
    private lateinit var toTextView: EditText
    private lateinit var priceTextView: EditText
    private lateinit var statusTextView: TextView
    private lateinit var driverInfoTextView: TextView
    private lateinit var paymentReminderTextView: TextView
    private lateinit var addressContainer: View
    private var currentOrderId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        initViews()
        supportFragmentManager.findFragmentById(R.id.map)?.view?.visibility = View.VISIBLE
        sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        loadSavedData()

        ordersRef = FirebaseDatabase.getInstance().getReference("orders")

        findViewById<Button>(R.id.orderButton).setOnClickListener { orderTaxi() }
        setupMap()
    }

    private fun initViews() {
        fromTextView = findViewById(R.id.fromTextView)
        toTextView = findViewById(R.id.toTextView)
        priceTextView = findViewById(R.id.priceTextView)
        statusTextView = findViewById(R.id.statusTextView)
        driverInfoTextView = findViewById(R.id.driverInfoTextView)
        paymentReminderTextView = findViewById(R.id.paymentReminderTextView)
        addressContainer = findViewById(R.id.addressContainer)
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun saveData() {
        val fromAddress = fromTextView.text.toString()
        val toAddress = toTextView.text.toString()
        val price = priceTextView.text.toString()

        if (fromAddress.isEmpty() || toAddress.isEmpty() || price.isEmpty()) {
            showToast("Пожалуйста, введите все данные.")
            return
        }

        with(sharedPreferences.edit()) {
            putString("fromAddress", fromAddress)
            putString("toAddress", toAddress)
            putString("price", price)
            apply()
        }

        showToast("Данные сохранены.")
    }

    private fun loadSavedData() {
        fromTextView.setText(sharedPreferences.getString("fromAddress", ""))
        toTextView.setText(sharedPreferences.getString("toAddress", ""))
        priceTextView.setText(sharedPreferences.getString("price", ""))
    }

    private fun orderTaxi() {
        saveData()
        statusTextView.text = "Статус: ищем водителя..."

        val fromAddress = fromTextView.text.toString()
        val toAddress = toTextView.text.toString()
        val price = priceTextView.text.toString()

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userRating = snapshot.child("averageRating").getValue(Float::class.java) ?: 5.0f
                val paymentMethod = snapshot.child("paymentMethod").getValue(String::class.java) ?: "Неизвестно"

                val newOrderId = ordersRef.push().key ?: return
                val orderData = OrderData(fromAddress, toAddress, price, "pending", userId, userRating, paymentMethod)

                ordersRef.child(newOrderId).setValue(orderData)
                currentOrderId = newOrderId

                listenToOrderStatus(newOrderId)
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Ошибка получения данных пользователя.")
            }
        })
    }

    private fun listenToOrderStatus(orderId: String) {
        ordersRef.child(orderId).child("status").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                when (snapshot.getValue(String::class.java)) {
                    "accepted" -> {
                        handleOrderAccepted(orderId)
                    }
                    "rejected" -> {
                        statusTextView.text = "Ваш заказ отклонен, ищем другого водителя"
                    }
                    "arrived" -> {
                        showToast("Водитель прибыл")
                    }
                    "finished" -> {
                        showRatingDialog()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Ошибка обновления статуса заказа.")
            }
        })
    }

    private fun handleOrderAccepted(orderId: String) {
        statusTextView.visibility = View.GONE
        addressContainer.visibility = View.GONE
        driverInfoTextView.visibility = View.VISIBLE
        paymentReminderTextView.visibility = View.VISIBLE

        // Скрыть карту
        supportFragmentManager.findFragmentById(R.id.map)?.view?.visibility = View.GONE

        loadDriverInfo(orderId)
    }

    private fun loadDriverInfo(orderId: String) {
        ordersRef.child(orderId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val driverId = snapshot.child("driverId").getValue(String::class.java)
                driverId?.let {
                    FirebaseDatabase.getInstance().getReference("drivers").child(it).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(driverSnapshot: DataSnapshot) {
                            val driverName = driverSnapshot.child("fullName").getValue(String::class.java) ?: "Неизвестно"
                            val carNumber = driverSnapshot.child("carNumber").getValue(String::class.java) ?: "Неизвестно"
                            val rating = driverSnapshot.child("averageRating").getValue(Float::class.java) ?: 0.0f
                            val phoneNumber = driverSnapshot.child("phoneNumber").getValue(String::class.java) ?: "Неизвестно"

                            driverInfoTextView.text = "Водитель: $driverName\nНомер машины: $carNumber\nРейтинг: $rating\nТелефон: $phoneNumber"
                            loadDriverLocation(driverSnapshot)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            showToast("Ошибка загрузки данных водителя.")
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Ошибка получения информации о заказе.")
            }
        })
    }

    private fun loadDriverLocation(driverSnapshot: DataSnapshot) {
        val driverLocation = LatLng(
            driverSnapshot.child("latitude").getValue(Double::class.java) ?: 0.0,
            driverSnapshot.child("longitude").getValue(Double::class.java) ?: 0.0
        )
        mMap.addMarker(MarkerOptions().position(driverLocation).title("Водитель"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(driverLocation, 15f))
    }

    private fun showRatingDialog() {
        val builder = AlertDialog.Builder(this).apply {
            setTitle("Оцените водителя")
            val ratingBar = RatingBar(this@MapsActivity).apply {
                numStars = 5
                stepSize = 1f
            }
            setView(ratingBar)

            setPositiveButton("Оценить") { dialog, _ ->
                updateDriverRating(ratingBar.rating)
                dialog.dismiss()
                showToast("Спасибо за вашу оценку!")
                navigateToHomeActivity()
            }

            setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
                navigateToHomeActivity()
            }
        }
        builder.create().show()
    }

    private fun updateDriverRating(rating: Float) {
        currentOrderId?.let { orderId ->
            ordersRef.child(orderId).child("driverId").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val driverId = snapshot.getValue(String::class.java) ?: return
                    val driverRef = FirebaseDatabase.getInstance().getReference("drivers").child(driverId)
                    driverRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(driverSnapshot: DataSnapshot) {
                            val currentRating = driverSnapshot.child("rating").getValue(Float::class.java) ?: return
                            val newRating = (currentRating + rating) / 2

                            driverRef.child("rating").setValue(newRating)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            showToast("Ошибка обновления рейтинга.")
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Ошибка получения данных водителя.")
                }
            })
        }
    }

    private fun navigateToHomeActivity() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val currentLocation = LatLng(it.latitude, it.longitude)
                mMap.addMarker(MarkerOptions().position(currentLocation).title("Вы здесь"))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
            } ?: showToast("Не удалось определить местоположение")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}