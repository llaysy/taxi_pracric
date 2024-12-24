package com.example.test_pracric

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.test_pracric.user.OrderData
import com.example.test_pracric.user.UserData
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

class DriverOrderActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var orderDetailsTextView: TextView
    private lateinit var acceptButton: Button
    private lateinit var rejectButton: Button
    private lateinit var arrivedButton: Button
    private lateinit var finishButton: Button
    private lateinit var endShiftButton: Button
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var databaseRef: DatabaseReference
    private lateinit var driverRef: DatabaseReference
    private var currentOrderId: String? = null
    private lateinit var mMap: GoogleMap
    private val driverId by lazy { FirebaseAuth.getInstance().currentUser?.uid }
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_order)

        orderDetailsTextView = findViewById(R.id.orderDetailsTextView)
        acceptButton = findViewById(R.id.acceptButton)
        rejectButton = findViewById(R.id.rejectButton)
        arrivedButton = findViewById(R.id.arrivedButton)
        finishButton = findViewById(R.id.finishButton)
        endShiftButton = findViewById(R.id.endShiftButton)
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        databaseRef = FirebaseDatabase.getInstance().getReference("orders")
        driverRef = FirebaseDatabase.getInstance().getReference("drivers").child(driverId!!)

        // Инициализация клиента для определения местоположения
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mapFragment.getMapAsync(this)
        listenForOrders()

        // Делаем кнопку "Закончить работу" видимой в начале
        endShiftButton.visibility = View.VISIBLE

        acceptButton.setOnClickListener {
            currentOrderId?.let { orderId ->
                driverId?.let { driverId ->
                    databaseRef.child(orderId).child("status").setValue("accepted")
                    databaseRef.child(orderId).child("driverId").setValue(driverId)
                    Toast.makeText(this, "Заказ принят", Toast.LENGTH_SHORT).show()
                    arrivedButton.visibility = View.VISIBLE
                    acceptButton.visibility = View.GONE
                    rejectButton.visibility = View.GONE
                    endShiftButton.visibility = View.GONE // Скрываем кнопку "Закончить работу" на время поездки
                }
            }
        }

        rejectButton.setOnClickListener {
            currentOrderId?.let { orderId ->
                databaseRef.child(orderId).child("status").setValue("rejected")
                hideOrderDetails()
            }
        }

        arrivedButton.setOnClickListener {
            mapFragment.view?.visibility = View.VISIBLE
            finishButton.visibility = View.VISIBLE
            arrivedButton.visibility = View.GONE
            currentOrderId?.let { orderId ->
                databaseRef.child(orderId).child("status").setValue("arrived")
            }
            Toast.makeText(this, "Водитель прибыл", Toast.LENGTH_SHORT).show()
        }

        finishButton.setOnClickListener {
            currentOrderId?.let { orderId ->
                databaseRef.child(orderId).child("status").setValue("finished")
                showRatingDialog(orderId)
                mapFragment.view?.visibility = View.GONE
            }
        }

        endShiftButton.setOnClickListener {
            setDriverInactive()
            val intent = Intent(this, DriverHomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

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

    private fun listenForOrders() {
        databaseRef.orderByChild("status").equalTo("pending").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val order = snapshot.getValue(OrderData::class.java)
                order?.let {
                    currentOrderId = snapshot.key
                    displayOrder(it)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val order = snapshot.getValue(OrderData::class.java)
                if (order?.status == "accepted" || order?.status == "rejected") {
                    hideOrderDetails()
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun displayOrder(order: OrderData) {
        orderDetailsTextView.text = "Откуда: ${order.fromAddress}\nКуда: ${order.toAddress}\nЦена: ${order.price}\n" +
                "Рейтинг пользователя: ${order.userRating}\nМетод оплаты: ${order.paymentMethod}"
        acceptButton.visibility = View.VISIBLE
        rejectButton.visibility = View.VISIBLE
        arrivedButton.visibility = View.GONE
        endShiftButton.visibility = View.GONE // Скрываем кнопку "Закончить работу" на время поездки
    }

    private fun hideOrderDetails() {
        orderDetailsTextView.text = ""
        acceptButton.visibility = View.GONE
        rejectButton.visibility = View.GONE
        arrivedButton.visibility = View.GONE
        finishButton.visibility = View.GONE
        endShiftButton.visibility = View.VISIBLE // Показываем кнопку "Закончить работу" в исходном состоянии
    }

    private fun showRatingDialog(orderId: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Оцените пассажира")

        val ratingBar = RatingBar(this)
        ratingBar.numStars = 5
        ratingBar.stepSize = 1f
        builder.setView(ratingBar)

        builder.setPositiveButton("Оценить") { dialog, _ ->
            val rating = ratingBar.rating
            updatePassengerRating(orderId, rating)
            dialog.dismiss()
            Toast.makeText(this, "Спасибо за вашу оценку!", Toast.LENGTH_SHORT).show()
            resetUI()
            endShiftButton.visibility = View.VISIBLE // Показываем кнопку "Закончить работу" после выставления рейтинга
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.dismiss()
            resetUI()
            endShiftButton.visibility = View.VISIBLE // Показываем кнопку "Закончить работу" после отмены диалога
        }

        builder.create().show()
    }

    private fun updatePassengerRating(orderId: String, rating: Float) {
        val orderRef = databaseRef.child(orderId)
        orderRef.child("userId").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userId = snapshot.getValue(String::class.java) ?: return
                val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
                userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(userSnapshot: DataSnapshot) {
                        val userData = userSnapshot.getValue(UserData::class.java) ?: return
                        val currentRating = userData.rating
                        val newRating = (currentRating + rating) / 2 // Среднее арифметическое

                        userRef.child("rating").setValue(newRating)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@DriverOrderActivity, "Ошибка обновления рейтинга.", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DriverOrderActivity, "Ошибка получения данных пользователя.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setDriverInactive() {
        driverRef.child("onlineStatus").setValue(false).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Вы завершили смену. Статус обновлен.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Ошибка при обновлении статуса.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetUI() {
        orderDetailsTextView.text = "Детали заказа"
        acceptButton.visibility = View.GONE
        rejectButton.visibility = View.GONE
        arrivedButton.visibility = View.GONE
        finishButton.visibility = View.GONE
        mapFragment.view?.visibility = View.GONE
        currentOrderId = null
    }
}