package com.example.test_pracric

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test_pracric.user.DriverData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {
    private lateinit var etFullName: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var etCarNumber: EditText
    private lateinit var etLicenseNumber: EditText
    private lateinit var etCarModel: EditText
    private lateinit var btnRegister: Button
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Инициализация Firebase
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        // Инициализация компонентов
        etFullName = findViewById(R.id.etFullName)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        etCarNumber = findViewById(R.id.etCarNumber)
        etLicenseNumber = findViewById(R.id.etLicenseNumber)
        etCarModel = findViewById(R.id.etCarModel)
        btnRegister = findViewById(R.id.btnRegister)

        // Установка обработчика кнопки "Зарегистрироваться"
        btnRegister.setOnClickListener {
            registerDriver()
        }
    }

    private fun registerDriver() {
        val fullName = etFullName.text.toString().trim()
        val phoneNumber = etPhoneNumber.text.toString().trim()
        val carNumber = etCarNumber.text.toString().trim()
        val licenseNumber = etLicenseNumber.text.toString().trim()
        val carModel = etCarModel.text.toString().trim()

        // Проверка на пустые поля
        if (fullName.isEmpty() || phoneNumber.isEmpty() || carNumber.isEmpty() || licenseNumber.isEmpty() || carModel.isEmpty()) {
            Toast.makeText(this, "Все поля должны быть заполнены", Toast.LENGTH_SHORT).show()
            return
        }

        // Получение текущего пользователя
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            // Создание объекта водителя
            val driverData = DriverData(fullName, phoneNumber, true, carNumber, licenseNumber, carModel)

            // Сохранение данных водителя в Firebase
            database.child("drivers").child(userId).setValue(driverData)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Регистрация водителя успешна", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Log.e("RegisterActivity", "Ошибка сохранения данных: ${task.exception?.message}")
                        Toast.makeText(this, "Ошибка сохранения данных: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("RegisterActivity", "Ошибка сохранения данных: $exception")
                    Toast.makeText(this, "Ошибка сохранения данных: $exception", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.e("RegisterActivity", "Ошибка получения пользователя")
            Toast.makeText(this, "Ошибка получения пользователя", Toast.LENGTH_SHORT).show()
        }
    }
}