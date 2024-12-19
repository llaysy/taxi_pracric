package com.example.test_pracric.doc

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.test_pracric.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RightsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var fullNameTextView: TextView
    private lateinit var issueDateTextView: TextView
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rights)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        fullNameTextView = findViewById(R.id.fullNameTextView)
        issueDateTextView = findViewById(R.id.issueDateTextView)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener {
            onBackPressed()
        }

        loadDriverRights()
    }

    private fun loadDriverRights() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            database.child("drivers").child(currentUser.uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Извлекаем полное имя
                        val fullName = dataSnapshot.child("fullName").getValue(String::class.java) ?: "Не указано"
                        val issueDate = dataSnapshot.child("issueDate").getValue(String::class.java) ?: "Не указано"

                        // Устанавливаем значения в TextView
                        fullNameTextView.text = fullName
                        issueDateTextView.text = issueDate
                    } else {
                        Log.d("RightsActivity", "Driver data not found for userId: ${currentUser.uid}")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("RightsActivity", "Database error: ${databaseError.message}")
                }
            })
        }
    }
}