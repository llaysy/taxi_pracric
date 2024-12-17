package com.example.test_pracric

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var profileName: TextView
    private lateinit var phoneTextView: TextView
    private lateinit var genderTextView: TextView
    private lateinit var dobTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var backButton: ImageView // Добавьте переменную для кнопки "Назад"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile) // Убедитесь, что это соответствует имени вашего макета

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        profileName = findViewById(R.id.profileName)
        phoneTextView = findViewById(R.id.phoneTextView)
        genderTextView = findViewById(R.id.genderTextView)
        dobTextView = findViewById(R.id.dobTextView)
        emailTextView = findViewById(R.id.emailTextView)
        backButton = findViewById(R.id.backButton) // Убедитесь, что у вас есть ID для кнопки "Назад"

        // Проверка наличия текущего пользователя
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Пользователь аутентифицирован
            checkUserRole(currentUser.uid)
        } else {
            // Обработка случая, когда пользователь не аутентифицирован
        }

        // Обработка клика на кнопку "Назад"
        backButton.setOnClickListener {
            navigateBack()
        }
    }

    private fun checkUserRole(userId: String) {
        database.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val isDriver = dataSnapshot.child("isDriver").getValue(Boolean::class.java) ?: false
                    val name = dataSnapshot.child("name").getValue(String::class.java) ?: "Не указано"
                    val email = dataSnapshot.child("email").getValue(String::class.java) ?: "Не указано"

                    profileName.text = name
                    emailTextView.text = email

                    if (!isDriver) {
                        // Для обычных пользователей
                        val phone = dataSnapshot.child("phone").getValue(String::class.java) ?: "Не указано"
                        val gender = dataSnapshot.child("gender").getValue(String::class.java) ?: "Не указан"
                        val dob = dataSnapshot.child("dob").getValue(String::class.java) ?: "Не указано"

                        phoneTextView.text = phone
                        genderTextView.text = gender
                        dobTextView.text = dob
                    } else {
                        // Для водителей
                        phoneTextView.text = dataSnapshot.child("phone").getValue(String::class.java) ?: "Не указано"
                        genderTextView.text = "Не указан"
                        dobTextView.text = "Не указано"
                    }
                } else {
                    // Обработка случая, когда данные пользователя не найдены
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Обработка возможных ошибок
            }
        })
    }

    private fun navigateBack() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            database.child("users").child(currentUser.uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val isDriver = dataSnapshot.child("isDriver").getValue(Boolean::class.java) ?: false
                        val intent = if (isDriver) {
                            Intent(this@ProfileActivity, DriverHomeActivity::class.java)
                        } else {
                            Intent(this@ProfileActivity, HomeActivity::class.java)
                        }
                        startActivity(intent)
                        finish() // Закрытие текущей активности
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Обработка возможных ошибок
                }
            })
        }
    }
}