package com.example.test_pracric

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    private lateinit var tvRedirectSignUp: TextView
    private lateinit var etEmail: EditText
    private lateinit var etPass: EditText
    private lateinit var btnLogin: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Log.d("LoginActivity", "LoginActivity started")

        tvRedirectSignUp = findViewById(R.id.tvRedirectSignUp)
        btnLogin = findViewById(R.id.btnLogin)
        etEmail = findViewById(R.id.etEmailAddress)
        etPass = findViewById(R.id.etPassword)

        auth = FirebaseAuth.getInstance()

        // Проверка на наличие текущего пользователя
        if (auth.currentUser != null) {
            // Если пользователь уже зашел, перенаправляем на соответствующий экран
            redirectUser(auth.currentUser?.uid)
        }

        btnLogin.setOnClickListener {
            login()
        }

        tvRedirectSignUp.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun login() {
        val email = etEmail.text.toString().trim()
        val pass = etPass.text.toString().trim()

        // Проверка на пустые поля
        if (email.isBlank() || pass.isBlank()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Вход успешен!", Toast.LENGTH_SHORT).show()
                val user = auth.currentUser
                user?.let {
                    redirectUser(it.uid)
                }
                etEmail.text.clear()
                etPass.text.clear()
            } else {
                // Обработка ошибок входа
                val errorMessage = when (task.exception) {
                    is FirebaseAuthInvalidUserException -> "Пользователь не найден. Пожалуйста, зарегистрируйтесь."
                    is FirebaseAuthInvalidCredentialsException -> "Неверный пароль. Попробуйте снова."
                    else -> "Ошибка входа: ${task.exception?.message}"
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun redirectUser(userId: String?) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("drivers").child(userId!!)

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.d("LoginActivity", "Data snapshot exists: ${dataSnapshot.exists()}") // Логируем, существует ли снимок данных

                if (dataSnapshot.exists()) {
                    val isDriver = dataSnapshot.child("driver").getValue(Boolean::class.java) ?: false
                    Log.d("LoginActivity", "User isDriver: $isDriver") // Логируем значение isDriver

                    // Перенаправляем пользователя на соответствующую активность
                    val intent = if (isDriver) {
                        Intent(this@LoginActivity, DriverHomeActivity::class.java)
                    } else {
                        Intent(this@LoginActivity, HomeActivity::class.java)
                    }
                    startActivity(intent)
                    finish() // Закрываем текущую активность
                } else {
                    Log.d("LoginActivity", "User not found in drivers, redirecting to HomeActivity")
                    startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                    finish() // Закрываем текущую активность
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("LoginActivity", "Database error: ${databaseError.message}")
                Toast.makeText(this@LoginActivity, "Ошибка получения данных: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}