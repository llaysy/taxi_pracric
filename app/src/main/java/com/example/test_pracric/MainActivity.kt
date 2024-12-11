package com.example.test_pracric

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test_pracric.user.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etConfPass: EditText
    private lateinit var etPass: EditText
    private lateinit var etName: EditText
    private lateinit var btnSignUp: Button
    private lateinit var btnRegisterAsDriver: Button // Новая кнопка для регистрации водителя
    private lateinit var tvRedirectLogin: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Проверка на наличие текущего пользователя
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Если пользователь уже аутентифицирован, сразу переходим в HomeActivity
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        // Инициализация пользовательского интерфейса
        etEmail = findViewById(R.id.etSEmailAddress)
        etConfPass = findViewById(R.id.etSConfPassword)
        etPass = findViewById(R.id.etSPassword)
        etName = findViewById(R.id.etSName)
        btnSignUp = findViewById(R.id.btnSSigned)
        btnRegisterAsDriver = findViewById(R.id.btnRegisterAsDriver) // Новая кнопка для регистрации водителя
        tvRedirectLogin = findViewById(R.id.tvRedirectLogin)

        // Инициализация DatabaseReference
        database = FirebaseDatabase.getInstance().reference

        // Установка обработчиков событий
        btnSignUp.setOnClickListener {
            signUpUser()
        }

        btnRegisterAsDriver.setOnClickListener { // Обработчик для новой кнопки
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        tvRedirectLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun signUpUser() {
        val email = etEmail.text.toString()
        val pass = etPass.text.toString()
        val confPass = etConfPass.text.toString()
        val name = etName.text.toString()

        // Проверка на пустые поля
        if (email.isBlank() || pass.isBlank() || confPass.isBlank() || name.isBlank()) {
            Toast.makeText(this, "Все поля должны быть заполнены", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверка на совпадение паролей
        if (pass != confPass) {
            Toast.makeText(this, "Пароль и пароль подтверждения не совпадают", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверка длины пароля и наличия хотя бы одной цифры
        if (!isPasswordValid(pass)) {
            Toast.makeText(this, "Пароль должен содержать минимум 8 символов и хотя бы одну цифру", Toast.LENGTH_SHORT).show()
            return
        }

        // Регистрация пользователя
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    // Создание объекта пользователя с рейтингом 5.00
                    val userData = UserData(name, email, 5.00f, false)

                    // Сохранение данных пользователя в Firebase
                    database.child("users").child(userId).setValue(userData)
                        .addOnCompleteListener { dbTask ->
                            if (dbTask.isSuccessful) {
                                Toast.makeText(this, "Регистрация успешна", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, HomeActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this, "Ошибка сохранения данных: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            } else {
                Toast.makeText(this, "Ошибка регистрации: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        // Проверка длины пароля
        if (password.length < 8) return false

        // Проверка наличия хотя бы одной цифры
        val hasDigit = password.any { it.isDigit() }
        return hasDigit
    }
}