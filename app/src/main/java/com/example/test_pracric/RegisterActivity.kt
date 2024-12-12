package com.example.test_pracric

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test_pracric.user.DriverData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText // Поле для подтверждения пароля
    private lateinit var etFullName: EditText
    private lateinit var etPTS: EditText // ПТС (в формате 00ТО000000)
    private lateinit var etSTS: EditText // СТС (в формате 00AA000000)
    private lateinit var etCarNumber: EditText // Номер машины (в формате A000AA00)
    private lateinit var etPhoneNumber: EditText // Номер телефона (11 цифр)
    private lateinit var etLicenseNumber: EditText // Права (в формате 0000000000)
    private lateinit var btnRegister: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Инициализация FirebaseAuth и DatabaseReference
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Инициализация пользовательского интерфейса
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword) // Инициализация поля подтверждения пароля
        etFullName = findViewById(R.id.etFullName)
        etPTS = findViewById(R.id.etPTS)
        etSTS = findViewById(R.id.etSTS)
        etCarNumber = findViewById(R.id.etCarNumber)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        etLicenseNumber = findViewById(R.id.etLicenseNumber)
        btnRegister = findViewById(R.id.btnRegister)

        // Установка обработчика событий
        btnRegister.setOnClickListener {
            registerDriver()
        }
    }

    private fun registerDriver() {
        val email = etEmail.text.toString()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()
        val fullName = etFullName.text.toString()
        val pts = etPTS.text.toString()
        val sts = etSTS.text.toString()
        val carNumber = etCarNumber.text.toString()
        val phoneNumber = etPhoneNumber.text.toString()
        val licenseNumber = etLicenseNumber.text.toString()

        // Проверка на пустые поля
        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank() ||
            fullName.isBlank() || pts.isBlank() || sts.isBlank() ||
            carNumber.isBlank() || phoneNumber.isBlank() || licenseNumber.isBlank()) {
            Toast.makeText(this, "Все поля должны быть заполнены", Toast.LENGTH_SHORT).show()
            return
        }

        // Валидация формата данных
        if (!isValidEmail(email)) {
            Toast.makeText(this, "Неверный формат электронной почты", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidPhoneNumber(phoneNumber)) {
            Toast.makeText(this, "Номер телефона должен состоять из 11 цифр", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidPTS(pts)) {
            Toast.makeText(this, "ПТС должен быть в формате 00ТО000000", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidSTS(sts)) {
            Toast.makeText(this, "СТС должен быть в формате 00AA000000", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidCarNumber(carNumber)) {
            Toast.makeText(this, "Номер машины должен быть в формате A000AA00", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidLicenseNumber(licenseNumber)) {
            Toast.makeText(this, "Права должны быть в формате 0000000000", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверка совпадения паролей
        if (password != confirmPassword) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверка существования email
        auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val signInMethods = task.result?.signInMethods
                if (signInMethods != null && signInMethods.isNotEmpty()) {
                    // Если учетная запись с таким email уже существует
                    Toast.makeText(this, "Электронная почта уже используется", Toast.LENGTH_SHORT).show()
                } else {
                    // Регистрация пользователя
                    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { regTask ->
                        if (regTask.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                val driverData = DriverData(
                                    email = email,
                                    fullName = fullName,
                                    pts = pts,
                                    sts = sts,
                                    carNumber = carNumber,
                                    phoneNumber = phoneNumber,
                                    licenseNumber = licenseNumber,
                                    isDriver = true, // Присваивание значения true для поля isDriver
                                    rating = 5.0f // Присвоение начального рейтинга
                                )
                                // Сохранение данных водителя в Firebase
                                database.child("drivers").child(userId).setValue(driverData)
                                    .addOnCompleteListener { dbTask ->
                                        if (dbTask.isSuccessful) {
                                            Toast.makeText(this, "Регистрация водителя успешна", Toast.LENGTH_SHORT).show()
                                            val intent = Intent(this, DriverHomeActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            Toast.makeText(this, "Ошибка сохранения данных: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                        } else {
                            //Toast.makeText(this, "Ошибка регистрации: ${regTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Ошибка проверки электронной почты: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.length == 11 && phone.all { it.isDigit() }
    }

    private fun isValidPTS(pts: String): Boolean {
        return pts.matches(Regex("^[0-9]{2}[А-Я]{2}[0-9]{6}$"))
    }

    private fun isValidSTS(sts: String): Boolean {
        return sts.matches(Regex("^[0-9]{2}[А-Я]{2}[0-9]{6}$"))
    }

    private fun isValidCarNumber(carNumber: String): Boolean {
        return carNumber.matches(Regex("^[А-Я]{1}[0-9]{3}[А-Я]{2}[0-9]{2}$"))
    }

    private fun isValidLicenseNumber(license: String): Boolean {
        return license.matches(Regex("^[0-9]{10}$"))
    }
}