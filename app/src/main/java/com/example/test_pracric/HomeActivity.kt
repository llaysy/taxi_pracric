package com.example.test_pracric

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.test_pracric.user.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var drawerLayout: DrawerLayout

    // UI Components
    private lateinit var profileName: TextView
    private lateinit var profileLocation: TextView
    private lateinit var profilePaymentMethod: TextView
    private lateinit var profileRating: TextView
    private lateinit var btnWhereTo: Button
    private lateinit var profileText: TextView
    private lateinit var settingsText: TextView
    private lateinit var hamburgerIcon: ImageView
    private lateinit var logoutText: TextView
    private lateinit var paymentMethodLabel: TextView
    private lateinit var infoText: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        Log.d("HomeActivity", "HomeActivity started")

        // Инициализация компонентов
        initViews()

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Установка обработчиков событий
        setEventListeners()

        // Загрузка данных пользователя
        loadUserData()
    }

    private fun initViews() {
        profileName = findViewById(R.id.profile_name)
        profileLocation = findViewById(R.id.profile_location_value)
        profilePaymentMethod = findViewById(R.id.profile_payment_method_value)
        profileRating = findViewById(R.id.profile_rating)
        drawerLayout = findViewById(R.id.drawer_layout)
        btnWhereTo = findViewById(R.id.btnWhereTo)
        profileText = findViewById(R.id.profile_text)
        settingsText = findViewById(R.id.settings_text)
        hamburgerIcon = findViewById(R.id.hamburger_icon)
        logoutText = findViewById(R.id.logout_text)
        paymentMethodLabel = findViewById(R.id.payment_method_label)
        infoText = findViewById(R.id.info_text)
    }

    private fun setEventListeners() {
        btnWhereTo.setOnClickListener {
            startActivity(Intent(this, MapsActivity::class.java))
        }

        infoText.setOnClickListener {
            startActivity(Intent(this, InfoActivity::class.java))
        }

        settingsText.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        hamburgerIcon.setOnClickListener {
            toggleDrawer()
        }

        logoutText.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        profileText.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        paymentMethodLabel.setOnClickListener {
            showPaymentMethodDialog()
        }

        // Закрытие бокового меню при нажатии на пустое пространство
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                hamburgerIcon.visibility = if (slideOffset > 0) View.GONE else View.VISIBLE
            }

            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })
    }

    private fun loadUserData() {
        auth.currentUser?.uid?.let { userId ->
            // Launch a coroutine on the Main scope
            lifecycleScope.launch {
                try {
                    val snapshot = withContext(Dispatchers.IO) {
                        database.child("users").child(userId).get().await() // Await the result
                    }
                    val userData = snapshot.getValue(UserData::class.java)
                    userData?.let {
                        profileName.text = it.name
                        profileLocation.text = it.location
                        profilePaymentMethod.text = it.paymentMethod
                        profileRating.text = "Рейтинг: ${it.rating}"
                        paymentMethodLabel.text = "Способ оплаты: ${it.paymentMethod}"
                    } ?: showToast("Данные пользователя не найдены")
                } catch (e: Exception) {
                    showToast("Ошибка получения данных: ${e.message}")
                }
            }
        }
    }

    private fun showPaymentMethodDialog() {
        val paymentMethods = arrayOf("Наличные", "Перевод на карту")
        val icons = intArrayOf(R.drawable.ic_cash, R.drawable.ic_card)

        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment_method, null)
        builder.setView(dialogView)

        val paymentMethodsList = dialogView.findViewById<ListView>(R.id.paymentMethodsList)
        paymentMethodsList.adapter = PaymentMethodAdapter(this, paymentMethods, icons)

        builder.setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()

        paymentMethodsList.setOnItemClickListener { _, _, position, _ ->
            val selectedMethod = paymentMethods[position]
            paymentMethodLabel.text = "Способ оплаты: $selectedMethod"
            updatePaymentMethodInDatabase(selectedMethod)
            dialog.dismiss()
        }
    }

    private fun updatePaymentMethodInDatabase(selectedMethod: String) {
        auth.currentUser?.uid?.let { userId ->
            database.child("users").child(userId).child("paymentMethod").setValue(selectedMethod)
                .addOnSuccessListener {
                    showToast("Способ оплаты обновлен")
                }
                .addOnFailureListener { e ->
                    showToast("Ошибка обновления: ${e.message}")
                }
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this).apply {
            setTitle("Подтверждение выхода")
            setMessage("Вы уверены, что хотите выйти из аккаунта?")
            setPositiveButton("Да") { _, _ -> logout() }
            setNegativeButton("Нет") { dialog, _ -> dialog.dismiss() }
            show()
        }
    }

    private fun logout() {
        auth.signOut()
        showToast("Вы вышли из аккаунта")
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}