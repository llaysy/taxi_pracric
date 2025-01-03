package com.example.test_pracric

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.example.test_pracric.user.DriverData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DriverHomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var profileAvatar: ImageView
    private lateinit var profileName: TextView
    private lateinit var profilePhone: TextView
    private lateinit var logoutText: TextView
    private lateinit var hamburgerIcon: ImageView
    private lateinit var profileText: TextView
    private lateinit var btnWhereTo: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_home)

        auth = FirebaseAuth.getInstance()
        drawerLayout = findViewById(R.id.drawer_layout)
        profileAvatar = findViewById(R.id.profile_avatar)
        profileName = findViewById(R.id.profile_name)
        profilePhone = findViewById(R.id.profile_phone)
        logoutText = findViewById(R.id.logout_text)
        hamburgerIcon = findViewById(R.id.hamburger_icon)
        profileText = findViewById(R.id.profile_text)
        btnWhereTo = findViewById(R.id.btnWhereTo)

        val userId = auth.currentUser?.uid
        userId?.let {
            loadDriverData(it)
        }

        profileAvatar.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        logoutText.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        hamburgerIcon.setOnClickListener {
            openDrawer()
        }

        profileText.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            drawerLayout.close()
        }

        btnWhereTo.setOnClickListener {
            goOnline()
            val intent = Intent(this, DriverOrderActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadDriverData(userId: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("drivers").child(userId)

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val driverData = dataSnapshot.getValue(DriverData::class.java)
                    driverData?.let {
                        profileName.text = it.fullName
                        profilePhone.text = it.phoneNumber
                    }
                } else {
                    Toast.makeText(this@DriverHomeActivity, "Данные водителя не найдены", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@DriverHomeActivity, "Ошибка загрузки данных: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openDrawer() {
        drawerLayout.open()
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

    private fun goOnline() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val driverStatusRef = FirebaseDatabase.getInstance().getReference("drivers").child(userId).child("onlineStatus")
        driverStatusRef.setValue(true)
    }
}