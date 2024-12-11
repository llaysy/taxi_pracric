package com.example.test_pracric

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.test_pracric.user.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var profileName: TextView
    private lateinit var profileLocation: TextView
    private lateinit var profilePaymentMethod: TextView
    private lateinit var profileRating: TextView
    private lateinit var profileAvatar: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var hamburgerIcon: ImageView
    private lateinit var logoutText: TextView
    private lateinit var paymentMethodLabel: TextView
    private lateinit var btnWhereTo: Button // Кнопка "Куда едем?"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Инициализация элементов
        tvWelcome = findViewById(R.id.tvWelcome)
        profileName = findViewById(R.id.profile_name)
        profileLocation = findViewById(R.id.profile_location_value)
        profilePaymentMethod = findViewById(R.id.profile_payment_method_value)
        profileRating = findViewById(R.id.profile_rating)
        profileAvatar = findViewById(R.id.profile_avatar)
        drawerLayout = findViewById(R.id.drawer_layout)
        hamburgerIcon = findViewById(R.id.hamburger_icon)
        logoutText = findViewById(R.id.logout_text)
        paymentMethodLabel = findViewById(R.id.payment_method_label)
        btnWhereTo = findViewById(R.id.btnWhereTo) // Инициализация кнопки

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Обработчик нажатия на иконку гамбургера
        hamburgerIcon.setOnClickListener {
            toggleDrawer()
        }

        // Обработчик нажатия на "Выйти из аккаунта"
        logoutText.setOnClickListener {
            showLogoutConfirmationDialog()
        }

//        // Обработчик нажатия на кнопку "Куда едем?"
//        btnWhereTo.setOnClickListener {
//            val intent = Intent(this, MapActivity::class.java)
//            startActivity(intent)
//        }

        // Получение данных пользователя из базы данных
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userData = snapshot.getValue(UserData::class.java)
                    if (userData != null) {
                        profileName.text = userData.name
                        profileLocation.text = userData.location
                        profilePaymentMethod.text = userData.paymentMethod
                        profileRating.text = "Рейтинг: ${userData.rating}"
                        tvWelcome.text = "Добро пожаловать, ${userData.name}!"
                        paymentMethodLabel.text = "Способ оплаты: ${userData.paymentMethod}"
                    } else {
                        Toast.makeText(this@HomeActivity, "Данные пользователя не найдены", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HomeActivity, "Ошибка получения данных: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            tvWelcome.text = "Добро пожаловать!"
        }

        // Обработчик нажатия на способ оплаты
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

    private fun showPaymentMethodDialog() {
        val paymentMethods = arrayOf("Наличные", "Перевод на карту")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Выберите способ оплаты")

        builder.setItems(paymentMethods) { dialog, which ->
            val selectedMethod = paymentMethods[which]
            paymentMethodLabel.text = "Способ оплаты: $selectedMethod"

            // Обновить способ оплаты в базе данных
            updatePaymentMethodInDatabase(selectedMethod)
        }

        builder.setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun updatePaymentMethodInDatabase(selectedMethod: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.child("users").child(userId).child("paymentMethod").setValue(selectedMethod)
                .addOnSuccessListener {
                    Toast.makeText(this, "Способ оплаты обновлен", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка обновления: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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
        finish()
    }

    private fun toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}