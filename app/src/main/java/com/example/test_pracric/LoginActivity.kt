package com.example.test_pracric

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
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

        tvRedirectSignUp = findViewById(R.id.tvRedirectSignUp)
        btnLogin = findViewById(R.id.btnLogin)
        etEmail = findViewById(R.id.etEmailAddress)
        etPass = findViewById(R.id.etPassword)

        auth = FirebaseAuth.getInstance()

        btnLogin.setOnClickListener {
            login()
        }

        tvRedirectSignUp.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }



    private fun login() {
        val email = etEmail.text.toString()
        val pass = etPass.text.toString()

        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Вход успешен!", Toast.LENGTH_SHORT).show()

                // Получаем текущего пользователя
                val user = auth.currentUser
                user?.let {
                    val userId = it.uid
                    val databaseReference = FirebaseDatabase.getInstance().getReference("drivers").child(userId)

                    // Получаем данные о пользователе
                    databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            // Проверяем, существует ли пользователь и его статус
                            if (dataSnapshot.exists()) {
                                val driver = dataSnapshot.child("isDriver").getValue(Boolean::class.java) ?: false
                                val intent = if (driver) {
                                    Intent(this@LoginActivity, DriverHomeActivity::class.java)
                                } else {
                                    Intent(this@LoginActivity, HomeActivity::class.java)
                                }
                                startActivity(intent)
                            } else {
                                // Если пользователь не найден, перенаправляем в HomeActivity
                                startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                            }
                            finish() // Закрываем текущую активность
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Toast.makeText(this@LoginActivity, "Ошибка получения данных: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            } else {
                Toast.makeText(this, "Ошибка входа", Toast.LENGTH_SHORT).show()
            }
        }
    }
}