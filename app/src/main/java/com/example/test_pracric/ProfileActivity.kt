package com.example.test_pracric

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.test_pracric.doc.DocumentsActivity
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
    private lateinit var backButton: ImageView
    private lateinit var documentsTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        profileName = findViewById(R.id.profileName)
        phoneTextView = findViewById(R.id.phoneTextView)
        genderTextView = findViewById(R.id.genderTextView)
        dobTextView = findViewById(R.id.dobTextView)
        emailTextView = findViewById(R.id.emailTextView)
        backButton = findViewById(R.id.backButton)
        documentsTextView = findViewById(R.id.documentsTextView)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkUserRole(currentUser.uid)
        }

        backButton.setOnClickListener {
            navigateBack()
        }

        documentsTextView.setOnClickListener {
            val intent = Intent(this, DocumentsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkUserRole(userId: String) {
        database.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val isDriver = dataSnapshot.child("isDriver").getValue(Boolean::class.java) ?: false
                    Log.d("ProfileActivity", "isDriver: $isDriver") // Проверка значения

                    val name = dataSnapshot.child("name").getValue(String::class.java) ?: "Не указано"
                    profileName.text = name

                    if (isDriver) {
                        // Убедитесь, что вы получаете правильные данные для водителя
                        phoneTextView.text = dataSnapshot.child("phone").getValue(String::class.java) ?: "Не указано"
                        genderTextView.text = "Не указан"
                        dobTextView.text = "Не указано"

                        // Показать текст "Документы"
                        documentsTextView.visibility = View.VISIBLE
                        Log.d("ProfileActivity", "Documents should be visible for driver.")
                    } else {
                        // Для обычных пользователей
                        phoneTextView.text = dataSnapshot.child("phone").getValue(String::class.java) ?: "Не указано"
                        genderTextView.text = dataSnapshot.child("gender").getValue(String::class.java) ?: "Не указан"
                        dobTextView.text = dataSnapshot.child("dob").getValue(String::class.java) ?: "Не указано"

                        // Скрыть текст "Документы"
                        documentsTextView.visibility = View.GONE
                        Log.d("ProfileActivity", "Documents are hidden for regular user.")
                    }
                } else {
                    Log.d("ProfileActivity", "User data not found.")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("ProfileActivity", "Database error: ${databaseError.message}")
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
                        finish()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Обработка возможных ошибок
                }
            })
        }
    }
}