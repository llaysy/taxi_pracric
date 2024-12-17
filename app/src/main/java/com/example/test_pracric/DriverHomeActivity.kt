package com.example.test_pracric

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.test_pracric.user.DriverData
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class DriverHomeActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var profileName: TextView
    private lateinit var profileLocation: TextView
    private lateinit var profileRating: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_driver_home)

        drawerLayout = findViewById(R.id.drawer_layout)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val navigationView: NavigationView = findViewById(R.id.nav_view)

        // Получение заголовка навигационного меню
        val headerView = navigationView.getHeaderView(0)
        profileName = headerView.findViewById(R.id.profile_name)
        profileLocation = headerView.findViewById(R.id.profile_location)
        profileRating = headerView.findViewById(R.id.profile_rating)

        // Установка слушателя на элемент "Выйти из аккаунта"
        val logoutText: TextView = navigationView.findViewById(R.id.logout_text)
        logoutText.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Установка слушателя на другие элементы навигационного меню
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    // Обработка нажатия на "Профиль"
                    true
                }
                R.id.nav_settings -> {
                    // Обработка нажатия на "Настройки"
                    true
                }
                else -> false
            }
        }

        // Открытие бокового меню при нажатии на иконку
        findViewById<ImageView>(R.id.hamburger_icon).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Получение данных пользователя
        getUserData()
    }

    private fun getUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            // Измените путь на "drivers" вместо "users"
            database.child("drivers").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val driverData = snapshot.getValue(DriverData::class.java)
                    if (driverData != null) {
                        Log.d("DriverData", "Driver Name: ${driverData.fullName}")
                        profileName.text = driverData.fullName
                        profileLocation.text = driverData.email // или любое другое поле
                        profileRating.text = "Рейтинг: ${driverData.rating}"
                    } else {
                        Toast.makeText(this@DriverHomeActivity, "Данные водителя не найдены", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@DriverHomeActivity, "Ошибка получения данных: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            profileName.text = "Неизвестный пользователь"
        }
    }

    private fun showLogoutConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Подтверждение выхода")
        builder.setMessage("Вы уверены, что хотите выйти из аккаунта?")
        builder.setPositiveButton("Да") { dialog, _ ->
            logout()
        }
        builder.setNegativeButton("Нет") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun logout() {
        auth.signOut()
        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Закрытие текущей активности
    }

    override fun onBackPressed() {
        // Закрытие бокового меню при нажатии кнопки назад
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}