package com.example.test_pracric

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.test_pracric.doc.DocumentsActivity
import com.example.test_pracric.user.DriverData
import com.example.test_pracric.user.UserData
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
            onBackButtonClicked()
        }

        documentsTextView.setOnClickListener {
            val intent = Intent(this, DocumentsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkUserRole(userId: String) {
        Log.d("ProfileActivity", "Checking user role for userId: $userId")

        // Сначала проверяем, является ли пользователь водителем
        database.child("drivers").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Пользователь найден в группе водителей
                    val driverData = dataSnapshot.getValue(DriverData::class.java)
                    driverData?.let {
                        profileName.text = it.fullName // Измените на правильное поле
                        emailTextView.text = it.email
                        phoneTextView.text = it.phoneNumber
                        genderTextView.text = "Не указан"
                        dobTextView.text = "Не указано"
                        documentsTextView.visibility = View.VISIBLE
                    }
                } else {
                    // Если не найден в группе водителей, проверяем группу пользователей
                    checkPassengerUser(userId)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("ProfileActivity", "Database error: ${databaseError.message}")
            }
        })
    }

    private fun checkPassengerUser(userId: String) {
        database.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Пользователь найден в группе пассажиров
                    val userData = dataSnapshot.getValue(UserData::class.java)
                    userData?.let {
                        profileName.text = it.name
                        emailTextView.text = it.email
                        phoneTextView.text = "Не указано" // Номер телефона для пассажиров не указан
                        genderTextView.text = it.gender ?: "Не указан"
                        dobTextView.text = it.dob ?: "Не указано"
                        documentsTextView.visibility = View.GONE
                    }
                } else {
                    Log.d("ProfileActivity", "User data not found for userId: $userId")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("ProfileActivity", "Database error: ${databaseError.message}")
            }
        })
    }

    private fun onBackButtonClicked() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Проверяем сначала группу водителей
            database.child("drivers").child(currentUser.uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Если пользователь водитель, переходим в DriverHomeActivity
                        val intent = Intent(this@ProfileActivity, DriverHomeActivity::class.java)
                        startActivity(intent)
                    } else {
                        // Если не водитель, проверяем группу пользователей
                        checkPassengerUserOnBack(currentUser.uid)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("ProfileActivity", "Database error: ${databaseError.message}")
                }
            })
        }
    }

    private fun checkPassengerUserOnBack(userId: String) {
        database.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Если пользователь пассажир, переходим в HomeActivity
                    val intent = Intent(this@ProfileActivity, HomeActivity::class.java)
                    startActivity(intent)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("ProfileActivity", "Database error: ${databaseError.message}")
            }
        })
    }

}